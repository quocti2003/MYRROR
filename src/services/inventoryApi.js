import axios from "axios";

// Sử dụng proxy của Vite (xem vite.config.js)
// Trong dev: /api/* sẽ được proxy đến localhost:8085
// Trong production: cần set VITE_INVENTORY_API_URL
const API_BASE_URL = import.meta.env.VITE_INVENTORY_API_URL || "";

const INVENTORY_API_BASE_URL = API_BASE_URL;
const MISA_API_BASE_URL = API_BASE_URL;

// Create axios instance cho Inventory API
const inventoryApi = axios.create({
  baseURL: INVENTORY_API_BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Create axios instance cho MISA API
const misaApi = axios.create({
  baseURL: MISA_API_BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Response interceptor để xử lý lỗi
inventoryApi.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("Inventory API Error:", error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// Response interceptor cho MISA API
misaApi.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("MISA API Error:", error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// ===== INVENTORY PRODUCTS API =====
export const inventoryProductsAPI = {
  // 1. GET /api/products/dashboard - Lấy thống kê tổng quan
  getDashboard: () => inventoryApi.get("/api/products/dashboard"),

  // 2. GET /api/products/all - Lấy tất cả sản phẩm (không phân trang)
  getAll: () => inventoryApi.get("/api/products/all"),

  // 3. GET /api/products - Lấy sản phẩm với phân trang & filter
  getWithPagination: (params = {}) =>
    inventoryApi.get("/api/products", { params }),

  // 4. GET /api/products/{id} - Lấy sản phẩm theo ID
  getById: (id) => inventoryApi.get(`/api/products/${id}`),

  // 5. GET /api/products/sku/{sku} - Lấy sản phẩm theo SKU (cho Scanner)
  getBySku: (sku) => inventoryApi.get(`/api/products/sku/${encodeURIComponent(sku)}`),

  // 6. GET /api/products/search?q={keyword} - Tìm kiếm sản phẩm
  search: (keyword, params = {}) =>
    inventoryApi.get("/api/products/search", { params: { q: keyword, ...params } }),

  // 7. PUT /api/products/sku/{sku} - Cập nhật sản phẩm theo SKU
  updateBySku: (sku, data) =>
    inventoryApi.put(`/api/products/sku/${encodeURIComponent(sku)}`, data),

  // 8. DELETE /api/products/{id} - Xóa sản phẩm (soft delete)
  delete: (id) => inventoryApi.delete(`/api/products/${id}`),

  // 9. POST /api/products/print-label - Xuất dữ liệu in label (JSON)
  printLabelJson: (data) => inventoryApi.post("/api/products/print-label", data),

  // 10. POST /api/products/print-label/csv - Xuất dữ liệu in label (CSV)
  printLabelCsv: (data) =>
    inventoryApi.post("/api/products/print-label/csv", data, {
      responseType: "text",
    }),
};

// ===== MISA STOCK API =====
// API tra cứu tồn kho real-time từ hệ thống MISA
export const misaStockAPI = {
  /**
   * Kiểm tra MISA Integration Status
   * @returns {Promise<{ configured: boolean, message: string }>}
   */
  getStatus: async () => {
    const response = await misaApi.get("/api/misa/stock/status");
    return response.data?.data || response.data;
  },

  /**
   * Lấy stock real-time từ MISA theo code
   * @param {string} code - SKU (mirrorCode) hoặc barcode của sản phẩm
   * @returns {Promise<MisaStockResponse>}
   *
   * Response format:
   * {
   *   productCode: "RNG-18KWG-LG-PEAR-10.11CT",
   *   productName: "18KWG Lab Grown Pear 10.11CT",
   *   totalOnHand: 69.0,
   *   totalOrdered: 0.0,
   *   branchStocks: [{ branchId, branchName, onHand, ordered, preOrdered }]
   * }
   */
  getByCode: async (code) => {
    const response = await misaApi.get(
      `/api/misa/stock/${encodeURIComponent(code)}`
    );
    return response.data?.data || response.data;
  },
};

// Legacy alias for backward compatibility
export const misaProductStockAPI = {
  getByCode: async (code) => {
    // Sử dụng API products/sku để lấy thông tin product bao gồm misaOnHand
    const response = await inventoryApi.get(
      `/api/products/sku/${encodeURIComponent(code)}`
    );
    const product = response.data?.data || response.data;

    if (!product || !product.id) {
      return { found: false, errorMessage: "Không tìm thấy sản phẩm" };
    }

    // Map to old format for backward compatibility
    return {
      found: true,
      productId: product.id,
      code: product.skuId,
      name: product.name,
      barcode: product.sku,
      description: product.description,
      sellingPrice: product.price,
      picture: product.imageUrl,
      pictureUrls: product.imageUrls,
      totalOnHand: product.misaOnHand || 0,
      availableQuantity: product.misaOnHand || 0,
      stockStatus: product.misaOnHand > 0 ? "IN_STOCK" : "OUT_OF_STOCK",
      // Product details
      metalType: product.metalType,
      metalPurity: product.metalPurity,
      stoneType: product.stoneType,
      weightGrams: product.weightGrams,
      status: product.status,
    };
  },
};

// ===== HELPER FUNCTIONS =====
/**
 * Lấy màu hiển thị theo stock status
 * @param {string} status - IN_STOCK, LOW_STOCK, OUT_OF_STOCK
 * @returns {string} hex color
 */
export const getStockStatusColor = (status) => {
  const colors = {
    IN_STOCK: "#22c55e",    // Xanh lá
    LOW_STOCK: "#f59e0b",   // Vàng cam
    OUT_OF_STOCK: "#ef4444", // Đỏ
  };
  return colors[status] || "#6b7280";
};

/**
 * Lấy text hiển thị theo stock status
 * @param {string} status - IN_STOCK, LOW_STOCK, OUT_OF_STOCK
 * @returns {string} Vietnamese text
 */
export const getStockStatusText = (status) => {
  const texts = {
    IN_STOCK: "Còn hàng",
    LOW_STOCK: "Sắp hết",
    OUT_OF_STOCK: "Hết hàng",
  };
  return texts[status] || "Không xác định";
};

export default inventoryApi;
