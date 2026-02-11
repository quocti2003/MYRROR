// src/routes/index.jsx

import { Routes, Route, Navigate, useLocation, useParams } from "react-router-dom";
import { lazy, Suspense } from "react";
import NavbarV4 from "@components/navbar/NavbarV4";
import Footer from "@components/footer/Footer";
import { ROUTES } from "@/constants/routes";
import { useImmersiveModal } from "@/contexts/ImmersiveModalContext";
import lazyWithRetry from "@/utils/lazyWithRetry";

// Lazy-load components (with retry to handle stale chunks after deploy)
const HomePage = lazyWithRetry(() => import("@pages/HomePage"));
const ProductsPage = lazyWithRetry(() => import("@pages/ProductsPage"));
const CollectionPage = lazyWithRetry(() => import("@pages/CollectionPage"));
const CollectionDetailPage = lazyWithRetry(() => import("@pages/CollectionDetailPage"));
const ProductDetailPage = lazyWithRetry(() => import("@pages/ProductDetailPage"));
const ServicesPage = lazyWithRetry(() => import("@pages/ServicesPage"));
const ServicesDetailPage = lazyWithRetry(() => import("@pages/ServicesDetailPage"));
const SupportPage = lazyWithRetry(() => import("@pages/SupportPage"));
const SupportDetailPage = lazyWithRetry(() => import("@pages/SupportDetailPage"));
const NotFoundPage = lazyWithRetry(() => import("@pages/NotFoundPage"));
const QrRedirect = lazyWithRetry(() => import("@pages/QrRedirect"));

const UniverseSection = lazyWithRetry(() =>
  import("@components/home-page/universeSection/MirrorExp.jsx")
);
const ManageProducts = lazyWithRetry(() =>
  import("@components/manage-products/ManageProducts.jsx")
);
const AdminDashboard = lazyWithRetry(() =>
  import("@components/admin-dashboard/AdminDashboard.jsx")
);
const VendorDashboard = lazyWithRetry(() =>
  import("@components/vendor-dashboard/VendorDashboard.jsx")
);
const DesignerDashboard = lazyWithRetry(() =>
  import("@components/designer-dashboard/DesignerDashboard.jsx")
);
const AuthPage = lazyWithRetry(() => import("@pages/AuthPage"));
const Login = lazyWithRetry(() => import("@components/login/Login"));
const Register = lazyWithRetry(() => import("@components/register/Register"));
const ForgotPassword = lazyWithRetry(() =>
  import("@components/forgot-password/ForgotPassword")
);
const EmailVerification = lazyWithRetry(() =>
  import("@components/email-verification/EmailVerification")
);
const Profile = lazyWithRetry(() => import("@components/profile/Profile"));
const ProtectedRoute = lazyWithRetry(() => import("@components/auth/ProtectedRoute"));
const AllGemsPage = lazyWithRetry(() => import("@pages/AllGemsPage"));
const AllNewsPage = lazyWithRetry(() => import("@pages/AllNewsPage"));
const NewCutPage = lazyWithRetry(() => import("@pages/NewCutPage"));
const MilanPage = lazyWithRetry(() => import("@pages/MilanPage"));
const ContactPage = lazyWithRetry(() => import("@pages/ContactPage"));
const DBExplorerPage = lazyWithRetry(() => import("@pages/DBExplorerPage"));

// News Detail Wrapper Component
const NewsDetailWrapper = () => {
  const { slug } = useParams();

  // Map slugs to their respective components
  const newsPages = {
    milan: MilanPage,
    "new-cut": NewCutPage,
    // Add more news article slugs here
  };

  const PageComponent = newsPages[slug] || NewCutPage;
  return <PageComponent />;
};
const AboutPage = lazyWithRetry(() => import("@pages/AboutPage"));
const LocationsPage = lazyWithRetry(() => import("@pages/LocationsPage"));
const WelcomePage = lazyWithRetry(() => import("@pages/WelcomePage"));
const ProductsV2 = lazyWithRetry(() => import("@components/productsV2/Products.jsx"));
const ProductsLeft = lazyWithRetry(() =>
  import("@components/productsV2/ProductsLeft.jsx")
);
const ScavengerHunt = lazyWithRetry(() =>
  import("@components/scavenger-hunt/ScavengerHunt")
);
const PremiumPage = lazyWithRetry(() => import("@pages/PremiumPage"));
const PremiumDevPage = lazyWithRetry(() => import("@pages/PremiumDevPage"));
const SimpleMeshInspector = lazyWithRetry(() =>
  import("@components/ijewelTryOn/quocti_dancefloor/SimpleMeshInspector")
);

