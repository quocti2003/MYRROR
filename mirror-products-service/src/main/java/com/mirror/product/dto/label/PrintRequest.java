package com.mirror.product.dto.label;

import com.mirror.product.enums.PrintMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to send print job to printer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintRequest {

    /**
     * Print method: NETWORK (LAN/WiFi), USB, SPOOLER
     */
    @Builder.Default
    private PrintMethod printMethod = PrintMethod.NETWORK;

    /**
     * Printer address:
     * - For NETWORK: IP address (e.g., "192.168.1.100" or "192.168.1.100:9100")
     * - For USB/SPOOLER: Printer name as shown in Windows (e.g., "Zebra ZD421")
     */
    @NotBlank(message = "Printer address is required")
    private String printerAddress;
}
