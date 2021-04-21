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

import java.nio.file.Path;

/**
 * Gem repository index.
 *
 * @since 1.0
 */
public interface GemIndex {

    /**
     * Update index.
     * @param path Repository index path
     */
    void update(Path path);

    /**
     * Synchronized decorator.
     * @since 1.0
     */
    final class Synchronized implements GemIndex {

        /**
         * Origin gem index instance.
         */
        private final GemIndex origin;

        /**
         * Wrap origin with synchronized decoradtor.
         * @param origin Gem index
         */
        public Synchronized(final GemIndex origin) {
            this.origin = origin;
        }

        @Override
        public void update(final Path path) {
            synchronized (this.origin) {
                this.origin.update(path);
            }
        }
    }
}
