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

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Identities;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Responses on api key requests.
 *
 * @since 0.3
 */
public final class ApiKeySlice implements Slice {

    /**
     * Basic authentication prefix.
     */
    private static final String PREFIX = "Basic ";

    /**
     * The users.
     */
    private final Identities users;

    /**
     * The Ctor.
     * @param users The users.
     */
    public ApiKeySlice(final Identities users) {
        this.users = users;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Response response;
        final Optional<String> user = this.users.user(line, headers);
        if (user.isPresent()) {
            final String key = new RqHeaders(headers, Authorization.NAME).stream()
                .findFirst()
                .filter(hdr -> hdr.startsWith(ApiKeySlice.PREFIX))
                .map(hdr -> hdr.substring(ApiKeySlice.PREFIX.length()))
                .get();
            response = new RsWithBody(key, StandardCharsets.UTF_8);
        } else {
            response = new RsWithStatus(RsStatus.UNAUTHORIZED);
        }
        return response;
    }
}
