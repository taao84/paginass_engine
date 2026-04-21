package com.paginass.service;

import com.paginass.dto.permissions.PermissionsMatrixDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Service
public class PermissionsService {

    @Value("classpath:security/permissions-policy.yml")
    private Resource policyResource;

    private PermissionsMatrixDto matrix;

    @PostConstruct
    public void load() throws IOException {
        Yaml yaml = new Yaml(new Constructor(PermissionsMatrixDto.class, new LoaderOptions()));
        try (InputStream is = policyResource.getInputStream()) {
            matrix = yaml.loadAs(is, PermissionsMatrixDto.class);
        }
    }

    public PermissionsMatrixDto getMatrix() {
        return matrix;
    }
}
