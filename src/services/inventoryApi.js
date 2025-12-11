import axios from "axios";

// Inventory API Base URL - kết nối với backend Spring Boot
const INVENTORY_API_BASE_URL =
  import.meta.env.VITE_INVENTORY_API_URL || "http://localhost:8080";

// Create axios instance cho Inventory API
const inventoryApi = axios.create({
  baseURL: INVENTORY_API_BASE_URL,
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

export default inventoryApi;
