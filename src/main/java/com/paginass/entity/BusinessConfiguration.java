package com.paginass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "business_configurations")
@Getter
@Setter
@NoArgsConstructor
public class BusinessConfiguration {

    @Id
    @Column(name = "business_id")
    private UUID businessId;

    @Column(name = "max_featured_products")
    private Integer maxFeaturedProducts;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;
}
