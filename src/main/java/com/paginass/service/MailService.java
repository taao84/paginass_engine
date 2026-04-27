package com.paginass.service;

public interface MailService {
    void sendVerificationEmail(String to, String verifyLink);
    void sendPasswordResetEmail(String to, String resetLink);
}
