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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Returns some basic information about the given gem.
 *
 * @since 1.0
 */
public final class RubyObjJson implements GemInfo {

    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

    /**
     * Ruby initialization flag.
     */
    private boolean issetup;

    /**
     * New Ruby object JSON converter.
     * @param runtime Is Ruby runtime
     * @param ruby Is Ruby system
     */
    RubyObjJson(final RubyRuntimeAdapter runtime, final Ruby ruby) {
        this.runtime = runtime;
        this.ruby = ruby;
        this.issetup = false;
    }

    /**
     * Create JSON info for gem.
     * @param gempath Full path to gem file or null
     * @return JsonObjectBuilder result
     */
    public JsonObject info(final Path gempath) {
        final String script;
        JsonObject object = null;
        if (!this.issetup) {
            this.issetup = true;
            this.runtime.eval(this.ruby, "require 'rubygems/package.rb'");
        }
        try {
            script = IOUtils.toString(
                Gem.class.getResourceAsStream("/info.rb"),
                StandardCharsets.UTF_8
            );
            this.runtime.eval(this.ruby, script);
            final IRubyInfo info = (IRubyInfo) JavaEmbedUtils.invokeMethod(
                this.ruby,
                this.runtime.eval(this.ruby, "Ex"),
                "new",
                new Object[]{gempath},
                IRubyInfo.class
            );
            final org.jruby.RubyString obj = info.info();
            final ByteArrayInputStream bis = new ByteArrayInputStream(obj.getBytes());
            final JsonReader jsonreader = Json.createReader(bis);
            object = jsonreader.readObject();
            jsonreader.close();
        } catch (final IOException exc) {
            throw new ArtipieIOException(exc);
        }
        return object;
    }
}
