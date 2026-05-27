package com.paginass.service;

import com.paginass.dto.FeaturedProductCreateRequestDto;
import com.paginass.dto.FeaturedProductDto;
import com.paginass.entity.FeaturedProduct;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.FeaturedProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class FeaturedProductService {

    private static final int MAX_PRODUCTS = 6;
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;

    private final FeaturedProductRepository featuredProductRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<FeaturedProductDto> getHighlightedBusinessProducts(UUID businessId) {
        return featuredProductRepository
                .findByBusinessIdOrderByDisplayOrderAscCreatedAtAsc(businessId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("@bizAuth.isOwnerOrAdmin(#businessId)")
    @Transactional
    public FeaturedProductDto createHighlightedBusinessProduct(UUID businessId,
                                            FeaturedProductCreateRequestDto request,
                                            MultipartFile image) {
        if (!businessProfileRepository.existsById(businessId)) {
            throw new ResponseStatusException(NOT_FOUND, "Business not found");
        }
        if (featuredProductRepository.countByBusinessId(businessId) >= MAX_PRODUCTS) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Maximum of " + MAX_PRODUCTS + " featured products allowed");
        }
        validateImage(image);

        String subPath = saveImage(businessId, image);

        FeaturedProduct product = new FeaturedProduct();
        product.setBusinessId(businessId);
        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImageSubpath(subPath);
        product.setDisplayOrder((int) featuredProductRepository.countByBusinessId(businessId));

        return toDto(featuredProductRepository.save(product));
    }

    @PreAuthorize("@bizAuth.isOwnerOrAdmin(#businessId)")
    @Transactional
    public void deleteHighlightedBusinessProduct(UUID businessId, UUID productId) {
        FeaturedProduct product = featuredProductRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found"));

        if (!product.getBusinessId().equals(businessId)) {
            throw new ResponseStatusException(FORBIDDEN, "Product does not belong to this business");
        }

        String subPath = product.getImageSubpath();
        featuredProductRepository.delete(product);

        if (subPath != null) {
            try {
                storageService.deleteFile(subPath);
            } catch (IOException e) {
                // Log but don't fail — the DB record is already removed
            }
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Product image is required");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(BAD_REQUEST, "Image must be 3 MB or smaller");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith(MediaType.IMAGE_JPEG.getType() + "/")
                && !contentType.startsWith("image/")) {
            throw new ResponseStatusException(BAD_REQUEST, "Only image files are accepted");
        }
    }

    private String saveImage(UUID businessId, MultipartFile image) {
        String originalFilename = image.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : ".jpg";
        String subPath = "products/" + businessId + "/" + UUID.randomUUID() + ext;
        try {
            storageService.saveFile(subPath, image.getInputStream(), image.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to store product image");
        }
        return subPath;
    }

    private FeaturedProductDto toDto(FeaturedProduct p) {
        String imageUrl = p.getImageSubpath() != null
                ? storageService.toPublicUrl(p.getImageSubpath())
                : null;
        return new FeaturedProductDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                imageUrl,
                p.getDisplayOrder()
        );
    }
}
