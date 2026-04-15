package com.paginass.service;

public interface MailService {
    void sendVerificationEmail(String to, String verifyLink);
}
