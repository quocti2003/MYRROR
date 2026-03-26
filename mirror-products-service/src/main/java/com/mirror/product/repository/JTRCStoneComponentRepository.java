package com.mirror.product.repository;

import com.mirror.product.entity.JTRCStoneComponent;
import com.mirror.product.enums.StoneRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JTRCStoneComponentRepository extends BaseRepository<JTRCStoneComponent, String> {

    @Override
    @Query("SELECT s FROM JTRCStoneComponent s WHERE s.id = :id AND s.isActive = true AND s.isDeleted = false")
    Optional<JTRCStoneComponent> findActiveById(@Param("id") String id);

    @Query("SELECT s FROM JTRCStoneComponent s WHERE s.jtrc.id = :jtrcId AND s.isActive = true AND s.isDeleted = false ORDER BY s.stoneRole")
    List<JTRCStoneComponent> findActiveByJtrcId(@Param("jtrcId") String jtrcId);

    @Query("SELECT s FROM JTRCStoneComponent s WHERE s.jtrc.id = :jtrcId AND s.stoneRole = :stoneRole AND s.isActive = true AND s.isDeleted = false")
    List<JTRCStoneComponent> findActiveByJtrcIdAndStoneRole(@Param("jtrcId") String jtrcId, @Param("stoneRole") StoneRole stoneRole);

    @Query("SELECT COUNT(s) FROM JTRCStoneComponent s WHERE s.jtrc.id = :jtrcId AND s.isActive = true AND s.isDeleted = false")
    long countActiveByJtrcId(@Param("jtrcId") String jtrcId);

    @Query("SELECT s FROM JTRCStoneComponent s WHERE s.jtrc.id = :jtrcId AND s.stoneRole IN ('MAIN', 'MAIN_2') AND s.isActive = true AND s.isDeleted = false")
    List<JTRCStoneComponent> findMainStonesByJtrcId(@Param("jtrcId") String jtrcId);

    @Query("SELECT s FROM JTRCStoneComponent s WHERE s.jtrc.id = :jtrcId AND s.stoneRole IN ('SIDE', 'SIDE_2', 'MELEE') AND s.isActive = true AND s.isDeleted = false")
    List<JTRCStoneComponent> findAccentStonesByJtrcId(@Param("jtrcId") String jtrcId);
}
