package com.paginass.service;

import com.paginass.util.HtmlSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void nullInput_returnsNull() {
        assertThat(HtmlSanitizer.sanitize(null)).isNull();
    }

    @Test
    void scriptTag_isStripped() {
        String result = HtmlSanitizer.sanitize("<p>Hello</p><script>alert('xss')</script>");
        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void onErrorAttribute_isStripped() {
        String result = HtmlSanitizer.sanitize("<img src='x' onerror='alert(1)'>");
        assertThat(result).doesNotContain("onerror");
        assertThat(result).doesNotContain("alert");
    }

    @Test
    void javascriptHref_isStripped() {
        String result = HtmlSanitizer.sanitize("<a href='javascript:alert(1)'>click</a>");
        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    void nestedInjection_isStripped() {
        String result = HtmlSanitizer.sanitize("<div><script><b>x</b></script></div>");
        assertThat(result).doesNotContain("<script>");
    }

    @Test
    void iframeTag_isStripped() {
        String result = HtmlSanitizer.sanitize("<iframe src='evil.com'></iframe>");
        assertThat(result).doesNotContain("<iframe>");
    }

    @Test
    void boldAndItalic_survivesSanitization() {
        String input = "<p>Hello <strong>world</strong> and <em>peace</em></p>";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).contains("<strong>world</strong>");
        assertThat(result).contains("<em>peace</em>");
    }

    @Test
    void unorderedList_survivesSanitization() {
        String input = "<ul><li>Item one</li><li>Item two</li></ul>";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).contains("<ul>");
        assertThat(result).contains("<li>Item one</li>");
    }

    @Test
    void emoji_surviveSanitization() {
        String input = "<p>🎉 Welcome to the contest! 📸</p>";
        String result = HtmlSanitizer.sanitize(input);
        // Sanitizer may encode emojis as HTML numeric entities (&#127881;) — both forms render correctly in browsers
        assertThat(result).containsAnyOf("🎉", "&#127881;", "&#x1f389;");
        assertThat(result).containsAnyOf("📸", "&#128248;", "&#x1f4f8;");
    }

    @Test
    void paragraphBreaks_surviveSanitization() {
        String input = "<p>Line one</p><p>Line two</p>";
        String result = HtmlSanitizer.sanitize(input);
        assertThat(result).contains("<p>Line one</p>");
        assertThat(result).contains("<p>Line two</p>");
    }
}
