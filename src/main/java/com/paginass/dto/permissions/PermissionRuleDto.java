package com.paginass.dto.permissions;

import lombok.Data;

@Data
public class PermissionRuleDto {
    private String operation;
    private String contributor;
    private String editor;
    private String owner;
    private String admin;
}
