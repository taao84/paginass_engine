package com.paginass.dto.permissions;

import lombok.Data;

import java.util.List;

@Data
public class PermissionsMatrixDto {
    private List<ResourcePermissionsDto> resources;
}
