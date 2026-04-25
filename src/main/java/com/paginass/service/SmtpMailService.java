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
}
