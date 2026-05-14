package com.paginass.service;

public interface MailService {
    void sendVerificationEmail(String to, String verifyLink);
    void sendPasswordResetEmail(String to, String resetLink);
    void sendClaimApprovedEmail(String to, String claimantName, String businessName, String miniSiteUrl);
}
