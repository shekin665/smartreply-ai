package com.smartreply.ai.model;

import java.util.Locale;

/**
 * Languages the mail generator and the reply generator can write in.
 * <p>
 * Each language has a phrase file {@code i18n/phrases_<code>.properties} holding the fixed wording
 * (greetings, closings, "you are invited to...", and so on). Text the user typed, such as an event
 * title or a key point, is never translated and is copied into the mail exactly as written.
 */
public enum Language {
    ENGLISH("en", "English", "English"),
    TAMIL("ta", "தமிழ்", "Tamil"),
    HINDI("hi", "हिन्दी", "Hindi"),
    TELUGU("te", "తెలుగు", "Telugu"),
    MALAYALAM("ml", "മലയാളം", "Malayalam"),
    FRENCH("fr", "Français", "French"),
    SPANISH("es", "Español", "Spanish"),
    GERMAN("de", "Deutsch", "German");

    private final String code;
    private final String nativeName;
    private final String englishName;

    Language(String code, String nativeName, String englishName) {
        this.code = code;
        this.nativeName = nativeName;
        this.englishName = englishName;
    }

    public String getCode() {
        return code;
    }

    public String getNativeName() {
        return nativeName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(code);
    }

    /** What the language picker shows, for example "தமிழ் (Tamil)". English is just "English". */
    public String getLabel() {
        return this == ENGLISH ? englishName : nativeName + " (" + englishName + ")";
    }
}
