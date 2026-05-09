package com.paginass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "banner_history")
@Getter
@Setter
@NoArgsConstructor
public class BannerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sub_path", nullable = false)
    private String subPath;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    public BannerHistory(String subPath) {
        this.subPath = subPath;
        this.uploadedAt = OffsetDateTime.now();
    }
}
