/**
 * Stock Reconciliation Report Export Utility
 * Generates DOCX report following Vietnamese standard template "Biên bản tổng hợp kiểm kê"
 * Based on Mẫu số 05 - VT (Thông tư số 99/2025/TT-BTC)
 */

import {
  Document,
  Packer,
  Paragraph,
  TextRun,
  Table,
  TableRow,
  TableCell,
  AlignmentType,
  BorderStyle,
  WidthType,
  VerticalAlign,
  ShadingType,
  PageOrientation,
} from 'docx';
import { saveAs } from 'file-saver';

// Table border style
const tableBorder = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const cellBorders = { top: tableBorder, bottom: tableBorder, left: tableBorder, right: tableBorder };

// Helper to create a centered text paragraph
const centeredParagraph = (text, options = {}) => new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: options.spacingAfter || 0, before: options.spacingBefore || 0 },
  children: [new TextRun({ text, bold: options.bold || false, size: options.size || 22, font: "Times New Roman" })]
});

// Helper to create a left-aligned text paragraph
const leftParagraph = (text, options = {}) => new Paragraph({
  alignment: AlignmentType.LEFT,
  spacing: { after: options.spacingAfter || 0, before: options.spacingBefore || 0 },
  indent: options.indent ? { left: options.indent } : undefined,
  children: [new TextRun({ text, bold: options.bold || false, size: options.size || 22, font: "Times New Roman" })]
});

// Helper to create table cell with centered text
const centeredCell = (text, options = {}) => new TableCell({
  borders: cellBorders,
  width: options.width ? { size: options.width, type: WidthType.DXA } : undefined,
  shading: options.shading ? { fill: options.shading, type: ShadingType.CLEAR } : undefined,
  verticalAlign: VerticalAlign.CENTER,
  rowSpan: options.rowSpan,
  columnSpan: options.columnSpan,
  children: [new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: text?.toString() || "", bold: options.bold || false, size: options.size || 20, font: "Times New Roman" })]
  })]
});

// Helper to create table cell with left-aligned text
const leftCell = (text, options = {}) => new TableCell({
  borders: cellBorders,
  width: options.width ? { size: options.width, type: WidthType.DXA } : undefined,
  shading: options.shading ? { fill: options.shading, type: ShadingType.CLEAR } : undefined,
  verticalAlign: VerticalAlign.CENTER,
  rowSpan: options.rowSpan,
  columnSpan: options.columnSpan,
  children: [new Paragraph({
    alignment: AlignmentType.LEFT,
    children: [new TextRun({ text: text?.toString() || "", bold: options.bold || false, size: options.size || 20, font: "Times New Roman" })]
  })]
});

// Format number with thousand separators
const formatNumber = (num) => {
  if (num === null || num === undefined || num === "") return "";
  return new Intl.NumberFormat('vi-VN').format(num);
};

// Format currency
const formatCurrency = (num) => {
  if (num === null || num === undefined || num === "" || num === 0) return "";
  return new Intl.NumberFormat('vi-VN').format(num);
};

/**
 * Generate Stock Reconciliation Report
 * @param {Object} params - Report parameters
 * @param {string} params.companyName - Company name
 * @param {string} params.department - Department/Division
 * @param {string} params.documentNumber - Document number
 * @param {Date} params.inventoryDate - Date of inventory
 * @param {Array} params.committee - Array of committee members [{name, position, role}]
 * @param {Array} params.items - Array of inventory items
 * @param {string} params.warehouseName - Warehouse name
 */
