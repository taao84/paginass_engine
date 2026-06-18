package com.paginass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "reply_email", nullable = false, length = 255)
    private String replyEmail;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "read_by")
    private UUID readBy;
}
