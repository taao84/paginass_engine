package com.paginass.controller;

import com.paginass.dto.BusinessLikeDto;
import com.paginass.service.BusinessLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessLikeController {

    private final BusinessLikeService businessLikeService;

    @PostMapping("/{businessId}/like")
    public ResponseEntity<BusinessLikeDto> toggleLike(@PathVariable UUID businessId) {
        return ResponseEntity.ok(businessLikeService.toggleLike(businessId));
    }

    @GetMapping("/{businessId}/like-status")
    public ResponseEntity<BusinessLikeDto> getLikeStatus(@PathVariable UUID businessId) {
        return ResponseEntity.ok(businessLikeService.getLikeStatus(businessId));
    }
}
