/**
 * PDF Invoice Extraction API Service
 * Calls the backend API to extract data from PDF invoices
 *
 * Backend hosted on AWS - configure VITE_PDF_API_URL in .env
 *
 * @typedef {Object} InvoiceItem
 * @property {number} stt - Số thứ tự
 * @property {string} name - Tên sản phẩm
 * @property {string} unit - Đơn vị tính
 * @property {number} quantity - Số lượng
 * @property {number|null} unitPrice - Đơn giá
 * @property {number|null} total - Thành tiền
 *
 * @typedef {Object} InvoiceData
 * @property {string} invoiceCode - Ký hiệu hóa đơn
 * @property {string} invoiceNumber - Số hóa đơn
 * @property {string} invoiceDate - Ngày hóa đơn (DD/MM/YYYY)
 * @property {string} paymentMethod - Hình thức thanh toán
 * @property {string} customerCompany - Tên công ty
 * @property {string} customerName - Tên khách hàng
 * @property {string} customerTaxCode - Mã số thuế
 * @property {string} customerAddress - Địa chỉ
 * @property {string} customerPhone - Số điện thoại
 * @property {string} customerIdNumber - Số CCCD
 * @property {InvoiceItem[]} items - Danh sách sản phẩm
 * @property {number} subtotal - Tổng tiền (số)
 * @property {string} totalInWords - Tổng tiền bằng chữ
 *
 * @typedef {Object} ExtractResponse
 * @property {boolean} success
 * @property {InvoiceData} [data]
 * @property {string} [error]
 */

// AWS Backend URL - set in .env file: VITE_PDF_API_URL=https://your-aws-backend.com
const API_BASE_URL = import.meta.env.VITE_PDF_API_URL || 'http://localhost:8085';
const API_ENDPOINT = `${API_BASE_URL}/api/v1/printing/extract-invoice`;
const HEALTH_ENDPOINT = `${API_BASE_URL}/api/v1/printing/extract-invoice/health`;

/**
 * Extract invoice data from a PDF file
 * @param {File} file - The PDF file to extract data from
 * @param {Object} options - Options object
 * @param {Function} options.onProgress - Progress callback
 * @returns {Promise<Object>} - Extracted invoice data
 */
export async function extractInvoiceFromPdf(file, options = {}) {
  const { onProgress } = options;

  try {
    // Stage 1: Preparing
    if (onProgress) {
      onProgress({ stage: 'reading', progress: 10 });
    }

    // Create form data
    const formData = new FormData();
    formData.append('pdf', file);

    // Stage 2: Uploading
    if (onProgress) {
      onProgress({ stage: 'extracting', progress: 30 });
    }

    // Call API
    const response = await fetch(API_ENDPOINT, {
      method: 'POST',
      body: formData,
    });

    // Stage 3: Processing response
    if (onProgress) {
      onProgress({ stage: 'parsing', progress: 70 });
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error || `HTTP error: ${response.status}`);
    }

    const result = await response.json();

    if (!result.success) {
      throw new Error(result.error || 'Failed to extract invoice data');
    }

    // Stage 4: Done
    if (onProgress) {
      onProgress({ stage: 'done', progress: 100 });
    }

    return result.data;
  } catch (error) {
    console.error('PDF extraction error:', error);
    throw error;
  }
}

/**
 * Alias for backward compatibility
 */
export const readPdfInvoice = extractInvoiceFromPdf;

/**
 * Check if the PDF extraction service is healthy
 * @returns {Promise<{healthy: boolean, message?: string}>}
 */
export async function checkHealth() {
  try {
    const response = await fetch(HEALTH_ENDPOINT, {
      method: 'GET',
    });

    if (response.ok) {
      return { healthy: true };
    }

    return { healthy: false, message: `HTTP ${response.status}` };
  } catch (error) {
    return { healthy: false, message: error.message };
  }
}

export default {
  extractInvoiceFromPdf,
  readPdfInvoice,
  checkHealth,
};
