package com.mirror.product.mapper;

import com.mirror.product.dto.CertificateRequest;
import com.mirror.product.dto.CertificateResponse;
import com.mirror.product.entity.Certificate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CertificateMapper {

    public Certificate toEntity(CertificateRequest request) {
        if (request == null) {
            return null;
        }

        return Certificate.builder()
                .certificateCode(request.getCertificateCode())
                .certificateType(request.getCertificateType())
                .certificateUrl(request.getCertificateUrl())
                .build();
    }

    public CertificateResponse toResponse(Certificate entity) {
        if (entity == null) {
            return null;
        }

        CertificateResponse response = new CertificateResponse();
        response.setId(entity.getId());
        response.setCertificateCode(entity.getCertificateCode());
        response.setCertificateType(entity.getCertificateType());
        response.setCertificateUrl(entity.getCertificateUrl());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    public List<CertificateResponse> toResponseList(List<Certificate> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntity(Certificate entity, CertificateRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getCertificateCode() != null) {
            entity.setCertificateCode(request.getCertificateCode());
        }
        if (request.getCertificateType() != null) {
            entity.setCertificateType(request.getCertificateType());
        }
        if (request.getCertificateUrl() != null) {
            entity.setCertificateUrl(request.getCertificateUrl());
        }
    }
}
