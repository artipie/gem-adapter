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

import com.artipie.asto.fs.FileStorage;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rs.RsStatus;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.jruby.javasupport.JavaEmbedUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A test for gem submit operation.
 *
 * @since 0.2
 */
public class SubmitGemITCase {

    @Test
    public void submitResultsInOkResponse(@TempDir final Path temp) throws IOException {
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new GemSlice(
                new FileStorage(temp),
                JavaEmbedUtils.initialize(new ArrayList<>(0)),
                Permissions.FREE,
                (lgn, pwd) -> Optional.empty(),
                "submitResultsInOkResponse"
            )
        );
        final WebClient web = WebClient.create(vertx);
        final int port = server.start();
        final byte[] gem = Files.readAllBytes(
            Paths.get("./src/test/resources/builder-3.2.4.gem")
        );
        final int code = web.post(port, "localhost", "/api/v1/gems")
            .rxSendBuffer(Buffer.buffer(gem))
            .blockingGet()
            .statusCode();
        MatcherAssert.assertThat(
            code,
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        web.close();
        server.close();
        vertx.close();
    }
}
