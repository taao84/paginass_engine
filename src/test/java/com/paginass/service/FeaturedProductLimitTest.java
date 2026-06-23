package com.paginass.service;

import com.paginass.dto.FeaturedProductCreateRequestDto;
import com.paginass.dto.FeaturedProductsListDto;
import com.paginass.entity.BusinessConfiguration;
import com.paginass.entity.FeaturedProduct;
import com.paginass.repository.BusinessConfigurationRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.FeaturedProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@ExtendWith(MockitoExtension.class)
class FeaturedProductLimitTest {

    private static final int GLOBAL_DEFAULT = 6;

    @Mock private FeaturedProductRepository featuredProductRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private BusinessConfigurationRepository businessConfigurationRepository;
    @Mock private StorageService storageService;
    @Mock private SettingsService settingsService;

    @InjectMocks
    private FeaturedProductService featuredProductService;

    private UUID businessId;
    private MockMultipartFile validImage;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        validImage = new MockMultipartFile(
                "image", "product.jpg", "image/jpeg", new byte[1024]);
        lenient().when(settingsService.getMaxProductImageMb()).thenReturn(8);
        lenient().when(settingsService.getMaxProductImageBytes()).thenReturn(8L * 1024 * 1024);
    }

    // -------------------------------------------------------------------------
    // resolveMaxProducts via getHighlightedBusinessProducts
    // -------------------------------------------------------------------------

    @Test
    void resolveMaxProducts_usesGlobalDefaultWhenNoLimitsRowExists() {
        when(featuredProductRepository.findByBusinessIdOrderByDisplayOrderAscCreatedAtAsc(businessId))
                .thenReturn(Collections.emptyList());
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.empty());

        FeaturedProductsListDto result = featuredProductService.getHighlightedBusinessProducts(businessId);

        assertThat(result.maxAllowed()).isEqualTo(GLOBAL_DEFAULT);
    }

    @Test
    void resolveMaxProducts_usesGlobalDefaultWhenRowExistsWithNullColumn() {
        BusinessConfiguration limitsWithNullValue = new BusinessConfiguration();
        limitsWithNullValue.setBusinessId(businessId);
        limitsWithNullValue.setMaxFeaturedProducts(null);

        when(featuredProductRepository.findByBusinessIdOrderByDisplayOrderAscCreatedAtAsc(businessId))
                .thenReturn(Collections.emptyList());
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(limitsWithNullValue));

        FeaturedProductsListDto result = featuredProductService.getHighlightedBusinessProducts(businessId);

        assertThat(result.maxAllowed()).isEqualTo(GLOBAL_DEFAULT);
    }

    @Test
    void resolveMaxProducts_usesPerBusinessOverrideWhenSet() {
        BusinessConfiguration limitsWithOverride = new BusinessConfiguration();
        limitsWithOverride.setBusinessId(businessId);
        limitsWithOverride.setMaxFeaturedProducts(10);

        when(featuredProductRepository.findByBusinessIdOrderByDisplayOrderAscCreatedAtAsc(businessId))
                .thenReturn(Collections.emptyList());
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(limitsWithOverride));

        FeaturedProductsListDto result = featuredProductService.getHighlightedBusinessProducts(businessId);

        assertThat(result.maxAllowed()).isEqualTo(10);
    }

    // -------------------------------------------------------------------------
    // createHighlightedBusinessProduct — limit enforcement
    // -------------------------------------------------------------------------

    @Test
    void createProduct_enforcesGlobalDefaultLimitOf6() {
        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.empty());
        when(featuredProductRepository.countByBusinessId(businessId)).thenReturn(6L);

        FeaturedProductCreateRequestDto request =
                new FeaturedProductCreateRequestDto("7th Product", null, null);

        assertThatThrownBy(() ->
                featuredProductService.createHighlightedBusinessProduct(businessId, request, validImage))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).contains("6");
                });

        verify(featuredProductRepository, never()).save(any());
    }

    @Test
    void createProduct_enforcesPerBusinessOverrideLimit() {
        BusinessConfiguration limits = new BusinessConfiguration();
        limits.setBusinessId(businessId);
        limits.setMaxFeaturedProducts(12);

        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(limits));
        when(featuredProductRepository.countByBusinessId(businessId)).thenReturn(12L);

        FeaturedProductCreateRequestDto request =
                new FeaturedProductCreateRequestDto("13th Product", null, null);

        assertThatThrownBy(() ->
                featuredProductService.createHighlightedBusinessProduct(businessId, request, validImage))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(UNPROCESSABLE_ENTITY);
                    assertThat(rse.getReason()).contains("12");
                });

        verify(featuredProductRepository, never()).save(any());
    }

    @Test
    void createProduct_succeedsAtExactlyPerBusinessLimit() throws Exception {
        BusinessConfiguration limits = new BusinessConfiguration();
        limits.setBusinessId(businessId);
        limits.setMaxFeaturedProducts(12);

        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(limits));
        // 11 products already saved — the 12th is allowed
        when(featuredProductRepository.countByBusinessId(businessId)).thenReturn(11L);
        when(storageService.saveFile(anyString(), any(), anyString())).thenReturn(null);
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/img.jpg");

        FeaturedProduct savedProduct = new FeaturedProduct();
        savedProduct.setId(UUID.randomUUID());
        savedProduct.setBusinessId(businessId);
        savedProduct.setTitle("12th Product");
        savedProduct.setDisplayOrder(11);
        savedProduct.setImageSubpath("products/" + businessId + "/abc.jpg");
        when(featuredProductRepository.save(any(FeaturedProduct.class))).thenReturn(savedProduct);

        FeaturedProductCreateRequestDto request =
                new FeaturedProductCreateRequestDto("12th Product", null, new BigDecimal("5000.00"));

        var result = featuredProductService.createHighlightedBusinessProduct(businessId, request, validImage);

        assertThat(result.title()).isEqualTo("12th Product");
        verify(featuredProductRepository).save(any(FeaturedProduct.class));
    }

    @Test
    void createProduct_returnsCorrectEffectiveLimitInListDto() {
        BusinessConfiguration limits = new BusinessConfiguration();
        limits.setBusinessId(businessId);
        limits.setMaxFeaturedProducts(12);

        when(featuredProductRepository.findByBusinessIdOrderByDisplayOrderAscCreatedAtAsc(businessId))
                .thenReturn(List.of());
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(limits));

        FeaturedProductsListDto dto = featuredProductService.getHighlightedBusinessProducts(businessId);

        assertThat(dto.maxAllowed()).isEqualTo(12);
    }
}
