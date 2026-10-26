package com.smartreply.ai.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Free, rule-based clean-up for English text the user typed: fixes spacing, capital letters, common
 * texting short-forms and common spelling slips, and can make the text more formal or shorter.
 * It does not understand meaning, so it never changes what the writer said, only how it is written.
 */
public final class TextCleaner {

    private TextCleaner() { }

    private static final Map<String, String> FIXES = new LinkedHashMap<>();
    static {
        String[][] pairs = {
            {"pls", "please"}, {"plz", "please"}, {"thx", "thanks"}, {"tnx", "thanks"}, {"u", "you"}, {"ur", "your"},
            {"msg", "message"}, {"tmrw", "tomorrow"}, {"tomorow", "tomorrow"}, {"tommorow", "tomorrow"},
            {"tomorrow's", "tomorrow's"}, {"recieve", "receive"}, {"recieved", "received"},
            {"definately", "definitely"}, {"teh", "the"}, {"seperate", "separate"}, {"occured", "occurred"},
            {"adress", "address"}, {"wich", "which"}, {"becuase", "because"}, {"beacuse", "because"},
            {"untill", "until"}, {"thier", "their"}, {"alot", "a lot"}, {"informtion", "information"},
            {"meeting's", "meeting's"}, {"apointment", "appointment"}, {"appoinment", "appointment"},
            {"schedual", "schedule"}, {"assignmnet", "assignment"}, {"submision", "submission"},
            {"ok", "okay"}, {"asap", "as soon as possible"}
        };
        for (String[] p : pairs) FIXES.put(p[0], p[1]);
    }

    private static final String[][] CONTRACTIONS = {
        {"don't", "do not"}, {"doesn't", "does not"}, {"didn't", "did not"}, {"can't", "cannot"},
        {"won't", "will not"}, {"isn't", "is not"}, {"aren't", "are not"}, {"wasn't", "was not"},
        {"couldn't", "could not"}, {"wouldn't", "would not"}, {"shouldn't", "should not"},
        {"haven't", "have not"}, {"hasn't", "has not"}, {"I'm", "I am"}, {"I'll", "I will"},
        {"I've", "I have"}, {"I'd", "I would"}, {"we're", "we are"}, {"we'll", "we will"},
        {"we've", "we have"}, {"they're", "they are"}, {"you're", "you are"}, {"you'll", "you will"},
        {"let's", "let us"}, {"that's", "that is"}
    };

    private static final List<String> FILLERS = List.of(
            "just", "really", "very", "actually", "basically", "literally", "simply", "quite");

    /** Fixes spacing, capitals, short-forms and common spelling slips. */
    public static String tidy(String text) {
        if (text == null) return "";
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append('\n');
            out.append(tidyLine(lines[i]));
        }
        return out.toString().replaceAll("\n{3,}", "\n\n").strip();
    }

    /** Tidy, then a more formal register: no contractions, "Dear" instead of "Hi", "Thank you" instead of "Thanks". */
    public static String formal(String text) {
        String t = tidy(text);
        for (String[] c : CONTRACTIONS) {
            t = replaceWord(t, c[0], c[1]);
        }
        t = Pattern.compile("(?m)^(Hi|Hey|Hello)\\b(?=\\s+\\S)").matcher(t).replaceAll("Dear");
        t = Pattern.compile("(?m)^Thanks\\b").matcher(t).replaceAll("Thank you");
        t = replaceWord(t, "thanks", "thank you");
        t = replaceWord(t, "okay", "fine");
        return capitaliseSentences(t);
    }

    /** Tidy, then drop filler words such as "just" and "really". */
    public static String shorter(String text) {
        String t = tidy(text);
        for (String filler : FILLERS) {
            t = Pattern.compile("(?i)\\b" + filler + "\\b[ ]?").matcher(t).replaceAll("");
        }
        t = t.replaceAll("(?i)\\bplease note that ", "").replaceAll("[ ]{2,}", " ").replaceAll(" +([,.!?;:])", "$1");
        return capitaliseSentences(t);
    }

    private static String tidyLine(String line) {
        String s = line.replaceAll("[ \\t\\u00a0]+", " ").strip();
        if (s.isEmpty() || (!s.contains(" ") && (s.contains("://") || s.contains("@")))) return s; // a bare link or address
        s = s.replaceAll(" +([,.!?;:])", "$1");
        s = s.replaceAll("([,;:])(?=[\\p{L}])", "$1 ");
        s = s.replaceAll("([.!?])(?=[A-Z])(?![A-Z]{2,}\\b)", "$1 ");
        s = s.replaceAll("([!?]){2,}", "$1").replaceAll("\\.{4,}", "...");
        for (Map.Entry<String, String> e : FIXES.entrySet()) {
            s = replaceWord(s, e.getKey(), e.getValue());
        }
        s = Pattern.compile("\\bi(?='(?:m|ll|ve|d)\\b)|\\bi\\b").matcher(s).replaceAll("I");
        s = capitaliseGreetingName(s);
        s = capitaliseSentences(s);
        return addFullStop(s);
    }

    /** "hi meena," -> "Hi Meena," */
    private static String capitaliseGreetingName(String s) {
        Matcher m = Pattern.compile("^(?i)(hi|hello|hey|dear)\\s+(\\p{L}+)").matcher(s);
        if (!m.find()) return s;
        return cap(m.group(1)) + " " + cap(m.group(2)) + s.substring(m.end());
    }

    private static String capitaliseSentences(String s) {
        StringBuilder sb = new StringBuilder();
        boolean start = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (start && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                start = false;
            } else {
                sb.append(c);
                if (c == '\n') start = true;
                else if ((c == '.' || c == '!' || c == '?') && i + 1 < s.length() && s.charAt(i + 1) == ' ') start = true;
                else if (!Character.isWhitespace(c) && !isOpener(c)) start = false;
            }
        }
        return sb.toString();
    }

    private static boolean isOpener(char c) {
        return c == '"' || c == '(' || c == '“' || c == '•' || c == '-';
    }

    /** Adds a full stop to a sentence-like line (4+ words) that ends on a letter or digit. */
    private static String addFullStop(String s) {
        if (s.split(" ").length < 4) return s;
        char last = s.charAt(s.length() - 1);
        return Character.isLetterOrDigit(last) ? s + "." : s;
    }

    private static String replaceWord(String text, String word, String replacement) {
        Pattern p = Pattern.compile("(?<![\\p{L}\\p{N}'])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}'])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String found = m.group();
            String rep = Character.isUpperCase(found.charAt(0)) && !found.equals(found.toLowerCase()) && !word.equals("I'm")
                    ? cap(replacement) : replacement;
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String cap(String w) {
        return w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase();
    }
}