// Event Pages
const EventPage = lazyWithRetry(() => import("@pages/Event/EventPage"));
const EventGuidePage = lazyWithRetry(() => import("@pages/Event/EventGuidePage"));
const EventLoginPage = lazyWithRetry(() => import("@pages/Event/EventLoginPage"));
const EventNamePage = lazyWithRetry(() => import("@pages/Event/EventNamePage"));
const EventChooseShapePage = lazyWithRetry(() => import("@pages/Event/EventChooseShapePage"));
const EventAdminPage = lazyWithRetry(() => import("@pages/Event/EventAdminPage"));
const EventPlaceNotePage = lazyWithRetry(() => import("@pages/Event/EventPlaceNotePage"));
const EventWriteMessagePage = lazyWithRetry(() => import("@pages/Event/EventWriteMessagePage"));
const EventChooseNotePage = lazyWithRetry(() => import("@pages/Event/EventChooseNotePage"));
const EventThankYouPage = lazyWithRetry(() => import("@pages/Event/EventThankYouPage"));
const Model3DFullscreenPage = lazyWithRetry(() => import("@pages/Event/Model3DFullscreenPage"));

// Event Protected Route
const EventProtectedRoute = lazyWithRetry(() => import("@components/event/EventProtectedRoute"));

// Ring Customizer
const RingCustomizer = lazy(() =>
  import("@components/quoctiCustomizer/RingCustomizer")
);

// Product Finder
const ProductFinderPage = lazyWithRetry(() => import("@pages/ProductFinder/ProductFinderPage"));
const ProductFinderResultPage = lazyWithRetry(() => import("@pages/ProductFinder/ProductFinderResultPage"));
const ProductFinderAdminPage = lazyWithRetry(() => import("@pages/ProductFinder/ProductFinderAdminPage"));

// POD Admin Management (pages nested under AdminDashboard)
const PodAdminDashboard = lazyWithRetry(() => import("@pages/PodAdmin/Dashboard"));
const PodAdminPartners = lazyWithRetry(() => import("@pages/PodAdmin/Partners"));
const PodAdminPartnerCreate = lazyWithRetry(() => import("@pages/PodAdmin/PartnerCreate"));
const PodAdminPartnerDetail = lazyWithRetry(() => import("@pages/PodAdmin/PartnerDetail"));
const PodAdminPods = lazyWithRetry(() => import("@pages/PodAdmin/Pods"));
const PodAdminPodCreate = lazyWithRetry(() => import("@pages/PodAdmin/PodCreate"));
const PodAdminPodDetail = lazyWithRetry(() => import("@pages/PodAdmin/PodDetail"));
const PodAdminPodLocations = lazyWithRetry(() => import("@pages/PodAdmin/PodLocations"));
const PodAdminQrCodes = lazyWithRetry(() => import("@pages/PodAdmin/QrCodes"));
const PodAdminScans = lazyWithRetry(() => import("@pages/PodAdmin/Scans"));
const PodAdminUserAttributions = lazyWithRetry(() => import("@pages/PodAdmin/UserAttributions"));
const PodAdminCommissions = lazyWithRetry(() => import("@pages/PodAdmin/Commissions"));
const PodAdminCommissionDetail = lazyWithRetry(() => import("@pages/PodAdmin/CommissionDetail"));

