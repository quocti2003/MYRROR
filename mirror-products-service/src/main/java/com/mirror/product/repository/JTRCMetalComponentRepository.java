package com.mirror.product.repository;

import com.mirror.product.entity.JTRCMetalComponent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JTRCMetalComponentRepository extends BaseRepository<JTRCMetalComponent, String> {

    @Override
    @Query("SELECT m FROM JTRCMetalComponent m WHERE m.id = :id AND m.isActive = true AND m.isDeleted = false")
    Optional<JTRCMetalComponent> findActiveById(@Param("id") String id);

    @Query("SELECT m FROM JTRCMetalComponent m WHERE m.jtrc.id = :jtrcId AND m.isActive = true AND m.isDeleted = false")
    Optional<JTRCMetalComponent> findActiveByJtrcId(@Param("jtrcId") String jtrcId);

    @Query("SELECT COUNT(m) > 0 FROM JTRCMetalComponent m WHERE m.jtrc.id = :jtrcId AND m.isActive = true AND m.isDeleted = false")
    boolean existsActiveByJtrcId(@Param("jtrcId") String jtrcId);
}
