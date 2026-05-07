package com.paginass.repository;

import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests against a real PostgreSQL instance.
 * These catch issues H2 cannot surface, such as native enum cast failures
 * and dirty-tracking behaviour differences.
 */
@DataJpaTest(properties = "spring.flyway.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class AdvertisementRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setup() {
        owner = new User();
        owner.setOauthProvider("local");
        owner.setOauthId(UUID.randomUUID().toString());
        owner.setEmail("repo-it-" + UUID.randomUUID() + "@example.com");
        owner.setRole(UserRole.CONTRIBUTOR);
        owner.setEmailVerified(true);
        userRepository.save(owner);
    }

    // --- native enum cast ---

    @Test
    void findByCategory_doesNotThrowEnumCastException() {
        advertisementRepository.save(buildAd(AdCategory.SALE, Instant.now().plus(30, ChronoUnit.DAYS)));

        // Previously failed: "operator does not exist: ad_category = character varying"
        // Fixed by adding @JdbcTypeCode(SqlTypes.NAMED_ENUM) to Advertisement.category
        assertThatNoException().isThrownBy(() ->
                advertisementRepository.findAllByCategoryAndExpiresAtAfterOrderByCreatedAtDesc(
                        AdCategory.SALE, Instant.now()));
    }

    @Test
    void findByCategory_returnsMatchingAds() {
        advertisementRepository.save(buildAd(AdCategory.SALE, Instant.now().plus(30, ChronoUnit.DAYS)));
        advertisementRepository.save(buildAd(AdCategory.JOB, Instant.now().plus(30, ChronoUnit.DAYS)));

        List<Advertisement> result = advertisementRepository
                .findAllByCategoryAndExpiresAtAfterOrderByCreatedAtDesc(AdCategory.SALE, Instant.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(AdCategory.SALE);
    }

    // --- expiry filtering ---

    @Test
    void findAllActive_excludesExpiredAds() {
        advertisementRepository.save(buildAd(AdCategory.SALE, Instant.now().plus(10, ChronoUnit.DAYS)));
        advertisementRepository.save(buildAd(AdCategory.JOB, Instant.now().minus(1, ChronoUnit.DAYS)));

        List<Advertisement> result = advertisementRepository
                .findAllByExpiresAtAfterOrderByCreatedAtDesc(Instant.now());

        assertThat(result).hasSize(1);
    }

    @Test
    void findExpiredAdsWithImages_returnsOnlyExpiredWithImages() {
        Advertisement expiredWithImage = buildAd(AdCategory.SALE, Instant.now().minus(1, ChronoUnit.DAYS));
        expiredWithImage.getImageUrls().add("ads/img.jpg");
        advertisementRepository.save(expiredWithImage);

        advertisementRepository.save(buildAd(AdCategory.JOB, Instant.now().minus(1, ChronoUnit.DAYS)));

        Advertisement activeWithImage = buildAd(AdCategory.SALE, Instant.now().plus(10, ChronoUnit.DAYS));
        activeWithImage.getImageUrls().add("ads/active.jpg");
        advertisementRepository.save(activeWithImage);

        List<Advertisement> result = advertisementRepository.findExpiredAdsWithImages(Instant.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(expiredWithImage.getId());
    }

    // --- dirty-tracking / transaction fix ---

    @Test
    void imageUrls_mutationPersistedByDirtyTracking() {
        // This test verifies the behaviour that AdvertisementService.persistImages relies on:
        // loading an entity, mutating imageUrls, and letting the transaction flush the change.
        // If persistImages were called without a transaction (via `this` instead of `self`),
        // the mutation would be lost and this pattern would silently fail.
        Advertisement ad = buildAd(AdCategory.SALE, Instant.now().plus(30, ChronoUnit.DAYS));
        advertisementRepository.save(ad);
        UUID adId = ad.getId();

        // Simulate what persistImages does inside its @Transactional boundary:
        // load, mutate (dirty-track), and let the session flush on commit.
        Advertisement managed = advertisementRepository.findById(adId).orElseThrow();
        managed.getImageUrls().add("ads/test-image.jpg");
        // No explicit save() — relies on dirty-tracking flush at transaction end.
        entityManager.flush();

        // Evict and reload to confirm the URL was actually written to DB.
        entityManager.clear();
        Advertisement reloaded = advertisementRepository.findById(adId).orElseThrow();
        assertThat(reloaded.getImageUrls()).containsExactly("ads/test-image.jpg");
    }

    // --- eventDate round-trip ---

    @Test
    void eventDate_persistsAndLoadsCorrectly() {
        Instant eventTime = Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Advertisement ad = buildAd(AdCategory.EVENT, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.setEventDate(eventTime);
        advertisementRepository.save(ad);
        entityManager.flush();

        entityManager.clear();
        Advertisement reloaded = advertisementRepository.findById(ad.getId()).orElseThrow();
        assertThat(reloaded.getEventDate()).isEqualTo(eventTime);
    }

    // --- helpers ---

    private Advertisement buildAd(AdCategory category, Instant expiresAt) {
        Advertisement ad = new Advertisement();
        ad.setOwnerId(owner.getId());
        ad.setCategory(category);
        ad.setTitle("Test ad");
        ad.setContent("Content");
        ad.setPrice(category == AdCategory.SALE ? BigDecimal.TEN : null);
        ad.setTags(new ArrayList<>());
        ad.setImageUrls(new ArrayList<>());
        ad.setExpiresAt(expiresAt);
        return ad;
    }
}
