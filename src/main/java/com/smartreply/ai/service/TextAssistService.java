package com.smartreply.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartreply.ai.exception.TextAssistException;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.Tone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Improves and/or translates text the user typed, using only free tools:
 * <ul>
 *   <li>clean-up and style (more formal, shorter) by {@link TextCleaner} rules, English only;</li>
 *   <li>translation by the free MyMemory service (no key needed). The free allowance is limited per day,
 *       so the app can send an optional contact email ({@code smartreply.translate.email}) to raise it.</li>
 * </ul>
 */
@Service
public class TextAssistService {

    static final String API_URL = "https://api.mymemory.translated.net/get";
    static final int MAX_CHUNK = 450;
    static final int MAX_REQUESTS = 40;

    private final RestClient restClient;
    private final String contactEmail;

    public TextAssistService(RestClient.Builder restClientBuilder,
                             @Value("${smartreply.translate.email:}") String contactEmail) {
        this.restClient = restClientBuilder.build();
        this.contactEmail = contactEmail == null ? "" : contactEmail.trim();
    }

    public String assist(String text, Language language, Tone style) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Type some text first.");
        }
        String result = switch (style == null ? Tone.PROFESSIONAL : style) {
            case FORMAL -> TextCleaner.formal(text);
            case CONCISE -> TextCleaner.shorter(text);
            default -> TextCleaner.tidy(text);
        };
        if (language != null) {
            result = translate(result, language);
        }
        return result;
    }

    /** Translates text only (no clean-up), for reading incoming and outgoing mail in another language. */
    public String translateOnly(String text, Language target) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("There is nothing to translate.");
        }
        if (target == null) {
            throw new IllegalArgumentException("Choose a language.");
        }
        return translate(text.replace("\r\n", "\n"), target);
    }

    /** Translates line by line so the layout (greeting, paragraphs, sign-off) is kept. */
    String translate(String text, Language target) {
        List<String> chunks = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            chunks.add(line);
        }
        int requests = 0;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) out.append('\n');
            String line = chunks.get(i);
            if (line.isBlank() || looksUntranslatable(line)) {
                out.append(line);
                continue;
            }
            List<String> parts = TextSplitter.split(line, MAX_CHUNK);
            for (int j = 0; j < parts.size(); j++) {
                if (++requests > MAX_REQUESTS) {
                    throw new TextAssistException("This text is too long to translate in one go. Try a shorter part.", false, null);
                }
                if (j > 0) out.append(' ');
                out.append(translateChunk(parts.get(j), target));
            }
        }
        return out.toString();
    }

    private String translateChunk(String chunk, Language target) {
        try {
            JsonNode response = restClient.get()
                    .uri(API_URL + "?q={q}&langpair={pair}" + (contactEmail.isEmpty() ? "" : "&de={de}"),
                            contactEmail.isEmpty()
                                    ? new Object[]{chunk, "Autodetect|" + target.getCode()}
                                    : new Object[]{chunk, "Autodetect|" + target.getCode(), contactEmail})
                    .retrieve()
                    .body(JsonNode.class);
            String translated = response == null ? "" : response.path("responseData").path("translatedText").asText("");
            return parseTranslation(translated);
        } catch (TextAssistException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TextAssistException("The free translation service could not be reached. Please try again in a moment.", false, ex);
        }
    }

    static String parseTranslation(String translated) {
        if (translated == null || translated.isBlank()) {
            throw new TextAssistException("The translation service returned nothing. Please try again.", false, null);
        }
        if (translated.toUpperCase().contains("MYMEMORY WARNING")) {
            throw new TextAssistException(
                    "The free translation limit for today has been reached. Please try again tomorrow.", false, null);
        }
        return translated.replace("&#39;", "'").replace("&quot;", "\"").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">");
    }

    /**
     * Lines with a web link are left as they are (a translation service can break the link), and so are
     * bare e-mail addresses and lines with no letters in them.
     */
    static boolean looksUntranslatable(String line) {
        String t = line.strip();
        if (t.contains("://") || t.startsWith("www.")) return true;
        if (t.matches("\\S+@\\S+\\.\\S+")) return true;
        return t.chars().noneMatch(Character::isLetter);
    }
}
