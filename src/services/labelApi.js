import axios from "axios";

// Base URL configuration
const isLocalDev = typeof window !== 'undefined' && window.location.hostname === 'localhost';
const API_BASE_URL = isLocalDev
  ? '' // Use relative URLs through Vite proxy
  : (import.meta.env.VITE_API_BASE_URL || "https://nsa4fef6um.ap-southeast-1.awsapprunner.com");

// Create axios instance for Label API
const labelApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, // Longer timeout for ZPL generation
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor - add auth token
labelApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
labelApi.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("Label API Error:", error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// =====================
// Label Templates API
// =====================
export const labelTemplateApi = {
  // Get all templates
  getAll: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.status) queryParams.append("status", params.status);
    if (params.search) queryParams.append("search", params.search);
    if (params.page !== undefined) queryParams.append("page", params.page);
    if (params.size) queryParams.append("size", params.size);
    return labelApi.get(`/api/v1/admin/label-templates?${queryParams}`);
  },

  // Get template by ID
  getById: (id) => labelApi.get(`/api/v1/admin/label-templates/${id}`),

  // Create template
  create: (data) => labelApi.post("/api/v1/admin/label-templates", data),

  // Update template
  update: (id, data) => labelApi.put(`/api/v1/admin/label-templates/${id}`, data),

  // Delete template
  delete: (id) => labelApi.delete(`/api/v1/admin/label-templates/${id}`),

  // Duplicate template
  duplicate: (id) => labelApi.post(`/api/v1/admin/label-templates/${id}/duplicate`),

  // Get default template
  getDefault: () => labelApi.get("/api/v1/admin/label-templates/default"),
};

// =====================
// Print Jobs API
// =====================
export const printJobApi = {
  // Get all print jobs
  getAll: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.status) queryParams.append("status", params.status);
    if (params.templateId) queryParams.append("templateId", params.templateId);
    if (params.startDate) queryParams.append("startDate", params.startDate);
    if (params.endDate) queryParams.append("endDate", params.endDate);
    if (params.page !== undefined) queryParams.append("page", params.page);
    if (params.size) queryParams.append("size", params.size);
    return labelApi.get(`/api/v1/admin/print-jobs?${queryParams}`);
  },

  // Get print job by ID
  getById: (id, includeItems = false) =>
    labelApi.get(`/api/v1/admin/print-jobs/${id}?includeItems=${includeItems}`),

  // Create print job
  create: (data) => labelApi.post("/api/v1/admin/print-jobs", data),

  // Cancel print job
  cancel: (id) => labelApi.delete(`/api/v1/admin/print-jobs/${id}`),

  // Retry failed items
  retry: (id) => labelApi.post(`/api/v1/admin/print-jobs/${id}/retry`),

  // Complete a print job
  complete: (id) => labelApi.post(`/api/v1/admin/print-jobs/${id}/complete`),

  // Force complete a stuck print job
  forceComplete: (id) => labelApi.post(`/api/v1/admin/print-jobs/${id}/force-complete`),

  // Reset a stuck job back to PENDING
  reset: (id) => labelApi.post(`/api/v1/admin/print-jobs/${id}/reset`),

  // Mark item as printed
  markItemPrinted: (itemId) =>
    labelApi.post(`/api/v1/admin/print-jobs/items/${itemId}/printed`),

  // Mark item as failed
  markItemFailed: (itemId, error) =>
    labelApi.post(`/api/v1/admin/print-jobs/items/${itemId}/failed?error=${encodeURIComponent(error)}`),
};

// =====================
// Label Generation API
// =====================
export const labelGenerationApi = {
  // Generate ZPL
  generateZPL: (data) => labelApi.post("/api/v1/admin/labels/generate-zpl", data),

  // Generate TSPL
  generateTSPL: (data) => labelApi.post("/api/v1/admin/labels/generate-tspl", data),
};

// =====================
// Printer API
// =====================
export const printerApi = {
  // Get available printers (USB/Spooler)
  getAvailablePrinters: () => labelApi.get("/api/v1/admin/printer/available"),

  // Test printer connection
  testConnection: (data) => labelApi.post("/api/v1/admin/printer/test", data),

  // Print a job
  printJob: (jobId, data) => labelApi.post(`/api/v1/admin/printer/jobs/${jobId}/print`, data),

  // Send ZPL directly
  sendZpl: (zplData, printerConfig) =>
    labelApi.post(`/api/v1/admin/printer/send-zpl?zplData=${encodeURIComponent(zplData)}`, printerConfig),

  // Send ZPL via request body (for long ZPL)
  sendZplBody: (zplData, printerConfig) =>
    labelApi.post("/api/v1/admin/printer/send-zpl-body", { ...printerConfig, zplData }),

  // Print test label
  printTestLabel: (data) => labelApi.post("/api/v1/admin/printer/test-print", data),

  // Print jewelry label - with optional RFID encoding
  printJewelryLabel: (price, productName, barcode, printerConfig, includeRFID = false, productId = null, skuCode = null) => {
    const params = new URLSearchParams();
    params.append("price", price);
    params.append("productName", productName);
    params.append("barcode", barcode);
    params.append("includeRFID", includeRFID);
    if (productId) params.append("productId", productId);
    if (skuCode) params.append("skuCode", skuCode);
    return labelApi.post(`/api/v1/admin/printer/print-jewelry-label?${params}`, printerConfig);
  },
};

