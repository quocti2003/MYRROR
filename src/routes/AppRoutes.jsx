// src/routes/index.jsx

import { Routes, Route, useLocation, useParams } from "react-router-dom";
import { lazy, Suspense } from "react";
import NavbarV4 from "@components/navbar/NavbarV4";
import Footer from "@components/footer/Footer";
import { ROUTES } from "@/constants/routes";
import { useImmersiveModal } from "@/contexts/ImmersiveModalContext";
// Lazy-load components
const HomePage = lazy(() => import("@pages/HomePage"));
const ProductsPage = lazy(() => import("@pages/ProductsPage"));
const CollectionPage = lazy(() => import("@pages/CollectionPage"));
const CollectionDetailPage = lazy(() => import("@pages/CollectionDetailPage"));
const ProductDetailPage = lazy(() => import("@pages/ProductDetailPage"));
const ServicesPage = lazy(() => import("@pages/ServicesPage"));
const ServicesDetailPage = lazy(() => import("@pages/ServicesDetailPage"));
const SupportPage = lazy(() => import("@pages/SupportPage"));
const SupportDetailPage = lazy(() => import("@pages/SupportDetailPage"));
const NotFoundPage = lazy(() => import("@pages/NotFoundPage"));

const UniverseSection = lazy(() =>
  import("@components/home-page/universeSection/MirrorExp.jsx")
);
const ManageProducts = lazy(() =>
  import("@components/manage-products/ManageProducts.jsx")
);
const AdminDashboard = lazy(() =>
  import("@components/admin-dashboard/AdminDashboard.jsx")
);
const VendorDashboard = lazy(() =>
  import("@components/vendor-dashboard/VendorDashboard.jsx")
);
const DesignerDashboard = lazy(() =>
  import("@components/designer-dashboard/DesignerDashboard.jsx")
);
const AuthPage = lazy(() => import("@pages/AuthPage"));
const Login = lazy(() => import("@components/login/Login"));
const Register = lazy(() => import("@components/register/Register"));
const ForgotPassword = lazy(() =>
  import("@components/forgot-password/ForgotPassword")
);
const EmailVerification = lazy(() =>
  import("@components/email-verification/EmailVerification")
);
const Profile = lazy(() => import("@components/profile/Profile"));
const ProtectedRoute = lazy(() => import("@components/auth/ProtectedRoute"));
const AllGemsPage = lazy(() => import("@pages/AllGemsPage"));
const AllNewsPage = lazy(() => import("@pages/AllNewsPage"));
const AllNewsPageV2 = lazy(() => import("@pages/AllNewsPageV2"));
const NewCutPage = lazy(() => import("@pages/NewCutPage"));
const MilanPage = lazy(() => import("@pages/MilanPage"));
const ContactPage = lazy(() => import("@pages/ContactPage"));
const ContactPageV2 = lazy(() => import("@pages/ContactPageV2"));
const DBExplorerPage = lazy(() => import("@pages/DBExplorerPage"));

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
const AboutPage = lazy(() => import("@pages/AboutPage"));
const LocationsPage = lazy(() => import("@pages/LocationsPage"));
const WelcomePage = lazy(() => import("@pages/WelcomePage"));
const ImmersiveShowroomPage = lazy(() =>
  import("@pages/ImmersiveShowroomPage")
);
const SubmitPage = lazy(() => import("@pages/SubmitPage"));
const SubmitSuccessPage = lazy(() => import("@pages/SubmitSuccessPage"));
const ProductsV2 = lazy(() => import("@components/productsV2/Products.jsx"));
const ProductsLeft = lazy(() =>
  import("@components/productsV2/ProductsLeft.jsx")
);
const ScavengerHunt = lazy(() =>
  import("@components/scavenger-hunt/ScavengerHunt")
);
const BookAppointmentPage = lazy(() => import("@pages/BookAppointmentPage"));
const PremiumPage = lazy(() => import("@pages/PremiumPage"));
const PremiumDevPage = lazy(() => import("@pages/PremiumDevPage"));
const SimpleMeshInspector = lazy(() =>
  import("@components/ijewelTryOn/quocti_dancefloor/SimpleMeshInspector")
);

// Event Pages
const EventPage = lazy(() => import("@pages/Event/EventPage"));
const EventGuidePage = lazy(() => import("@pages/Event/EventGuidePage"));
const EventLoginPage = lazy(() => import("@pages/Event/EventLoginPage"));
const EventNamePage = lazy(() => import("@pages/Event/EventNamePage"));
const EventChooseShapePage = lazy(() => import("@pages/Event/EventChooseShapePage"));
const EventDisplayPage = lazy(() => import("@pages/Event/EventDisplayPage"));
const EventAdminPage = lazy(() => import("@pages/Event/EventAdminPage"));
const ChristmasMusicPage = lazy(() => import("@pages/Event/ChristmasMusicPage"));
const EventPlaceNotePage = lazy(() => import("@pages/Event/EventPlaceNotePage"));
const EventWriteMessagePage = lazy(() => import("@pages/Event/EventWriteMessagePage"));
const EventChooseNotePage = lazy(() => import("@pages/Event/EventChooseNotePage"));
const EventChooseNotePageV2 = lazy(() => import("@pages/Event/EventChooseNotePageV2"));
const EventThankYouPage = lazy(() => import("@pages/Event/EventThankYouPage"));
const Model3DFullscreenPage = lazy(() => import("@pages/Event/Model3DFullscreenPage"));

