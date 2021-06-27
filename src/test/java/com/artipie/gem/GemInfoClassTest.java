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
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * A test for gem submit operation.
 *
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class GemInfoClassTest {
    /**
     * Path to upload gem file.
     */
    static final String GVIZ_STR = "gviz-0.3.5.gem";

    /**
     * Path to upload gem file.
     */
    static final String THOR_STR = "thor-0.19.1.gem";

    @Test
    public void queryResultsInOkResponse(@TempDir final Path tmp) throws IOException {
        final Path repo = Paths.get(tmp.toString());
        final Path target = repo.resolve(GemInfoClassTest.GVIZ_STR);
        try (InputStream is = this.getClass()
            .getResourceAsStream("/".concat(GemInfoClassTest.GVIZ_STR));
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        final Storage storage = new FileStorage(tmp);
        MatcherAssert.assertThat(
            new GemInfoClass(storage, new Gem(storage)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(
                        new IsJson(
                            new JsonHas(
                                "homepage",
                                new JsonValueIs("https://github.com/melborne/Gviz")
                            )
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/api/v1/gems/gviz.json"),
                Headers.EMPTY,
                com.artipie.asto.Content.EMPTY
            )
        );
    }

    @Test
    public void queryResultsInOk(@TempDir final Path tmp) throws IOException {
        final Charset encoding = Charset.forName("ISO-8859-1");
        final Path repo = Paths.get(tmp.toString());
        final Path target = repo.resolve(GemInfoClassTest.GVIZ_STR);
        final Path ttarget = repo.resolve(GemInfoClassTest.THOR_STR);
        try (InputStream is = this.getClass()
            .getResourceAsStream("/".concat(GemInfoClassTest.GVIZ_STR));
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        try (InputStream is = this.getClass()
            .getResourceAsStream("/".concat(GemInfoClassTest.THOR_STR));
            OutputStream os = Files.newOutputStream(ttarget)) {
            IOUtils.copy(is, os);
        }
        final InputStream data = this.getClass().getResourceAsStream("/test/dependencies.data");
        final StringWriter writer = new StringWriter();
        IOUtils.copy(data, writer, encoding);
        final String thestring = writer.toString();
        final Storage storage = new FileStorage(tmp);
        MatcherAssert.assertThat(
            new GemInfoClass(storage, new Gem(storage)),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(
                        thestring,
                        encoding
                    )
                ),
                new RequestLine(RqMethod.GET, "/api/v1/dependencies?gems=gviz,thor"),
                Headers.EMPTY,
                com.artipie.asto.Content.EMPTY
            )
        );
    }
}