// =====================
// WebUSB Printer API (Client-side printing)
// =====================
import { getWebUsbPrinter } from '../utils/webUsbPrinter';

export const webUsbPrinterApi = {
  // Check if WebUSB is supported
  isSupported: () => {
    return typeof navigator !== 'undefined' && 'usb' in navigator;
  },

  // Get printer instance
  getPrinter: () => getWebUsbPrinter(),

  // Connect to printer (shows browser dialog)
  connect: async () => {
    const printer = getWebUsbPrinter();
    return await printer.connect();
  },

  // Disconnect from printer
  disconnect: async () => {
    const printer = getWebUsbPrinter();
    await printer.disconnect();
  },

  // Check if connected
  isConnected: () => {
    const printer = getWebUsbPrinter();
    return printer.isConnected();
  },

  // Get device info
  getDeviceInfo: () => {
    const printer = getWebUsbPrinter();
    return printer.getDeviceInfo();
  },

  // Print ZPL data
  print: async (zplData) => {
    const printer = getWebUsbPrinter();
    return await printer.print(zplData);
  },

  // Print ZPL with validation
  printZPL: async (zpl) => {
    const printer = getWebUsbPrinter();
    return await printer.printZPL(zpl);
  },

  // Print test label
  printTestLabel: async () => {
    const printer = getWebUsbPrinter();
    return await printer.printTestLabel();
  },

  // Generate ZPL from backend and print via WebUSB
  generateAndPrint: async (data) => {
    // 1. Generate ZPL from backend
    const response = await labelApi.post("/api/v1/admin/labels/generate-zpl", data);
    const zplData = response.data.zpl || response.data;

    // 2. Print via WebUSB
    const printer = getWebUsbPrinter();
    if (!printer.isConnected()) {
      await printer.connect();
    }
    return await printer.printZPL(zplData);
  },

  // Print jewelry label via WebUSB
  printJewelryLabelWebUsb: async (price, productName, barcode, includeRFID = false, productId = null, skuCode = null) => {
    // 1. Generate ZPL from backend
    const params = new URLSearchParams();
    params.append("price", price);
    params.append("productName", productName);
    params.append("barcode", barcode);
    params.append("includeRFID", includeRFID);
    if (productId) params.append("productId", productId);
    if (skuCode) params.append("skuCode", skuCode);

    const response = await labelApi.get(`/api/v1/admin/labels/jewelry-zpl?${params}`);
    const zplData = response.data.zpl || response.data;

    // 2. Print via WebUSB
    const printer = getWebUsbPrinter();
    if (!printer.isConnected()) {
      await printer.connect();
    }
    return await printer.printZPL(zplData);
  },
};

// =====================
// RFID Tags API
// =====================
export const rfidApi = {
  // Register tag
  registerTag: (data) => labelApi.post("/api/v1/rfid/tags", data),

  // Get tag by EPC
  getTagByEPC: (epc) => labelApi.get(`/api/v1/rfid/tags/${epc}`),

  // Get product by EPC
  getProductByEPC: (epc) => labelApi.get(`/api/v1/rfid/tags/${epc}/product`),

  // Record scan (RFID EPC)
  recordScan: (data) => labelApi.post("/api/v1/rfid/scan", data),

  // Scan by barcode (no RFID tag required)
  scanByBarcode: (barcode) => labelApi.get(`/api/v1/rfid/scan-barcode/${encodeURIComponent(barcode)}`),

  // Get tags by product
  getTagsByProduct: (productId) => labelApi.get(`/api/v1/rfid/products/${productId}/tags`),

  // Void tag
  voidTag: (epc, reason) =>
    labelApi.post(`/api/v1/rfid/tags/${epc}/void?reason=${encodeURIComponent(reason)}`),

  // Get scan history
  getScanHistory: (epc, params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.append("page", params.page);
    if (params.size) queryParams.append("size", params.size);
    return labelApi.get(`/api/v1/rfid/tags/${epc}/scans?${queryParams}`);
  },

  // Get all tags
  getAllTags: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.status) queryParams.append("status", params.status);
    if (params.productId) queryParams.append("productId", params.productId);
    if (params.page !== undefined) queryParams.append("page", params.page);
    if (params.size) queryParams.append("size", params.size);
    return labelApi.get(`/api/v1/rfid/tags?${queryParams}`);
  },

  // Generate EPC (for testing)
  generateEPC: (productId, sku) => {
    const params = new URLSearchParams();
    params.append("productId", productId);
    if (sku) params.append("sku", sku);
    return labelApi.get(`/api/v1/rfid/generate-epc?${params}`);
  },

  // Decode EPC
  decodeEPC: (epc) => labelApi.get(`/api/v1/rfid/decode-epc/${epc}`),
};

// Re-export WebUSB utilities for convenience
export { getWebUsbPrinter, resetWebUsbPrinter } from '../utils/webUsbPrinter';
export { default as useWebUsbPrinter } from '../hooks/useWebUsbPrinter';

export default labelApi;
