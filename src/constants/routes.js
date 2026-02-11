// Routes Constants - Centralized route management
// Only update routes here, all other files will use these constants

export const ROUTES = {
  // Public routes (keep original)
  HOME: "/",
  SCAVENGER_HUNT: "/scavenger-hunt",
  VERIFY_EMAIL: "/verify-email",

  // UUID-protected routes
  WELCOME: "/5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  HOME_PAGE: "/8a9b0c1d-2e3f-4a5b-6c7d-8e9f0a1b2c3d",
  AUTH: "/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  AUTH_LOGIN: "/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/login",
  AUTH_REGISTER: "/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/register",
  FORGOT_PASSWORD: "/a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d/forgot-password",
  PRODUCTS: "/b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
  COLLECTIONS: "/d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a",
  COLLECTION_DETAIL: "/d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a/:collectionId",
  SERVICES: "/e5f6a7b8-c9d0-1e2f-3a4b-5c6d7e8f9a0b",
  SERVICES_DETAIL: "/e5f6a7b8-c9d0-1e2f-3a4b-5c6d7e8f9a0b/detail",
  SUPPORT: "/f6a7b8c9-d0e1-2f3a-4b5c-6d7e8f9a0b1c",
  SUPPORT_DETAIL: "/f6a7b8c9-d0e1-2f3a-4b5c-6d7e8f9a0b1c/detail",
  CONTACT: "/a7b8c9d0-e1f2-3a4b-5c6d-7e8f9a0b1c2d",
  ABOUT: "/b8c9d0e1-f2a3-4b5c-6d7e-8f9a0b1c2d3e",
  LOCATIONS: "/c9d0e1f2-a3b4-5c6d-7e8f-9a0b1c2d3e4f",
  NEWS: "/d0e1f2a3-b4c5-6d7e-8f9a-0b1c2d3e4f5a",
  NEWS_DETAIL: "/d0e1f2a3-b4c5-6d7e-8f9a-0b1c2d3e4f5a/:slug",
  ALL_GEMS: "/c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f",
  PRODUCT_DETAIL: "/f4g5h6i7-j8k9-0l1m-2n3o-4p5q6r7s8t9u/:productId",
  USER_PROFILE: "/e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
  PREMIUM: "/premium", // Premium AR Try-On
  PREMIUM_DEV: "/premium-dev", // Premium AR Development
  MESH_INSPECTOR: "/mesh-inspector", // 3D Mesh Inspector Tool
  DB_EXPLORER: "/db-explorer", // DB Explorer - Export CSV/XLSX
  PRODUCTS_V2: "/7b8e9f0a-3c4d-5e6f-7a8b-9c0d1e2f3a4b",
  PRODUCTS_LEFT: "/4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a",

  // Dashboard routes (keep original - protected by auth)
  DASHBOARD_ADMIN: "/dashboard/admin",
  DASHBOARD_ADMIN_MANAGE: "/dashboard/admin/manage-products",
  DASHBOARD_VENDOR: "/dashboard/vendor",
  DASHBOARD_DESIGNER: "/dashboard/designer",

  // Event routes - The Muse of Love
  EVENT: "/event", // Keep original event route
  EVENT_GUIDE: "/the-muse-of-love-grown", // Main landing page
  EVENT_LOGIN: "/the-muse-of-love-grown/login",
  EVENT_NAME: "/the-muse-of-love-grown/name",
  EVENT_CHOOSE_SHAPE: "/the-muse-of-love-grown/choose-shape",
  EVENT_ADMIN: "/the-muse-of-love-grown/admin",
  EVENT_PLACE_NOTE: "/the-muse-of-love-grown/your-note", // New place note screen (step 1)
  EVENT_WRITE_MESSAGE: "/the-muse-of-love-grown/your-melody", // Write message screen (step 2)
  EVENT_CHOOSE_NOTE: "/the-muse-of-love-grown/your-wallpaper", // Your wallpaper screen (step 3)
  EVENT_THANKYOU: "/the-muse-of-love-grown/mirror-thankyou", // Thank you screen (final)
  EVENT_RING_VIEWER: "/the-muse-of-love-grown/ring-viewer", // Fullscreen 3D ring viewer


  // Ring Customizer
  RING_CUSTOMIZER: "/ring-customizer",
  // Product Finder - Quiz to find perfect product
  PRODUCT_FINDER: "/find-your-piece",
  PRODUCT_FINDER_CHOOSE_SHAPE: "/find-your-piece/choose-shape",
  PRODUCT_FINDER_CHOOSE_BAND: "/find-your-piece/choose-band",
  PRODUCT_FINDER_CHOOSE_SIDESTONE: "/find-your-piece/choose-sidestone",
  PRODUCT_FINDER_RESULT: "/find-your-piece/result",

  // POD Admin routes (nested under admin dashboard)
  POD_ADMIN: "/dashboard/admin/pod",
  POD_ADMIN_DASHBOARD: "/dashboard/admin/pod/dashboard",
  POD_ADMIN_PARTNERS: "/dashboard/admin/pod/partners",
  POD_ADMIN_PARTNER_DETAIL: "/dashboard/admin/pod/partners/:partnerId",
  POD_ADMIN_PARTNER_CREATE: "/dashboard/admin/pod/partners/create",
  POD_ADMIN_PODS: "/dashboard/admin/pod/pods",
  POD_ADMIN_LOCATIONS: "/dashboard/admin/pod/locations",
  POD_ADMIN_POD_DETAIL: "/dashboard/admin/pod/pods/:podId",
  POD_ADMIN_POD_CREATE: "/dashboard/admin/pod/pods/create",
  POD_ADMIN_QRCODES: "/dashboard/admin/pod/qr-codes",
  POD_ADMIN_QRCODE_DETAIL: "/dashboard/admin/pod/qr-codes/:qrCodeId",
  POD_ADMIN_SCANS: "/dashboard/admin/pod/scans",
  POD_ADMIN_USER_ATTRIBUTIONS: "/dashboard/admin/pod/user-attributions",
  POD_ADMIN_COMMISSIONS: "/dashboard/admin/pod/commissions",
  POD_ADMIN_COMMISSION_DETAIL: "/dashboard/admin/pod/commissions/:commissionId",

  // POD Partner Portal routes
  POD_PARTNER: "/pod-partner",
  POD_PARTNER_DASHBOARD: "/pod-partner/dashboard",
  POD_PARTNER_PODS: "/pod-partner/pods",
  POD_PARTNER_LOCATIONS: "/pod-partner/locations",
  POD_PARTNER_QRCODES: "/pod-partner/qr-codes",
  POD_PARTNER_SCANS: "/pod-partner/scans",
  POD_PARTNER_USER_ATTRIBUTIONS: "/pod-partner/user-attributions",
  POD_PARTNER_COMMISSIONS: "/pod-partner/commissions",

  // Phygital Partner Portal routes
  POD_PARTNER_PHYGITAL_DASHBOARD: "/pod-partner/phygital-dashboard",
  POD_PARTNER_INVENTORY: "/pod-partner/inventory",
  POD_PARTNER_INVENTORY_DETAIL: "/pod-partner/inventory/:inventoryId",
  POD_PARTNER_WHOLESALE_ORDERS: "/pod-partner/wholesale-orders",
  POD_PARTNER_WHOLESALE_ORDER_DETAIL: "/pod-partner/wholesale-orders/:orderId",
  POD_PARTNER_SALES: "/pod-partner/sales",
  POD_PARTNER_SALE_DETAIL: "/pod-partner/sales/:saleId",
  POD_PARTNER_SALES_REPORT: "/pod-partner/sales-report",

  // Admin Wholesale routes (nested under admin dashboard)
  POD_ADMIN_WHOLESALE_ORDERS: "/dashboard/admin/pod/wholesale-orders",
  POD_ADMIN_WHOLESALE_ORDER_DETAIL: "/dashboard/admin/pod/wholesale-orders/:orderId",
  POD_ADMIN_PHYGITAL_PARTNERS: "/dashboard/admin/pod/phygital-partners",
  POD_ADMIN_PHYGITAL_PARTNER_DETAIL: "/dashboard/admin/pod/phygital-partners/:partnerId",

  // RFID Scanner (for PDA devices)
  RFID_SCANNER: "/rfid-scanner",

  // RFID Label System routes (nested under admin dashboard)
  LABEL_TEMPLATES: "/dashboard/admin/label-templates",
  LABEL_DESIGNER_NEW: "/dashboard/admin/label-designer/new",
  LABEL_DESIGNER_EDIT: "/dashboard/admin/label-designer/:id",
  PRINT_JOBS: "/dashboard/admin/print-jobs",
  PRINT_JOB_CREATE: "/dashboard/admin/print-jobs/new",
  RFID_TAGS: "/dashboard/admin/rfid-tags",
};

