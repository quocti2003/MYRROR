import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import ProductsManager from "./ProductsManager";
import UnifiedOrdersManager from "./UnifiedOrdersManager";
import ProductFulfillment from "./ProductFulfillment";
import VendorMatching from "./VendorMatching";
import VendorOptimization from "./VendorOptimization";
import MultiCurrencyCalculator from "./MultiCurrencyCalculator";
import PreciousMetalDashboard from "./PreciousMetalDashboard";
import UnitConversionCalculator from "./UnitConversionCalculator";
import PaymentSchedulesManager from "./PaymentSchedulesManager";
import CategoriesManager from "./CategoriesManager";
import CollectionsManager from "./CollectionsManager";
import LocationsManager from "./LocationsManager";
import ComponentsManager from "./ComponentsManager";
import UsersManager from "./UsersManager";
import VendorsManager from "./VendorsManager";
import SkuCodesManager from "./SkuCodesManager";
import DashboardHome from "./DashboardHome";
import CollectionPlanWizard from "./CollectionPlanWizard";
import CustomsComplianceManager from "./CustomsComplianceManager";
import JewelrySpecificationManager from "./JewelrySpecificationManager";
import MarketTrendDashboard from "./MarketTrendDashboard";
import PurchaseOrderSummary from "./PurchaseOrderSummary";
import VendorSelectionWizard from "./VendorSelectionWizard";
import RBACMatrix from "./RBACMatrix";
import AppointmentsManager from "./AppointmentsManager";
import PackagePrintingKit from "./PackagePrintingKit";
import StockReconciliation from "./StockReconciliation";
import WarehouseManagement from "./WarehouseManagement";
// Product Ops Dashboard
import {
  ProductOpsDashboard,
  SKUGenerator,
  ProductFulfillment as ProductFulfillmentNew,
  MISAIntegration,
  ProductPublisher
} from "../product-ops-dashboard";
import "./AdminDashboard.css";
import { ROUTES } from "@/constants/routes";
import { useAuth } from "@/context/AuthContext";

