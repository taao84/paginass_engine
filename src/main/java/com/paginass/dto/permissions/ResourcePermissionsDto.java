package com.paginass.dto.permissions;

import lombok.Data;

import java.util.List;

@Data
public class ResourcePermissionsDto {
    private String name;
    private List<PermissionRuleDto> rules;
}
