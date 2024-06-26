/*
 * This file is part of creative, licensed under the MIT license
 *
 * Copyright (c) 2021-2023 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package team.unnamed.creative.metadata.pack;

import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;
import net.kyori.adventure.text.Component;
import net.kyori.examination.ExaminableProperty;
import net.kyori.examination.string.StringExaminer;
import team.unnamed.creative.metadata.MetadataPart;

final class PackMetaImpl implements PackMeta {

    private final PackFormat format;
    private final Component description;

    PackMetaImpl(
            final @NotNull PackFormat format,
            final @NotNull Component description
    ) {
        this.format = requireNonNull(format, "format");
        this.description = requireNonNull(description, "description");
    }

    @Override
    public @NotNull Class<? extends MetadataPart> type() {
        return PackMeta.class;
    }

    @Override
    public @NotNull PackFormat formats() {
        return format;
    }

    @Override
    public @NotNull Component description() {
        return description;
    }

    @Override
    public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
        return Stream.of(
                ExaminableProperty.of("format", format),
                ExaminableProperty.of("description", description)
        );
    }

    @Override
    public String toString() {
        return examine(StringExaminer.simpleEscaping());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackMetaImpl packMeta = (PackMetaImpl) o;
        return format.equals(packMeta.format)
                && description.equals(packMeta.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, description);
    }

}
