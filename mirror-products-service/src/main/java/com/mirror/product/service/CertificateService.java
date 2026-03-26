package com.mirror.product.service;

import com.mirror.product.dto.CertificateRequest;
import com.mirror.product.dto.CertificateResponse;
import com.mirror.product.entity.Certificate;
import com.mirror.product.enums.CertificateType;
import com.mirror.product.mapper.CertificateMapper;
import com.mirror.product.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;

    /**
     * Create a new certificate
     */
    @Transactional
    public CertificateResponse create(CertificateRequest request) {
        // Check if certificate code already exists
        if (certificateRepository.existsByCertificateCode(request.getCertificateCode())) {
            throw new IllegalStateException("Certificate code already exists: " + request.getCertificateCode());
        }

        Certificate certificate = certificateMapper.toEntity(request);
        Certificate saved = certificateRepository.save(certificate);

        log.info("Created certificate {} with code {}", saved.getId(), saved.getCertificateCode());

        return certificateMapper.toResponse(saved);
    }

    /**
     * Get certificate by ID
     */
    @Transactional(readOnly = true)
    public Optional<CertificateResponse> getById(String id) {
        return certificateRepository.findActiveById(id)
                .map(certificateMapper::toResponse);
    }

    /**
     * Get certificate by certificate code
     */
    @Transactional(readOnly = true)
    public Optional<CertificateResponse> getByCode(String code) {
        return certificateRepository.findByCertificateCode(code)
                .map(certificateMapper::toResponse);
    }

    /**
     * Get all certificates
     */
    @Transactional(readOnly = true)
    public List<CertificateResponse> getAll() {
        return certificateMapper.toResponseList(certificateRepository.findAllActive());
    }

    /**
     * Get certificates by type
     */
    @Transactional(readOnly = true)
    public List<CertificateResponse> getByType(CertificateType type) {
        return certificateMapper.toResponseList(certificateRepository.findByType(type));
    }

    /**
     * Update certificate
     */
    @Transactional
    public Optional<CertificateResponse> update(String id, CertificateRequest request) {
        return certificateRepository.findActiveById(id)
                .map(certificate -> {
                    // Check if new certificate code already exists (excluding current certificate)
                    if (request.getCertificateCode() != null &&
                        !request.getCertificateCode().equals(certificate.getCertificateCode()) &&
                        certificateRepository.existsByCertificateCodeAndIdNot(request.getCertificateCode(), id)) {
                        throw new IllegalStateException("Certificate code already exists: " + request.getCertificateCode());
                    }

                    certificateMapper.updateEntity(certificate, request);
                    Certificate saved = certificateRepository.save(certificate);

                    log.info("Updated certificate {}", id);

                    return certificateMapper.toResponse(saved);
                });
    }

    /**
     * Delete certificate (soft delete)
     */
    @Transactional
    public boolean delete(String id) {
        return certificateRepository.findActiveById(id)
                .map(certificate -> {
                    certificate.setIsDeleted(true);
                    certificateRepository.save(certificate);
                    log.info("Deleted certificate {}", id);
                    return true;
                })
                .orElse(false);
    }
}
