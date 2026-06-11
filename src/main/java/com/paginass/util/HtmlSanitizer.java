package com.paginass.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public final class HtmlSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.STYLES);

    private HtmlSanitizer() {}

    public static String sanitize(String html) {
        if (html == null) return null;
        return POLICY.sanitize(html);
    }
}
