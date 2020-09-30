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
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jruby.javasupport.JavaEmbedUtils;
import org.junit.jupiter.api.Assertions;
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
@SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.AvoidDuplicateLiterals"})
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public class GemCliITCase {

    @Test
    public void gemPushAndInstallWorks(@TempDir final Path temp)
        throws IOException, InterruptedException {
        final Path tempf = Files.createDirectories(temp.resolve("tempf"));
        final Path temps = Files.createDirectories(temp.resolve("temps"));
        final Path mount = Files.createDirectories(temp.resolve("mount"));
        final String key = new Base64Encoded("usr:pwd").asString();
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer first = new VertxSliceServer(
            vertx,
            new GemSlice(
                new FileStorage(tempf),
                JavaEmbedUtils.initialize(new ArrayList<>(0)),
                Permissions.FREE,
                (login, pwd) -> Optional.of(new Authentication.User("anonymous")),
                "first"
            )
        );
        final VertxSliceServer second = new VertxSliceServer(
            vertx,
            new GemSlice(
                new FileStorage(temps),
                JavaEmbedUtils.initialize(new ArrayList<>(0)),
                Permissions.FREE,
                (login, pwd) -> Optional.of(new Authentication.User("anonymous")),
                "second"
            )
        );
        final int fport = first.start();
        final int sport = second.start();
        final String hostf = String.format("http://host.testcontainers.internal:%d", fport);
        final String hosts = String.format("http://host.testcontainers.internal:%d", sport);
        Testcontainers.exposeHostPorts(fport);
        Testcontainers.exposeHostPorts(sport);
        final RubyContainer ruby = new RubyContainer()
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(mount.toAbsolutePath().toString(), "/home");
        final Path bgem = mount.resolve("builder-3.2.4.gem");
        final Path rgem = mount.resolve("rails-6.0.2.2.gem");
        Files.copy(Paths.get("./src/test/resources/builder-3.2.4.gem"), bgem);
        Files.copy(Paths.get("./src/test/resources/rails-6.0.2.2.gem"), rgem);
        ruby.start();
        MatcherAssert.assertThat(
            String.format("'gem push builder-3.2.4.gem failed with non-zero code", hostf),
            this.bash(
                ruby,
                String.format(
                    "GEM_HOST_API_KEY=%s gem push builder-3.2.4.gem --host %s",
                    key,
                    hostf
                )
            ),
            Matchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            String.format("'gem push rails-6.0.2.2.gem failed with non-zero code", hostf),
            this.bash(
                ruby,
                String.format(
                    "GEM_HOST_API_KEY=%s gem push rails-6.0.2.2.gem --host %s",
                    key,
                    hostf
                )
            ),
            Matchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            String.format("'gem push rails-6.0.2.2.gem failed with non-zero code", hostf),
            this.bash(
                ruby,
                String.format(
                    "GEM_HOST_API_KEY=%s gem push rails-6.0.2.2.gem --host %s",
                    key,
                    hosts
                )
            ),
            Matchers.equalTo(0)
        );
        Files.delete(bgem);
        Files.delete(rgem);
        MatcherAssert.assertThat(
            String.format("Unable to remove https://rubygems.org from the list of sources", hostf),
            this.bash(
                ruby,
                String.format("gem sources -r https://rubygems.org/", hostf)
            ),
            Matchers.equalTo(0)
        );
        MatcherAssert.assertThat(
            String.format("'gem fetch failed with non-zero code", hostf),
            this.bash(
                ruby,
                String.format("GEM_HOST_API_KEY=%s gem fetch -V builder --source %s", key, hostf)
            ),
            Matchers.equalTo(0)
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.bash(
                ruby,
                String.format("GEM_HOST_API_KEY=%s gem fetch -V builder --source %s", key, hosts)
            ),
            String.format(
                "'gem fetch should fail, since builder was not pushed to the second repo",
                hostf
            )
        );
        ruby.stop();
        first.close();
        second.close();
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
            throw new IllegalStateException("An error occurred");
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
