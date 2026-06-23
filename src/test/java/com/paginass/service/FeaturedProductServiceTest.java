package com.paginass.service;

import com.paginass.dto.FeaturedProductCreateRequestDto;
import com.paginass.dto.FeaturedProductDto;
import com.paginass.dto.FeaturedProductsListDto;
import com.paginass.entity.FeaturedProduct;
import com.paginass.repository.BusinessConfigurationRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.FeaturedProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeaturedProductServiceTest {

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
    void setup() {
        businessId = UUID.randomUUID();
        validImage = new MockMultipartFile(
                "image", "product.jpg", "image/jpeg", new byte[1024]);
        lenient().when(settingsService.getMaxProductImageMb()).thenReturn(8);
        lenient().when(settingsService.getMaxProductImageBytes()).thenReturn(8L * 1024 * 1024);
    }

    @Test
    void getProducts_returnsEmptyListWhenNoneExist() {
        when(featuredProductRepository.findByBusinessIdOrderByDisplayOrderAscCreatedAtAsc(businessId))
                .thenReturn(List.of());

        FeaturedProductsListDto result = featuredProductService.getHighlightedBusinessProducts(businessId);

        assertThat(result.products()).isEmpty();
    }

    @Test
    void createProduct_savesProductAndReturnsDto() throws IOException {
        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(featuredProductRepository.countByBusinessId(businessId)).thenReturn(2L);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.empty());
        when(storageService.saveFile(anyString(), any(), anyString())).thenReturn(null);
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/products/img.jpg");

        FeaturedProduct saved = new FeaturedProduct();
        saved.setId(UUID.randomUUID());
        saved.setBusinessId(businessId);
        saved.setTitle("Producto 1");
        saved.setDescription("Desc");
        saved.setPrice(new BigDecimal("9900.00"));
        saved.setImageSubpath("products/" + businessId + "/abc.jpg");
        saved.setDisplayOrder(2);
        when(featuredProductRepository.save(any(FeaturedProduct.class))).thenReturn(saved);

        var request = new FeaturedProductCreateRequestDto("Producto 1", "Desc", new BigDecimal("9900.00"));
        FeaturedProductDto dto = featuredProductService.createHighlightedBusinessProduct(businessId, request, validImage);

        assertThat(dto.title()).isEqualTo("Producto 1");
        assertThat(dto.imageUrl()).isEqualTo("https://cdn.example.com/products/img.jpg");

        ArgumentCaptor<FeaturedProduct> captor = ArgumentCaptor.forClass(FeaturedProduct.class);
        verify(featuredProductRepository).save(captor.capture());
        assertThat(captor.getValue().getBusinessId()).isEqualTo(businessId);
    }

    @Test
    void createProduct_rejectsWhenLimitReached() {
        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(featuredProductRepository.countByBusinessId(businessId)).thenReturn(6L);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.empty());

        var request = new FeaturedProductCreateRequestDto("Extra", null, null);

        assertThatThrownBy(() -> featuredProductService.createHighlightedBusinessProduct(businessId, request, validImage))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Maximum of 6");

        verify(featuredProductRepository, never()).save(any());
    }

    @Test
    void createProduct_rejectsTooLargeImage() {
        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(featuredProductRepository.countByBusinessId(businessId)).thenReturn(0L);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.empty());

        byte[] bigBytes = new byte[9 * 1024 * 1024]; // 9 MB — exceeds the 8 MB default
        MockMultipartFile bigImage = new MockMultipartFile("image", "big.jpg", "image/jpeg", bigBytes);

        var request = new FeaturedProductCreateRequestDto("Product", null, null);

        assertThatThrownBy(() -> featuredProductService.createHighlightedBusinessProduct(businessId, request, bigImage))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("8 MB");
    }

    @Test
    void deleteProduct_removesEntityAndStorageFile() throws IOException {
        UUID productId = UUID.randomUUID();
        FeaturedProduct product = new FeaturedProduct();
        product.setId(productId);
        product.setBusinessId(businessId);
        product.setImageSubpath("products/" + businessId + "/img.jpg");

        when(featuredProductRepository.findById(productId)).thenReturn(Optional.of(product));

        featuredProductService.deleteHighlightedBusinessProduct(businessId, productId);

        verify(featuredProductRepository).delete(product);
        verify(storageService).deleteFile(eq("products/" + businessId + "/img.jpg"));
    }

    @Test
    void deleteProduct_throwsWhenProductBelongsToDifferentBusiness() {
        UUID productId = UUID.randomUUID();
        FeaturedProduct product = new FeaturedProduct();
        product.setId(productId);
        product.setBusinessId(UUID.randomUUID()); // different business

        when(featuredProductRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> featuredProductService.deleteHighlightedBusinessProduct(businessId, productId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");

        verify(featuredProductRepository, never()).delete(any());
    }

    @Test
    void deleteProduct_doesNotFailWhenStorageDeletionThrows() throws IOException {
        UUID productId = UUID.randomUUID();
        FeaturedProduct product = new FeaturedProduct();
        product.setId(productId);
        product.setBusinessId(businessId);
        product.setImageSubpath("products/" + businessId + "/img.jpg");

        when(featuredProductRepository.findById(productId)).thenReturn(Optional.of(product));
        org.mockito.Mockito.doThrow(new IOException("R2 unavailable"))
                .when(storageService).deleteFile(anyString());

        // Should not propagate the IOException
        featuredProductService.deleteHighlightedBusinessProduct(businessId, productId);

        verify(featuredProductRepository).delete(product);
    }
}
