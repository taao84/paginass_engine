package com.paginass.controller;

import com.paginass.dto.AdminUserDto;
import com.paginass.dto.AdminUserPageDto;
import com.paginass.entity.UserRole;
import com.paginass.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<AdminUserPageDto> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(adminUserService.listUsers(search, role, enabled, page));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserDto> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UserRole newRole = UserRole.valueOf(body.get("role").toUpperCase());
        return ResponseEntity.ok(adminUserService.updateRole(id, newRole));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<AdminUserDto> setEnabled(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(adminUserService.setEnabled(id, body.get("enabled")));
    }

    @PostMapping("/{id}/temporary-password")
    public ResponseEntity<Map<String, String>> generateTemporaryPassword(@PathVariable UUID id) {
        String tempPassword = adminUserService.generateTemporaryPassword(id);
        return ResponseEntity.ok(Map.of("temporaryPassword", tempPassword));
    }

    @PostMapping("/{id}/password-reset-email")
    public ResponseEntity<Void> sendPasswordResetEmail(@PathVariable UUID id) {
        adminUserService.initiatePasswordReset(id);
        return ResponseEntity.noContent().build();
    }
}
