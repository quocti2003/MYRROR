package com.mirror.product.util;

import com.mirror.product.enums.EntityPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class SequenceIdGenerator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    private void initializeSequences() {
        for (EntityPrefix prefix : EntityPrefix.values()) {
            String sequenceName = prefix.name().toLowerCase() + "_seq";
            try {
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS " + sequenceName + " START 1");
            } catch (Exception e) {
                // Sequence might already exist, ignore
            }
        }
    }

    public String generateId(EntityPrefix entityPrefix) {
        String sequenceName = entityPrefix.name().toLowerCase() + "_seq";
        Long sequenceValue = jdbcTemplate.queryForObject(
            "SELECT nextval(?)",
            Long.class,
            sequenceName
        );
        return entityPrefix.generateId(sequenceValue);
    }

    /**
     * Generate ID with custom prefix and sequence name.
     * Used for entities that don't have an EntityPrefix enum value.
     */
    public String generateIdWithPrefix(String prefix, String sequenceName) {
        Long sequenceValue = jdbcTemplate.queryForObject(
            "SELECT nextval(?)",
            Long.class,
            sequenceName
        );
        return String.format("%s%06d", prefix, sequenceValue);
    }
}