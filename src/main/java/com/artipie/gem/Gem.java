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
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.fs.FileStorage;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.apache.commons.io.FileUtils;

/**
 * An SDK, which servers gem packages.
 * <p>
 * Performes gem index update using specified indexer implementation.
 * </p>
 * @since 1.0
 */
public final class Gem {

    /**
     * Gem indexer shared instance cache.
     */
    private final AtomicReference<GemIndex> cache;

    /**
     * Gem repository storage.
     */
    private final Storage storage;

    /**
     * Gem indexer supplier.
     */
    private final Supplier<GemIndex> indexer;

    /**
     * New Gem SDK with default indexer.
     * @param storage Repository storage.
     */
    Gem(final Storage storage) {
        this(storage, () -> RubyGemIndex.createNew());
    }

    /**
     * New Gem SDK.
     *
     * @param storage Repository storage.
     * @param indexer Gem indexer supplier
     */
    Gem(final Storage storage, final Supplier<GemIndex> indexer) {
        this.storage = storage;
        this.indexer = indexer;
        this.cache = new AtomicReference<>();
    }

    /**
     * Batch update Ruby gems for repository.
     *
     * @param prefix Location of repository
     * @return Completable action
     */
    public CompletionStage<Void> batchUpdate(final Key prefix) {
        final Storage remote = new SubStorage(prefix, this.storage);
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    return Files.createTempDirectory("gem");
                } catch (final IOException exc) {
                    throw new UncheckedIOException(exc);
                }
            }
        ).thenCompose(
            tmpdir -> Gem.copyStorage(remote, new FileStorage(tmpdir))
                .thenApply(ignore -> tmpdir)
        ).thenCompose(
            tmpdir -> this.sharedIndexer()
                .thenAccept(idx -> idx.update(tmpdir))
                .thenApply(ignore -> tmpdir)
        ).thenCompose(
            tmpdir -> Gem.copyStorage(new FileStorage(tmpdir), remote)
                .thenApply(ignore -> tmpdir)
        ).handle(Gem::removeTempDir);
    }

    /**
     * Handle async result.
     * @param tmpdir Path directory to remove
     * @param err Error
     * @return Nothing
     */
    private static Void removeTempDir(final Path tmpdir, final Throwable err) {
        try {
            FileUtils.deleteDirectory(new File(tmpdir.toString()));
        } catch (final IOException exc) {
            throw new UncheckedIOException(exc);
        }
        if (err != null) {
            throw new CompletionException(err);
        }
        return null;
    }

    /**
     * Copy storage from src to dst.
     * @param src Source storage
     * @param dst Destination storage
     * @return Async result
     */
    private static CompletionStage<Void> copyStorage(final Storage src, final Storage dst) {
        return Single.fromFuture(src.list(Key.ROOT))
            .flatMapObservable(Observable::fromIterable)
            .flatMapSingle(
                key -> Single.fromFuture(
                    src.value(key)
                        .thenCompose(content -> dst.save(key, content))
                        .thenApply(none -> true)
                )
            ).ignoreElements().to(CompletableInterop.await());
    }

    /**
     * Get shared ruby indexer instance.
     * @return Async result with gem index
     * @checkstyle ReturnCountCheck (15 lines)
     */
    private CompletionStage<GemIndex> sharedIndexer() {
        return CompletableFuture.supplyAsync(
            () -> this.cache.updateAndGet(
                value -> {
                    if (value == null) {
                        return new GemIndex.Synchronized(this.indexer.get());
                    }
                    return value;
                }
            )
        );
    }
}
