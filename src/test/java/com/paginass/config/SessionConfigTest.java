package com.paginass.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.DefaultCookieSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConfigTest {

    private final DefaultCookieSerializer serializer = new SessionConfig().cookieSerializer();

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
