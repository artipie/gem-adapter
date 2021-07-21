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

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.gem.ruby.RubyGemIndex;
import com.artipie.gem.ruby.RubyGemMeta;
import com.artipie.gem.ruby.SharedRuntime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Performes gem index update using specified indexer implementation.
 * </p>
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class Gem {

    /**
     * Read only set of metadata item names.
     */
    private static final Set<Key> META_NAMES = Collections.unmodifiableSet(
        Stream.of(
            "latest_specs.4.8", "latest_specs.4.8.gz", "prerelease_specs.4.8",
            "prerelease_specs.4.8.gz", "specs.4.8", "specs.4.8.gz"
        ).map(Key.From::new).collect(Collectors.toSet())
    );

    /**
     * Gem repository storage.
     */
    private final Storage storage;

    /**
     * Shared ruby runtime.
     */
    private final SharedRuntime shared;

    /**
     * New Gem SDK with default indexer.
     * @param storage Repository storage.
     */
    public Gem(final Storage storage) {
        this.storage = storage;
        this.shared = new SharedRuntime();
    }

    /**
     * Batch update Ruby gems for repository.
     *
     * @param gem Ruby gem for indexing
     * @return Completable action
     */
    public CompletionStage<Void> update(final Key gem) {
        return newTempDir().thenCompose(
            tmp -> new Copy(this.storage, key -> META_NAMES.contains(key) || key.equals(gem))
                .copy(new FileStorage(tmp))
                .thenApply(ignore -> tmp)
        ).thenCompose(
            tmp -> this.shared.apply(RubyGemIndex::new)
                .thenAccept(
                    index -> {
                        final Path dir = Paths.get(tmp.toString(), gem.string());
                        index.rename(dir);
                        index.update(tmp);
                    })
                .thenCompose(none -> new Copy(new FileStorage(tmp)).copy(this.storage))
                .handle(removeTempDir(tmp))
        );
    }

    /**
     * Gem info data.
     * @param gem Gem name
     * @param fmt Info format
     * @param <T> Format type
     * @return Future
     */
    public <T> CompletionStage<T> info(final String gem, final GemMeta.InfoFormat<T> fmt) {
        return newTempDir().thenCompose(
            tmp -> new Copy(this.storage, new IsGemKey(gem))
                .copy(new FileStorage(tmp))
                .thenApply(ignore -> tmp)
        ).thenCompose(
            tmp -> this.shared.apply(RubyGemMeta::new)
                .thenCompose(
                    info -> new FileStorage(tmp).list(Key.ROOT).thenApply(
                        items -> items.stream().findFirst()
                            .map(first -> Paths.get(tmp.toString(), first.string()))
                            .map(path -> info.info(path, fmt))
                            .orElseThrow(() -> new ArtipieIOException("gem not found"))
                    )
                ).handle(removeTempDir(tmp))
        );
    }

    /**
     * Create new temp dir asynchronously.
     * @return Future
     */
    private static CompletionStage<Path> newTempDir() {
        return CompletableFuture.supplyAsync(
            new UncheckedSupplier<>(
                () -> Files.createTempDirectory(Gem.class.getSimpleName())
            )
        );
    }

    /**
     * Handle async result.
     * @param tmpdir Path directory to remove
     * @param <T> Result type
     * @return Function handler
     */
    private static <T> BiFunction<T, Throwable, T> removeTempDir(
        final Path tmpdir) {
        return (res, err) -> {
            try {
                if (tmpdir != null) {
                    FileUtils.deleteDirectory(new File(tmpdir.toString()));
                }
            } catch (final IOException iox) {
                throw new ArtipieIOException(iox);
            }
            if (err != null) {
                throw new CompletionException(err);
            }
            return res;
        };
    }

    /**
     * Predicate to find gem key by name.
     * @since 1.0
     */
    private static final class IsGemKey implements Predicate<Key>  {

        /**
         * Gem name.
         */
        private final String name;

        /**
         * New predicate.
         * @param name Gem name
         */
        IsGemKey(final String name) {
            this.name = name;
        }

        @Override
        public boolean test(final Key key) {
            final String str = key.string();
            final int idx = str.lastIndexOf(this.name);
            boolean matches = false;
            if (idx >= 0) {
                final String tail = str.substring(idx + this.name.length());
                if (tail.isEmpty() || tail.matches("^[0-9a-zA-Z\\-\\.]+$")) {
                    matches = true;
                }
            }
            return matches;
        }
    }
}
