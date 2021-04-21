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
import java.util.Collections;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Ruby runtime gem index implementation.
 *
 * @since 1.0
 */
public final class RubyGemIndex implements GemIndex {

    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

    /**
     * New gem indexer.
     * @param runtime Ruby runtime
     * @param ruby Interpreter
     */
    public RubyGemIndex(final RubyRuntimeAdapter runtime, final Ruby ruby) {
        this.runtime = runtime;
        this.ruby = ruby;
    }

    @Override
    public void update(final Path path) {
        this.runtime.eval(
            this.ruby,
            String.format(
                "Gem::Indexer.new('%s', {build_modern:true}).generate_index",
                path.toAbsolutePath().toString()
            )
        );
    }

    /**
     * Initialize indexer.
     */
    public void initialize() {
        this.runtime.eval(this.ruby, "require 'rubygems/indexer.rb'");
    }

    /**
     * Create new gem indexer.
     * @return A new ruby gem indexer.
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static RubyGemIndex createNew() {
        final RubyGemIndex result = new RubyGemIndex(
            JavaEmbedUtils.newRuntimeAdapter(),
            JavaEmbedUtils.initialize(Collections.emptyList())
        );
        result.initialize();
        return result;
    }
}
