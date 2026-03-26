package com.mirror.product.service.label;

import com.mirror.product.enums.PrintMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Service for sending ZPL data to printers via different methods:
 * - NETWORK: TCP/IP raw printing (LAN/WiFi) - port 9100
 * - USB/SPOOLER: Windows Print Spooler via printer name
 */
@Service
@Slf4j
public class PrinterService {

    private static final int DEFAULT_PRINTER_PORT = 9100;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    /**
     * Send ZPL to printer based on print method
     */
    public PrintResult sendToPrinter(String zplData, PrintMethod method, String printerAddress) {
        if (zplData == null || zplData.isEmpty()) {
            return PrintResult.failure("ZPL data is empty");
        }
        if (printerAddress == null || printerAddress.isEmpty()) {
            return PrintResult.failure("Printer address is required");
        }

        return switch (method) {
            case NETWORK -> sendViaNetwork(zplData, printerAddress);
            case USB, SPOOLER -> sendViaSpooler(zplData, printerAddress);
        };
    }

    /**
     * Send ZPL via TCP/IP network (for LAN/WiFi connected printers)
     * Printer address format: "192.168.1.100" or "192.168.1.100:9100"
     */
    public PrintResult sendViaNetwork(String zplData, String printerAddress) {
        String host;
        int port = DEFAULT_PRINTER_PORT;

        // Parse host:port format
        if (printerAddress.contains(":")) {
            String[] parts = printerAddress.split(":");
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return PrintResult.failure("Invalid port number: " + parts[1]);
            }
        } else {
            host = printerAddress;
        }

        log.info("Sending ZPL to printer via network: {}:{}", host, port);

        try (Socket socket = new Socket()) {
            // Connect with timeout
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            // Send ZPL data
            try (OutputStream out = socket.getOutputStream()) {
                byte[] data = zplData.getBytes(StandardCharsets.UTF_8);
                out.write(data);
                out.flush();
            }

            log.info("Successfully sent {} bytes to printer {}:{}", zplData.length(), host, port);
            return PrintResult.success("Sent to printer " + host + ":" + port);

        } catch (IOException e) {
            String errorMsg = "Failed to send to printer " + host + ":" + port + " - " + e.getMessage();
            log.error(errorMsg, e);
            return PrintResult.failure(errorMsg);
        }
    }

    /**
     * Send ZPL via Windows Print Spooler (for USB connected printers)
     * Printer address is the printer name as shown in Windows (e.g., "Zebra ZD421")
     */
    public PrintResult sendViaSpooler(String zplData, String printerName) {
        log.info("Sending ZPL to printer via spooler: {}", printerName);

        try {
            // Find the printer by name
            PrintService printService = findPrintService(printerName);
            if (printService == null) {
                String availablePrinters = getAvailablePrinters();
                return PrintResult.failure("Printer not found: " + printerName +
                    ". Available printers: " + availablePrinters);
            }

            // Create print job
            DocPrintJob printJob = printService.createPrintJob();

            // Create document with ZPL data
            byte[] data = zplData.getBytes(StandardCharsets.UTF_8);
            Doc doc = new SimpleDoc(data, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);

            // Print attributes
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();

            // Send to printer
            printJob.print(doc, attributes);

            log.info("Successfully sent {} bytes to printer {} via spooler", data.length, printerName);
            return PrintResult.success("Sent to printer " + printerName + " via spooler");

        } catch (PrintException e) {
            String errorMsg = "Print error for " + printerName + ": " + e.getMessage();
            log.error(errorMsg, e);
            return PrintResult.failure(errorMsg);
        }
    }

    /**
     * Find print service by name
     */
    private PrintService findPrintService(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName) ||
                service.getName().toLowerCase().contains(printerName.toLowerCase())) {
                return service;
            }
        }
        return null;
    }

    /**
     * Get list of available printers
     */
    public String getAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < services.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(services[i].getName());
        }
        return sb.toString();
    }

    /**
     * Get list of available printers as array
     */
    public String[] getAvailablePrintersArray() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        String[] names = new String[services.length];
        for (int i = 0; i < services.length; i++) {
            names[i] = services[i].getName();
        }
        return names;
    }

    /**
     * Test printer connection
     */
    public PrintResult testConnection(PrintMethod method, String printerAddress) {
        if (method == PrintMethod.NETWORK) {
            return testNetworkConnection(printerAddress);
        } else {
            return testSpoolerConnection(printerAddress);
        }
    }

    /**
     * Test network printer connection
     */
    private PrintResult testNetworkConnection(String printerAddress) {
        String host;
        int port = DEFAULT_PRINTER_PORT;

        if (printerAddress.contains(":")) {
            String[] parts = printerAddress.split(":");
            host = parts[0];
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return PrintResult.failure("Invalid port: " + parts[1]);
            }
        } else {
            host = printerAddress;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            return PrintResult.success("Connected to " + host + ":" + port);
        } catch (IOException e) {
            return PrintResult.failure("Cannot connect to " + host + ":" + port + " - " + e.getMessage());
        }
    }

    /**
     * Test spooler printer connection
     */
    private PrintResult testSpoolerConnection(String printerName) {
        PrintService service = findPrintService(printerName);
        if (service != null) {
            return PrintResult.success("Found printer: " + service.getName());
        } else {
            return PrintResult.failure("Printer not found: " + printerName +
                ". Available: " + getAvailablePrinters());
        }
    }

    /**
     * Print result holder
     */
    public record PrintResult(boolean success, String message) {
        public static PrintResult success(String message) {
            return new PrintResult(true, message);
        }
        public static PrintResult failure(String message) {
            return new PrintResult(false, message);
        }
    }
}
