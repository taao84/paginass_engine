package com.paginass.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
public class ConsoleMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailService.class);

    @Override
    public void sendVerificationEmail(String to, String verifyLink) {
        log.info("[DEV] Verification email for {} → {}", to, verifyLink);
    }
}
