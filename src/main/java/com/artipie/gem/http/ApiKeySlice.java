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
package com.artipie.gem.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Responses on api key requests.
 *
 * @since 0.3
 */
public final class ApiKeySlice implements Slice {

    /**
     * The users.
     */
    private final Authentication auth;

    /**
     * The Ctor.
     * @param auth Auth.
     */
    public ApiKeySlice(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new BasicAuthScheme(this.auth)
                .authenticate(headers)
                .thenApply(AuthScheme.Result::user)
                .thenApply(
                    usr -> {
                        final Response response;
                        if (usr.isPresent()) {
                            final String key = new RqHeaders(headers, Authorization.NAME).stream()
                                .findFirst()
                                .filter(hdr -> hdr.startsWith(BasicAuthScheme.NAME))
                                .map(hdr -> hdr.substring(BasicAuthScheme.NAME.length() + 1))
                                .get();
                            response = new RsWithBody(key, StandardCharsets.UTF_8);
                        } else {
                            response = new RsWithStatus(RsStatus.UNAUTHORIZED);
                        }
                        return response;
                    }
                )
        );
    }

}
