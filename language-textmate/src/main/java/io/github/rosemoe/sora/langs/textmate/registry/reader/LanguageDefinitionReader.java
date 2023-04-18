/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.langs.textmate.registry.reader;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.registry.IGrammarSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition;
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition;

public class LanguageDefinitionReader {

    public static List<GrammarDefinition> read(String path) {
        @Nullable InputStream stream = FileProviderRegistry.getInstance().tryGetInputStream(path);
        if (stream == null) {
            return Collections.emptyList();
        }
        return read(new BufferedReader(new InputStreamReader(stream)));
    }

    private static List<GrammarDefinition> read(BufferedReader bufferedReader) {
        return new GsonBuilder().registerTypeAdapter(GrammarDefinition.class, (JsonDeserializer<GrammarDefinition>) (json, typeOfT, context) -> {
                    JsonObject object = json.getAsJsonObject();
                    String grammarPath = object.get("grammar").getAsString();
                    String name = object.get("name").getAsString();
                    String scopeName = object.get("scopeName").getAsString();


                    JsonElement embeddedLanguagesElement = object.get("embeddedLanguages");

                    JsonObject embeddedLanguages = null;

                    if (embeddedLanguagesElement != null && embeddedLanguagesElement.isJsonObject()) {
                        embeddedLanguages = embeddedLanguagesElement.getAsJsonObject();
                    }


                    JsonElement languageConfigurationElement = object.get("languageConfiguration");

                    String languageConfiguration = null;

                    if (languageConfigurationElement != null && !languageConfigurationElement.isJsonNull()) {
                        languageConfiguration = languageConfigurationElement.getAsString();
                    }


                    @Nullable InputStream grammarInput = FileProviderRegistry.getInstance().tryGetInputStream(
                            grammarPath
                    );
                    if (grammarInput == null) {
                        throw new IllegalArgumentException("grammar file can not be opened");
                    }
                    IGrammarSource grammarSource = IGrammarSource.fromInputStream(grammarInput, grammarPath, Charset.defaultCharset());

                    DefaultGrammarDefinition grammarDefinition = DefaultGrammarDefinition.withLanguageConfiguration(grammarSource, languageConfiguration, name, scopeName);

                    if (embeddedLanguages != null) {
                        HashMap<String, String> embeddedLanguagesMap = new HashMap<String, String>();

                        for (Map.Entry<String, JsonElement> entry : embeddedLanguages.entrySet()) {
                            JsonElement value = entry.getValue();

                            if (!value.isJsonNull()) {
                                embeddedLanguagesMap.put(entry.getKey(), value.getAsString());
                            }

                        }

                        return grammarDefinition.withEmbeddedLanguages(embeddedLanguagesMap);
                    } else {
                        return grammarDefinition;
                    }

                })
                .create()
                .fromJson(bufferedReader, LanguageDefinitionList.class).grammarDefinition;


    }


    static class LanguageDefinitionList {
        @SerializedName("languages")
        private List<GrammarDefinition> grammarDefinition;

        public LanguageDefinitionList(List<GrammarDefinition> grammarDefinition) {
            this.grammarDefinition = grammarDefinition;
        }

        public List<GrammarDefinition> getLanguageDefinition() {
            return grammarDefinition;
        }

        public void setLanguageDefinition(List<GrammarDefinition> grammarDefinition) {
            this.grammarDefinition = grammarDefinition;
        }
    }

}
