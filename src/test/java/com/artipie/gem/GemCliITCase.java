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
import com.artipie.gem.http.GemSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * A test which ensures {@code gem} console tool compatibility with the adapter.
 *
 * @since 0.2
 * @checkstyle StringLiteralsConcatenationCheck (500 lines)
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
@SuppressWarnings("PMD.SystemPrintln")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public class GemCliITCase {

    @Test
    public void gemPushAndInstallWorks(@TempDir final Path temp, @TempDir final Path mount)
        throws IOException, InterruptedException {
        final String key = new Base64Encoded("usr:pwd").asString();
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new GemSlice(new FileStorage(temp))
        );
        final int port = server.start();
        final String host = String.format("http://host.testcontainers.internal:%d", port);
        Testcontainers.exposeHostPorts(port);
        final RubyContainer ruby = new RubyContainer()
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(mount.toAbsolutePath().toString(), "/home");
        ruby.start();
        final Set<String> gems = new HashSet<>();
        gems.add("builder-3.2.4.gem");
        gems.add("rails-6.0.2.2.gem");
        for (final String gem : gems) {
            final Path target = mount.resolve(gem);
            try (InputStream is = this.getClass().getResourceAsStream("/".concat(gem));
                OutputStream os = Files.newOutputStream(target)) {
                IOUtils.copy(is, os);
            }
            MatcherAssert.assertThat(
                String.format("'gem push %s failed with non-zero code", host, gem),
                this.bash(
                    ruby,
                    String.format("GEM_HOST_API_KEY=%s gem push %s --host %s", key, gem, host)
                ),
                Matchers.equalTo(0)
            );
            Files.delete(target);
            MatcherAssert.assertThat(
                String.format("'gem fetch %s failed with non-zero code", host, gem),
                this.bash(
                    ruby,
                    String.format(
                        "GEM_HOST_API_KEY=%s gem fetch -V %s --source %s",
                        key, gem.substring(0, gem.indexOf('-')), host
                    )
                ),
                Matchers.equalTo(0)
            );
        }
        MatcherAssert.assertThat(
            String.format("Unable to remove https://rubygems.org from the list of sources", host),
            this.bash(
                ruby,
                String.format("gem sources -r https://rubygems.org/", host)
            ),
            Matchers.equalTo(0)
        );
        ruby.stop();
        ruby.close();
        server.close();
        vertx.close();
    }

    /**
     * Executes a bash command in a ruby container.
     * @param ruby The ruby container.
     * @param command Bash command to execute.
     * @return Exit code.
     * @throws IOException If fails.
     * @throws InterruptedException If fails.
     */
    private int bash(final RubyContainer ruby, final String command)
        throws IOException, InterruptedException {
        final Container.ExecResult exec = ruby.execInContainer(
            "/bin/bash",
            "-c",
            command
        );
        Logger.info(GemCliITCase.class, exec.getStdout());
        Logger.error(GemCliITCase.class, exec.getStderr());
        if (!exec.getStderr().equals("")) {
            throw new IllegalStateException(exec.getStderr());
        }
        return exec.getExitCode();
    }

    /**
     * Inner subclass to instantiate Ruby container.
     *
     * @since 0.1
     */
    private static class RubyContainer extends GenericContainer<RubyContainer> {
        RubyContainer() {
            super("ruby:2.7");
        }
    }
}
