package com.smartreply.ai.service;

import java.util.ArrayList;
import java.util.List;

/** Splits a long line into pieces of at most {@code max} characters, preferring sentence ends, then spaces. */
final class TextSplitter {

    private TextSplitter() { }

    static List<String> split(String line, int max) {
        List<String> parts = new ArrayList<>();
        String rest = line.strip();
        while (rest.length() > max) {
            int cut = lastIndexBefore(rest, max, ". ", "! ", "? ");
            if (cut <= 0) cut = rest.lastIndexOf(' ', max);
            if (cut <= 0) cut = max;
            else cut = Math.min(cut + 1, rest.length());
            parts.add(rest.substring(0, cut).strip());
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) parts.add(rest);
        return parts;
    }

    private static int lastIndexBefore(String s, int max, String... seps) {
        int best = -1;
        for (String sep : seps) {
            int i = s.lastIndexOf(sep, max - 1);
            best = Math.max(best, i);
        }
        return best;
    }
}
