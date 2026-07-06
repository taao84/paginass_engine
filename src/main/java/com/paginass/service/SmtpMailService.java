package com.paginass.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Override
    @Retryable(
        retryFor = MailException.class,
        maxAttemptsExpression = "${app.mail.retry.max-attempts:2}",
        backoff = @Backoff(delayExpression = "${app.mail.retry.delay-ms:1000}")
    )
    public void sendVerificationEmail(String to, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("Verifica tu correo — Paginass");
        message.setText(
                "Hola,\n\n" +
                "Haz clic en el siguiente enlace para verificar tu correo y activar tu cuenta:\n\n" +
                verifyLink + "\n\n" +
                "El enlace expira en 1 hora.\n\n" +
                "Si no creaste esta cuenta, puedes ignorar este mensaje.\n\n" +
                "— El equipo de Paginass"
        );
        mailSender.send(message);
    }

    @Recover
    public void recoverSendVerificationEmail(MailException e, String to, String verifyLink) {
        log.error("Failed to send verification email to {} after retries: {}", to, e.getMessage());
    }

    @Override
    @Retryable(
        retryFor = MailException.class,
        maxAttemptsExpression = "${app.mail.retry.max-attempts:2}",
        backoff = @Backoff(delayExpression = "${app.mail.retry.delay-ms:1000}")
    )
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("Restablece tu contraseña — Paginass");
        message.setText(
                "Hola,\n\n" +
                "Haz clic en el siguiente enlace para restablecer tu contraseña:\n\n" +
                resetLink + "\n\n" +
                "El enlace expira en 24 horas.\n\n" +
                "Si no solicitaste este cambio, puedes ignorar este mensaje.\n\n" +
                "— El equipo de Paginass"
        );
        mailSender.send(message);
    }

    @Recover
    public void recoverSendPasswordResetEmail(MailException e, String to, String resetLink) {
        log.error("Failed to send password reset email to {} after retries: {}", to, e.getMessage());
    }

    @Override
    @Retryable(
        retryFor = MailException.class,
        maxAttemptsExpression = "${app.mail.retry.max-attempts:2}",
        backoff = @Backoff(delayExpression = "${app.mail.retry.delay-ms:1000}")
    )
    public void sendClaimApprovedEmail(String to, String claimantName, String businessName, String miniSiteUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("Tu solicitud de propiedad fue aprobada — Paginass");
        message.setText(
                "Hola " + claimantName + ",\n\n" +
                "Tu solicitud para reclamar la propiedad de \"" + businessName + "\" ha sido aprobada.\n\n" +
                "Ya puedes administrar tu negocio desde tu mini-sitio:\n\n" +
                miniSiteUrl + "\n\n" +
                "— El equipo de Paginass"
        );
        mailSender.send(message);
    }

    @Recover
    public void recoverSendClaimApprovedEmail(MailException e, String to, String claimantName, String businessName, String miniSiteUrl) {
        log.error("Failed to send claim approved email to {} after retries: {}", to, e.getMessage());
    }

    @Override
    @Retryable(
        retryFor = MailException.class,
        maxAttemptsExpression = "${app.mail.retry.max-attempts:2}",
        backoff = @Backoff(delayExpression = "${app.mail.retry.delay-ms:1000}")
    )
    public void sendNewClaimNotificationEmail(String adminEmail, String claimantName, String claimantEmail, String businessName, String claimsQueueUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(adminEmail);
        message.setSubject("Nueva solicitud de propiedad — Paginass");
        message.setText(
                "Se ha recibido una nueva solicitud de propiedad.\n\n" +
                "Negocio: " + businessName + "\n" +
                "Solicitante: " + claimantName + "\n" +
                "Correo: " + claimantEmail + "\n\n" +
                "Revisa la solicitud aquí:\n" +
                claimsQueueUrl + "\n\n" +
                "— Paginass"
        );
        mailSender.send(message);
    }

    @Recover
    public void recoverSendNewClaimNotificationEmail(MailException e, String adminEmail, String claimantName, String claimantEmail, String businessName, String claimsQueueUrl) {
        log.error("Failed to send new claim notification to {} after retries: {}", adminEmail, e.getMessage());
    }

    @Override
    @Retryable(
        retryFor = MailException.class,
        maxAttemptsExpression = "${app.mail.retry.max-attempts:2}",
        backoff = @Backoff(delayExpression = "${app.mail.retry.delay-ms:1000}")
    )
    public void sendCampaignMessage(String to, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailFrom);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }

    @Recover
    public void recoverSendCampaignMessage(MailException e, String to, String subject, String message) {
        log.error("Failed to send campaign message to {} after retries: {}", to, e.getMessage());
    }
}