// Event Protected Route
const EventProtectedRoute = lazy(() => import("@components/event/EventProtectedRoute"));

// Interactive Experiences
const BirthdayCake = lazy(() =>
  import("@components/birthday-cake/BirthdayCake")
);
const TestNotesPage = lazy(() => import("@pages/Event/TestNotesPage"));

// Inventory Management
const InventoryLayout = lazy(() =>
  import("@components/inventory/InventoryLayout")
);
const InventoryDashboard = lazy(() =>
  import("@components/inventory/Dashboard")
);
const InventoryScanner = lazy(() => import("@components/inventory/Scanner"));
const InventoryProductForm = lazy(() =>
  import("@components/inventory/ProductForm")
);
const InventoryProductList = lazy(() =>
  import("@components/inventory/ProductList")
);
const InventoryProductDetail = lazy(() =>
  import("@components/inventory/ProductDetail")
);
const InventoryPrintLabel = lazy(() =>
  import("@components/inventory/PrintLabel")
);
const InventoryCreateOrder = lazy(() =>
  import("@components/inventory/CreateOrder")
);
const InventoryInvoicePreview = lazy(() =>
  import("@components/inventory/InvoicePreview")
);

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
      ROUTES.CONTACT_V2,
      ROUTES.ABOUT,
      ROUTES.LOCATIONS,
      ROUTES.NEWS,
      ROUTES.NEWS_V2,
      ROUTES.IMMERSIVE_SHOWROOM,
      ROUTES.BOOK_APPOINTMENT,
      ROUTES.MILAN_SUBMIT,
      `${ROUTES.MILAN_SUBMIT}/submit-success`,
      ROUTES.PRODUCTS_LEFT,
      ROUTES.USER_PROFILE,
      ROUTES.SCAVENGER_HUNT,
      ROUTES.DASHBOARD_ADMIN_MANAGE,
      ROUTES.DASHBOARD_ADMIN,
      ROUTES.DASHBOARD_VENDOR,
      ROUTES.DASHBOARD_DESIGNER,
      ROUTES.UNIVERSE_FINAL,
      ROUTES.FORGOT_PASSWORD,
      ROUTES.PREMIUM,
      ROUTES.PREMIUM_DEV,
      ROUTES.MESH_INSPECTOR,
      ROUTES.EVENT,
      ROUTES.EVENT_GUIDE,
      ROUTES.EVENT_LOGIN,
      ROUTES.EVENT_NAME,
      ROUTES.EVENT_CHOOSE_SHAPE,
      ROUTES.EVENT_DISPLAY,
      ROUTES.EVENT_ADMIN,
      ROUTES.EVENT_CHRISTMAS,
      ROUTES.EVENT_PLACE_NOTE,
      ROUTES.EVENT_WRITE_MESSAGE,
      ROUTES.EVENT_CHOOSE_NOTE,
      ROUTES.EVENT_CHOOSE_NOTE_V2,
      ROUTES.INVENTORY,
      ROUTES.INVENTORY_DASHBOARD,
      ROUTES.INVENTORY_SCANNER,
      ROUTES.INVENTORY_ADD_PRODUCT,
      ROUTES.INVENTORY_PRODUCTS,
      ROUTES.INVENTORY_PRINT,
      ROUTES.BIRTHDAY_CAKE,
      ROUTES.DB_EXPLORER,
      ROUTES.TEST_NOTES,
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
      location.pathname.startsWith(ROUTES.UNIVERSE_FINAL) ||
      location.pathname.startsWith(ROUTES.SCAVENGER_HUNT) ||
      location.pathname.startsWith(ROUTES.MILAN_SUBMIT) ||
      location.pathname.startsWith(ROUTES.EVENT_GUIDE) ||
      location.pathname.startsWith(ROUTES.INVENTORY)
    ) {
      return false;
    }

    return true; // It's a 404
  };

  const is404 = isNotFoundPage();

  const staticRoutesToHideNavBar =
    is404 ||
    location.pathname.startsWith(ROUTES.UNIVERSE_FINAL) ||
    location.pathname.startsWith(ROUTES.SCAVENGER_HUNT) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_ADMIN) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_VENDOR) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_DESIGNER) ||
    location.pathname === ROUTES.HOME ||
    location.pathname === ROUTES.WELCOME ||
    location.pathname === ROUTES.PREMIUM ||
    location.pathname === ROUTES.PREMIUM_DEV ||
    location.pathname === ROUTES.MESH_INSPECTOR ||
    location.pathname === ROUTES.BIRTHDAY_CAKE ||
    location.pathname === ROUTES.TEST_NOTES ||
    location.pathname.startsWith(ROUTES.EVENT) ||
    location.pathname.startsWith(ROUTES.EVENT_GUIDE) ||
    location.pathname.startsWith(ROUTES.INVENTORY) ||
    location.pathname === ROUTES.DB_EXPLORER;

  const staticRoutesToHideFooter =
    is404 ||
    location.pathname.startsWith(ROUTES.UNIVERSE_FINAL) ||
    location.pathname.startsWith(ROUTES.SCAVENGER_HUNT) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_ADMIN) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_VENDOR) ||
    location.pathname.startsWith(ROUTES.DASHBOARD_DESIGNER) ||
    location.pathname.startsWith(ROUTES.MILAN_SUBMIT) ||
    location.pathname === ROUTES.HOME ||
    location.pathname === ROUTES.WELCOME ||
    location.pathname === ROUTES.IMMERSIVE_SHOWROOM ||
    location.pathname === ROUTES.PREMIUM ||
    location.pathname === ROUTES.PREMIUM_DEV ||
    location.pathname === ROUTES.MESH_INSPECTOR ||
    location.pathname === ROUTES.BIRTHDAY_CAKE ||
    location.pathname === ROUTES.TEST_NOTES ||
    location.pathname.startsWith(ROUTES.EVENT) ||
    location.pathname.startsWith(ROUTES.EVENT_GUIDE) ||
    location.pathname.startsWith(ROUTES.INVENTORY) ||
    location.pathname === ROUTES.DB_EXPLORER;

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

            <Route path={ROUTES.CONTACT_V2} element={<ContactPageV2 />} />

            <Route path={ROUTES.ABOUT} element={<AboutPage />} />

            <Route path={ROUTES.LOCATIONS} element={<LocationsPage />} />

            <Route path={ROUTES.NEWS} element={<AllNewsPage />} />

            <Route path={ROUTES.NEWS_V2} element={<AllNewsPageV2 />} />

            <Route path={ROUTES.NEWS_DETAIL} element={<NewsDetailWrapper />} />

            <Route
              path={ROUTES.IMMERSIVE_SHOWROOM}
              element={<ImmersiveShowroomPage />}
            />

            <Route
              path={ROUTES.BOOK_APPOINTMENT}
              element={<BookAppointmentPage />}
            />

            {/* Milan Digital Jewelry Week - Route disabled (page still exists)
            <Route path={ROUTES.MILAN_SUBMIT}>
              <Route index element={<SubmitPage />} />
              <Route path="submit-success" element={<SubmitSuccessPage />} />
            </Route>
            */}

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
            />

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
            <Route path={ROUTES.EVENT_DISPLAY} element={<EventDisplayPage />} />
            <Route path={ROUTES.EVENT_ADMIN} element={<EventAdminPage />} />
            <Route path={ROUTES.EVENT_CHRISTMAS} element={<ChristmasMusicPage />} />

            {/* Event Routes - Protected (require login) */}
            <Route path={ROUTES.EVENT_NAME} element={<EventProtectedRoute><EventNamePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_CHOOSE_SHAPE} element={<EventProtectedRoute><EventChooseShapePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_PLACE_NOTE} element={<EventProtectedRoute><EventPlaceNotePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_WRITE_MESSAGE} element={<EventProtectedRoute><EventWriteMessagePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_CHOOSE_NOTE} element={<EventProtectedRoute><EventChooseNotePage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_CHOOSE_NOTE_V2} element={<EventProtectedRoute><EventChooseNotePageV2 /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_THANKYOU} element={<EventProtectedRoute><EventThankYouPage /></EventProtectedRoute>} />
            <Route path={ROUTES.EVENT_RING_VIEWER} element={<Model3DFullscreenPage />} />

            {/* Interactive Experiences */}
            <Route path={ROUTES.BIRTHDAY_CAKE} element={<BirthdayCake />} />
            <Route path={ROUTES.TEST_NOTES} element={<TestNotesPage />} />

            {/* Inventory Management Routes */}
            <Route path={ROUTES.INVENTORY} element={<InventoryLayout />}>
              <Route index element={<InventoryDashboard />} />
              <Route path="dashboard" element={<InventoryDashboard />} />
              <Route path="create-order" element={<InventoryCreateOrder />} />
              <Route path="scanner" element={<InventoryScanner />} />
              <Route path="add" element={<InventoryProductForm />} />
              <Route path="products" element={<InventoryProductList />} />
              <Route path="products/:id" element={<InventoryProductDetail />} />
              <Route
                path="products/:id/edit"
                element={<InventoryProductForm isEdit={true} />}
              />
              <Route path="print" element={<InventoryPrintLabel />} />
              <Route path="invoice" element={<InventoryInvoicePreview />} />
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
