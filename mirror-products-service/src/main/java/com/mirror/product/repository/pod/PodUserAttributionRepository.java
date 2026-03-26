package com.mirror.product.repository.pod;

import com.mirror.product.entity.pod.PodUserAttribution;
import com.mirror.product.enums.PodUserAttributionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PodUserAttributionRepository extends JpaRepository<PodUserAttribution, String>, JpaSpecificationExecutor<PodUserAttribution> {

    Optional<PodUserAttribution> findByUserIdAndPodIdAndStatus(Long userId, String podId, PodUserAttributionStatus status);

    List<PodUserAttribution> findByUserIdAndStatus(Long userId, PodUserAttributionStatus status);

    Page<PodUserAttribution> findByPartnerIdAndIsDeletedFalse(String partnerId, Pageable pageable);

    Page<PodUserAttribution> findByIsDeletedFalse(Pageable pageable);
}
