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

import com.artipie.ArtipieException;
import java.util.function.Supplier;

/**
 * Supplier to wrap checked supplier throwing checke exception
 * with unchecked one.
 * @param <T> Supplier type
 * @since 1.0
 * @todo #85:30min Move this class to artipie/asto repo
 *  This class was created due to lack of unchecked supplier
 *  implementation in asto project. Let's move it to asto repo.
 */
public final class UncheckedSupplier<T> implements Supplier<T> {

    /**
     * Supplier which throws checked exceptions.
     */
    private final CheckedSupplier<? extends T, ? extends Exception> checked;

    /**
     * Wrap checked supplier with unchecked.
     * @param checked Checked supplier
     */
    public UncheckedSupplier(final CheckedSupplier<T, ? extends Exception> checked) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public T get() {
        try {
            return this.checked.get();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception err) {
            throw new ArtipieException(err);
        }
    }

    /**
     * Checked supplier which throws exception.
     * @param <T> Supplier type
     * @param <E> Exception type
     * @since 1.0
     */
    @FunctionalInterface
    public interface CheckedSupplier<T, E extends Exception> {

        /**
         * Get value or throw exception.
         * @return Value
         * @throws Exception of type E
         */
        T get() throws E;
    }
}