const AdminDashboard = () => {
  const { user } = useAuth();
  const roles = user?.roles || [];
  const isAdminLike = roles.includes("SUPER_ADMIN") || roles.includes("ADMIN") || roles.includes("IT_ADMIN");

  // Sidebar state for mobile
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Close sidebar when clicking a menu item on mobile
  const handleMenuClick = (tabId) => {
    setActiveTab(tabId);
    if (window.innerWidth <= 1024) {
      setSidebarOpen(false);
    }
  };

  // Get portal branding based on user's primary role
  const getPortalInfo = () => {
    const primaryRole = roles[0];
    if (roles.includes("SUPER_ADMIN") || roles.includes("ADMIN") || roles.includes("IT_ADMIN")) {
      return { title: "Mirror Admin", subtitle: "Management Portal", breadcrumb: "Admin" };
    }
    if (roles.includes("PRODUCTION_OPS")) {
      return { title: "Mirror Production", subtitle: "Operations Portal", breadcrumb: "Production" };
    }
    if (roles.includes("SALES_CUSTOMER_OPS")) {
      return { title: "Mirror Sales", subtitle: "Customer Operations Portal", breadcrumb: "Sales" };
    }
    if (roles.includes("FINANCE")) {
      return { title: "Mirror Finance", subtitle: "Finance Portal", breadcrumb: "Finance" };
    }
    if (roles.includes("MARKETING")) {
      return { title: "Mirror Marketing", subtitle: "Marketing Portal", breadcrumb: "Marketing" };
    }
    if (roles.includes("CREATIVE_DESIGN")) {
      return { title: "Mirror Creative", subtitle: "Design Portal", breadcrumb: "Creative" };
    }
    if (roles.includes("LEGAL")) {
      return { title: "Mirror Legal", subtitle: "Legal Portal", breadcrumb: "Legal" };
    }
    return { title: "Mirror Staff", subtitle: "Staff Portal", breadcrumb: "Staff" };
  };

  const portalInfo = getPortalInfo();

  const location = useLocation();
  const navigate = useNavigate();

  // Get tab from URL query params
  const getTabFromUrl = useCallback(() => {
    const params = new URLSearchParams(location.search);
    return params.get('tab') || 'dashboard';
  }, [location.search]);

  const [activeTab, setActiveTabState] = useState(getTabFromUrl);

  // Update URL when tab changes
  const setActiveTab = useCallback((tabId) => {
    setActiveTabState(tabId);
    const params = new URLSearchParams(location.search);
    if (tabId === 'dashboard') {
      params.delete('tab');
    } else {
      params.set('tab', tabId);
    }
    const newSearch = params.toString();
    navigate(`${location.pathname}${newSearch ? `?${newSearch}` : ''}`, { replace: true });
  }, [navigate, location.pathname, location.search]);

  const menuItems = [
    { id: "dashboard", label: "Dashboard" },
    // Product Ops Dashboard (NEW)
    { id: "product-ops", label: "Product Ops Dashboard" },
    { id: "misa-integration", label: "MISA Integration" },
    { id: "product-publisher", label: "Product Publisher" },
    { id: "stock-reconciliation", label: "Stock Reconciliation" },
    // Existing tabs
    { id: "products", label: "Products" },
    { id: "product-fulfillment", label: "Product Fulfillment (Old)" },
    { id: "sku-codes", label: "SKU Codes" },
    { id: "orders", label: "Orders" },
    { id: "payments", label: "Payment schedules" },
    { id: "appointments", label: "Appointments" },
    { id: "package-printing-kit", label: "Package Printing Kit" },
    { id: "categories", label: "Categories" },
    { id: "collections", label: "Collections" },
    { id: "locations", label: "Locations" },
    { id: "warehouses", label: "Warehouses" },
    { id: "vendors", label: "Vendors" },
    // { id: "vendor-matching", label: "Vendor Matching" },
    // { id: "vendor-optimization", label: "Vendor Optimization" },
    // { id: "vendor-selection-wizard", label: "Vendor Selection Wizard" },
    // { id: "currency-calculator", label: "Currency Calculator" },
    // { id: "metal-prices", label: "Metal Prices" },
    // { id: "unit-converter", label: "Unit Converter" },
    // { id: "collection-plan-wizard", label: "Collection Plan Wizard" },
    // { id: "jewelry-specifications", label: "Jewelry Specifications" },
    // { id: "purchase-orders", label: "Purchase Orders" },
    // { id: "market-trends", label: "Market Trends" },
    // { id: "customs-compliance", label: "Customs & Compliance" },
    // { id: "components", label: "Components" },
    { id: "users", label: "Users" },
    { id: "rbac-matrix", label: "RBAC Matrix" },
  ];

  const tabAccess = useMemo(
    () => ({
      dashboard: roles,
      // Product Ops Dashboard access
      "product-ops": ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "misa-integration": ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "product-publisher": ["PRODUCTION_OPS", "MARKETING", "ADMIN", "IT_ADMIN"],
      "stock-reconciliation": ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      // Existing tabs
      products: ["CREATIVE_DESIGN", "ADMIN", "IT_ADMIN"],
      "product-fulfillment": ["CREATIVE_DESIGN", "MARKETING", "PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "sku-codes": ["PRODUCTION_OPS", "CREATIVE_DESIGN", "ADMIN", "IT_ADMIN"],
      orders: ["SALES_CUSTOMER_OPS", "FINANCE", "PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      payments: ["FINANCE", "SALES_CUSTOMER_OPS", "ADMIN", "IT_ADMIN"],
      appointments: ["SALES_CUSTOMER_OPS", "ADMIN", "IT_ADMIN"],
      "package-printing-kit": ["SALES_CUSTOMER_OPS", "MARKETING", "PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      categories: ["MARKETING", "CREATIVE_DESIGN", "ADMIN", "IT_ADMIN"],
      collections: ["PRODUCTION_OPS", "MARKETING", "CREATIVE_DESIGN", "ADMIN", "IT_ADMIN"],
      locations: ["ADMIN", "IT_ADMIN"],
      warehouses: ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN", "SUPER_ADMIN"],
      vendors: ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "vendor-matching": ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "vendor-optimization": ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "vendor-selection-wizard": ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "currency-calculator": ["FINANCE", "PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "metal-prices": ["FINANCE", "PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "unit-converter": ["PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "collection-plan-wizard": ["PRODUCTION_OPS", "CREATIVE_DESIGN", "ADMIN", "IT_ADMIN"],
      "jewelry-specifications": ["PRODUCTION_OPS", "CREATIVE_DESIGN", "ADMIN", "IT_ADMIN"],
      "purchase-orders": ["PRODUCTION_OPS", "FINANCE", "ADMIN", "IT_ADMIN"],
      "market-trends": ["MARKETING", "PRODUCTION_OPS", "ADMIN", "IT_ADMIN"],
      "customs-compliance": ["PRODUCTION_OPS", "LEGAL", "ADMIN", "IT_ADMIN"],
      users: ["ADMIN", "IT_ADMIN"],
      "rbac-matrix": ["ADMIN", "IT_ADMIN"],
    }),
    [roles]
  );

  const isTabAllowed = (id) => {
    const allowed = tabAccess[id];
    if (!allowed || allowed.length === 0) return isAdminLike;
    return isAdminLike || roles.some((r) => allowed.includes(r));
  };

  const visibleMenuItems = menuItems.filter((item) => isTabAllowed(item.id));

  // Sync state with URL when browser navigation occurs (back/forward)
  useEffect(() => {
    const urlTab = getTabFromUrl();
    if (urlTab !== activeTab) {
      setActiveTabState(urlTab);
    }
  }, [location.search, getTabFromUrl]);

  // Validate tab access and redirect to valid tab if needed
  useEffect(() => {
    const urlTab = getTabFromUrl();
    const validTabIds = menuItems.map(item => item.id);

    // If tab from URL is invalid or not allowed, redirect to first allowed tab
    if (!validTabIds.includes(urlTab) || !isTabAllowed(urlTab)) {
      if (visibleMenuItems.length > 0) {
        setActiveTab(visibleMenuItems[0].id);
      }
    }
  }, [activeTab, visibleMenuItems, getTabFromUrl, setActiveTab]);

  const handleHomeNavigation = () => {
    window.location.href = ROUTES.HOME_PAGE;
  };

  const renderActiveTab = () => {
    switch (activeTab) {
      case "dashboard":
        return <DashboardHome setActiveTab={setActiveTab} />;
      // Product Ops Dashboard
      case "product-ops":
        return <ProductOpsDashboard />;
      case "misa-integration":
        return <MISAIntegration />;
      case "product-publisher":
        return <ProductPublisher />;
      case "stock-reconciliation":
        return <StockReconciliation />;
      // Existing tabs
      case "products":
        return <ProductsManager />;
      case "product-fulfillment":
        return <ProductFulfillment />;
      case "sku-codes":
        return <SkuCodesManager />;
      case "orders":
        return <UnifiedOrdersManager />;
      case "payments":
        return <PaymentSchedulesManager />;
      case "appointments":
        return <AppointmentsManager />;
      case "package-printing-kit":
        return <PackagePrintingKit />;
      case "categories":
        return <CategoriesManager />;
      case "collections":
        return <CollectionsManager />;
      case "locations":
        return <LocationsManager />;
      case "warehouses":
        return <WarehouseManagement />;
      case "vendors":
        return <VendorsManager />;
      case "vendor-matching":
        return <VendorMatching />;
      case "vendor-optimization":
        return <VendorOptimization />;
      case "vendor-selection-wizard":
        return <VendorSelectionWizard onBack={() => setActiveTab("dashboard")} onComplete={() => setActiveTab("purchase-orders")} />;
      case "currency-calculator":
        return <MultiCurrencyCalculator />;
      case "metal-prices":
        return <PreciousMetalDashboard />;
      case "unit-converter":
        return <UnitConversionCalculator />;
      case "collection-plan-wizard":
        return <CollectionPlanWizard collectionPlanId="1" onBack={() => setActiveTab("dashboard")} onNext={() => setActiveTab("vendor-matching")} />;
      case "jewelry-specifications":
        return <JewelrySpecificationManager />;
      case "purchase-orders":
        return <PurchaseOrderSummary />;
      case "market-trends":
        return <MarketTrendDashboard />;
      case "customs-compliance":
        return <CustomsComplianceManager />;
      case "components":
        return <ComponentsManager />;
      case "users":
        return <UsersManager />;
      case "rbac-matrix":
        return <RBACMatrix />;
      default:
        return <DashboardHome />;
    }
  };

  const getPageInfo = () => {
    const pageMap = {
      dashboard: {
        title: `${portalInfo.breadcrumb} Dashboard`,
        description: "Overview of your Mirror Diamond management system",
      },
      // Product Ops Dashboard
      "product-ops": {
        title: "Product Operations Dashboard",
        description: "Manage product lifecycle from SKU generation to publication",
      },
      "misa-integration": {
        title: "MISA ERP Integration",
        description: "Manage MISA SKU creation and sync status",
      },
      "product-publisher": {
        title: "Product Publisher",
        description: "Review, approve, and publish products to website",
      },
      "stock-reconciliation": {
        title: "Stock Reconciliation",
        description: "Scan barcodes to reconcile physical inventory against system records",
      },
      // Existing tabs
      products: {
        title: "Products Management",
        description:
          "Manage your diamond products, rings, necklaces, and jewelry items",
      },
      "product-fulfillment": {
        title: "Product Fulfillment (Old)",
        description:
          "Complete product details, add images, and publish products to the website",
      },
      categories: {
        title: "Categories Management",
        description:
          "Organize products into categories like rings, necklaces, earrings",
      },
      "sku-codes": {
        title: "SKU Codes Management",
        description:
          "Generate standardized SKU codes and search existing SKUs with fuzzy matching",
      },
      orders: {
        title: "Orders Management",
        description: "Review new customer orders and coordinate follow-up",
      },
      "order-workflow": {
        title: "Order Workflow & Production",
        description: "Manage order lifecycle from confirmation to completion with MISA SKU enforcement",
      },
      payments: {
        title: "Payment Schedules",
        description: "Monitor installments across orders and record payments",
      },
      appointments: {
        title: "Appointments Management",
        description: "View and manage customer appointment bookings",
      },
      "package-printing-kit": {
        title: "Package Printing Kit",
        description: "Generate and print certificates and thank you cards for products",
      },
      collections: {
        title: "Collections Management",
        description: "Manage seasonal collections and product groupings",
      },
      locations: {
        title: "Store Locations",
        description:
          "Manage store locations, addresses, and contact information",
      },
      warehouses: {
        title: "Warehouse Management",
        description: "Manage warehouses, racks, slots, and inventory positions",
      },
      vendors: {
        title: "Vendors Management",
        description: "Manage vendor information, contracts, and supplier details",
      },
      "vendor-matching": {
        title: "Vendor Matching & Selection",
        description: "AI-powered vendor matching and selection for optimal sourcing",
      },
      "vendor-optimization": {
        title: "Vendor Optimization Engine",
        description: "Find optimal vendors based on cost, quality, and timeline constraints",
      },
      "currency-calculator": {
        title: "Multi-Currency Cost Calculator",
        description: "Calculate import costs with real-time exchange rates and customs duties",
      },
      "metal-prices": {
        title: "Precious Metal Price Tracker",
        description: "Real-time gold, silver, platinum prices with material cost calculations",
      },
      "unit-converter": {
        title: "Jewelry Unit Converter",
        description: "Convert between grams, ounces, lượng, and chỉ for jewelry measurements",
      },
      components: {
        title: "Components Management",
        description: "Manage product components and customizable options",
      },
      users: {
        title: "Users Management",
        description: "Manage user accounts, permissions, and access control",
      },
      "vendor-selection-wizard": {
        title: "Vendor Selection Wizard",
        description: "Guided workflow to find and select optimal vendors for your requirements",
      },
      "collection-plan-wizard": {
        title: "Collection Plan Wizard",
        description: "Step-by-step planning for new jewelry collections and product lines",
      },
      "jewelry-specifications": {
        title: "Jewelry Specifications",
        description: "Manage synthetic diamond jewelry designs and specifications",
      },
      "purchase-orders": {
        title: "Purchase Orders",
        description: "Track and manage all purchase orders from vendors",
      },
      "market-trends": {
        title: "Market Trend Analysis",
        description: "Vietnam jewelry import market intelligence and trend analysis",
      },
      "customs-compliance": {
        title: "Customs & Compliance",
        description: "Research duty rates and manage regulatory compliance for jewelry imports",
      },
      "rbac-matrix": {
        title: "RBAC Matrix",
        description: "View role purposes, UI access, and allowed actions",
      },
    };
    return pageMap[activeTab] || pageMap.dashboard;
  };

  return (
    <div className="admin-dashboard-container">
      {/* Mobile Overlay */}
      {sidebarOpen && (
        <div
          className="admin-sidebar-overlay"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar Navigation */}
      <div className={`admin-sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="admin-sidebar-header">
          <div className="admin-sidebar-header-content">
            <h1 className="admin-sidebar-title">{portalInfo.title}</h1>
            <p className="admin-sidebar-subtitle">{portalInfo.subtitle}</p>
          </div>
          <button
            className="admin-sidebar-close"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close menu"
          >
            ×
          </button>
        </div>

        <nav className="admin-sidebar-nav">
          {visibleMenuItems.map((item) => (
            <button
              key={item.id}
              className={`admin-nav-button ${
                activeTab === item.id ? "active" : ""
              }`}
              onClick={() => handleMenuClick(item.id)}
            >
              <span className="admin-nav-label">{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="admin-sidebar-footer">
          <button className="home-nav-button" onClick={handleHomeNavigation}>
            <span className="home-nav-label">← Back to Home</span>
          </button>
          <div className="admin-user-info">
            <div className="admin-user-avatar">{user?.firstName?.[0] || user?.email?.[0]?.toUpperCase() || 'U'}</div>
            <div className="admin-user-details">
              <div className="admin-user-name">{user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : user?.email || 'User'}</div>
              <div className="admin-user-role">{user?.roles?.[0]?.replace(/_/g, ' ') || 'Staff'}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="admin-main-content">
        {/* Mobile Header with Menu Toggle */}
        <div className="admin-mobile-header">
          <button
            className="admin-menu-toggle"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
          >
            <span className="hamburger-line"></span>
            <span className="hamburger-line"></span>
            <span className="hamburger-line"></span>
          </button>
          <span className="admin-mobile-title">{portalInfo.title}</span>
        </div>

        <div className="admin-content-header">
          <div className="admin-breadcrumb">
            <span>{portalInfo.breadcrumb}</span>
            <span className="breadcrumb-separator">›</span>
            <span>{getPageInfo().title}</span>
          </div>
          <h1 className="admin-page-title">{getPageInfo().title}</h1>
          <p className="admin-page-description">{getPageInfo().description}</p>
        </div>

        <div className="admin-content-body">{renderActiveTab()}</div>
      </div>
    </div>
  );
};

export default AdminDashboard;
