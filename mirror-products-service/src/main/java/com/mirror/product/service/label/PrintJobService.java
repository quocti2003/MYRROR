package com.mirror.product.service.label;

import com.mirror.product.dto.label.*;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.label.*;
import com.mirror.product.enums.PrintJobItemStatus;
import com.mirror.product.enums.PrintJobStatus;
import com.mirror.product.repository.MirrorProductRepository;
import com.mirror.product.repository.label.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrintJobService {

    private final PrintJobRepository printJobRepository;
    private final PrintJobItemRepository printJobItemRepository;
    private final RFIDLabelTemplateService templateService;
    private final MirrorProductRepository productRepository;
    private final ZPLGeneratorService zplGenerator;
    private final RFIDTagService rfidTagService;

    /**
     * Create a new print job
     */
    @Transactional
    public PrintJobResponse create(PrintJobCreateRequest request, String userId) {
        // Validate template
        LabelTemplate template = templateService.getEntityById(request.getTemplateId());

        // Validate products
        List<MirrorProduct> products = productRepository.findAllById(request.getProductIds());
        if (products.size() != request.getProductIds().size()) {
            throw new IllegalArgumentException("Some products not found");
        }

        // Determine options
        boolean includeRFID = request.getOptions() != null &&
            Boolean.TRUE.equals(request.getOptions().getIncludeRFID());
        int copies = request.getOptions() != null && request.getOptions().getCopies() != null ?
            request.getOptions().getCopies() : 1;

        // Create print job
        Map<String, Object> options = new HashMap<>();
        if (request.getOptions() != null) {
            options.put("includeRFID", includeRFID);
            options.put("copies", copies);
            options.put("darkness", request.getOptions().getDarkness());
            options.put("speed", request.getOptions().getSpeed());
        }

        PrintJob printJob = PrintJob.builder()
            .templateId(request.getTemplateId())
            .status(PrintJobStatus.PENDING)
            .totalLabels(products.size() * copies)
            .printedLabels(0)
            .failedLabels(0)
            .printerName(request.getPrinterName())
            .printMethod(request.getPrintMethod())
            .options(options)
            .createdBy(userId)
            .build();

        printJob = printJobRepository.save(printJob);

        // Generate ZPL and create items
        StringBuilder combinedZpl = new StringBuilder();
        List<PrintJobItem> items = new ArrayList<>();

        for (MirrorProduct product : products) {
            for (int i = 0; i < copies; i++) {
                String epc = includeRFID ? zplGenerator.generateEPC(product.getId(), product.getBarcode()) : null;
                String zpl = zplGenerator.generateZPL(template, product, includeRFID);

                PrintJobItem item = PrintJobItem.builder()
                    .printJobId(printJob.getId())
                    .productId(product.getId())
                    .epc(epc)
                    .zplData(zpl)
                    .status(PrintJobItemStatus.PENDING)
                    .build();

                items.add(item);
                combinedZpl.append(zpl);
            }
        }

        printJobItemRepository.saveAll(items);
        printJob.setZplData(combinedZpl.toString());
        printJob.setItems(items);
        printJobRepository.save(printJob);

        log.info("Created print job {} with {} items by user {}", printJob.getId(), items.size(), userId);
        return PrintJobResponse.fromEntity(printJob, true);
    }

    /**
     * Get print job by ID
     */
    public PrintJobResponse getById(String id, boolean includeItems) {
        PrintJob printJob = printJobRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Print job not found: " + id));
        return PrintJobResponse.fromEntity(printJob, includeItems);
    }

    /**
     * Get all print jobs with filters
     */
    public Page<PrintJobResponse> getAll(PrintJobStatus status, String templateId,
                                          Instant startDate, Instant endDate, Pageable pageable) {
        Page<PrintJob> jobs = printJobRepository.findWithFilters(status, templateId, startDate, endDate, pageable);
        return jobs.map(j -> PrintJobResponse.fromEntity(j, false));
    }

    /**
     * Start printing a job (mark as PRINTING)
     */
    @Transactional
    public void startPrinting(String id) {
        PrintJob printJob = printJobRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Print job not found: " + id));

        if (printJob.getStatus() != PrintJobStatus.PENDING) {
            throw new IllegalArgumentException("Can only start printing for PENDING jobs");
        }

        printJob.start();
        printJobRepository.save(printJob);
        log.info("Started printing job {}", id);
    }

    /**
     * Complete a print job (mark as COMPLETED)
     */
    @Transactional
    public void completePrinting(String id) {
        PrintJob printJob = printJobRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Print job not found: " + id));

        if (printJob.getStatus() != PrintJobStatus.PRINTING) {
            throw new IllegalArgumentException("Can only complete PRINTING jobs");
        }

        printJob.complete();
        printJobRepository.save(printJob);
        log.info("Completed printing job {}", id);
    }

    /**
     * Force complete a print job (for stuck jobs)
     */
    @Transactional
    public void forceComplete(String id, String userId) {
        PrintJob printJob = printJobRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Print job not found: " + id));

        printJob.complete();
        printJobRepository.save(printJob);
        log.info("Force completed print job {} by user {}", id, userId);
    }

    /**
     * Reset a stuck job back to PENDING
     */
    @Transactional
    public void resetToPending(String id, String userId) {
        PrintJob printJob = printJobRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Print job not found: " + id));

        printJob.setStatus(PrintJobStatus.PENDING);
        printJob.setPrintedLabels(0);
        printJob.setFailedLabels(0);
        printJob.setStartedAt(null);
        printJob.setCompletedAt(null);
        printJobRepository.save(printJob);
        log.info("Reset print job {} to PENDING by user {}", id, userId);
    }

    /**
     * Cancel a print job
     */
    @Transactional
    public void cancel(String id, String userId) {
        PrintJob printJob = printJobRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new RuntimeException("Print job not found: " + id));

        if (printJob.getStatus() == PrintJobStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed job");
        }

        printJob.cancel();
        printJobRepository.save(printJob);
        log.info("Cancelled print job {} by user {}", id, userId);
    }

    /**
     * Mark item as printed and register RFID tag
     */
    @Transactional
    public void markItemPrinted(String itemId, String userId) {
        PrintJobItem item = printJobItemRepository.findByIdAndIsDeletedFalse(itemId)
            .orElseThrow(() -> new RuntimeException("Print job item not found: " + itemId));

        item.markPrinted();
        printJobItemRepository.save(item);

        // Update job counts
        PrintJob job = printJobRepository.findByIdAndIsDeletedFalse(item.getPrintJobId())
            .orElseThrow(() -> new RuntimeException("Print job not found"));
        job.incrementPrinted();
        printJobRepository.save(job);

        // Register RFID tag if EPC is present
        if (item.getEpc() != null && !item.getEpc().isEmpty()) {
            try {
                RFIDTagRegisterRequest tagRequest = RFIDTagRegisterRequest.builder()
                    .epc(item.getEpc())
                    .productId(item.getProductId())
                    .printJobId(job.getId())
                    .printJobItemId(item.getId())
                    .build();
                rfidTagService.register(tagRequest);
            } catch (Exception e) {
                log.warn("Failed to register RFID tag for item {}: {}", itemId, e.getMessage());
            }
        }

        log.info("Marked item {} as printed", itemId);
    }

    /**
     * Mark item as failed
     */
    @Transactional
    public void markItemFailed(String itemId, String error) {
        PrintJobItem item = printJobItemRepository.findByIdAndIsDeletedFalse(itemId)
            .orElseThrow(() -> new RuntimeException("Print job item not found: " + itemId));

        item.markFailed(error);
        printJobItemRepository.save(item);

        // Update job counts
        PrintJob job = printJobRepository.findByIdAndIsDeletedFalse(item.getPrintJobId())
            .orElseThrow(() -> new RuntimeException("Print job not found"));
        job.incrementFailed();
        printJobRepository.save(job);

        log.info("Marked item {} as failed: {}", itemId, error);
    }

    /**
     * Retry failed items
     */
    @Transactional
    public PrintJobResponse retryFailed(String jobId, String userId) {
        PrintJob job = printJobRepository.findByIdAndIsDeletedFalse(jobId)
            .orElseThrow(() -> new RuntimeException("Print job not found: " + jobId));

        List<PrintJobItem> failedItems = printJobItemRepository.findFailedItems(jobId);
        if (failedItems.isEmpty()) {
            throw new IllegalArgumentException("No failed items to retry");
        }

        // Reset failed items
        for (PrintJobItem item : failedItems) {
            item.setStatus(PrintJobItemStatus.PENDING);
            item.setErrorMessage(null);
        }
        printJobItemRepository.saveAll(failedItems);

        // Update job
        job.setStatus(PrintJobStatus.PENDING);
        job.setFailedLabels(0);
        job.setErrorMessage(null);
        printJobRepository.save(job);

        log.info("Retrying {} failed items in job {} by user {}", failedItems.size(), jobId, userId);
        return PrintJobResponse.fromEntity(job, true);
    }

    /**
     * Generate ZPL for products without creating a job
     */
    public LabelGenerateResponse generateZPL(LabelGenerateRequest request) {
        LabelTemplate template = templateService.getEntityById(request.getTemplateId());
        List<MirrorProduct> products = productRepository.findAllById(request.getProductIds());

        if (products.size() != request.getProductIds().size()) {
            throw new IllegalArgumentException("Some products not found");
        }

        boolean includeRFID = request.getOptions() != null &&
            Boolean.TRUE.equals(request.getOptions().getIncludeRFID());

        List<LabelGenerateResponse.LabelData> labels = new ArrayList<>();
        StringBuilder combinedZpl = new StringBuilder();

        for (MirrorProduct product : products) {
            String epc = includeRFID ? zplGenerator.generateEPC(product.getId(), product.getBarcode()) : null;
            String zpl = zplGenerator.generateZPL(template, product, includeRFID);

            labels.add(LabelGenerateResponse.LabelData.builder()
                .productId(product.getId())
                .productName(product.getItemName())
                .sku(product.getSkuCode())
                .barcode(product.getBarcode())
                .epc(epc)
                .zpl(zpl)
                .build());

            combinedZpl.append(zpl);
        }

        return LabelGenerateResponse.builder()
            .totalLabels(labels.size())
            .combinedZpl(combinedZpl.toString())
            .labels(labels)
            .build();
    }
}
