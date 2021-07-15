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

package com.artipie.gem.ruby;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Share ruby runtime and interpreter.
 * @since 1.0
 */
public final class SharedRuntime {

    /**
     * Ruby runtime factory.
     */
    private final Supplier<Ruby> factory;

    /**
     * Synchronization lock.
     */
    private final Object lock;

    /**
     * Runtime cache.
     */
    private volatile Ruby runtime;

    /**
     * New default shared ruby runtime.
     */
    public SharedRuntime() {
        this(
            () -> JavaEmbedUtils.initialize(Collections.emptyList())
        );
    }

    /**
     * New shared ruby runtime with specified factory.
     * @param factory Runtime factory
     */
    public SharedRuntime(final Supplier<Ruby> factory) {
        this.factory = factory;
        this.lock = new Object();
    }

    /**
     * Apply shared runtime and interpreted to function async.
     * @param applier Function to apply
     * @param <T> Apply function result type
     * @return Future with result of the function
     */
    public <T> CompletionStage<T> apply(final Function<Ruby, T> applier) {
        return CompletableFuture.supplyAsync(
            () -> {
                if (this.runtime == null) {
                    synchronized (this.lock) {
                        if (this.runtime == null) {
                            this.runtime = this.factory.get();
                        }
                    }
                }
                return applier.apply(this.runtime);
            }
        );
    }
}