// Helper to get collection detail route with ID
export const getCollectionDetailRoute = (collectionId) => {
  return ROUTES.COLLECTION_DETAIL.replace(":collectionId", collectionId);
};

// Helper to get news detail route with slug
export const getNewsDetailRoute = (slug) => {
  return ROUTES.NEWS_DETAIL.replace(":slug", slug);
};

// Helper to get product detail route with ID
export const getProductDetailRoute = (productId) => {
  return ROUTES.PRODUCT_DETAIL.replace(":productId", productId);
};

// Helper to get POD admin partner detail route
export const getPodPartnerDetailRoute = (partnerId) => {
  return ROUTES.POD_ADMIN_PARTNER_DETAIL.replace(":partnerId", partnerId);
};

// Helper to get POD admin pod detail route
export const getPodDetailRoute = (podId) => {
  return ROUTES.POD_ADMIN_POD_DETAIL.replace(":podId", podId);
};

// Helper to get POD admin QR code detail route
export const getPodQrCodeDetailRoute = (qrCodeId) => {
  return ROUTES.POD_ADMIN_QRCODE_DETAIL.replace(":qrCodeId", qrCodeId);
};

// Helper to get POD admin commission detail route
export const getCommissionDetailRoute = (commissionId) => {
  return ROUTES.POD_ADMIN_COMMISSION_DETAIL.replace(":commissionId", commissionId);
};

// Helper to get partner inventory detail route
export const getPartnerInventoryDetailRoute = (inventoryId) => {
  return ROUTES.POD_PARTNER_INVENTORY_DETAIL.replace(":inventoryId", inventoryId);
};

// Helper to get partner wholesale order detail route
export const getPartnerWholesaleOrderDetailRoute = (orderId) => {
  return ROUTES.POD_PARTNER_WHOLESALE_ORDER_DETAIL.replace(":orderId", orderId);
};

// Helper to get partner sale detail route
export const getPartnerSaleDetailRoute = (saleId) => {
  return ROUTES.POD_PARTNER_SALE_DETAIL.replace(":saleId", saleId);
};

// Helper to get admin wholesale order detail route
export const getAdminWholesaleOrderDetailRoute = (orderId) => {
  return ROUTES.POD_ADMIN_WHOLESALE_ORDER_DETAIL.replace(":orderId", orderId);
};

// Helper to get admin phygital partner detail route
export const getAdminPhygitalPartnerDetailRoute = (partnerId) => {
  return ROUTES.POD_ADMIN_PHYGITAL_PARTNER_DETAIL.replace(":partnerId", partnerId);
};
