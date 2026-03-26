package com.mirror.product.repository;

import com.mirror.product.entity.JTRCLaborComponent;
import com.mirror.product.enums.JTRCLaborType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface JTRCLaborComponentRepository extends BaseRepository<JTRCLaborComponent, String> {

    @Override
    @Query("SELECT l FROM JTRCLaborComponent l WHERE l.id = :id AND l.isActive = true AND l.isDeleted = false")
    Optional<JTRCLaborComponent> findActiveById(@Param("id") String id);

    @Query("SELECT l FROM JTRCLaborComponent l WHERE l.jtrc.id = :jtrcId AND l.isActive = true AND l.isDeleted = false ORDER BY l.laborType")
    List<JTRCLaborComponent> findActiveByJtrcId(@Param("jtrcId") String jtrcId);

    @Query("SELECT l FROM JTRCLaborComponent l WHERE l.jtrc.id = :jtrcId AND l.laborType = :laborType AND l.isActive = true AND l.isDeleted = false")
    List<JTRCLaborComponent> findActiveByJtrcIdAndLaborType(@Param("jtrcId") String jtrcId, @Param("laborType") JTRCLaborType laborType);

    @Query("SELECT COALESCE(SUM(l.cost), 0) FROM JTRCLaborComponent l WHERE l.jtrc.id = :jtrcId AND l.isActive = true AND l.isDeleted = false")
    BigDecimal sumCostByJtrcId(@Param("jtrcId") String jtrcId);

    @Query("SELECT COUNT(l) FROM JTRCLaborComponent l WHERE l.jtrc.id = :jtrcId AND l.isActive = true AND l.isDeleted = false")
    long countActiveByJtrcId(@Param("jtrcId") String jtrcId);
}
