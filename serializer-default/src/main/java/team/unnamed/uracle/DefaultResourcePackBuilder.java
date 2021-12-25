/*
 * This file is part of uracle, licensed under the MIT license
 *
 * Copyright (c) 2021 Unnamed Team
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
package team.unnamed.uracle;

import net.kyori.adventure.key.Key;
import team.unnamed.uracle.font.BitMapFont;
import team.unnamed.uracle.font.Font;
import team.unnamed.uracle.font.LegacyUnicodeFont;
import team.unnamed.uracle.font.TrueTypeFont;
import team.unnamed.uracle.lang.Language;
import team.unnamed.uracle.lang.LanguageEntry;
import team.unnamed.uracle.model.BlockModel;
import team.unnamed.uracle.model.BlockState;
import team.unnamed.uracle.model.Element;
import team.unnamed.uracle.model.ElementFace;
import team.unnamed.uracle.model.ElementRotation;
import team.unnamed.uracle.model.ItemModel;
import team.unnamed.uracle.model.Model;
import team.unnamed.uracle.model.ModelDisplay;
import team.unnamed.uracle.model.block.BlockTexture;
import team.unnamed.uracle.model.item.ItemOverride;
import team.unnamed.uracle.model.item.ItemPredicate;
import team.unnamed.uracle.model.item.ItemTexture;
import team.unnamed.uracle.sound.Sound;
import team.unnamed.uracle.sound.SoundEvent;
import team.unnamed.uracle.sound.SoundRegistry;
import team.unnamed.uracle.texture.AnimationMeta;
import team.unnamed.uracle.texture.Texture;
import team.unnamed.uracle.texture.TextureMeta;
import team.unnamed.uracle.texture.VillagerMeta;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;

/**
 * The default {@link ResourcePackBuilder} implementation
 * that outputs the information to a delegated {@link TreeOutputStream}.
 *
 * @since 1.0.0
 */
