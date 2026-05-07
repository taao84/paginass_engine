package com.paginass.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConfigTest {

    private DefaultCookieSerializer serializer;

    @BeforeEach
    void setUp() {
        SessionConfig config = new SessionConfig();
        ReflectionTestUtils.setField(config, "useSecureCookie", true);
        ReflectionTestUtils.setField(config, "sameSite", "None");
        serializer = config.cookieSerializer();
        serializer.setUseBase64Encoding(false);
    }

    @Test
    void cookieName_isSESSION() {
        MockHttpServletResponse response = writeTestCookie("abc123");
        assertThat(response.getHeader("Set-Cookie")).contains("SESSION=abc123");
    }

    @Test
    void sameSite_isNone() {
        MockHttpServletResponse response = writeTestCookie("abc123");
        assertThat(response.getHeader("Set-Cookie")).contains("SameSite=None");
    }

    @Test
    void secure_isSet() {
        MockHttpServletResponse response = writeTestCookie("abc123");
        assertThat(response.getHeader("Set-Cookie")).contains("Secure");
    }

    @Test
    void httpOnly_isSet() {
        MockHttpServletResponse response = writeTestCookie("abc123");
        assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly");
    }

    private MockHttpServletResponse writeTestCookie(String sessionId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        serializer.writeCookieValue(new DefaultCookieSerializer.CookieValue(request, response, sessionId));
        return response;
    }
}
