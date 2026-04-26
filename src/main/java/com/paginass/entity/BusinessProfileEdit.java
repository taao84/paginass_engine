package com.paginass.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "business_profile_edits")
@Getter
@Setter
@NoArgsConstructor
public class BusinessProfileEdit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_profile_id", nullable = false)
    private UUID businessProfileId;

    @Column(name = "edited_by", nullable = false)
    private UUID editedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> snapshot;

    @Column(name = "edited_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime editedAt;
}