public class DefaultResourcePackBuilder
        implements ResourcePackBuilder {

    private static final String ASSETS = "assets/";
    private static final String JSON_EXT = ".json";
    private static final String PNG_EXT = ".png";
    private static final String MCMETA_EXT = ".mcmeta";

    private final TreeOutputStream output;

    public DefaultResourcePackBuilder(TreeOutputStream output) {
        this.output = output;
    }

    private void bitMapFont(BitMapFont font, AssetWriter writer) {
        writer
            .key("type").value("bitmap")
            .key("file").value(font.file());

        if (font.height() != BitMapFont.DEFAULT_HEIGHT) {
            // only write if height is not equal to the default height
            writer.key("height").value(font.height());
        }

        writer
            .key("ascent").value(font.ascent())
            .key("chars").startArray();

        for (String character : font.characters()) {
            writer.value(character);
        }

        writer.endArray();
    }

    private void legacyUnicodeFont(LegacyUnicodeFont font, AssetWriter writer) {
        writer
            .key("sizes").value(font.sizes())
            .key("template").value(font.template());
    }

    private void ttfFont(TrueTypeFont font, AssetWriter writer) {
        writer
            .key("file").value(font.file())
            .key("shift").startArray()
                .value(font.shift().x())
                .value(font.shift().y())
            .endArray()
            .key("size").value(font.size())
            .key("oversample").value(font.oversample())
            .key("skip").startArray();

        for (String toSkip : font.skip()) {
            writer.value(toSkip);
        }

        writer.endArray();
    }

    @Override
    public ResourcePackBuilder font(Key location, Font font) {
        try (AssetWriter writer = output.useEntry(location.toString())) {
            writer.startObject();
            switch (font.type()) {
                case BITMAP:
                    bitMapFont((BitMapFont) font, writer);
                    break;
                case LEGACY_UNICODE:
                    legacyUnicodeFont((LegacyUnicodeFont) font, writer);
                    break;
                case TTF:
                    ttfFont((TrueTypeFont) font, writer);
                    break;
            }
            writer.endObject();
        }
        return this;
    }

    @Override
    public ResourcePackBuilder language(Key location, Language language) {
        // create the JSON file path (assets/<namespace>/lang/file.json)
        String path = ASSETS + location.namespace() + "/lang/" + location.value() + JSON_EXT;
        try (AssetWriter writer = output.useEntry(path)) {
            writer.startObject();
            for (Map.Entry<String, String> entry : language.translations().entrySet()) {
                writer.key(entry.getKey()).value(entry.getValue());
            }
            writer.endObject();
        }
        return this;
    }

    private void writeModelProperties(Model model, AssetWriter writer) {
        // parent
        writer.key("parent").value(model.parent());

        // display
        writer.key("display").startObject();
        for (Map.Entry<ModelDisplay.Type, ModelDisplay> entry : model.display().entrySet()) {
            ModelDisplay.Type type = entry.getKey();
            ModelDisplay display = entry.getValue();

            writer.key(type.name().toLowerCase(Locale.ROOT)).startObject()
                    .key("rotation").value(display.rotation())
                    .key("translation").value(display.translation())
                    .key("scale").value(display.scale())
                .endObject();
        }
        writer.endObject();

        // elements
        writer.key("elements").startArray();
        for (Element element : model.elements()) {
            ElementRotation rotation = element.rotation();

            writer
                .key("from").value(element.from())
                .key("to").value(element.to())
                .key("rotation").startObject()
                    .key("origin").value(rotation.origin())
                    .key("axis").value(rotation.axis().name().toLowerCase(Locale.ROOT))
                    .key("angle").value(rotation.angle());

            if (rotation.rescale()) {
                // only write if not equal to default value
                writer.key("rescale").value(rotation.rescale());
            }
            writer.endObject();

            if (!element.shade()) {
                // only write if not equal to default value
                writer.key("shade").value(element.shade());
            }

            // faces
            writer.key("faces").startObject();
            for (Map.Entry<CubeFace, ElementFace> entry : element.faces().entrySet()) {
                CubeFace type = entry.getKey();
                ElementFace face = entry.getValue();

                writer.key(type.name().toLowerCase(Locale.ROOT)).startObject();
                if (face.uv() != null) {
                    // this is a pure function but IDE still warns me, I have already checked it!!!!!!!!!!!!!!!!!!!!!!!!
                    writer.key("uv").value(face.uv());
                }
                writer.key("texture").value(face.texture());
                if (face.cullFace() != null) {
                    writer.key("cullface").value(face.cullFace().name().toLowerCase(Locale.ROOT));
                }
                if (face.rotation() != 0) {
                    writer.key("rotation").value(face.rotation());
                }
                if (face.tintIndex() != null) {
                    writer.key("tintindex").value(face.tintIndex());
                }
                writer.endObject();
            }
            writer.endObject();
        }
        writer.endArray();
    }

    private void writeItemModel(ItemModel model, AssetWriter writer) {
        writer.startObject();
        writeModelProperties(model, writer);

        // textures
        ItemTexture textures = model.textures();
        writer.key("textures").startObject();
        // ah yes, don't repeat yourself
        if (textures.particle() != null) {
            writer.key("particle").value(textures.particle());
        }
        for (int i = 0; i < textures.layers().size(); i++) {
            writer.key("layer" + i).value(textures.layers().get(i));
        }
        for (Map.Entry<String, Key> variable : textures.variables().entrySet()) {
            writer.key(variable.getKey()).value(variable.getValue());
        }
        writer.endObject();

        if (model.guiLight() != ItemModel.GuiLight.SIDE) {
            // only write if not default
            writer.key("gui_light").value(model.guiLight().name().toLowerCase(Locale.ROOT));
        }

        // overrides
        writer.key("overrides").startArray();
        for (ItemOverride override : model.overrides()) {
            writer.startObject()
                .key("predicate").startObject();
            for (ItemPredicate predicate : override.predicate()) {
                writer.key(predicate.name()).value(predicate.value());
            }
            writer.endObject()
                .key("model").value(override.model())
                .endObject();
        }
        writer.endArray().endObject();
    }

    private void writeBlockModel(BlockModel model, AssetWriter writer) {
        writer.startObject();
        writeModelProperties(model, writer);
        if (!model.ambientOcclusion()) {
            // only write if not default value
            writer.key("ambientocclusion").value(model.ambientOcclusion());
        }

        // textures
        BlockTexture textures = model.textures();
        writer.key("textures").startObject();
        if (textures.particle() != null) {
            writer.key("particle").value(textures.particle());
        }
        for (Map.Entry<String, Key> variable : textures.variables().entrySet()) {
            writer.key(variable.getKey()).value(variable.getValue());
        }
        writer.endObject().endObject();
    }

    @Override
    public ResourcePackBuilder model(Key location, Model model) {
        String path = ASSETS + location.namespace() + "/models" + location.value();
        try (AssetWriter writer = output.useEntry(path)) {
            if (model instanceof ItemModel) {
                writeItemModel((ItemModel) model, writer);
            } else if (model instanceof BlockModel) {
                writeBlockModel((BlockModel) model, writer);
            } else {
                throw new IllegalArgumentException("Invalid model type");
            }
        }
        return this;
    }

    @Override
    public ResourcePackBuilder blockState(Key location, BlockState blockState) {
        return this;
    }

    @Override
    public ResourcePackBuilder sounds(String namespace, SoundRegistry registry) {
        String path = ASSETS + namespace + "/sounds" + JSON_EXT;

        try (AssetWriter writer = output.useEntry(path)) {
            writer.startObject();
            for (Map.Entry<String, SoundEvent> entry : registry.sounds().entrySet()) {
                SoundEvent event = entry.getValue();

                writer.key(entry.getKey()).startObject()
                        .key("replace").value(event.replace());

                if (event.subtitle() != null) {
                    writer.key("subtitle").value(event.subtitle());
                }
                if (event.sounds() != null) {
                    writer.key("sounds").startArray();
                    for (Sound sound : event.sounds()) {
                        // in order to make some optimizations, we
                        // have to do this
                        if (sound.allDefault()) {
                            // everything is default, just write the name
                            writer.value(sound.name());
                        } else {
                            writer.startObject()
                                .key("name").value(sound.name());
                            if (sound.volume() != Sound.DEFAULT_VOLUME) {
                                writer.key("volume").value(sound.volume());
                            }
                            if (sound.pitch() != Sound.DEFAULT_PITCH) {
                                writer.key("pitch").value(sound.pitch());
                            }
                            if (sound.weight() != Sound.DEFAULT_WEIGHT) {
                                writer.key("weight").value(sound.weight());
                            }
                            if (sound.stream() != Sound.DEFAULT_STREAM) {
                                writer.key("stream").value(sound.stream());
                            }
                            if (sound.attenuationDistance() != Sound.DEFAULT_ATTENUATION_DISTANCE) {
                                writer.key("attenuation_distance").value(sound.attenuationDistance());
                            }
                            if (sound.preload() != Sound.DEFAULT_PRELOAD) {
                                writer.key("preload").value(sound.preload());
                            }
                            if (sound.type() != Sound.DEFAULT_TYPE) {
                                writer.key("type").value(sound.type().name().toLowerCase(Locale.ROOT));
                            }
                            writer.endObject();
                        }
                    }
                    writer.endArray();
                }
                writer.endObject();
            }
            writer.endObject();
        }
        return this;
    }

    @Override
    public ResourcePackBuilder texture(Key location, Texture texture) {

        String path = ASSETS + location.namespace() + "/textures/" + location.value() + PNG_EXT;

        // write the actual texture PNG image
        try (AssetWriter writer = output.useEntry(path)) {
            texture.data().write(writer);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write texture", e);
        }

        TextureMeta meta = texture.meta();
        AnimationMeta animation = texture.animation();
        VillagerMeta villager = texture.villager();

        boolean hasMeta = meta != null;
        boolean hasAnimation = animation != null;
        boolean hasVillager = villager != null;

        if (!hasMeta && !hasAnimation && !hasVillager) {
            // no metadata to write
            return this;
        }

        try (AssetWriter writer = output.useEntry(path + MCMETA_EXT)) {
            writer.startObject();

            if (hasMeta) {
                writer.key("texture").startObject()
                    .key("blur").value(meta.blur())
                    .key("clamp").value(meta.clamp())
                    .key("mipmaps").startArray();
                for (int mipmap : meta.mipmaps()) {
                    writer.value(mipmap);
                }
                writer.endArray().endObject();
            }

            if (hasAnimation) {
                int frameTime = animation.frameTime();

                writer.key("animation").startObject()
                    .key("interpolate").value(animation.interpolate())
                    .key("width").value(animation.width())
                    .key("height").value(animation.height())
                    .key("frametime").value(frameTime)
                    .key("frames").startArray();

                for (AnimationMeta.Frame frame : animation.frames()) {
                    int index = frame.index();
                    int time = frame.frameTime();

                    if (frameTime == time) {
                        // same as default frameTime, we can skip it
                        writer.value(index);
                    } else {
                        // specific frameTime, write as an object
                        writer.startObject()
                            .key("index").value(index)
                            .key("time").value(time)
                            .endObject();
                    }
                }

                writer.endArray().endObject();
            }

            if (hasVillager) {
                String hat = villager.hat();
                writer.key("villager").startObject();
                if (hat != null) {
                    writer.key("hat").value(hat);
                }
                writer.endObject();
            }

            writer.endObject();
        }
        return this;
    }

    @Override
    public ResourcePackBuilder meta(PackMeta meta) {
        // write pack.mcmeta file
        try (AssetWriter writer = output.useEntry("pack.mcmeta")) {
            // {
            //   "pack": { "format": ?, "description": "?" }
            writer.startObject()
                .key("pack").startObject()
                    .key("format").value(meta.pack().format())
                    .key("description").value(meta.pack().description())
                .endObject();

            if (!meta.languages().isEmpty()) {
                // "language": {
                writer.key("language").startObject();

                for (Map.Entry<Key, LanguageEntry> entry : meta.languages().entrySet()) {
                    LanguageEntry language = entry.getValue();
                    // "?": { "name": ?, "region": ?, "bidirectional": ? }
                    writer.key(entry.getKey().asString()).startObject()
                        .key("name").value(language.name())
                        .key("region").value(language.region())
                        .key("bidirectional").value(language.bidirectional())
                        .endObject();
                }

                writer.endObject();
            }
            writer.endObject();
        }
        return this;
    }

    @Override
    public ResourcePackBuilder file(String path, Writable data) {
        try (AssetWriter writer = output.useEntry(path)) {
            data.write(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

}