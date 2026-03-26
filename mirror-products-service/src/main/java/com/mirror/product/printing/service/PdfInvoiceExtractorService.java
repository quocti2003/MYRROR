package com.mirror.product.printing.service;

import com.mirror.product.printing.dto.InvoiceDataDto;
import com.mirror.product.printing.dto.InvoiceItemDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfInvoiceExtractorService {

    public InvoiceDataDto extractInvoice(MultipartFile file) throws IOException {
        byte[] pdfBytes = file.getBytes();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.getNumberOfPages() == 0) {
                throw new IllegalArgumentException("PDF has no pages");
            }

            // Extract text
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String fullText = stripper.getText(document);

            // Extract tables using Tabula
            List<InvoiceItemDto> items = extractTableItems(document);

            // Extract fields using regex
            String invoiceDate = extractDate(fullText);
            String taxCode = extractField(fullText, "M[aã]\\s*s[oố]\\s*thu[eế]:\\s*(\\d+)");
            String address = extractField(fullText, "[ĐD][iị]a\\s*ch[iỉ]:\\s*([^\\n]+)");
            String phone = extractField(fullText, "[ĐD]i[eệ]n\\s*tho[aạ]i:\\s*(\\d+)");
            String invoiceCode = extractField(fullText, "K[yý]\\s*hi[eệ]u:\\s*(\\w+)");
            String invoiceNumber = extractField(fullText, "S[oố]:\\s*([^\\n]+)");
            String customerId = extractField(fullText, "CCCD\\s*ng[uư][oờ]i\\s*mua:\\s*(\\d+)");
            String customerPhone = extractField(fullText, "[ĐD]i[eệ]n\\s*tho[aạ]i:\\s*(\\d+).*CCCD");

            String customerName = extractField(fullText, "H[oọ]\\s*t[eê]n\\s*ng[uư][oờ]i\\s*mua\\s*h[aà]ng:\\s*([^\\n]+)");
            if (customerName.isEmpty()) {
                customerName = extractField(fullText, "T[eê]n\\s*ng[uư][oờ]i\\s*mua:\\s*([^\\n]+)");
            }

            String customerCompany = extractField(fullText, "T[eê]n\\s*[đd][oơ]n\\s*v[iị]:\\s*([^\\n]+)");
            String customerAddress = extractField(fullText, "[ĐD][iị]a\\s*ch[iỉ]\\s*ng[uư][oờ]i\\s*mua:\\s*([^\\n]+)");

            String paymentMethod = extractField(fullText, "H[iì]nh\\s*th[uứ]c\\s*thanh\\s*to[aá]n:\\s*([^\\n]+)");
            if (paymentMethod.isEmpty()) {
                paymentMethod = "TM/CK";
            }

            Long subtotal = extractMoney(fullText, "C[oộ]ng\\s*ti[eề]n.*?:\\s*([\\d\\.]+)");
            String totalInWords = extractField(fullText, "S[oố]\\s*ti[eề]n\\s*vi[eế]t\\s*b[aằ]ng\\s*ch[uữ]:\\s*([^\\n]+)");

            return InvoiceDataDto.builder()
                    .invoiceCode(invoiceCode.isEmpty() ? "2C25MYY" : invoiceCode)
                    .invoiceNumber(invoiceNumber)
                    .invoiceDate(invoiceDate)
                    .paymentMethod(paymentMethod)
                    .customerCompany(customerCompany.isEmpty() ? "Khach le khong lay hoa don" : customerCompany)
                    .customerName(customerName.isEmpty() ? "Khach le khong lay hoa don" : customerName)
                    .customerTaxCode(taxCode)
                    .customerAddress(customerAddress)
                    .customerPhone(customerPhone)
                    .customerIdNumber(customerId)
                    .items(items)
                    .subtotal(subtotal != null ? subtotal : 0L)
                    .totalInWords(totalInWords)
                    .build();
        }
    }

    private List<InvoiceItemDto> extractTableItems(PDDocument document) {
        List<InvoiceItemDto> items = new ArrayList<>();

        try (ObjectExtractor extractor = new ObjectExtractor(document)) {
            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
            Page page = extractor.extract(1);
            List<Table> tables = sea.extract(page);

            if (tables.isEmpty()) {
                return items;
            }

            Table table = tables.get(0);
            List<List<RectangularTextContainer>> rows = table.getRows();

            // Skip header row
            for (int i = 1; i < rows.size(); i++) {
                List<RectangularTextContainer> row = rows.get(i);
                if (row.isEmpty()) continue;

                String sttStr = getCellText(row, 0);
                if (sttStr.isEmpty()) continue;

                int stt;
                try {
                    stt = Integer.parseInt(sttStr.trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                String name = getCellText(row, 1).replace("\n", " ").replaceAll("\\s+", " ").trim();
                String unit = row.size() > 2 ? getCellText(row, 2).trim() : "cai";
                if (unit.isEmpty()) unit = "cai";

                int quantity = 1;
                if (row.size() > 3) {
                    try {
                        quantity = (int) Double.parseDouble(getCellText(row, 3).trim());
                    } catch (NumberFormatException e) {
                        quantity = 1;
                    }
                }

                Long unitPrice = null;
                if (row.size() > 4) {
                    unitPrice = parseMoney(getCellText(row, 4));
                }

                Long total = null;
                if (row.size() > 5) {
                    total = parseMoney(getCellText(row, 5));
                }

                items.add(InvoiceItemDto.builder()
                        .stt(stt)
                        .name(name)
                        .unit(unit)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .total(total)
                        .build());
            }
        } catch (Exception e) {
            // Table extraction failed, return empty list
        }

        return items;
    }

    private String getCellText(List<RectangularTextContainer> row, int index) {
        if (index >= row.size()) return "";
        RectangularTextContainer cell = row.get(index);
        return cell != null ? cell.getText() : "";
    }

    private String extractDate(String text) {
        Pattern pattern = Pattern.compile("Ng[aà]y\\s*(\\d+)\\s*th[aá]ng\\s*(\\d+)\\s*n[aă]m\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + "/" + matcher.group(2) + "/" + matcher.group(3);
        }
        return "";
    }

    private String extractField(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private Long extractMoney(String text, String regex) {
        String value = extractField(text, regex);
        return parseMoney(value);
    }

    private Long parseMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String cleaned = value.replaceAll("[^\\d]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
