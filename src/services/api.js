import axios from "axios";

// Base URLs are driven by Vite environment variables to support local/dev/prod
// In development on localhost, use empty string to leverage Vite's proxy (avoids CORS)
const isLocalDev = typeof window !== 'undefined' && window.location.hostname === 'localhost';
const API_BASE_URL = isLocalDev
  ? '' // Use relative URLs through Vite proxy
  : (import.meta.env.VITE_API_BASE_URL || "https://nsa4fef6um.ap-southeast-1.awsapprunner.com");

// In monolith architecture, AUTH uses same base URL as API
const AUTH_BASE_URL = isLocalDev
  ? '' // Use relative URLs through Vite proxy
  : (import.meta.env.VITE_API_BASE_URL || "https://nsa4fef6um.ap-southeast-1.awsapprunner.com");

const REFRESH_TOKEN_ENDPOINT = `${AUTH_BASE_URL}/api/v1/auth/refresh-token`;

// Debug logging
// console.log("🔧 API Configuration Debug:", {
//   "import.meta.env.VITE_API_BASE_URL": import.meta.env.VITE_API_BASE_URL,
//   "Final API_BASE_URL": API_BASE_URL,
//   "All env vars": import.meta.env,
// });

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    "Cache-Control": "no-cache",
    Pragma: "no-cache",
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;

      // Add X-User-Id header from JWT token
      try {
        const base64Payload = token.split(".")[1];
        const payload = JSON.parse(atob(base64Payload));
        if (payload.userId) {
          config.headers["X-User-Id"] = payload.userId.toString();
        }
      } catch (decodeError) {
        console.error("❌ Error decoding JWT for X-User-Id:", decodeError);
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const originalRequest = error.config || {};
    const requestUrl = originalRequest.url || "";

    const isAuthEndpoint =
      requestUrl.includes("/auth/authenticate") ||
      requestUrl.includes("/auth/refresh-token");

    const requiresAuth = ["/users/", "/orders/", "/me/"].some((path) =>
      requestUrl.includes(path)
    );

    if (
      status === 401 &&
      requiresAuth &&
      !isAuthEndpoint &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) {
        localStorage.removeItem("accessToken");
        return Promise.reject(error);
      }

      try {
        const refreshResponse = await axios.post(REFRESH_TOKEN_ENDPOINT, {
          refreshToken,
        });
        const { accessToken: newAccessToken } = refreshResponse.data || {};

        if (newAccessToken) {
          localStorage.setItem("accessToken", newAccessToken);
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        console.error("Token refresh failed", refreshError);
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        return Promise.reject(refreshError);
      }
    }

    console.error("API Error:", error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// ===== AUTHENTICATION API =====
// Note: Auth endpoints use /api/v1/auth/* path (merged from user-service)
export const authAPI = {
  // Login / Authenticate
  authenticate: (username, password) =>
    api.post("/api/v1/auth/authenticate", { username, password }),

  // Register new user
  register: (userData) => api.post("/api/v1/auth/register", userData),

  // Verify email with token (GET with query param)
  verifyEmail: (token) => api.get(`/api/v1/auth/verify-email?token=${token}`),

  // Resend verification email
  resendVerificationEmail: (email) =>
    api.post("/api/v1/auth/resend-verification-email", { email }),

  // Refresh token
  refreshToken: (refreshToken) =>
    api.post("/api/v1/auth/refresh-token", { refreshToken }),

  // Forgot password - Step 1: Request OTP
  forgotPassword: (email) => api.post("/api/v1/auth/forgot-password", { email }),

  // Verify OTP - Step 2: Verify OTP and get reset token
  verifyOtp: (email, otp) => api.post("/api/v1/auth/verify-otp", { email, otp }),

  // Reset password - Step 3: Reset with token
  resetPassword: (resetToken, newPassword) =>
    api.post("/api/v1/auth/reset-password", { resetToken, newPassword }),

  // Resend password reset OTP
  resendPasswordResetOtp: (email) =>
    api.post("/api/v1/auth/resend-password-reset-otp", { email }),
};

// ===== LOCATIONS API =====
export const locationsAPI = {
  // Get all active locations
  getAll: () => api.get("/api/locations"),

  // Get location by ID
  getById: (id) => api.get(`/api/locations/${id}`),

  // Filter locations by city
  getByCity: (city) =>
    api.get(`/api/locations/city/${encodeURIComponent(city)}`),

  // Filter locations by type
  getByType: (type) => api.get(`/api/locations/type/${type}`),

  // Filter locations by both city and type
  getFiltered: (params) => api.get("/api/locations/filter", { params }),

  // Search locations
  search: (searchTerm) =>
    api.get(`/api/locations/search?q=${encodeURIComponent(searchTerm)}`),

  // Get filter options (cities, types)
  getFilterOptions: () => api.get("/api/locations/filters"),

  // Get location statistics
  getStatistics: () => api.get("/api/locations/statistics"),

  // Health check
  health: () => api.get("/api/locations/health"),

  // CRUD operations
  create: (locationData) => api.post("/api/locations", locationData),
  update: (id, locationData) => api.put(`/api/locations/${id}`, locationData),
  delete: (id) => api.delete(`/api/locations/${id}`),
  hardDelete: (id) => api.delete(`/api/locations/${id}/permanent`),
  activate: (id) => api.post(`/api/locations/${id}/activate`),
  deactivate: (id) => api.post(`/api/locations/${id}/deactivate`),
};

// ===== PRODUCTS API =====
export const productsAPI = {
  // Get all active products
  getAll: (params = {}) => api.get("/api/products", { params }),

  // Get all products including inactive (admin)
  getAllIncludingInactive: () => api.get("/api/products/all"),

  // Get product by ID
  getById: (id) => api.get(`/api/products/${id}`),

  // Get product by SKU
  getBySku: (sku) => api.get(`/api/products/sku/${sku}`),

  // Get featured products
  getFeatured: () => api.get("/api/products/featured"),

  // Get available products (in stock)
  getAvailable: (params = {}) => api.get("/api/products/available", { params }),

  // Get products by category
  getByCategory: (categoryId, params = {}) =>
    api.get(`/api/products/category/${categoryId}`, { params }),

  // Get products by metal type
  getByMetalType: (metalType) => api.get(`/api/products/metal/${metalType}`),

  // Search products
  search: (searchTerm, params = {}) =>
    api.get(`/api/products/search?q=${encodeURIComponent(searchTerm)}`, {
      params,
    }),

  // Advanced filtering
  getFiltered: (params = {}) => api.get("/api/products/filter", { params }),

  // Get products by price range
  getByPriceRange: (min, max) =>
    api.get(`/api/products/price-range?min=${min}&max=${max}`),

  // Get products by vendor ID
  getByVendorId: (vendorId) => api.get(`/api/products?vendorId=${vendorId}`),

  // Get current user's vendor products (authenticated endpoint)
  getCurrentUserProducts: () => api.get("/api/products"),

  // Get low stock products
  getLowStock: () => api.get("/api/products/low-stock"),

  // Get out of stock products
  getOutOfStock: () => api.get("/api/products/out-of-stock"),

  // Get filter options
  getFilterOptions: () => api.get("/api/products/filters"),

  // Get product statistics
  getStatistics: () => api.get("/api/products/statistics"),

  // Health check
  health: () => api.get("/api/products/health"),

  // CRUD operations
  create: (productData) => api.post("/api/products", productData),
  update: (id, productData) => api.put(`/api/products/${id}`, productData),
  delete: (id) => api.delete(`/api/products/${id}`),
  hardDelete: (id) => api.delete(`/api/products/${id}/permanent`),
  toggleFeatured: (id) => api.post(`/api/products/${id}/toggle-featured`),
  updateStock: (id, quantity) =>
    api.put(`/api/products/${id}/stock`, { quantity }),

  // Product Fulfillment operations
  getPendingFulfillment: () => api.get("/api/products/pending-fulfillment"),
  getDraft: () => api.get("/api/products/draft"),
  getFulfilledDrafts: () => api.get("/api/products/fulfilled-drafts"),
  fulfill: (id, fulfillmentData) =>
    api.post(`/api/products/${id}/fulfill`, fulfillmentData),
  markReadyForRelease: (id) =>
    api.put(`/api/products/${id}/ready-for-release`),

  // Product Publisher operations
  getReadyForRelease: () => api.get("/api/products/ready-for-release"),
  getPublished: () => api.get("/api/products/published"),
  publish: (id) => api.post(`/api/products/${id}/publish`),
  unpublish: (id) => api.post(`/api/products/${id}/unpublish`),
  archive: (id) => api.post(`/api/products/${id}/archive`),
};

// ===== ORDERS API =====
export const ordersAPI = {
  create: (orderData) => api.post("/api/orders", orderData),
  getAll: (params = {}) => api.get("/api/orders", { params }),
  getById: (id) => api.get(`/api/orders/${id}`),
  getPaymentSchedule: (id) => api.get(`/api/orders/${id}/payment-schedule`),
  getPaymentSchedules: (params = {}) =>
    api.get(`/api/orders/payment-schedule`, { params }),
  // Assign or update vendor on an order
  assignVendor: (id, vendorId) =>
    api.put(`/api/orders/${id}/vendor`, { vendorId }),
  updateStatus: (id, payload) => api.post(`/api/orders/${id}/status`, payload),
  updatePaymentTerms: (id, payload) =>
    api.put(`/api/orders/${id}/payment-terms`, payload),
  recordPayment: (orderId, scheduleId, payload) =>
    api.post(
      `/api/orders/${orderId}/payment-schedule/${scheduleId}/payments`,
      payload
    ),
  markSchedulePaid: (orderId, scheduleId, payload) =>
    api.post(
      `/api/orders/${orderId}/payment-schedule/${scheduleId}/payments`,
      payload
    ),

  // NEW: Order Workflow endpoints (MISA SKU enforcement)
  confirm: (id, data) => api.post(`/api/orders/${id}/confirm`, data),
  startProduction: (id) => api.post(`/api/orders/${id}/start-production`),
  ship: (id, data) => api.post(`/api/orders/${id}/ship`, data),
  complete: (id, data) => api.post(`/api/orders/${id}/complete`, data),
  getAwaitingMisaSku: () => api.get('/api/orders/awaiting-misa-sku'),
  markMisaSkuCreated: (id, misaItemId) =>
    api.post(`/api/orders/${id}/misa-sku-created`, null, {
      params: { misaItemId }
    }),
};

// ===== APPOINTMENTS API =====
export const appointmentsAPI = {
  // Create a new appointment
  create: (appointmentData) => api.post("/api/appointments", appointmentData),

  // Get all appointments
  getAll: () => api.get("/api/appointments"),

  // Get appointment by ID
  getById: (id) => api.get(`/api/appointments/${id}`),

  // Get booked time slots for a date (optionally filtered by venue)
  getBookedSlots: (date, venueId = null) => {
    const params = { date };
    if (venueId) params.venueId = venueId;
    return api.get("/api/appointments/slots", { params });
  },

  // Get unavailable dates for a venue within a date range (where ALL slots are blocked/booked)
  getUnavailableDates: (startDate, endDate, venueId = null) => {
    const params = { startDate, endDate };
    if (venueId) params.venueId = venueId;
    return api.get("/api/appointments/unavailable-dates", { params });
  },

  // Get appointments by date
  getByDate: (date) => api.get("/api/appointments/by-date", { params: { date } }),

  // Get appointments by venue and date
  getByVenueAndDate: (venueId, date) =>
    api.get("/api/appointments/by-venue-date", { params: { venueId, date } }),

  // Get upcoming appointments
  getUpcoming: () => api.get("/api/appointments/upcoming"),

  // Get appointments by customer email
  getByEmail: (email) =>
    api.get("/api/appointments/by-email", { params: { email } }),

  // Get appointments by status
  getByStatus: (status) =>
    api.get("/api/appointments/by-status", { params: { status } }),

  // Update appointment
  update: (id, appointmentData) =>
    api.put(`/api/appointments/${id}`, appointmentData),

  // Confirm appointment
  confirm: (id) => api.post(`/api/appointments/${id}/confirm`),

  // Complete appointment
  complete: (id) => api.post(`/api/appointments/${id}/complete`),

  // Cancel appointment
  cancel: (id, reason = null) =>
    api.post(`/api/appointments/${id}/cancel`, { reason }),

  // Mark as no-show
  markNoShow: (id) => api.post(`/api/appointments/${id}/no-show`),

  // Add staff notes
  addNotes: (id, notes) =>
    api.put(`/api/appointments/${id}/notes`, { notes }),

  // Delete appointment
  delete: (id) => api.delete(`/api/appointments/${id}`),
};

// ===== BLOCKED SLOTS API (Admin) =====
export const blockedSlotsAPI = {
  // Create a new blocked slot
  create: (blockedSlotData) =>
    api.post("/api/appointments/blocked-slots", blockedSlotData),

  // Get all blocked slots
  getAll: () => api.get("/api/appointments/blocked-slots"),

  // Get blocked slot by ID
  getById: (id) => api.get(`/api/appointments/blocked-slots/${id}`),

  // Get blocked slots by date
  getByDate: (date) =>
    api.get("/api/appointments/blocked-slots/by-date", { params: { date } }),

  // Get blocked slots by venue and date
  getByVenueAndDate: (venueId, date) =>
    api.get("/api/appointments/blocked-slots/by-venue-date", {
      params: { venueId, date },
    }),

  // Get blocked slots by date range
  getByDateRange: (startDate, endDate, venueId = null) => {
    const params = { startDate, endDate };
    if (venueId) params.venueId = venueId;
    return api.get("/api/appointments/blocked-slots/by-date-range", { params });
  },

  // Check if a slot is blocked
  checkBlocked: (venueId, date, time) =>
    api.get("/api/appointments/blocked-slots/check", {
      params: { venueId, date, time },
    }),

  // Get blocked times for a venue and date
  getBlockedTimes: (venueId, date) =>
    api.get("/api/appointments/blocked-slots/blocked-times", {
      params: { venueId, date },
    }),

  // Update blocked slot
  update: (id, blockedSlotData) =>
    api.put(`/api/appointments/blocked-slots/${id}`, blockedSlotData),

  // Delete blocked slot
  delete: (id) => api.delete(`/api/appointments/blocked-slots/${id}`),
};

// ===== COLLECTIONS API =====
export const collectionsAPI = {
  // Get all active collections
  getAll: () => api.get("/api/collections"),
  // Get all collections including inactive (admin)
  getAllIncludingInactive: () => api.get("/api/collections/all"),

  // Get collection by ID
  getById: (id) => api.get(`/api/collections/${id}`),

  // Get collection by name
  getByName: (name) =>
    api.get(`/api/collections/name/${encodeURIComponent(name)}`),

  // Get all collections with their products
  getAllWithProducts: () => api.get("/api/collections/with-products"),

  // Get collection with products
  getWithProducts: (id) => api.get(`/api/collections/${id}/products`),

  // Get products in collection (products list only)
  getProductsInCollection: (id) =>
    api.get(`/api/collections/${id}/products-list`),

  // Get featured collections
  getFeatured: () => api.get("/api/collections/featured"),

  // Get collections by year
  getByYear: (year) => api.get(`/api/collections/year/${year}`),

  // Get collections by season and year
  getBySeasonAndYear: (season, year) =>
    api.get(`/api/collections/season/${season}/year/${year}`),

  // Search collections
  search: (searchTerm) =>
    api.get(`/api/collections/search?q=${encodeURIComponent(searchTerm)}`),

  // Get filter options
  getFilterOptions: () => api.get("/api/collections/filters"),

  // Get collection statistics
  getStatistics: () => api.get("/api/collections/statistics"),

  // Health check
  health: () => api.get("/api/collections/health"),

  // CRUD operations
  create: (collectionData) => api.post("/api/collections", collectionData),
  update: (id, collectionData) =>
    api.put(`/api/collections/${id}`, collectionData),
  delete: (id) => api.delete(`/api/collections/${id}`),
  hardDelete: (id) => api.delete(`/api/collections/${id}/permanent`),

  // Collection-Product management
  addProductToCollection: (collectionId, productData) =>
    api.post(`/api/collections/${collectionId}/products`, productData),
  removeProductFromCollection: (collectionId, productId) =>
    api.delete(`/api/collections/${collectionId}/products/${productId}`),
  updateProductSortOrder: (collectionId, productId, sortOrder) =>
    api.put(
      `/api/collections/${collectionId}/products/${productId}/sort-order`,
      { sortOrder }
    ),
  toggleHeroProduct: (collectionId, productId) =>
    api.post(
      `/api/collections/${collectionId}/products/${productId}/toggle-hero`
    ),
  toggleFeatured: (id) => api.patch(`/api/collections/${id}/featured`),
};

// ===== SKU DEFINITIONS API =====
export const skusAPI = {
  // Get all active SKU definitions
  getAll: () =>
    api.get("/api/skus").then((response) => ({
      ...response,
      data: (response.data || []).map((item) => ({
        ...item,
        categoryName: item.skuName,
        categoryCode: item.skuCode,
      })),
    })),

  // Get SKU definition by ID
  getById: (id) =>
    api.get(`/api/skus/${id}`).then((response) => ({
      ...response,
      data: response.data
        ? {
            ...response.data,
            categoryName: response.data.skuName,
            categoryCode: response.data.skuCode,
          }
        : response.data,
    })),

  // Get SKU definition by name
  getByName: (name) =>
    api.get(`/api/skus/name/${encodeURIComponent(name)}`).then((response) => ({
      ...response,
      data: response.data
        ? {
            ...response.data,
            categoryName: response.data.skuName,
            categoryCode: response.data.skuCode,
          }
        : response.data,
    })),

  // Check if SKU exists by name
  checkExists: (name) =>
    api.get(`/api/skus/exists/${encodeURIComponent(name)}`),

  // Get active count
  getActiveCount: () => api.get("/api/skus/count"),

  // CRUD operations
  create: (skuData) => {
    const payload = {
      skuName: skuData.skuName ?? skuData.categoryName,
      description: skuData.description ?? skuData.categoryDescription,
      skuCode: skuData.skuCode ?? skuData.categoryCode,
    };
    return api.post("/api/skus", payload);
  },
  update: (id, skuData) => {
    const payload = {
      skuName: skuData.skuName ?? skuData.categoryName,
      description: skuData.description ?? skuData.categoryDescription,
      skuCode: skuData.skuCode ?? skuData.categoryCode,
    };
    return api.put(`/api/skus/${id}`, payload);
  },
  delete: (id) => api.delete(`/api/skus/${id}`),
  deactivate: (id) => api.patch(`/api/skus/${id}/deactivate`),
};

// ===== CATEGORIES API (MISA Product Categories - READ ONLY) =====
export const categoriesAPI = {
  // Get all active categories synced from MISA
  getAll: () => api.get("/api/categories"),

  // Get category by MISA category ID
  getById: (categoryId) => api.get(`/api/categories/${categoryId}`),

  // Get root categories (no parent)
  getRootCategories: () => api.get("/api/categories/root"),

  // Get child categories by parent ID
  getChildCategories: (parentId) => api.get(`/api/categories/children/${parentId}`),

  // Note: Categories are managed in MISA ERP and synced to Mirror.
  // For backwards compatibility with components expecting CRUD operations,
  // these methods throw errors directing users to MISA
  create: () => {
    throw new Error("Categories must be created in MISA ERP and will sync automatically");
  },
  update: () => {
    throw new Error("Categories must be updated in MISA ERP and will sync automatically");
  },
  delete: () => {
    throw new Error("Categories must be deleted in MISA ERP and will sync automatically");
  },
};

// ===== CERTIFICATES API =====
export const certificatesAPI = {
  // Get all certificates
  getAll: () => api.get("/api/certificates"),

  // Get certificate by ID
  getById: (id) => api.get(`/api/certificates/${id}`),

  // Get certificate by code
  getByCode: (code) => api.get(`/api/certificates/by-code?code=${encodeURIComponent(code)}`),

  // Get certificates by type (IGI, etc.)
  getByType: (type) => api.get(`/api/certificates/by-type?type=${type}`),

  // Create new certificate
  create: (certificateData) => api.post("/api/certificates", certificateData),

  // Update certificate
  update: (id, certificateData) => api.put(`/api/certificates/${id}`, certificateData),

  // Delete certificate (soft delete)
  delete: (id) => api.delete(`/api/certificates/${id}`),
};

// ===== COMPONENTS API =====
export const componentsAPI = {
  // Get all active components
  getAll: () =>
    api.get("/api/components").then((response) => ({
      ...response,
      data: (response.data || []).map((component) => ({
        ...component,
        categoryId: component.skuId,
        categoryName: component.skuName,
      })),
    })),

  // Get component by ID
  getById: (id) =>
    api.get(`/api/components/${id}`).then((response) => ({
      ...response,
      data: response.data
        ? {
            ...response.data,
            categoryId: response.data.skuId,
            categoryName: response.data.skuName,
          }
        : response.data,
    })),

  // Get components by SKU definition ID (legacy name kept for compatibility)
  getByCategoryId: (skuId) =>
    api.get(`/api/components/sku/${skuId}`).then((response) => ({
      ...response,
      data: (response.data || []).map((component) => ({
        ...component,
        categoryId: component.skuId,
        categoryName: component.skuName,
      })),
    })),

  // Get component by name
  getByName: (name) =>
    api.get(`/api/components/name/${encodeURIComponent(name)}`),

  // Check if component exists
  checkExists: (name, skuId) =>
    api.get(
      `/api/components/exists/${encodeURIComponent(
        name
      )}/sku/${skuId}`
    ),

  // Get active count
  getActiveCount: () => api.get("/api/components/count"),

  // CRUD operations
  create: (componentData) => {
    const payload = {
      ...componentData,
      skuId: componentData.skuId ?? componentData.categoryId,
    };
    delete payload.categoryId;
    return api.post("/api/components", payload);
  },
  update: (id, componentData) => {
    const payload = {
      ...componentData,
      skuId: componentData.skuId ?? componentData.categoryId,
    };
    delete payload.categoryId;
    return api.put(`/api/components/${id}`, payload);
  },
  delete: (id) => api.delete(`/api/components/${id}`),
  deactivate: (id) => api.patch(`/api/components/${id}/deactivate`),
};

// ===== COMPONENT OPTIONALS API =====
export const componentOptionalsAPI = {
  // Get all active component optionals
  getAll: () => api.get("/api/component-optionals"),

  // Get component optional by ID
  getById: (id) => api.get(`/api/component-optionals/${id}`),

  // Get component optionals by component ID
  getByComponentId: (componentId) =>
    api.get(`/api/component-optionals/component/${componentId}`),

  // Get component optional by name
  getByName: (name) =>
    api.get(`/api/component-optionals/name/${encodeURIComponent(name)}`),

  // Check if component optional exists
  checkExists: (name, componentId) =>
    api.get(
      `/api/component-optionals/exists/${encodeURIComponent(
        name
      )}/component/${componentId}`
    ),

  // Get active count
  getActiveCount: () => api.get("/api/component-optionals/count"),

  // CRUD operations
  create: (data) => api.post("/api/component-optionals", data),
  update: (id, data) => api.put(`/api/component-optionals/${id}`, data),
  delete: (id) => api.delete(`/api/component-optionals/${id}`),
  deactivate: (id) => api.patch(`/api/component-optionals/${id}/deactivate`),
};

// ===== USERS API =====
// Note: User endpoints use /api/v1/users/* path (merged from user-service)
export const usersAPI = {
  // Get all users
  getAll: () => api.get("/api/v1/users"),

  // Get user by ID
  getById: (id) => api.get(`/api/v1/users/${id}`),

  // Get user by username
  getByUsername: (username) => api.get(`/api/v1/users/username/${username}`),

  // Get user by email
  getByEmail: (email) =>
    api.get(`/api/v1/users/email/${encodeURIComponent(email)}`),

  // Get users by role
  getByRole: (role) => api.get(`/api/v1/users/role/${role}`),

  // Get users by status
  getByStatus: (status) => api.get(`/api/v1/users/status/${status}`),

  // Search users
  search: (searchTerm) =>
    api.get(`/api/v1/users/search?q=${encodeURIComponent(searchTerm)}`),

  // Get user statistics
  getStatistics: () => api.get("/api/v1/users/statistics"),

  // Health check
  health: () => api.get("/api/v1/users/health"),

  // CRUD operations
  create: (userData) => api.post("/api/v1/users", userData),
  createByAdmin: (userData) => api.post("/api/v1/users/admin/create", userData),
  update: (id, userData) => api.put(`/api/v1/users/${id}`, userData),
  delete: (id) => api.delete(`/api/v1/users/${id}`),
  updateStatus: (id, status) =>
    api.patch(`/api/v1/users/${id}/status`, { status }),
  updateRole: (id, role) => api.patch(`/api/v1/users/${id}/role`, { role }),
};

// ===== RBAC MATRIX =====
export const roleMatrixAPI = {
  getMatrix: () => api.get("/internal/roles/matrix"),
};

// ===== UTILITY FUNCTIONS =====

/**
 * Handle API errors consistently
 */
export const handleAPIError = (error, defaultMessage = "An error occurred") => {
  if (error.response) {
    // Server responded with error status
    const message =
      error.response.data?.message ||
      error.response.data?.error ||
      `Server error: ${error.response.status}`;
    return {
      message,
      status: error.response.status,
      data: error.response.data,
    };
  } else if (error.request) {
    // Request made but no response
    return {
      message: "Network error - please check your connection",
      status: null,
    };
  } else {
    // Something else happened
    return { message: error.message || defaultMessage, status: null };
  }
};

/**
 * Create a loading state hook for API calls
 */
export const createApiHook = (apiCall) => {
  return async (params = {}) => {
    try {
      const response = await apiCall(params);
      return { data: response.data, error: null, loading: false };
    } catch (error) {
      const errorInfo = handleAPIError(error);
      return { data: null, error: errorInfo, loading: false };
    }
  };
};

// ===== DESIGNERS API =====
export const designersAPI = {
  // Get all active designers (filtered by current user if authenticated)
  getAll: () => api.get("/api/designers"),

  // Get current user's designers (authenticated endpoint)
  getCurrentUserDesigners: () => api.get("/api/designers?filter=current-user"),

  // Get current user's designer info for dashboard
  getCurrentDesignerInfo: () =>
    api.get("/api/designers?filter=current-user").then((response) => {
      const designers = response.data;

      return {
        data: designers && designers.length > 0 ? designers[0] : null,
        hasDesigner: designers && designers.length > 0,
      };
    }),

  // Get designer by ID
  getById: (id) => api.get(`/api/designers/${id}`),

  // Get designer by code
  getByCode: (code) => api.get(`/api/designers/code/${code}`),

  // Get designers by specialty
  getBySpecialty: (specialty) =>
    api.get(`/api/designers/specialty/${specialty}`),

  // Get verified designers
  getVerified: () => api.get("/api/designers/verified"),

  // Get featured designers
  getFeatured: () => api.get("/api/designers/featured"),

  // Search designers with pagination
  search: (searchTerm, params = {}) => {
    const { page = 0, size = 20 } = params;
    return api.get(`/api/designers/search`, {
      params: { search: searchTerm, page, size },
    });
  },

  // Check if designer exists by code
  checkExists: (code) => api.get(`/api/designers/exists/${code}`),

  // Get active count
  getActiveCount: () => api.get("/api/designers/count"),

  // CRUD operations
  create: (designerData) => api.post("/api/designers", designerData),
  update: (id, designerData) => api.put(`/api/designers/${id}`, designerData),
  delete: (id) => api.delete(`/api/designers/${id}`),
  deactivate: (id) => api.patch(`/api/designers/${id}/deactivate`),

  // Get design products for a specific designer
  getDesigns: (designerId) => api.get(`/api/designers/${designerId}/designs`),

  // Get design products for current user's designer
  getCurrentDesignerDesigns: () =>
    designersAPI.getCurrentDesignerInfo().then((designerInfo) => {
      if (designerInfo.hasDesigner && designerInfo.data) {
        return designersAPI.getDesigns(designerInfo.data.id);
      } else {
        return { data: [] };
      }
    }),

  // Get designer dashboard data by designer ID
  getDashboard: (designerId) =>
    api.get(`/api/designers/${designerId}/dashboard`),

  // Get designer dashboard data for current user
  getCurrentUserDashboard: () => api.get("/api/designers/dashboard"),
};

// Keep the old designerAPI for backward compatibility but mark as deprecated
export const designerAPI = {
  // Get designer dashboard data - DEPRECATED: Use designersAPI.getCurrentUserDashboard()
  getDashboard: () => designersAPI.getCurrentUserDashboard(),
};

// ===== VENDORS API =====
export const vendorsAPI = {
  // Get all active vendors (filtered by current user if authenticated)
  getAll: () => api.get("/api/vendors"),

  // Get current user's vendors (authenticated endpoint)
  getCurrentUserVendors: () => api.get("/api/vendors?filter=current-user"),

  // Get current user's vendor info for dashboard
  getCurrentVendorInfo: () =>
    api.get("/api/vendors?filter=current-user").then((response) => {
      const vendors = response.data;

      return {
        data: vendors && vendors.length > 0 ? vendors[0] : null,
        hasVendor: vendors && vendors.length > 0,
      };
    }),

  // Get vendor by ID
  getById: (id) => api.get(`/api/vendors/${id}`),

  // Get vendor by code
  getByCode: (code) => api.get(`/api/vendors/code/${code}`),

  // Get vendors by country
  getByCountry: (country) => api.get(`/api/vendors/country/${country}`),

  // Get vendors by type
  getByType: (vendorType) => api.get(`/api/vendors/type/${vendorType}`),

  // Search vendors with pagination
  search: (searchTerm, params = {}) => {
    const { page = 0, size = 20 } = params;
    return api.get(`/api/vendors/search`, {
      params: { search: searchTerm, page, size },
    });
  },

  // Check if vendor exists by code
  checkExists: (code) => api.get(`/api/vendors/exists/${code}`),

  // Get active count
  getActiveCount: () => api.get("/api/vendors/count"),

  // CRUD operations
  create: (vendorData) => api.post("/api/vendors", vendorData),
  update: (id, vendorData) => api.put(`/api/vendors/${id}`, vendorData),
  delete: (id) => api.delete(`/api/vendors/${id}`),
  deactivate: (id) => api.patch(`/api/vendors/${id}/deactivate`),

  // Get products for a specific vendor
  getProducts: (vendorId) => api.get(`/api/vendors/${vendorId}/products`),

  // Get products for current user's vendor
  getCurrentVendorProducts: () =>
    vendorsAPI.getCurrentVendorInfo().then((vendorInfo) => {
      if (vendorInfo.hasVendor && vendorInfo.data) {
        return vendorsAPI.getProducts(vendorInfo.data.id);
      } else {
        return { data: [] };
      }
    }),

  // Get orders for a specific vendor
  getVendorOrders: (vendorId) => api.get(`/api/vendors/${vendorId}/orders`),

  // Get orders for current user's vendor
  getCurrentVendorOrders: () =>
    vendorsAPI.getCurrentVendorInfo().then((vendorInfo) => {
      if (vendorInfo.hasVendor && vendorInfo.data) {
        return vendorsAPI.getVendorOrders(vendorInfo.data.id);
      } else {
        return { data: [] };
      }
    }),
};

// ===== SKU CODES API =====
// NOTE: These endpoints have been consolidated into /api/skus/ on the backend
// The API paths below use the new merged endpoints
export const skuCodesAPI = {
  // Generate jewelry SKU
  generateJewelrySku: (codeData) =>
    api.post("/api/skus/generate/jewelry", codeData),

  // Generate packaging SKU
  generatePackagingSku: (codeData) =>
    api.post("/api/skus/generate/packaging", codeData),

  // Fetch generated SKUs saved in the catalog
  getGeneratedSkus: (params = {}) =>
    api.get("/api/skus/generated", { params }),

  // Bulk import jewelry SKUs from CSV
  importJewelrySkus: (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post("/api/skus/import/jewelry", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  // Bulk import packaging SKUs from CSV
  importPackagingSkus: (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post("/api/skus/import/packaging", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  // Search SKUs with fuzzy matching
  searchSkus: (query, limit = 50, threshold = 0.3) =>
    api.get("/api/skus/search", {
      params: { q: query, threshold, limit },
    }),

  // Search SKUs using POST
  searchSkusPost: (searchData) =>
    api.post("/api/skus/search", searchData),

  // Export all generated SKUs to MISA template
  exportAllToMisa: () =>
    api.get("/api/skus/export-misa", {
      responseType: "blob",
    }),

  // Export selected products by IDs to MISA template
  exportByIdsToMisa: (productIds) =>
    api.post("/api/skus/export-misa-by-ids", productIds, {
      responseType: "blob",
    }),

  // Export products by category to MISA template
  exportByCategoryToMisa: (category) =>
    api.get("/api/skus/export-misa-by-category", {
      params: { category },
      responseType: "blob",
    }),
};

// ===== DROPDOWN CONFIGURATION API =====
export const dropdownConfigAPI = {
  // Get all SKU prefix options
  getPrefixes: () => api.get("/api/dropdown-config/prefixes"),

  // Get all material options
  getMaterials: () => api.get("/api/dropdown-config/materials"),

  // Get all material color options
  getMaterialColors: () => api.get("/api/dropdown-config/material-colors"),

  // Get all stone origin options
  getStoneOrigins: () => api.get("/api/dropdown-config/stone-origins"),

  // Get all stone shape options
  getStoneShapes: () => api.get("/api/dropdown-config/stone-shapes"),

  // Get all stone weight options
  getStoneWeights: () => api.get("/api/dropdown-config/stone-weights"),

  // Get all side stones options
  getSideStones: () => api.get("/api/dropdown-config/side-stones"),

  // Get all country of origin options
  getCountries: () => api.get("/api/dropdown-config/countries"),
};

// ===== FILE UPLOAD API =====
export const fileUploadAPI = {
  // Upload file (public endpoint - no auth required)
  upload: (
    file,
    description = "",
    bucketName = "mirror-storage",
    folderPath = "public"
  ) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("description", description);
    formData.append("bucketName", bucketName);
    formData.append("folderPath", folderPath);

    // Use raw axios without interceptors to bypass Authorization header
    return axios.post(`${API_BASE_URL}/api/files/upload`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      timeout: 60000, // 60 seconds for file upload
    });
  },
};

// ===== CLOUDFLARE R2 API =====
// Requires authentication
// Staff roles: no rate limit | USER role: 20 uploads/hour
export const r2API = {
  // Upload file directly to R2 (requires auth)
  upload: (file, folder = "uploads") => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("folder", folder);

    const token = localStorage.getItem("accessToken");
    const headers = {
      "Content-Type": "multipart/form-data",
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    return axios.post(`${API_BASE_URL}/api/r2/upload`, formData, {
      headers,
      timeout: 60000,
    });
  },

  // Get presigned URL for direct upload (optional - for large files)
  getPresignedUploadUrl: (filename, folder = "uploads", contentType = "application/octet-stream") => {
    return api.post("/api/r2/presigned-upload", null, {
      params: { filename, folder, contentType }
    });
  },

  // Get public URL for a file
  getPublicUrl: (key) => {
    return api.get("/api/r2/public-url", { params: { key } });
  },

  // Delete file from R2
  deleteFile: (key) => {
    return api.delete("/api/r2/files", { params: { key } });
  },

  // Health check
  health: () => api.get("/api/r2/health"),
};

// ===== NOTIFICATIONS API =====
export const notificationsAPI = {
  // Send email notification (public endpoint - no auth required)
  sendEmail: (recipients, emailType, model, captchaToken) => {
    // Use raw axios without interceptors to bypass Authorization header
    return axios.post(
      `${API_BASE_URL}/api/notifications/email`,
      {
        recipients,
        emailType,
        model,
        captchaToken,
      },
      {
        headers: {
          "Content-Type": "application/json",
        },
        timeout: 30000,
      }
    );
  },
};

// ===== MISA AMIS API =====
export const misaAmisAPI = {
  // Get AMIS authentication status
  getStatus: () => api.get("/api/misa/amis/status"),

  // Authenticate with MISA AMIS
  authenticate: () => api.post("/api/misa/amis/authenticate"),

  // Get inventory balance from MISA AMIS
  getInventoryBalance: (params = {}) =>
    api.get("/api/misa/amis/inventory-balance", { params }),

  // Get warehouses from MISA AMIS
  getWarehouses: (params = {}) =>
    api.get("/api/misa/amis/warehouses", { params }),

  // Get inventory items from MISA AMIS
  getInventoryItems: (params = {}) =>
    api.get("/api/misa/amis/inventory-items", { params }),
};

// ===== CURRENCY API ===== (FREE exchangerate-api.com)
export const currencyAPI = {
  // Get exchange rate between two currencies
  getExchangeRate: (from, to) =>
    api.get("/api/currency/rate", { params: { from, to } }),

  // Get all exchange rates for a base currency
  getAllExchangeRates: (baseCurrency) =>
    api.get(`/api/currency/rates/${baseCurrency}`),

  // Calculate import cost with customs, VAT, and fees
  calculateImportCost: (request) =>
    api.post("/api/currency/calculate-import-cost", request),
};

// ===== VENDOR MATCHING API =====
export const vendorMatchingAPI = {
  // Find matching vendors for a collection plan
  findMatchingVendors: (request) =>
    api.post("/api/vendor-matching/find", request),

  // Get vendor matches by collection plan ID
  getMatchesByCollectionPlan: (collectionPlanId) =>
    api.get(`/api/vendor-matching/collection-plan/${collectionPlanId}`),
};

// ===== VENDOR OPTIMIZATION API =====
export const vendorOptimizationAPI = {
  // Calculate optimal vendors based on multi-criteria optimization
  calculateOptimalVendors: (request) =>
    api.post("/api/vendor-optimization/calculate", request),
};

// ===== PRECIOUS METAL API ===== (PAID API - metals-api.com or goldapi.io)
// NOTE: Requires PAID API key ($50-200/month) - currently using mock data
export const preciousMetalAPI = {
  // Get current precious metal prices in USD
  getPricesInUSD: () => api.get("/api/precious-metals/prices/usd"),

  // Get current precious metal prices in VND
  getPricesInVND: () => api.get("/api/precious-metals/prices/vnd"),

  // Calculate material cost for a given weight and metal type
  calculateMaterialCost: (request) =>
    api.post("/api/precious-metals/calculate-material-cost", request),
};

// ===== LAB-GROWN DIAMONDS API =====
export const diamondsAPI = {
  // Get all active diamonds
  getAllDiamonds: () => api.get("/api/diamonds"),

  // Get diamond by ID
  getDiamondById: (id) => api.get(`/api/diamonds/${id}`),

  // Get diamond by SKU code
  getDiamondBySkuCode: (skuCode) => api.get(`/api/diamonds/sku/${skuCode}`),

  // Get diamond by certificate number
  getDiamondByCertNumber: (certNumber) => api.get(`/api/diamonds/cert/${certNumber}`),

  // Get diamonds by invoice number
  getDiamondsByInvoice: (invoiceNumber) => api.get(`/api/diamonds/invoice/${invoiceNumber}`),

  // Get diamonds by status
  getDiamondsByStatus: (status) => api.get(`/api/diamonds/status/${status}`),

  // Search diamonds with filters
  searchDiamonds: (filters) => api.post("/api/diamonds/search", filters),

  // Get inventory statistics
  getInventoryStats: () => api.get("/api/diamonds/stats"),

  // Generate SKU code
  generateSku: (request) => api.post("/api/diamonds/generate-sku", request),

  // Create new diamond
  createDiamond: (request) => api.post("/api/diamonds", request),

  // Update diamond
  updateDiamond: (id, request) => api.put(`/api/diamonds/${id}`, request),

  // Update diamond status
  updateDiamondStatus: (id, status) => api.patch(`/api/diamonds/${id}/status`, { status }),

  // Delete diamond (soft delete)
  deleteDiamond: (id) => api.delete(`/api/diamonds/${id}`),
};

// Stock Reconciliation API
export const stockReconciliationAPI = {
  // Create a new stock reconciliation record with report file
  createRecord: (data, file) => {
    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
    if (file) {
      formData.append("file", file);
    }
    return api.post("/api/stock-reconciliation", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  // Get a single record by ID
  getRecord: (id) => api.get(`/api/stock-reconciliation/${id}`),

  // List records with filters
  listRecords: (params = {}) => api.get("/api/stock-reconciliation", { params }),

  // Get download URL for report file
  getDownloadUrl: (id) => api.get(`/api/stock-reconciliation/${id}/download`),

  // Delete a record (soft delete)
  deleteRecord: (id) => api.delete(`/api/stock-reconciliation/${id}`),
};

// ===== WAREHOUSE MANAGEMENT API =====
export const warehouseAPI = {
  // Warehouses
  getAll: () => api.get("/api/warehouses"),
  getById: (id) => api.get(`/api/warehouses/${id}`),
  create: (data) => api.post("/api/warehouses", data),
  update: (id, data) => api.put(`/api/warehouses/${id}`, data),
  delete: (id) => api.delete(`/api/warehouses/${id}`),

  // Racks
  getRacks: (warehouseId) => api.get(`/api/warehouses/${warehouseId}/racks`),
  createRack: (warehouseId, data) => api.post(`/api/warehouses/${warehouseId}/racks`, data),
  updateRack: (id, data) => api.put(`/api/warehouses/racks/${id}`, data),
  deleteRack: (id) => api.delete(`/api/warehouses/racks/${id}`),

  // Slots
  getSlots: (rackId) => api.get(`/api/warehouses/racks/${rackId}/slots`),
  createSlot: (rackId, data) => api.post(`/api/warehouses/racks/${rackId}/slots`, data),
  createSlotsBatch: (rackId, data) => api.post(`/api/warehouses/racks/${rackId}/slots/batch`, data),

  // Positions
  getPending: (params) => api.get("/api/warehouses/positions/pending", { params }),
  assignPosition: (data) => api.post("/api/warehouses/positions/assign", data),
  bulkAssign: (data) => api.post("/api/warehouses/positions/bulk-assign", data),

  // Stock & Stats
  stockInward: (data) => api.post("/api/warehouses/stock/inward", data),
  getStats: () => api.get("/api/warehouses/stats"),
};
// Export the axios instance for custom calls
export default api;
