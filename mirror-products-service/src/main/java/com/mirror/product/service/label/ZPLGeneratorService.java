package com.mirror.product.service.label;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirror.product.entity.MirrorProduct;
import com.mirror.product.entity.label.LabelTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for generating ZPL (Zebra Programming Language) commands
 * from label templates and product data.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ZPLGeneratorService {

    private final ObjectMapper objectMapper;

    // Company prefix for EPC generation (Mirror Diamond)
    private static final String COMPANY_PREFIX = "0319135410";

    /**
     * Generate ZPL from template and product
     */
    public String generateZPL(LabelTemplate template, MirrorProduct product, boolean includeRFID) {
        StringBuilder zpl = new StringBuilder();

        int widthDots = template.getWidthInDots();
        int heightDots = template.getHeightInDots();

        // Start label
        zpl.append("^XA\n");
        zpl.append(String.format("^PW%d\n", widthDots));
        zpl.append(String.format("^LL%d\n", heightDots));
        zpl.append("^LH0,0\n");

        // RFID encoding (if enabled and tag available)
        if (includeRFID) {
            String epc = generateEPC(product.getId(), product.getBarcode());
            log.info("Generated EPC for product {}: {}", product.getId(), epc);

            // RFID setup for UHF tags
            zpl.append("^RS8\n"); // Retry 8 times on RFID errors
            zpl.append("^RR3\n"); // RFID read retries

            // Write EPC to RFID tag - using EPC memory bank (bank 1)
            // Format: ^RFW,a,b,c,d where:
            // a = data format (H=hex, A=ASCII)
            // b = starting block (1=EPC memory bank)
            // c = number of bytes to write
            // For 96-bit EPC = 12 bytes = 24 hex chars
            zpl.append("^RFW,H,1,12^FD").append(epc).append("^FS\n");

            // Alternative format for some printers: direct EPC write
            // zpl.append("^RFW,E^FD").append(epc).append("^FS\n");
        }

        // Parse canvas JSON and generate ZPL for each element
        boolean hasContent = false;
        try {
            String canvasJson = template.getCanvasJson();
            if (canvasJson != null && !canvasJson.isEmpty()) {
                JsonNode canvas = objectMapper.readTree(canvasJson);
                JsonNode objects = canvas.get("objects");

                if (objects != null && objects.isArray() && objects.size() > 0) {
                    for (JsonNode obj : objects) {
                        String elementZpl = generateElementZPL(obj, product, template.getDpi());
                        if (elementZpl != null && !elementZpl.isEmpty()) {
                            zpl.append(elementZpl);
                            hasContent = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing canvas JSON: {}", e.getMessage());
        }

        // Fallback: If no content from canvas, generate default barcode label
        if (!hasContent) {
            log.warn("No content from canvas, generating default barcode label");
            String barcode = product.getBarcode();
            if (barcode == null || barcode.isEmpty()) {
                barcode = product.getSkuCode();
            }
            if (barcode != null && !barcode.isEmpty()) {
                // Default barcode at center
                int barcodeX = widthDots / 4;
                int barcodeY = heightDots / 4;
                zpl.append(String.format("^FO%d,%d\n", barcodeX, barcodeY));
                // EAN-13 if 13 digits, else Code128
                if (barcode.length() == 13 && barcode.matches("\\d+")) {
                    zpl.append("^BEN,80,Y,N\n");
                } else {
                    zpl.append("^BCN,80,Y,N,N\n");
                }
                zpl.append(String.format("^FD%s^FS\n", barcode));
            }
        }

        // End label
        zpl.append("^PQ1\n");
        zpl.append("^XZ\n");

        return zpl.toString();
    }

    /**
     * Generate ZPL for a single canvas element
     */
    private String generateElementZPL(JsonNode element, MirrorProduct product, int dpi) {
        StringBuilder zpl = new StringBuilder();
        String type = element.has("type") ? element.get("type").asText() : "";

        int x = mmToDots(element.has("left") ? element.get("left").asDouble() : 0, dpi);
        int y = mmToDots(element.has("top") ? element.get("top").asDouble() : 0, dpi);

        switch (type) {
            case "i-text":
            case "text":
                zpl.append(generateTextZPL(element, product, x, y));
                break;

            case "image":
                // Check if it's a barcode
                if (element.has("barcodeType")) {
                    zpl.append(generateBarcodeZPL(element, product, x, y));
                } else if (element.has("qrValue") || "qrcode".equals(element.path("elementType").asText())) {
                    zpl.append(generateQRCodeZPL(element, product, x, y));
                }
                break;

            case "rect":
                zpl.append(generateRectZPL(element, x, y, dpi));
                break;

            case "line":
                zpl.append(generateLineZPL(element, x, y, dpi));
                break;

            default:
                // Unknown element type, skip
                break;
        }

        return zpl.toString();
    }

    /**
     * Generate ZPL for text element
     */
    private String generateTextZPL(JsonNode element, MirrorProduct product, int x, int y) {
        StringBuilder zpl = new StringBuilder();

        String text = element.has("text") ? element.get("text").asText() : "";
        int fontSize = element.has("fontSize") ? element.get("fontSize").asInt() : 24;

        // Handle data binding
        String dataBinding = element.has("dataBinding") ? element.get("dataBinding").asText() : null;
        if (dataBinding != null && !dataBinding.isEmpty()) {
            text = resolveDataBinding(dataBinding, product);
        } else {
            // Check for template syntax {fieldName}
            text = replaceTemplateVariables(text, product);
        }

        // Convert font size to ZPL units (approximate)
        int zplFontHeight = (int) (fontSize * 0.75);
        int zplFontWidth = zplFontHeight;

        zpl.append(String.format("^FO%d,%d", x, y));
        zpl.append(String.format("^A0N,%d,%d", zplFontHeight, zplFontWidth));
        zpl.append(String.format("^FD%s^FS\n", escapeZPL(text)));

        return zpl.toString();
    }

    /**
     * Generate ZPL for barcode element
     */
    private String generateBarcodeZPL(JsonNode element, MirrorProduct product, int x, int y) {
        StringBuilder zpl = new StringBuilder();

        String barcodeType = element.has("barcodeType") ? element.get("barcodeType").asText() : "CODE128";
        String value = element.has("barcodeValue") ? element.get("barcodeValue").asText() : "";
        int height = element.has("barcodeHeight") ? element.get("barcodeHeight").asInt() : 50;

        // Handle data binding
        String dataBinding = element.has("dataBinding") ? element.get("dataBinding").asText() : null;
        if (dataBinding != null && !dataBinding.isEmpty()) {
            value = resolveDataBinding(dataBinding, product);
        }

        zpl.append(String.format("^FO%d,%d", x, y));

        switch (barcodeType.toUpperCase()) {
            case "EAN13":
            case "EAN-13":
                zpl.append(String.format("^BEN,%d,Y,N", height));
                break;
            case "EAN8":
            case "EAN-8":
                zpl.append(String.format("^B8N,%d,Y,N", height));
                break;
            case "UPCA":
            case "UPC-A":
                zpl.append(String.format("^BUN,%d,Y,N,Y", height));
                break;
            case "CODE128":
            default:
                zpl.append(String.format("^BCN,%d,Y,N,N", height));
                break;
        }

        zpl.append(String.format("^FD%s^FS\n", value));

        return zpl.toString();
    }

    /**
     * Generate ZPL for QR code element
     */
    private String generateQRCodeZPL(JsonNode element, MirrorProduct product, int x, int y) {
        StringBuilder zpl = new StringBuilder();

        String value = element.has("qrValue") ? element.get("qrValue").asText() : "";
        int magnification = element.has("qrMagnification") ? element.get("qrMagnification").asInt() : 5;

        // Handle data binding
        String dataBinding = element.has("dataBinding") ? element.get("dataBinding").asText() : null;
        if (dataBinding != null && !dataBinding.isEmpty()) {
            value = resolveDataBinding(dataBinding, product);
        } else {
            value = replaceTemplateVariables(value, product);
        }

        zpl.append(String.format("^FO%d,%d", x, y));
        zpl.append(String.format("^BQN,2,%d", magnification));
        zpl.append(String.format("^FDQA,%s^FS\n", value));

        return zpl.toString();
    }

    /**
     * Generate ZPL for rectangle
     */
    private String generateRectZPL(JsonNode element, int x, int y, int dpi) {
        StringBuilder zpl = new StringBuilder();

        int width = mmToDots(element.has("width") ? element.get("width").asDouble() : 10, dpi);
        int height = mmToDots(element.has("height") ? element.get("height").asDouble() : 10, dpi);
        int thickness = element.has("strokeWidth") ? element.get("strokeWidth").asInt() : 1;

        zpl.append(String.format("^FO%d,%d", x, y));
        zpl.append(String.format("^GB%d,%d,%d^FS\n", width, height, thickness));

        return zpl.toString();
    }

    /**
     * Generate ZPL for line
     */
    private String generateLineZPL(JsonNode element, int x, int y, int dpi) {
        StringBuilder zpl = new StringBuilder();

        int x2 = mmToDots(element.has("x2") ? element.get("x2").asDouble() : 0, dpi);
        int y2 = mmToDots(element.has("y2") ? element.get("y2").asDouble() : 0, dpi);
        int thickness = element.has("strokeWidth") ? element.get("strokeWidth").asInt() : 1;

        int width = Math.abs(x2 - x);
        int height = Math.max(Math.abs(y2 - y), thickness);

        if (width == 0) width = thickness;
        if (height == 0) height = thickness;

        zpl.append(String.format("^FO%d,%d", Math.min(x, x2), Math.min(y, y2)));
        zpl.append(String.format("^GB%d,%d,%d^FS\n", width, height, thickness));

        return zpl.toString();
    }

    /**
     * Resolve data binding to actual product value
     */
    private String resolveDataBinding(String binding, MirrorProduct product) {
        if (product == null || binding == null) return "";

        switch (binding.toLowerCase()) {
            case "name":
                return product.getItemName() != null ? product.getItemName() : "";
            case "sku":
            case "skucode":
                return product.getSkuCode() != null ? product.getSkuCode() : "";
            case "barcode":
                return product.getBarcode() != null ? product.getBarcode() : "";
            case "price":
                return product.getPrice() != null ? formatPrice(product.getPrice()) : "";
            case "description":
                return product.getDescription() != null ? product.getDescription() : "";
            case "category":
                return product.getCategory() != null ? product.getCategory() : "";
            case "qrurl":
            case "qr_url":
                return "https://mirror.vn/p/" + product.getId();
            case "id":
                return product.getId() != null ? product.getId() : "";
            default:
                return "";
        }
    }

    /**
     * Replace {fieldName} template variables
     */
    private String replaceTemplateVariables(String text, MirrorProduct product) {
        if (text == null || product == null) return text;

        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String binding = matcher.group(1);
            String value = resolveDataBinding(binding, product);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Generate SGTIN-96 EPC from product info
     */
    public String generateEPC(String productId, String sku) {
        // SGTIN-96 format
        String header = "30"; // SGTIN-96 header (0x30)
        int filter = 3; // Consumer item
        int partition = 5;

        // Extract item reference from SKU (numeric part)
        String itemRef = "0000000";
        if (sku != null) {
            String numericPart = sku.replaceAll("[^0-9]", "");
            if (numericPart.length() > 0) {
                itemRef = numericPart.substring(0, Math.min(7, numericPart.length()));
            }
        }
        itemRef = String.format("%07d", Integer.parseInt(itemRef.isEmpty() ? "0" : itemRef));

        // Serial from product ID (numeric part)
        String serial = "00000000000";
        if (productId != null) {
            String numericPart = productId.replaceAll("[^0-9]", "");
            if (numericPart.length() > 0) {
                serial = String.format("%011d", Long.parseLong(numericPart));
            }
        }

        // Build binary string
        StringBuilder binary = new StringBuilder();
        binary.append(String.format("%8s", Integer.toBinaryString(Integer.parseInt(header, 16))).replace(' ', '0'));
        binary.append(String.format("%3s", Integer.toBinaryString(filter)).replace(' ', '0'));
        binary.append(String.format("%3s", Integer.toBinaryString(partition)).replace(' ', '0'));
        binary.append(String.format("%24s", new BigInteger(COMPANY_PREFIX).toString(2)).replace(' ', '0'));
        binary.append(String.format("%20s", new BigInteger(itemRef).toString(2)).replace(' ', '0'));
        binary.append(String.format("%38s", new BigInteger(serial).toString(2)).replace(' ', '0'));

        // Convert to hex
        BigInteger binaryInt = new BigInteger(binary.toString(), 2);
        return String.format("%024X", binaryInt);
    }

    /**
     * Decode EPC to extract product info
     */
    public EPCData decodeEPC(String epcHex) {
        if (epcHex == null || epcHex.length() != 24) {
            return null;
        }

        try {
            BigInteger epcInt = new BigInteger(epcHex, 16);
            String binary = String.format("%96s", epcInt.toString(2)).replace(' ', '0');

            String header = binary.substring(0, 8);
            int filter = Integer.parseInt(binary.substring(8, 11), 2);
            int partition = Integer.parseInt(binary.substring(11, 14), 2);
            String companyPrefix = new BigInteger(binary.substring(14, 38), 2).toString();
            String itemRef = new BigInteger(binary.substring(38, 58), 2).toString();
            String serial = new BigInteger(binary.substring(58, 96), 2).toString();

            return new EPCData(
                Integer.parseInt(header, 2),
                filter,
                partition,
                companyPrefix,
                itemRef,
                serial
            );
        } catch (Exception e) {
            log.error("Error decoding EPC: {}", epcHex, e);
            return null;
        }
    }

    /**
     * Convert mm to dots
     */
    private int mmToDots(double mm, int dpi) {
        return (int) Math.round(mm / 25.4 * dpi);
    }

    /**
     * Format price for display
     */
    private String formatPrice(java.math.BigDecimal price) {
        if (price == null) return "";
        return String.format("%,.0f VND", price);
    }

    /**
     * Escape special characters for ZPL
     */
    private String escapeZPL(String text) {
        if (text == null) return "";
        // Replace special ZPL characters
        return text
            .replace("^", "")
            .replace("~", "")
            .replace("\\", "");
    }

    /**
     * EPC data holder
     */
    public record EPCData(
        int header,
        int filter,
        int partition,
        String companyPrefix,
        String itemReference,
        String serialNumber
    ) {}
}
