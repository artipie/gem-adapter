/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.gem;

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.auth.SliceAuth;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * A slice, which servers gem packages.
 *
 * @todo #13:30min Initialize on first request.
 *  Currently, Ruby runtime initialization and Slice evaluation is happening during the GemSlice
 *  construction. Instead, the Ruby runtime initialization and Slice evaluation should happen
 *  on first request.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 * @since 0.1
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GemSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param storage The storage.
     */
    public GemSlice(final Storage storage) {
        this(storage,
            JavaEmbedUtils.initialize(new ArrayList<>(0)),
            Permissions.FREE,
            (login, pwd) -> Optional.of(new Authentication.User("anonymous")),
            "default"
        );
    }

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param runtime The Jruby runtime.
     * @param permissions The permissions.
     * @param auth The auth.
     * @param repo The repo identifier.
     */
    public GemSlice(final Storage storage,
        final Ruby runtime,
        final Permissions permissions,
        final Authentication auth,
        final String repo) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.POST),
                        new RtRule.ByPath("/api/v1/gems")
                    ),
                    new SliceAuth(
                        GemSlice.rubyLookUp(
                            runtime,
                            "SubmitGem",
                            storage,
                            GemSlice.tmpDirForRepo(repo)
                        ),
                        new Permission.ByName(permissions, Action.Standard.WRITE),
                        new GemApiKeyIdentities(auth)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath("/api/v1/api_key")
                    ),
                    new ApiKeySlice(auth)
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath(GemInfo.PATH_PATTERN)
                    ),
                    new GemInfo(storage)
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.GET),
                    new SliceAuth(
                        new SliceDownload(storage),
                        new Permission.ByName(permissions, Action.Standard.READ),
                        new GemApiKeyIdentities(auth)
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(new RsWithStatus(RsStatus.NOT_FOUND))
                )
            )
        );
    }

    /**
     * Lookup an instance of slice, implemented with JRuby.
     * @param runtime The JRuby runtime.
     * @param rclass The name of a slice class, implemented in JRuby.
     * @param params The ctor params.
     * @return The Slice.
     */
    private static Slice rubyLookUp(
        final Ruby runtime,
        final String rclass,
        final Object... params) {
        try {
            final RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
            final String script = IOUtils.toString(
                GemSlice.class.getResourceAsStream(String.format("/%s.rb", rclass)),
                StandardCharsets.UTF_8
            );
            evaler.eval(runtime, script);
            return (Slice) JavaEmbedUtils.invokeMethod(
                runtime,
                evaler.eval(runtime, rclass),
                "new",
                params,
                Slice.class
            );
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }

    /**
     * Temp dir, which will be removed on jvm exit.
     * @param repo The repo identifier.
     * @return The dir name.
     */
    private static String tmpDirForRepo(final String repo) {
        final String fname = String.join(
            "-",
            repo,
            "gem-tmp-index"
        );
        Runtime.getRuntime().addShutdownHook(
            new Thread(
                () -> {
                    try {
                        FileUtils.deleteDirectory(new File(fname));
                    } catch (final IOException exc) {
                        throw new UncheckedIOException(exc);
                    }
                }
            )
        );
        return fname;
    }
}
