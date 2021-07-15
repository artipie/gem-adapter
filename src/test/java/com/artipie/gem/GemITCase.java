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

import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * A test which ensures {@code gem} console tool compatibility with the adapter.
 *
 * @since 1.0
 */
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public class GemITCase {

    @Test
    public void testGem(final @TempDir Path tmp) throws IOException {
        final Path repo = Paths.get(tmp.toString(), "Artipie");
        final String gemstr = "gems";
        final Path path = repo.resolve(gemstr);
        try {
            Files.createDirectories(path);
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
        final String builderstr = "builder-3.2.4.gem";
        final Path target = path.resolve(builderstr);
        try (InputStream is = this.getClass().getResourceAsStream("/builder-3.2.4.gem");
            OutputStream os = Files.newOutputStream(target)) {
            IOUtils.copy(is, os);
        }
        final Gem gem = new Gem(new FileStorage(repo));
        gem.update(new Key.From(gemstr, builderstr)).toCompletableFuture().join();
        final List<String> files = Files.walk(repo).map(Path::toString)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            files,
            Matchers.hasItem("".concat(repo.toString())
                .concat("/prerelease_specs.4.8.gz")
            )
        );
        MatcherAssert.assertThat(
            files,
            Matchers.hasItem("".concat(repo.toString())
                .concat("/quick/Marshal.4.8/builder-3.2.4.gemspec.rz")
            )
        );
        MatcherAssert.assertThat(
            files, Matchers.hasItem("".concat(repo.toString())
                .concat("/latest_specs.4.8")
            )
        );
        MatcherAssert.assertThat(
            files,
            Matchers.hasItem("".concat(repo.toString())
                .concat("/gems/builder-3.2.4.gem")
            )
        );
        MatcherAssert.assertThat(
            files, Matchers.hasItem(
                "".concat(repo.toString()).concat("/specs.4.8.gz")
            )
        );
        MatcherAssert.assertThat(
            files, Matchers.hasItem("".concat(repo.toString())
                .concat("/latest_specs.4.8.gz")
            )
        );
    }
}
