// Routes Constants - Centralized route management
// Only update routes here, all other files will use these constants

export const ROUTES = {
  // Public routes (keep original)
  HOME: "/",
  IMMERSIVE_SHOWROOM: "/immersive-showroom",
  BOOK_APPOINTMENT: "/book-an-appointment",
  MILAN_SUBMIT: "/mirror-in-milan-digital-jewelry-week",
  MILAN_SUBMIT_SUCCESS: "/mirror-in-milan-digital-jewelry-week/submit-success",
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
  CONTACT_V2: "/contact-v2",
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
  UNIVERSE_FINAL: "/1f2e3d4c-5b6a-7c8d-9e0f-1a2b3c4d5e6f",
  NEWS_V2: "/5a6b7c8d-9e0f-1a2b-3c4d-5e6f7a8b9c0d",

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
  EVENT_DISPLAY: "/the-muse-of-love-grown/display",
  EVENT_ADMIN: "/the-muse-of-love-grown/admin",
  EVENT_CHRISTMAS: "/the-muse-of-love-grown/christmas",
  EVENT_PLACE_NOTE: "/the-muse-of-love-grown/your-note", // New place note screen (step 1)
  EVENT_WRITE_MESSAGE: "/the-muse-of-love-grown/your-melody", // Write message screen (step 2)
  EVENT_CHOOSE_NOTE: "/the-muse-of-love-grown/your-wallpaper", // Your wallpaper screen (step 3)
  EVENT_CHOOSE_NOTE_V2: "/the-muse-of-love-grown/your-wallpaper-2", // Your wallpaper screen v2 (simplified)
  EVENT_THANKYOU: "/the-muse-of-love-grown/mirror-thankyou", // Thank you screen (final)
  EVENT_RING_VIEWER: "/the-muse-of-love-grown/ring-viewer", // Fullscreen 3D ring viewer

  // Interactive experiences
  BIRTHDAY_CAKE: "/birthday-cake",
  TEST_NOTES: "/test-notes", // Test page for music notes

  // Inventory Management routes
  INVENTORY: "/inventory",
  INVENTORY_DASHBOARD: "/inventory/dashboard",
  INVENTORY_SCANNER: "/inventory/scanner",
  INVENTORY_ADD_PRODUCT: "/inventory/add",
  INVENTORY_PRODUCTS: "/inventory/products",
  INVENTORY_PRODUCT_DETAIL: "/inventory/products/:id",
  INVENTORY_PRODUCT_EDIT: "/inventory/products/:id/edit",
  INVENTORY_PRINT: "/inventory/print",
  INVENTORY_CREATE_ORDER: "/inventory/create-order",
  INVENTORY_INVOICE: "/inventory/invoice",
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

// Helper to get inventory product detail route
export const getInventoryProductDetailRoute = (id) => {
  return ROUTES.INVENTORY_PRODUCT_DETAIL.replace(":id", id);
};

// Helper to get inventory product edit route
export const getInventoryProductEditRoute = (id) => {
  return ROUTES.INVENTORY_PRODUCT_EDIT.replace(":id", id);
};
