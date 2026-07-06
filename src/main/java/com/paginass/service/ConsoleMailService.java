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

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("[DEV] Password reset email for {} → {}", to, resetLink);
    }

    @Override
    public void sendClaimApprovedEmail(String to, String claimantName, String businessName, String miniSiteUrl) {
        log.info("[DEV] Claim approved email for {} ({}) — business '{}' → {}", to, claimantName, businessName, miniSiteUrl);
    }

    @Override
    public void sendNewClaimNotificationEmail(String adminEmail, String claimantName, String claimantEmail, String businessName, String claimsQueueUrl) {
        log.info("[DEV] New claim notification → {} | claimant: {} ({}) | business: '{}' | queue: {}", adminEmail, claimantName, claimantEmail, businessName, claimsQueueUrl);
    }

    @Override
    public void sendCampaignMessage(String to, String subject, String message) {
        log.info("[DEV] Campaign message for {} — subject '{}': {}", to, subject, message);
    }
}