// POD Partner Portal
const PartnerPortalLayout = lazyWithRetry(() =>
  import("@components/partner-portal/PartnerPortalLayout")
);
const PartnerPortalDashboard = lazyWithRetry(() => import("@pages/PartnerPortal/Dashboard"));
const PartnerPortalPods = lazyWithRetry(() => import("@pages/PartnerPortal/Pods"));
const PartnerPortalPodLocations = lazyWithRetry(() => import("@pages/PartnerPortal/PodLocations"));
const PartnerPortalQrCodes = lazyWithRetry(() => import("@pages/PartnerPortal/QrCodes"));
const PartnerPortalScans = lazyWithRetry(() => import("@pages/PartnerPortal/Scans"));
const PartnerPortalUserAttributions = lazyWithRetry(() => import("@pages/PartnerPortal/UserAttributions"));
const PartnerPortalCommissions = lazyWithRetry(() => import("@pages/PartnerPortal/Commissions"));

// Phygital Partner Portal
const PartnerPhygitalDashboard = lazyWithRetry(() => import("@pages/PartnerPortal/PhygitalDashboard"));
const PartnerInventory = lazyWithRetry(() => import("@pages/PartnerPortal/Inventory"));
const PartnerInventoryDetail = lazyWithRetry(() => import("@pages/PartnerPortal/InventoryDetail"));
const PartnerWholesaleOrders = lazyWithRetry(() => import("@pages/PartnerPortal/WholesaleOrders"));
const PartnerWholesaleOrderDetail = lazyWithRetry(() => import("@pages/PartnerPortal/WholesaleOrderDetail"));
const PartnerSales = lazyWithRetry(() => import("@pages/PartnerPortal/Sales"));
const PartnerSaleDetail = lazyWithRetry(() => import("@pages/PartnerPortal/SaleDetail"));
const PartnerSalesReport = lazyWithRetry(() => import("@pages/PartnerPortal/SalesReport"));

// Admin Wholesale
const AdminWholesaleOrders = lazyWithRetry(() => import("@pages/PodAdmin/WholesaleOrders"));
const AdminWholesaleOrderDetail = lazyWithRetry(() => import("@pages/PodAdmin/WholesaleOrderDetail"));
const AdminPhygitalPartners = lazyWithRetry(() => import("@pages/PodAdmin/PhygitalPartners"));
const AdminPhygitalPartnerDetail = lazyWithRetry(() => import("@pages/PodAdmin/PhygitalPartnerDetail"));

// RFID Label Printing System
const LabelTemplates = lazyWithRetry(() => import("@pages/PodAdmin/LabelTemplates"));
const LabelDesigner = lazyWithRetry(() => import("@pages/PodAdmin/LabelDesigner"));
const PrintJobs = lazyWithRetry(() => import("@pages/PodAdmin/PrintJobs"));
const PrintJobCreate = lazyWithRetry(() => import("@pages/PodAdmin/PrintJobCreate"));
const RFIDTags = lazyWithRetry(() => import("@pages/PodAdmin/RFIDTags"));

// RFID Scanner (for PDA devices)
const ScannerPage = lazyWithRetry(() => import("@pages/Scanner/ScannerPage"));

