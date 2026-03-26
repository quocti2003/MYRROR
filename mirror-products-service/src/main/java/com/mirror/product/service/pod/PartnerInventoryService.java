package com.mirror.product.service.pod;

import com.mirror.product.dto.pod.*;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.pod.InventoryMovement;
import com.mirror.product.entity.pod.PartnerInventory;
import com.mirror.product.entity.pod.PodPartner;
import com.mirror.product.enums.InventoryMovementType;
import com.mirror.product.exception.pod.*;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.pod.InventoryMovementRepository;
import com.mirror.product.repository.pod.PartnerInventoryRepository;
import com.mirror.product.repository.pod.PodPartnerRepository;
import com.mirror.product.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PartnerInventoryService extends BaseService<PartnerInventory, String> {

    private static final Set<InventoryMovementType> MANUAL_MOVEMENT_TYPES = EnumSet.of(
            InventoryMovementType.ADJUSTMENT_IN,
            InventoryMovementType.ADJUSTMENT_OUT,
            InventoryMovementType.DAMAGED_OUT,
            InventoryMovementType.RETURN_IN,
            InventoryMovementType.TRANSFER_OUT
    );

    private final PartnerInventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final PodPartnerRepository partnerRepository;
    private final MirrorProductRepository productRepository;

    public PartnerInventoryService(
            PartnerInventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            PodPartnerRepository partnerRepository,
            MirrorProductRepository productRepository
    ) {
        super(inventoryRepository);
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.partnerRepository = partnerRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public PartnerInventory update(String id, PartnerInventory entity) {
        PartnerInventory existing = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found: " + id));
        existing.setReorderLevel(entity.getReorderLevel());
        existing.setMaxStockLevel(entity.getMaxStockLevel());
        return inventoryRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Page<PartnerInventoryResponse> getInventory(String partnerId, Pageable pageable) {
        validatePhygitalPartner(partnerId);
        return inventoryRepository.findByPartnerIdAndIsDeletedFalse(partnerId, pageable)
                .map(PartnerInventoryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PartnerInventoryResponse getInventoryItem(String partnerId, String inventoryId) {
        PartnerInventory inventory = inventoryRepository.findByIdAndPartnerIdAndIsDeletedFalse(inventoryId, partnerId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory item not found: " + inventoryId));
        return PartnerInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public PartnerInventoryResponse addInventoryItem(String partnerId, InventoryAddRequest request) {
        log.info("Adding inventory item for partner: {}, product: {}", partnerId, request.getProductId());
        PodPartner partner = validatePhygitalPartner(partnerId);

        // Check if already exists
        inventoryRepository.findByPartnerIdAndProductIdAndIsDeletedFalse(partnerId, request.getProductId())
                .ifPresent(existing -> {
                    throw new InvalidPricingException("Inventory already exists for this product. Use adjust endpoint instead.");
                });

        MirrorProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Product not found: " + request.getProductId()));

        BigDecimal retailPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal wholesalePrice = partner.getWholesalePrice(retailPrice);

        PartnerInventory inventory = PartnerInventory.builder()
                .partnerId(partnerId)
                .productId(request.getProductId())
                .quantityOnHand(request.getInitialQuantity() != null ? request.getInitialQuantity() : 0)
                .wholesalePrice(wholesalePrice)
                .partnerRetailPrice(request.getPartnerRetailPrice())
                .mirrorRetailPrice(retailPrice)
                .reorderLevel(request.getReorderLevel() != null ? request.getReorderLevel() : 5)
                .maxStockLevel(request.getMaxStockLevel() != null ? request.getMaxStockLevel() : 50)
                .build();

        if (request.getPartnerRetailPrice() != null) {
            validatePricing(partner, wholesalePrice, request.getPartnerRetailPrice());
        }

        inventory = inventoryRepository.save(inventory);

        // Record initial stock movement if quantity > 0
        if (request.getInitialQuantity() != null && request.getInitialQuantity() > 0) {
            recordMovement(inventory, InventoryMovementType.WHOLESALE_IN,
                    request.getInitialQuantity(), 0, request.getInitialQuantity(),
                    null, null, "Initial stock", null);
        }

        log.info("Inventory item created: {}", inventory.getId());
        return PartnerInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public PartnerInventoryResponse updateRetailPrice(String partnerId, String inventoryId, InventoryPriceUpdateRequest request) {
        PartnerInventory inventory = inventoryRepository.findByIdAndPartnerIdAndIsDeletedFalse(inventoryId, partnerId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory item not found: " + inventoryId));
        PodPartner partner = partnerRepository.findActiveById(inventory.getPartnerId())
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found"));

        if (!Boolean.TRUE.equals(partner.getCanSetOwnPrices())) {
            throw new InvalidPricingException("This partner is not allowed to set custom prices");
        }

        validatePricing(partner, inventory.getWholesalePrice(), request.getPartnerRetailPrice());
        inventory.setPartnerRetailPrice(request.getPartnerRetailPrice());
        inventory = inventoryRepository.save(inventory);

        log.info("Retail price updated for inventory: {}", inventoryId);
        return PartnerInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public PartnerInventoryResponse adjustStock(String partnerId, String inventoryId, InventoryAdjustRequest request, String adjustedBy) {
        PartnerInventory inventory = inventoryRepository.findByIdAndPartnerIdAndIsDeletedFalse(inventoryId, partnerId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory item not found: " + inventoryId));

        if (!MANUAL_MOVEMENT_TYPES.contains(request.getMovementType())) {
            throw new InvalidOrderStateException("Invalid movement type for manual adjustment: " + request.getMovementType());
        }

        int quantityBefore = inventory.getQuantityOnHand();
        int quantityAfter;

        switch (request.getMovementType()) {
            case ADJUSTMENT_IN, RETURN_IN:
                quantityAfter = quantityBefore + request.getQuantity();
                break;
            case ADJUSTMENT_OUT, DAMAGED_OUT, TRANSFER_OUT:
                if (inventory.getQuantityAvailable() < request.getQuantity()) {
                    throw new InsufficientStockException("Insufficient stock. Available: " + inventory.getQuantityAvailable());
                }
                quantityAfter = quantityBefore - request.getQuantity();
                break;
            default:
                throw new InvalidOrderStateException("Invalid movement type for manual adjustment: " + request.getMovementType());
        }

        inventory.setQuantityOnHand(quantityAfter);
        inventory = inventoryRepository.save(inventory);

        recordMovement(inventory, request.getMovementType(),
                request.getQuantity(), quantityBefore, quantityAfter,
                null, null, request.getNotes(), adjustedBy);

        log.info("Stock adjusted for inventory: {}, {} -> {}", inventoryId, quantityBefore, quantityAfter);
        return PartnerInventoryResponse.fromEntity(inventory);
    }

    @Transactional
    public void receiveWholesaleOrder(String orderId, String partnerId, List<WholesaleOrderItemResponse> items) {
        log.info("Receiving wholesale order {} for partner {}", orderId, partnerId);
        for (WholesaleOrderItemResponse item : items) {
            PartnerInventory inventory = inventoryRepository
                    .findByPartnerIdAndProductIdForUpdate(partnerId, item.getProductId())
                    .orElse(null);

            if (inventory == null) {
                // Auto-create inventory record for new product
                inventory = PartnerInventory.builder()
                        .partnerId(partnerId)
                        .productId(item.getProductId())
                        .quantityOnHand(0)
                        .wholesalePrice(item.getWholesalePrice())
                        .mirrorRetailPrice(item.getRetailPrice())
                        .reorderLevel(5)
                        .maxStockLevel(50)
                        .build();
                inventory = inventoryRepository.save(inventory);
                log.info("Auto-created inventory record {} for product {} (wholesale order {})",
                        inventory.getId(), item.getProductId(), orderId);
            }

            int quantityBefore = inventory.getQuantityOnHand();
            int quantityAfter = quantityBefore + item.getQuantity();

            inventory.setQuantityOnHand(quantityAfter);
            inventory.setWholesalePrice(item.getWholesalePrice());
            inventory.setLastRestockedAt(Instant.now());
            inventoryRepository.save(inventory);

            recordMovement(inventory, InventoryMovementType.WHOLESALE_IN,
                    item.getQuantity(), quantityBefore, quantityAfter,
                    orderId, "WHOLESALE_ORDER", "Wholesale order received", null);
        }
    }

    @Transactional
    public void deductForSale(String saleId, String partnerId, String productId, int quantity) {
        PartnerInventory inventory = inventoryRepository
                .findByPartnerIdAndProductIdForUpdate(partnerId, productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));

        if (inventory.getQuantityAvailable() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product " + productId +
                    ". Available: " + inventory.getQuantityAvailable() + ", Requested: " + quantity);
        }

        int quantityBefore = inventory.getQuantityOnHand();
        int quantityAfter = quantityBefore - quantity;
        inventory.setQuantityOnHand(quantityAfter);
        inventoryRepository.save(inventory);

        recordMovement(inventory, InventoryMovementType.SALE_OUT,
                quantity, quantityBefore, quantityAfter,
                saleId, "PARTNER_SALE", "Sale deduction", null);
    }

    @Transactional
    public void restoreForCancelledSale(String saleId, String partnerId, String productId, int quantity) {
        PartnerInventory inventory = inventoryRepository
                .findByPartnerIdAndProductIdForUpdate(partnerId, productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));

        int quantityBefore = inventory.getQuantityOnHand();
        int quantityAfter = quantityBefore + quantity;
        inventory.setQuantityOnHand(quantityAfter);
        inventoryRepository.save(inventory);

        recordMovement(inventory, InventoryMovementType.RETURN_IN,
                quantity, quantityBefore, quantityAfter,
                saleId, "PARTNER_SALE", "Sale cancelled/returned - stock restored", null);
    }

    @Transactional
    public int recalculateWholesalePrices(String partnerId) {
        PodPartner partner = validatePhygitalPartner(partnerId);
        List<PartnerInventory> items = inventoryRepository.findByPartnerIdAndIsDeletedFalse(partnerId);
        int updated = 0;
        for (PartnerInventory item : items) {
            BigDecimal newWholesale = partner.getWholesalePrice(item.getMirrorRetailPrice());
            if (newWholesale != null && item.getWholesalePrice().compareTo(newWholesale) != 0) {
                log.info("Recalculating wholesale price for {}: {} -> {}", item.getId(), item.getWholesalePrice(), newWholesale);
                item.setWholesalePrice(newWholesale);
                inventoryRepository.save(item);
                updated++;
            }
        }
        log.info("Recalculated wholesale prices for partner {}: {} items updated", partnerId, updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<PartnerInventoryResponse> getLowStockAlerts(String partnerId) {
        return inventoryRepository.findLowStockByPartner(partnerId).stream()
                .map(PartnerInventoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getInventoryValue(String partnerId) {
        return inventoryRepository.calculateInventoryValue(partnerId);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getMovementHistory(String partnerId, String inventoryId, Pageable pageable) {
        // Verify ownership
        inventoryRepository.findByIdAndPartnerIdAndIsDeletedFalse(inventoryId, partnerId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory item not found: " + inventoryId));
        return movementRepository.findByInventoryIdAndIsDeletedFalseOrderByCreatedAtDesc(inventoryId, pageable)
                .map(InventoryMovementResponse::fromEntity);
    }

    // === PRIVATE HELPERS ===

    private PodPartner validatePhygitalPartner(String partnerId) {
        PodPartner partner = partnerRepository.findActiveById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException("Partner not found: " + partnerId));
        if (!partner.isPhygitalPartner()) {
            throw new UnauthorizedPartnerAccessException("This feature is only available for Phygital partners");
        }
        return partner;
    }

    private void validatePricing(PodPartner partner, BigDecimal wholesalePrice, BigDecimal retailPrice) {
        if (wholesalePrice.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal markup = retailPrice.subtract(wholesalePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(wholesalePrice, 2, RoundingMode.HALF_UP);

        if (partner.getMinMarkupPercent() != null && markup.compareTo(partner.getMinMarkupPercent()) < 0) {
            throw new InvalidPricingException("Retail price too low. Minimum markup: " + partner.getMinMarkupPercent() + "%");
        }
        if (partner.getMaxMarkupPercent() != null && markup.compareTo(partner.getMaxMarkupPercent()) > 0) {
            throw new InvalidPricingException("Retail price too high. Maximum markup: " + partner.getMaxMarkupPercent() + "%");
        }
    }

    private void recordMovement(PartnerInventory inventory, InventoryMovementType type,
                                 int quantity, int before, int after,
                                 String referenceId, String referenceType, String notes, String createdBy) {
        InventoryMovement movement = InventoryMovement.builder()
                .inventoryId(inventory.getId())
                .partnerId(inventory.getPartnerId())
                .productId(inventory.getProductId())
                .movementType(type)
                .quantity(quantity)
                .quantityBefore(before)
                .quantityAfter(after)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .notes(notes)
                .createdBy(createdBy)
                .build();
        movementRepository.save(movement);
    }
}