export const generateStockReconciliationReport = async (params) => {
  const {
    companyName = "MIRROR DIAMOND",
    department = "Kho hàng",
    documentNumber = "",
    inventoryDate = new Date(),
    committee = [],
    items = [],
    warehouseName = ""
  } = params;

  // Format date
  const day = inventoryDate.getDate();
  const month = inventoryDate.getMonth() + 1;
  const year = inventoryDate.getFullYear();
  const hours = inventoryDate.getHours();
  const minutes = inventoryDate.getMinutes();

  // Calculate totals
  let totalBookAmount = 0;
  let totalPhysicalAmount = 0;
  let totalExcessAmount = 0;
  let totalMissingAmount = 0;

  items.forEach(item => {
    const bookAmount = (item.misaCount || 0) * (item.unitPrice || 0);
    const physicalAmount = (item.physicalCount || 0) * (item.unitPrice || 0);
    totalBookAmount += bookAmount;
    totalPhysicalAmount += physicalAmount;

    if (item.difference > 0) {
      totalExcessAmount += item.difference * (item.unitPrice || 0);
    } else if (item.difference < 0) {
      totalMissingAmount += Math.abs(item.difference) * (item.unitPrice || 0);
    }
  });

  // Column widths for landscape A4 (total usable ~14000 DXA)
  const colWidths = [
    500,   // STT
    2000,  // Tên hàng
    1200,  // Mã số
    600,   // ĐVT
    900,   // Đơn giá
    700,   // SL sổ
    1100,  // Tiền sổ
    700,   // SL kiểm
    1100,  // Tiền kiểm
    700,   // SL thừa
    1000,  // Tiền thừa
    700,   // SL thiếu
    1000,  // Tiền thiếu
    600,   // Tốt
    600,   // Kém
    600,   // Mất
  ];

  // Create header row with merged cells
  const headerRow1 = new TableRow({
    tableHeader: true,
    children: [
      centeredCell("STT", { rowSpan: 3, width: colWidths[0], bold: true, shading: "D9E2F3" }),
      centeredCell("Tên, nhãn hiệu, quy cách vật tư, dụng cụ, sản phẩm, hàng hóa", { rowSpan: 3, width: colWidths[1], bold: true, shading: "D9E2F3" }),
      centeredCell("Mã số", { rowSpan: 3, width: colWidths[2], bold: true, shading: "D9E2F3" }),
      centeredCell("ĐVT", { rowSpan: 3, width: colWidths[3], bold: true, shading: "D9E2F3" }),
      centeredCell("Đơn giá", { rowSpan: 3, width: colWidths[4], bold: true, shading: "D9E2F3" }),
      centeredCell("Theo sổ kế toán", { columnSpan: 2, bold: true, shading: "D9E2F3" }),
      centeredCell("Theo kiểm kê", { columnSpan: 2, bold: true, shading: "D9E2F3" }),
      centeredCell("Chênh lệch", { columnSpan: 4, bold: true, shading: "D9E2F3" }),
      centeredCell("Phẩm chất", { columnSpan: 3, bold: true, shading: "D9E2F3" }),
    ]
  });

  const headerRow2 = new TableRow({
    tableHeader: true,
    children: [
      // Empty cells for merged columns (STT, Tên, Mã, ĐVT, Đơn giá)
      centeredCell("Số lượng", { width: colWidths[5], bold: true, shading: "D9E2F3" }),
      centeredCell("Thành tiền", { width: colWidths[6], bold: true, shading: "D9E2F3" }),
      centeredCell("Số lượng", { width: colWidths[7], bold: true, shading: "D9E2F3" }),
      centeredCell("Thành tiền", { width: colWidths[8], bold: true, shading: "D9E2F3" }),
      centeredCell("Thừa", { columnSpan: 2, bold: true, shading: "D9E2F3" }),
      centeredCell("Thiếu", { columnSpan: 2, bold: true, shading: "D9E2F3" }),
      centeredCell("Còn tốt 100%", { width: colWidths[13], bold: true, shading: "D9E2F3" }),
      centeredCell("Kém phẩm chất", { width: colWidths[14], bold: true, shading: "D9E2F3" }),
      centeredCell("Mất phẩm chất", { width: colWidths[15], bold: true, shading: "D9E2F3" }),
    ]
  });

  const headerRow3 = new TableRow({
    tableHeader: true,
    children: [
      // Empty cells for merged columns
      centeredCell("", { width: colWidths[5], shading: "D9E2F3" }),
      centeredCell("", { width: colWidths[6], shading: "D9E2F3" }),
      centeredCell("", { width: colWidths[7], shading: "D9E2F3" }),
      centeredCell("", { width: colWidths[8], shading: "D9E2F3" }),
      centeredCell("SL", { width: colWidths[9], bold: true, shading: "D9E2F3" }),
      centeredCell("Tiền", { width: colWidths[10], bold: true, shading: "D9E2F3" }),
      centeredCell("SL", { width: colWidths[11], bold: true, shading: "D9E2F3" }),
      centeredCell("Tiền", { width: colWidths[12], bold: true, shading: "D9E2F3" }),
      centeredCell("", { width: colWidths[13], shading: "D9E2F3" }),
      centeredCell("", { width: colWidths[14], shading: "D9E2F3" }),
      centeredCell("", { width: colWidths[15], shading: "D9E2F3" }),
    ]
  });

  // Column labels row (A, B, C, D, 1, 2, 3, ...)
  const labelRow = new TableRow({
    children: [
      centeredCell("A", { width: colWidths[0], size: 18 }),
      centeredCell("B", { width: colWidths[1], size: 18 }),
      centeredCell("C", { width: colWidths[2], size: 18 }),
      centeredCell("D", { width: colWidths[3], size: 18 }),
      centeredCell("1", { width: colWidths[4], size: 18 }),
      centeredCell("2", { width: colWidths[5], size: 18 }),
      centeredCell("3", { width: colWidths[6], size: 18 }),
      centeredCell("4", { width: colWidths[7], size: 18 }),
      centeredCell("5", { width: colWidths[8], size: 18 }),
      centeredCell("6", { width: colWidths[9], size: 18 }),
      centeredCell("7", { width: colWidths[10], size: 18 }),
      centeredCell("8", { width: colWidths[11], size: 18 }),
      centeredCell("9", { width: colWidths[12], size: 18 }),
      centeredCell("10", { width: colWidths[13], size: 18 }),
      centeredCell("11", { width: colWidths[14], size: 18 }),
      centeredCell("12", { width: colWidths[15], size: 18 }),
    ]
  });

  // Data rows
  const dataRows = items.map((item, index) => {
    const bookAmount = (item.misaCount || 0) * (item.unitPrice || 0);
    const physicalAmount = (item.physicalCount || 0) * (item.unitPrice || 0);
    const excessQty = item.difference > 0 ? item.difference : "";
    const excessAmount = item.difference > 0 ? item.difference * (item.unitPrice || 0) : "";
    const missingQty = item.difference < 0 ? Math.abs(item.difference) : "";
    const missingAmount = item.difference < 0 ? Math.abs(item.difference) * (item.unitPrice || 0) : "";

    return new TableRow({
      children: [
        centeredCell((index + 1).toString(), { width: colWidths[0] }),
        leftCell(item.name || "", { width: colWidths[1] }),
        centeredCell(item.sku || "", { width: colWidths[2] }),
        centeredCell(item.unit || "Cái", { width: colWidths[3] }),
        centeredCell(formatNumber(item.unitPrice || 0), { width: colWidths[4] }),
        centeredCell(formatNumber(item.misaCount), { width: colWidths[5] }),
        centeredCell(formatCurrency(bookAmount), { width: colWidths[6] }),
        centeredCell(formatNumber(item.physicalCount), { width: colWidths[7] }),
        centeredCell(formatCurrency(physicalAmount), { width: colWidths[8] }),
        centeredCell(formatNumber(excessQty), { width: colWidths[9] }),
        centeredCell(formatCurrency(excessAmount), { width: colWidths[10] }),
        centeredCell(formatNumber(missingQty), { width: colWidths[11] }),
        centeredCell(formatCurrency(missingAmount), { width: colWidths[12] }),
        centeredCell(item.quality === "good" ? "✓" : "", { width: colWidths[13] }),
        centeredCell(item.quality === "poor" ? "✓" : "", { width: colWidths[14] }),
        centeredCell(item.quality === "lost" ? "✓" : "", { width: colWidths[15] }),
      ]
    });
  });

  // Total row
  const totalRow = new TableRow({
    children: [
      centeredCell("", { width: colWidths[0], bold: true }),
      centeredCell("Cộng", { width: colWidths[1], bold: true }),
      centeredCell("x", { width: colWidths[2] }),
      centeredCell("x", { width: colWidths[3] }),
      centeredCell("x", { width: colWidths[4] }),
      centeredCell("x", { width: colWidths[5] }),
      centeredCell(formatCurrency(totalBookAmount), { width: colWidths[6], bold: true }),
      centeredCell("x", { width: colWidths[7] }),
      centeredCell(formatCurrency(totalPhysicalAmount), { width: colWidths[8], bold: true }),
      centeredCell("x", { width: colWidths[9] }),
      centeredCell(formatCurrency(totalExcessAmount), { width: colWidths[10], bold: true }),
      centeredCell("x", { width: colWidths[11] }),
      centeredCell(formatCurrency(totalMissingAmount), { width: colWidths[12], bold: true }),
      centeredCell("x", { width: colWidths[13] }),
      centeredCell("x", { width: colWidths[14] }),
      centeredCell("x", { width: colWidths[15] }),
    ]
  });

  // Create the document
  const doc = new Document({
    styles: {
      default: {
        document: {
          run: { font: "Times New Roman", size: 24 }
        }
      }
    },
    sections: [{
      properties: {
        page: {
          size: { orientation: PageOrientation.LANDSCAPE },
          margin: { top: 720, right: 720, bottom: 720, left: 720 }
        }
      },
      children: [
        // Header section with company info and form reference
        new Table({
          columnWidths: [7000, 7000],
          rows: [
            new TableRow({
              children: [
                new TableCell({
                  borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
                  children: [
                    leftParagraph(`Đơn vị: ${companyName}`, { bold: true }),
                    leftParagraph(`Bộ phận: ${department}`),
                  ]
                }),
                new TableCell({
                  borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
                  children: [
                    new Paragraph({
                      alignment: AlignmentType.RIGHT,
                      children: [new TextRun({ text: "Mẫu số 05 - VT", bold: true, font: "Times New Roman", size: 22 })]
                    }),
                    new Paragraph({
                      alignment: AlignmentType.RIGHT,
                      children: [new TextRun({ text: "(Ban hành theo Thông tư số 99/2025/TT-BTC", font: "Times New Roman", size: 18, italics: true })]
                    }),
                    new Paragraph({
                      alignment: AlignmentType.RIGHT,
                      children: [new TextRun({ text: "ngày 27/10/2025 của Bộ trưởng Bộ Tài chính)", font: "Times New Roman", size: 18, italics: true })]
                    }),
                  ]
                }),
              ]
            })
          ]
        }),

        // Empty line
        new Paragraph({ children: [] }),

        // Title
        centeredParagraph("BIÊN BẢN TỔNG HỢP KIỂM KÊ VẬT TƯ, CÔNG CỤ, SẢN PHẨM, HÀNG HÓA", { bold: true, size: 28, spacingAfter: 100 }),

        // Document number
        centeredParagraph(`Số: ${documentNumber || "..................................."}`, { spacingAfter: 200 }),

        // Inventory date/time
        leftParagraph(`- Thời điểm kiểm kê: ${hours}h${minutes.toString().padStart(2, '0')} ngày ${day} tháng ${month} năm ${year}`, { spacingAfter: 100 }),

        // Warehouse
        leftParagraph(`- Kho: ${warehouseName}`, { spacingAfter: 100 }),

        // Committee section
        leftParagraph("- Ban kiểm kê gồm:", { spacingAfter: 100 }),

        // Committee members
        ...(committee.length > 0 ? committee.map((member, index) =>
          leftParagraph(
            `Ông/Bà: ${member.name || "................................."} Chức vụ: ${member.position || "................................."} Đại diện: ${member.role || (index === 0 ? "Trưởng ban" : "Ủy viên")}`,
            { indent: 360, spacingAfter: 50 }
          )
        ) : [
          leftParagraph("Ông/Bà: ................................. Chức vụ: ................................. Đại diện: Trưởng ban/Chủ tịch hội đồng", { indent: 360, spacingAfter: 50 }),
          leftParagraph("Ông/Bà: ................................. Chức vụ: ................................. Đại diện: Ủy viên", { indent: 360, spacingAfter: 50 }),
          leftParagraph("Ông/Bà: ................................. Chức vụ: ................................. Đại diện: Ủy viên", { indent: 360, spacingAfter: 50 }),
        ]),

        // Items section
        leftParagraph("- Đã kiểm kê kho có những mặt hàng dưới đây:", { spacingAfter: 200, spacingBefore: 100 }),

        // Main table
        new Table({
          columnWidths: colWidths,
          rows: [
            headerRow1,
            headerRow2,
            headerRow3,
            labelRow,
            ...dataRows,
            totalRow,
          ]
        }),

        // Empty lines before signatures
        new Paragraph({ children: [], spacing: { before: 400 } }),

        // Signature section
        new Table({
          columnWidths: [3500, 3500, 3500, 3500],
          rows: [
            new TableRow({
              children: [
                new TableCell({
                  borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
                  children: [
                    centeredParagraph("Giám đốc", { bold: true }),
                    centeredParagraph("(Ý kiến giải quyết số chênh lệch)", { size: 18, italics: true }),
                    centeredParagraph("(Ký, họ tên)", { size: 18, italics: true }),
                  ]
                }),
                new TableCell({
                  borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
                  children: [
                    centeredParagraph("Kế toán trưởng", { bold: true }),
                    centeredParagraph("(Ký, họ tên)", { size: 18, italics: true }),
                  ]
                }),
                new TableCell({
                  borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
                  children: [
                    centeredParagraph("Thủ kho", { bold: true }),
                    centeredParagraph("(Ký, họ tên)", { size: 18, italics: true }),
                  ]
                }),
                new TableCell({
                  borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
                  children: [
                    new Paragraph({
                      alignment: AlignmentType.CENTER,
                      children: [new TextRun({ text: `Ngày ${day} tháng ${month} năm ${year}`, font: "Times New Roman", size: 22, italics: true })]
                    }),
                    centeredParagraph("Trưởng ban/Chủ tịch hội đồng kiểm kê", { bold: true }),
                    centeredParagraph("(Ký, họ tên)", { size: 18, italics: true }),
                  ]
                }),
              ]
            })
          ]
        }),

        // Footer note
        new Paragraph({ children: [], spacing: { before: 600 } }),
        leftParagraph("Ghi chú: Tùy theo đặc điểm hoạt động sản xuất kinh doanh và yêu cầu quản lý của đơn vị mình, doanh nghiệp được xây dựng, thiết kế biểu mẫu chứng từ kế toán.", { size: 18, italics: true }),
      ]
    }]
  });

  // Generate the document blob
  const buffer = await Packer.toBlob(doc);
  const fileName = `Bien_ban_kiem_ke_${warehouseName.replace(/\s+/g, '_')}_${day}-${month}-${year}.docx`;

  // Option to return blob without saving (for API upload)
  if (params.returnBlob) {
    return { blob: buffer, fileName };
  }

  // Default: save file locally
  saveAs(buffer, fileName);
  return fileName;
};

export default generateStockReconciliationReport;
