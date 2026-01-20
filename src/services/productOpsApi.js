import axios from "axios";

// Base URL for Product Ops API
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  "https://nsa4fef6um.ap-southeast-1.awsapprunner.com";

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("Product Ops API Error:", error);
    return Promise.reject(error);
  }
);

// ==================== DASHBOARD & OVERVIEW ====================

/**
 * Get all products with workflow status
 * @param {Object} params - Query parameters
 * @param {string} params.status - Filter by status (DRAFT, IN_PROGRESS, READY, PUBLISHED, ALL)
 * @param {string} params.search - Search query
 */
export const getAllProductsWithWorkflow = async (params = {}) => {
  const response = await api.get("/api/product-ops/products", { params });
  return response.data;
};

/**
 * Get workflow summary statistics
 */
export const getWorkflowSummary = async () => {
  const response = await api.get("/api/product-ops/summary");
  return response.data;
};

/**
 * Get single product workflow
 * @param {string} productId - Product ID
 */
export const getProductWorkflow = async (productId) => {
  const response = await api.get(`/api/product-ops/products/${productId}`);
  return response.data;
};

// ==================== SKU GENERATION ====================

/**
 * Generate internal SKU
 * @param {Object} request - SKU generation request
 * @param {string} request.category - Category code
 */
export const generateInternalSKU = async (request) => {
  const response = await api.post("/api/product-ops/generate-sku", request);
  return response.data;
};

/**
 * Get next sequence number for category
 * @param {string} category - Category code
 */
export const getNextSequenceForCategory = async (category) => {
  const response = await api.get("/api/product-ops/next-sequence", {
    params: { category },
  });
  return response.data;
};

/**
 * Validate SKU uniqueness
 * @param {string} sku - SKU code to validate
 */
export const validateSKUUniqueness = async (sku) => {
  const response = await api.post("/api/product-ops/validate-sku", { sku });
  return response.data;
};

// ==================== DRAFT PRODUCT CREATION ====================

/**
 * Create draft product
 * @param {Object} request - Product creation request
 * @param {string} request.name - Product name
 * @param {string} request.internalSKU - Internal SKU
 * @param {string} request.category - Category
 */
export const createDraftProduct = async (request) => {
  const response = await api.post("/api/product-ops/create-draft", request);
  return response.data;
};

// ==================== PRODUCT FULFILLMENT ====================

/**
 * Update product fulfillment (images, description, etc.)
 * @param {string} productId - Product ID
 * @param {Object} request - Fulfillment data
 */
export const updateProductFulfillment = async (productId, request) => {
  const response = await api.put(
    `/api/product-ops/products/${productId}/fulfillment`,
    request
  );
  return response.data;
};

// ==================== MISA INTEGRATION ====================

/**
 * Get MISA products (synced from MISA)
 * @param {string} status - Optional status filter (SYNCED, PENDING, FAILED)
 */
export const getMISAProducts = async (status = null) => {
  const response = await api.get("/api/product-ops/misa/products", {
    params: { status },
  });
  return response.data;
};

/**
 * Get MISA product by SKU code
 * @param {string} skuCode - SKU code
 * NOTE: Backend endpoint not implemented yet
 */
// export const getMISAProductBySkuCode = async (skuCode) => {
//   const response = await api.get(
//     `/api/product-ops/misa/products/${skuCode}`
//   );
//   return response.data;
// };

/**
 * Get products pending MISA sync
 * NOTE: Backend endpoint not implemented yet - use getMISAProducts(status='PENDING') instead
 */
// export const getPendingMISAProducts = async () => {
//   const response = await api.get("/api/product-ops/misa/pending");
//   return response.data;
// };

/**
 * Request MISA SKU creation
 * @param {string} productId - Product ID
 */
export const requestMISASKU = async (productId) => {
  const response = await api.post(
    `/api/product-ops/products/${productId}/request-misa-sku`
  );
  return response.data;
};

/**
 * Retry MISA sync
 * @param {string} productId - Product ID
 */
export const retryMISASync = async (productId) => {
  const response = await api.post(
    `/api/product-ops/products/${productId}/retry-misa-sync`
  );
  return response.data;
};

/**
 * Update MISA SKU manually
 * @param {string} productId - Product ID
 * @param {Object} request - Request with misaSKU
 */
export const updateMISASKU = async (productId, request) => {
  const response = await api.put(
    `/api/product-ops/products/${productId}/misa-sku`,
    request
  );
  return response.data;
};

// ==================== MARK READY & PUBLISH ====================

/**
 * Mark product as ready for release
 * @param {string} productId - Product ID
 */
export const markProductReady = async (productId) => {
  const response = await api.post(
    `/api/product-ops/products/${productId}/mark-ready`
  );
  return response.data;
};

/**
 * Publish product to website
 * @param {string} productId - Product ID
 */
export const publishProduct = async (productId) => {
  const response = await api.post(
    `/api/product-ops/products/${productId}/publish`
  );
  return response.data;
};

/**
 * Unpublish product from website
 * @param {string} productId - Product ID
 */
export const unpublishProduct = async (productId) => {
  const response = await api.post(
    `/api/product-ops/products/${productId}/unpublish`
  );
  return response.data;
};

// ==================== WORKFLOW UPDATES ====================

/**
 * Update workflow step
 * @param {string} productId - Product ID
 * @param {Object} request - Step update request
 * @param {number} request.stepId - Step ID (1-7)
 * @param {string} request.status - Step status (complete, pending, blocked)
 * @param {string} request.notes - Optional notes
 */
export const updateWorkflowStep = async (productId, request) => {
  const response = await api.put(
    `/api/product-ops/products/${productId}/workflow-step`,
    request
  );
  return response.data;
};

/**
 * Get workflow history for product
 * @param {string} productId - Product ID
 */
export const getWorkflowHistory = async (productId) => {
  const response = await api.get(
    `/api/product-ops/products/${productId}/workflow-history`
  );
  return response.data;
};

/**
 * Get MISA sync history for product
 * @param {string} productId - Product ID
 */
export const getMISAHistory = async (productId) => {
  const response = await api.get(
    `/api/product-ops/products/${productId}/misa-history`
  );
  return response.data;
};

// ==================== QUEUES ====================

/**
 * Get products in draft queue
 */
export const getDraftProducts = async () => {
  return getAllProductsWithWorkflow({ status: "DRAFT" });
};

/**
 * Get products in ready queue
 */
export const getReadyProducts = async () => {
  const response = await api.get("/api/product-ops/ready-products");
  return response.data;
};

/**
 * Get published products
 */
export const getPublishedProducts = async () => {
  const response = await api.get("/api/product-ops/published-products");
  return response.data;
};

// Export default API instance for custom requests
export default api;
