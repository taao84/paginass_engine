package com.paginass.service;

public interface MailService {
    void sendVerificationEmail(String to, String verifyLink);
    void sendPasswordResetEmail(String to, String resetLink);
    void sendClaimApprovedEmail(String to, String claimantName, String businessName, String miniSiteUrl);
    void sendNewClaimNotificationEmail(String adminEmail, String claimantName, String claimantEmail, String businessName, String claimsQueueUrl);
    void sendCampaignMessage(String to, String subject, String message);
}
