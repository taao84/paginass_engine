package com.paginass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Value("${app.session.use-secure-cookie}")
    private boolean useSecureCookie;

    @Value("${app.session.same-site}")
    private String sameSite;

    @Bean
    public DefaultCookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseSecureCookie(useSecureCookie);
        serializer.setSameSite(sameSite);
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}
