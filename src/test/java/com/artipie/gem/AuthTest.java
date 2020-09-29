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

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.jruby.javasupport.JavaEmbedUtils;
import org.junit.jupiter.api.Test;

/**
 * A test for api key endpoint.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ReturnCountCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class AuthTest {

    @Test
    public void keyIsReturned() {
        final String token = "aGVsbG86d29ybGQ=";
        final Headers headers = new Headers.From(
            new Authorization(String.format("Basic %s", token))
        );
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                JavaEmbedUtils.initialize(new ArrayList<>(0)),
                Permissions.FREE,
                (login, pwd) -> Optional.of(new Authentication.User("anonymous")),
                "keyIsReturned"
            ).response(
                new RequestLine("GET", "/api/v1/api_key").toString(),
                headers,
                Flowable.empty()
            ), new RsHasBody(token.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    public void unauthorizedWhenNoIdentity() {
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                JavaEmbedUtils.initialize(new ArrayList<>(0)),
                Permissions.FREE,
                (lgn, pwd) -> Optional.empty(),
                "unauthorizedWhenNoIdentity"
            ).response(
                new RequestLine("GET", "/api/v1/api_key").toString(),
                new Headers.From(),
                Flowable.empty()
            ), new RsHasStatus(RsStatus.UNAUTHORIZED)
        );
    }

    @Test
    public void notAllowedToPushUsersAreRejected() throws IOException {
        final String lgn = "usr";
        final String pwd = "pwd";
        final String token = new Base64Encoded(String.format("%s:%s", lgn, pwd)).asString();
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                JavaEmbedUtils.initialize(new ArrayList<>(0)),
                (name, action) -> !name.name().equals(lgn),
                (username, password) -> {
                    if (username.equals(lgn) && password.equals(pwd)) {
                        return Optional.of(new Authentication.User(lgn));
                    } else {
                        return Optional.empty();
                    }
                },
                "notAllowedToPushUsersAreRejected"
            ).response(
                new RequestLine("POST", "/api/v1/gems").toString(),
                new Headers.From(new Authorization(token)),
                Flowable.empty()
            ), new RsHasStatus(RsStatus.FORBIDDEN)
        );
    }

    @Test
    public void notAllowedToInstallsUsersAreRejected() throws IOException {
        final String lgn = "usr";
        final String pwd = "pwd";
        final String token = new Base64Encoded(String.format("%s:%s", lgn, pwd)).asString();
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                JavaEmbedUtils.initialize(new ArrayList<>(0)),
                (name, action) -> !name.name().equals(lgn),
                (username, password) -> {
                    if (username.equals(lgn) && password.equals(pwd)) {
                        return Optional.of(new Authentication.User(lgn));
                    } else {
                        return Optional.empty();
                    }
                },
                "notAllowedToInstallsUsersAreRejected"
            ).response(
                new RequestLine("GET", "specs.4.8").toString(),
                new Headers.From(new Authorization(token)),
                Flowable.empty()
            ), new RsHasStatus(RsStatus.FORBIDDEN)
        );
    }
}
