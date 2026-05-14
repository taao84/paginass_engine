package com.paginass.util;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

public final class TagNormalizer {

    private TagNormalizer() {}

    public static List<String> normalize(List<String> tags) {
        if (tags == null) return List.of();
        return tags.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    // NFD decomposes ñ into n + combining tilde (U+0303).
                    // Remove all combining diacritical marks except U+0303 so ñ is preserved,
                    // then recompose so n + U+0303 becomes ñ again.
                    String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);
                    String stripped = nfd.replaceAll("[\\p{InCombiningDiacriticalMarks}&&[^̃]]", "");
                    String recomposed = Normalizer.normalize(stripped, Normalizer.Form.NFC);
                    String cleaned = recomposed.replaceAll("[^\\p{L}\\p{N}\\s]", "");
                    return cleaned.replaceAll("\\s+", " ").trim().toLowerCase();
                })
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
