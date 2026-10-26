package com.smartreply.ai.i18n;

import com.smartreply.ai.model.Language;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * Looks up the fixed wording for a language. The wording lives in plain text files
 * ({@code i18n/phrases_en.properties}, {@code phrases_ta.properties}, ...), so a new language is one new file
 * plus one line in {@link Language}, and a translator never needs to touch Java.
 * <p>
 * If a language file is missing a line, the English wording is used instead of failing, so an unfinished
 * translation can never break the app. {@code PhraseBookTest} checks that no line is missing.
 */
@Component
public class PhraseBook {

    private final Map<Language, Properties> bundles = new EnumMap<>(Language.class);

    public PhraseBook() {
        for (Language language : Language.values()) {
            bundles.put(language, load(language));
        }
    }

    /** The wording for {@code phrase} in {@code language}, with {@code {0}}, {@code {1}}... replaced by {@code args}. */
    public String text(Language language, Phrase phrase, String... args) {
        String template = bundles.get(language).getProperty(phrase.name());
        if (template == null || template.isBlank()) {
            template = bundles.get(Language.ENGLISH).getProperty(phrase.name(), phrase.name());
        }
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", args[i] == null ? "" : args[i]);
        }
        return template;
    }

    /** True if the language file has its own wording for this phrase (used by the completeness test). */
    public boolean has(Language language, Phrase phrase) {
        String value = bundles.get(language).getProperty(phrase.name());
        return value != null && !value.isBlank();
    }

    /** The raw line, before any {@code {0}} is replaced (used by the completeness test). */
    public String template(Language language, Phrase phrase) {
        return bundles.get(language).getProperty(phrase.name(), "");
    }

    private static Properties load(Language language) {
        String path = "/i18n/phrases_" + language.getCode() + ".properties";
        try (InputStream in = PhraseBook.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing phrase file " + path);
            }
            Properties properties = new Properties();
            // Read as UTF-8 so Tamil, Hindi, Telugu and Malayalam are stored as readable text.
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read phrase file " + path, e);
        }
    }
}
