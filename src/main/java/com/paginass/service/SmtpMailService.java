package com.paginass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Override
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
}