export default function AppRoutes() {
  const location = useLocation();
  const { isOpen: isImmersiveModalOpen } = useImmersiveModal();

  // Check if current path matches any defined route (to detect 404)
  const isNotFoundPage = () => {
    const definedRoutes = [
      ROUTES.HOME,
      ROUTES.HOME_PAGE,
      ROUTES.WELCOME,
      ROUTES.AUTH,
      `${ROUTES.AUTH}/login`,
      `${ROUTES.AUTH}/register`,
      ROUTES.VERIFY_EMAIL,
      ROUTES.PRODUCTS,
      ROUTES.PRODUCTS_V2,
      ROUTES.ALL_GEMS,
      ROUTES.COLLECTIONS,
      ROUTES.SERVICES,
      ROUTES.SERVICES_DETAIL,
      ROUTES.SUPPORT,
      ROUTES.SUPPORT_DETAIL,
      ROUTES.CONTACT,
      ROUTES.ABOUT,
      ROUTES.LOCATIONS,
      ROUTES.NEWS,
      ROUTES.PRODUCTS_LEFT,
      ROUTES.USER_PROFILE,
      ROUTES.SCAVENGER_HUNT,
      ROUTES.DASHBOARD_ADMIN_MANAGE,
      ROUTES.DASHBOARD_ADMIN,
      ROUTES.DASHBOARD_VENDOR,
      ROUTES.DASHBOARD_DESIGNER,
      ROUTES.FORGOT_PASSWORD,
      ROUTES.PREMIUM,
      ROUTES.PREMIUM_DEV,
      ROUTES.MESH_INSPECTOR,
      ROUTES.EVENT,
      ROUTES.EVENT_GUIDE,
      ROUTES.EVENT_LOGIN,
      ROUTES.EVENT_NAME,
      ROUTES.EVENT_CHOOSE_SHAPE,
      ROUTES.EVENT_ADMIN,
      ROUTES.EVENT_PLACE_NOTE,
      ROUTES.EVENT_WRITE_MESSAGE,
      ROUTES.EVENT_CHOOSE_NOTE,
      ROUTES.DB_EXPLORER,
      ROUTES.RING_CUSTOMIZER,
      ROUTES.PRODUCT_FINDER,
      ROUTES.PRODUCT_FINDER_CHOOSE_SHAPE,
      ROUTES.PRODUCT_FINDER_CHOOSE_BAND,
      ROUTES.PRODUCT_FINDER_CHOOSE_SIDESTONE,
      ROUTES.PRODUCT_FINDER_RESULT,
      // POD Partner Portal routes
      ROUTES.POD_PARTNER,
      ROUTES.POD_PARTNER_DASHBOARD,
      ROUTES.POD_PARTNER_PODS,
      ROUTES.POD_PARTNER_LOCATIONS,
      ROUTES.POD_PARTNER_QRCODES,
      ROUTES.POD_PARTNER_SCANS,
      ROUTES.POD_PARTNER_USER_ATTRIBUTIONS,
      ROUTES.POD_PARTNER_COMMISSIONS,
      // Phygital Partner routes
      ROUTES.POD_PARTNER_PHYGITAL_DASHBOARD,
      ROUTES.POD_PARTNER_INVENTORY,
      ROUTES.POD_PARTNER_WHOLESALE_ORDERS,
      ROUTES.POD_PARTNER_SALES,
      ROUTES.POD_PARTNER_SALES_REPORT,
    ];

    // Check exact matches
    if (definedRoutes.includes(location.pathname)) {
      return false;
    }

    // Check dynamic routes (with params)
    const productDetailBase = ROUTES.PRODUCT_DETAIL.replace("/:productId", "");
    if (
      location.pathname.startsWith(ROUTES.COLLECTIONS + "/") ||
      location.pathname.startsWith(ROUTES.NEWS + "/") ||
      location.pathname.startsWith(productDetailBase + "/") ||
      location.pathname.startsWith(ROUTES.DASHBOARD_ADMIN) ||
      location.pathname.startsWith(ROUTES.DASHBOARD_VENDOR) ||
      location.pathname.startsWith(ROUTES.DASHBOARD_DESIGNER) ||
      location.pathname.startsWith(ROUTES.SCAVENGER_HUNT) ||
      location.pathname.startsWith(ROUTES.EVENT_GUIDE) ||
      location.pathname.startsWith(ROUTES.POD_PARTNER)
    ) {
      return false;
    }

    return true; // It's a 404
  };

  const is404 = isNotFoundPage();

  const staticRoutesToHideNavBar =
    is404 ||
    location.pathname.startsWith(ROUTES.SCAVENGER_HUNT) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_ADMIN) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_VENDOR) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_DESIGNER) ||
    location.pathname === ROUTES.HOME ||
    location.pathname === ROUTES.WELCOME ||
    location.pathname === ROUTES.PREMIUM ||
    location.pathname === ROUTES.PREMIUM_DEV ||
    location.pathname === ROUTES.MESH_INSPECTOR ||
    location.pathname.startsWith(ROUTES.EVENT) ||
    location.pathname.startsWith(ROUTES.EVENT_GUIDE) ||
    location.pathname.startsWith(ROUTES.POD_PARTNER) ||
    location.pathname === ROUTES.DB_EXPLORER ||
    location.pathname === ROUTES.RING_CUSTOMIZER;

  const staticRoutesToHideFooter =
    is404 ||
    location.pathname.startsWith(ROUTES.SCAVENGER_HUNT) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_ADMIN) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_VENDOR) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_DESIGNER) ||
    location.pathname === ROUTES.HOME ||
    location.pathname === ROUTES.WELCOME ||
    location.pathname === ROUTES.PREMIUM ||
    location.pathname === ROUTES.PREMIUM_DEV ||
    location.pathname === ROUTES.MESH_INSPECTOR ||
    location.pathname.startsWith(ROUTES.EVENT) ||
    location.pathname.startsWith(ROUTES.EVENT_GUIDE) ||
    location.pathname.startsWith(ROUTES.POD_PARTNER) ||
    location.pathname === ROUTES.DB_EXPLORER ||
    location.pathname === ROUTES.RING_CUSTOMIZER;

  const shouldShowNavbar = !staticRoutesToHideNavBar && !isImmersiveModalOpen;
  const shouldShowFooter = !staticRoutesToHideFooter;

  return (
    <>
      {/* Conditional Navbar */}
      {shouldShowNavbar && <NavbarV4 />}

      {/* Main content wrapper for reveal footer effect */}
      <main className="page-main-content">
        {/* Routes */}
        <Suspense
          fallback={
            <div style={{ textAlign: "center", padding: "2rem" }}>
              Loading...
            </div>
          }
        >
          <Routes>
            {/* Default route "/" shows WelcomePage */}
            <Route path={ROUTES.HOME} element={<WelcomePage />} />

            <Route path={ROUTES.HOME_PAGE} element={<HomePage />} />
            <Route path={ROUTES.WELCOME} element={<WelcomePage />} />

            <Route path={ROUTES.AUTH} element={<AuthPage />}>
              <Route path="login" element={<Login />} />
              <Route path="register" element={<Register />} />
            </Route>

            <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPassword />} />
            <Route path={ROUTES.VERIFY_EMAIL} element={<EmailVerification />} />

            <Route path="/q/:shortCode" element={<QrRedirect />} />

            <Route path={ROUTES.PRODUCTS} element={<ProductsPage />} />

            <Route path={ROUTES.PRODUCTS_V2} element={<ProductsV2 />} />

            <Route path={ROUTES.ALL_GEMS} element={<AllGemsPage />} />

            <Route path={ROUTES.COLLECTIONS} element={<CollectionPage />} />

            <Route
              path={ROUTES.COLLECTION_DETAIL}
              element={<CollectionDetailPage />}
            />

            <Route
              path={ROUTES.PRODUCT_DETAIL}
              element={<ProductDetailPage />}
            />

            <Route path={ROUTES.SERVICES} element={<ServicesPage />} />

            <Route
              path={ROUTES.SERVICES_DETAIL}
              element={<ServicesDetailPage />}
            />

            <Route path={ROUTES.SUPPORT} element={<SupportPage />} />

            <Route
              path={ROUTES.SUPPORT_DETAIL}
              element={<SupportDetailPage />}
            />

            <Route path={ROUTES.CONTACT} element={<ContactPage />} />

            <Route path={ROUTES.ABOUT} element={<AboutPage />} />

            <Route path={ROUTES.LOCATIONS} element={<LocationsPage />} />

            <Route path={ROUTES.NEWS} element={<AllNewsPage />} />

            <Route path={ROUTES.NEWS_DETAIL} element={<NewsDetailWrapper />} />


            {/* for observing UI universe-section final */}
            <Route path="/universe-section" element={<UniverseSection />} />

            <Route path={ROUTES.PRODUCTS_LEFT} element={<ProductsLeft />} />

            <Route path={ROUTES.USER_PROFILE} element={<Profile />} />

            {/* Premium AR Route */}
            <Route path={ROUTES.PREMIUM} element={<PremiumPage />} />

            {/* Premium AR Development Route */}
            <Route path={ROUTES.PREMIUM_DEV} element={<PremiumDevPage />} />

            {/* Mesh Inspector Tool */}
            <Route
              path={ROUTES.MESH_INSPECTOR}
              element={<SimpleMeshInspector />}
            />

            {/* DB Explorer - Export CSV/XLSX */}
            <Route path={ROUTES.DB_EXPLORER} element={<DBExplorerPage />} />

            {/* RFID Scanner - for PDA devices */}
            <Route path={ROUTES.RFID_SCANNER} element={<ScannerPage />} />

            <Route path={ROUTES.SCAVENGER_HUNT} element={<ScavengerHunt />} />

            <Route
              path={ROUTES.DASHBOARD_ADMIN_MANAGE}
              element={
                <ProtectedRoute
                  allowedRoles={[
                    "SUPER_ADMIN",
                    "ADMIN",
                    "IT_ADMIN",
                    "PRODUCTION_OPS",
                    "SALES_CUSTOMER_OPS",
                    "FINANCE",
                    "MARKETING",
                    "CREATIVE_DESIGN",
                    "LEGAL",
                  ]}
                >
                  <ManageProducts />
                </ProtectedRoute>
              }
            />

            <Route
              path={ROUTES.DASHBOARD_ADMIN}
              element={
                <ProtectedRoute
                  allowedRoles={[
                    "SUPER_ADMIN",
                    "ADMIN",
                    "IT_ADMIN",
                    "PRODUCTION_OPS",
                    "SALES_CUSTOMER_OPS",
                    "FINANCE",
                    "MARKETING",
                    "CREATIVE_DESIGN",
                    "LEGAL",
                  ]}
                >
                  <AdminDashboard />
                </ProtectedRoute>
              }
            >
              {/* POD Admin child routes (rendered via Outlet in AdminDashboard) */}
              <Route path="pod" element={<PodAdminDashboard />} />
              <Route path="pod/dashboard" element={<PodAdminDashboard />} />
              <Route path="pod/partners" element={<PodAdminPartners />} />
              <Route path="pod/partners/create" element={<PodAdminPartnerCreate />} />
              <Route path="pod/partners/:partnerId" element={<PodAdminPartnerDetail />} />
              <Route path="pod/pods" element={<PodAdminPods />} />
              <Route path="pod/pods/create" element={<PodAdminPodCreate />} />
              <Route path="pod/pods/:podId" element={<PodAdminPodDetail />} />
              <Route path="pod/locations" element={<PodAdminPodLocations />} />
              <Route path="pod/qr-codes" element={<PodAdminQrCodes />} />
              <Route path="pod/qr-codes/:qrCodeId" element={<PodAdminQrCodes />} />
              <Route path="pod/scans" element={<PodAdminScans />} />
              <Route path="pod/user-attributions" element={<PodAdminUserAttributions />} />
              <Route path="pod/commissions" element={<PodAdminCommissions />} />
              <Route path="pod/commissions/:commissionId" element={<PodAdminCommissionDetail />} />
              <Route path="pod/wholesale-orders" element={<AdminWholesaleOrders />} />
              <Route path="pod/wholesale-orders/:orderId" element={<AdminWholesaleOrderDetail />} />
              <Route path="pod/phygital-partners" element={<AdminPhygitalPartners />} />
              <Route path="pod/phygital-partners/:partnerId" element={<AdminPhygitalPartnerDetail />} />

              {/* RFID Label Printing System */}
              <Route path="label-templates" element={<LabelTemplates />} />
              <Route path="label-designer/new" element={<LabelDesigner />} />
              <Route path="label-designer/:id" element={<LabelDesigner />} />
              <Route path="print-jobs" element={<PrintJobs />} />
              <Route path="print-jobs/new" element={<PrintJobCreate />} />
              <Route path="rfid-tags" element={<RFIDTags />} />
            </Route>

            <Route
              path={ROUTES.DASHBOARD_VENDOR}
              element={
                <ProtectedRoute requiredRole="VENDOR">
                  <VendorDashboard />
                </ProtectedRoute>
              }
            />

            <Route
              path={ROUTES.DASHBOARD_DESIGNER}
              element={
                <ProtectedRoute requiredRole="DESIGNER">
                  <DesignerDashboard />
                </ProtectedRoute>
              }
            />

            {/* Event Routes - Public */}
            <Route path={ROUTES.EVENT} element={<EventPage />} />
            <Route path={ROUTES.EVENT_GUIDE} element={<EventGuidePage />} />
            <Route path={ROUTES.EVENT_LOGIN} element={<EventLoginPage />} />
            <Route path={ROUTES.EVENT_ADMIN} element={<EventAdminPage />} />

            {/* Event Routes - Protected (require login) */}
            <Route path={ROUTES.EVENT_NAME} element={<EventProtectedRoute><EventNamePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_CHOOSE_SHAPE} element={<EventProtectedRoute><EventChooseShapePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_PLACE_NOTE} element={<EventProtectedRoute><EventPlaceNotePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_WRITE_MESSAGE} element={<EventProtectedRoute><EventWriteMessagePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_CHOOSE_NOTE} element={<EventProtectedRoute><EventChooseNotePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_THANKYOU} element={<EventProtectedRoute><EventThankYouPage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_RING_VIEWER} element={<Model3DFullscreenPage />} />

            {/* Ring Customizer */}
            <Route path={ROUTES.RING_CUSTOMIZER} element={<RingCustomizer />} />

            {/* Product Finder */}
            <Route path={ROUTES.PRODUCT_FINDER} element={<Navigate to={ROUTES.PRODUCT_FINDER_CHOOSE_SHAPE} replace />} />
            <Route path="/find-your-piece/:step" element={<ProductFinderPage />} />
            <Route path={ROUTES.PRODUCT_FINDER_RESULT} element={<ProductFinderResultPage />} />

            {/* Product Finder Admin */}
            <Route
              path="/admin/product-finder/combinations"
              element={
                <ProtectedRoute
                  allowedRoles={["SUPER_ADMIN", "ADMIN", "IT_ADMIN"]}
                >
                  <ProductFinderAdminPage />
                </ProtectedRoute>
              }
            />

            {/* POD Partner Portal Routes */}
            <Route path={ROUTES.POD_PARTNER} element={<PartnerPortalLayout />}>
              <Route index element={<PartnerPortalDashboard />} />
              <Route path="dashboard" element={<PartnerPortalDashboard />} />
              <Route path="pods" element={<PartnerPortalPods />} />
              <Route path="locations" element={<PartnerPortalPodLocations />} />
              <Route path="qr-codes" element={<PartnerPortalQrCodes />} />
              <Route path="scans" element={<PartnerPortalScans />} />
              <Route path="user-attributions" element={<PartnerPortalUserAttributions />} />
              <Route path="commissions" element={<PartnerPortalCommissions />} />
              {/* Phygital Partner Routes */}
              <Route path="phygital-dashboard" element={<PartnerPhygitalDashboard />} />
              <Route path="inventory" element={<PartnerInventory />} />
              <Route path="inventory/:inventoryId" element={<PartnerInventoryDetail />} />
              <Route path="wholesale-orders" element={<PartnerWholesaleOrders />} />
              <Route path="wholesale-orders/:orderId" element={<PartnerWholesaleOrderDetail />} />
              <Route path="sales" element={<PartnerSales />} />
              <Route path="sales/:saleId" element={<PartnerSaleDetail />} />
              <Route path="sales-report" element={<PartnerSalesReport />} />
            </Route>

            {/* 404 - Catch all route for non-existent paths */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </main>

      {/* Spacer to reveal footer */}
      {shouldShowFooter && <div className="footer-reveal-spacer" />}

      {/* Conditional Footer - Fixed at bottom */}
      {shouldShowFooter && <Footer />}
    </>
  );
}
