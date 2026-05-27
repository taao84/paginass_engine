package com.paginass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.dto.FeaturedProductCreateRequestDto;
import com.paginass.dto.FeaturedProductDto;
import com.paginass.service.FeaturedProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile/businesses/{id}/products")
@RequiredArgsConstructor
public class FeaturedProductController {

    private final FeaturedProductService featuredProductService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<FeaturedProductDto>> listHighlightedBusinessProducts(@PathVariable UUID id) {
        return ResponseEntity.ok(featuredProductService.getHighlightedBusinessProducts(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FeaturedProductDto createHighlightedBusinessProduct(
            @PathVariable UUID id,
            @RequestParam("data") String dataJson,
            @RequestParam("image") MultipartFile image) throws Exception {
        FeaturedProductCreateRequestDto request = objectMapper.readValue(dataJson, FeaturedProductCreateRequestDto.class);
        return featuredProductService.createHighlightedBusinessProduct(id, request, image);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHighlightedBusinessProduct(@PathVariable UUID id, @PathVariable UUID productId) {
        featuredProductService.deleteHighlightedBusinessProduct(id, productId);
    }
}
