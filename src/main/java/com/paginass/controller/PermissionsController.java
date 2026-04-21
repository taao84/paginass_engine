package com.paginass.controller;

import com.paginass.dto.permissions.PermissionsMatrixDto;
import com.paginass.service.PermissionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class PermissionsController {

    private final PermissionsService permissionsService;

    @GetMapping("/permissions")
    public ResponseEntity<PermissionsMatrixDto> getPermissions() {
        return ResponseEntity.ok(permissionsService.getMatrix());
    }
}
