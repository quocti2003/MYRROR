package com.mirror.product.enums;

/**
 * Production capability types that vendors/partners can perform
 * Used for flexible role assignment in production workflows
 */
public enum PartnerCapabilityType {
    STONE_SOURCING,     // Procure raw/rough stones
    STONE_CUTTING,      // Cut rough stones to shape
    STONE_POLISHING,    // Polish cut stones
    SIDE_STONE_PREP,    // Prepare side/melee stones
    METAL_CASTING,      // Cast metal settings/mountings
    METAL_FINISHING,    // Finish metal (polish, texture, engrave)
    SETTING,            // Set stones into metal mountings
    ASSEMBLY,           // Final assembly of components
    QUALITY_CHECK,      // Quality control inspection
    PACKAGING           // Final packaging for delivery
}
