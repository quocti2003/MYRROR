import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import { inventoryProductsAPI } from "@/services/inventoryApi";
import {
  Package,
  DollarSign,
  CheckCircle,
  Clock,
  Wrench,
  ScanLine,
  Plus,
  List,
  TrendingUp,
  AlertCircle,
} from "lucide-react";
import "./Dashboard.css";

const Dashboard = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    total: 0,
    available: 0,
    hold: 0,
    warranty: 0,
    totalValue: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [recentProducts, setRecentProducts] = useState([]);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError("");

      // Gọi API dashboard để lấy thống kê
      const dashboardResponse = await inventoryProductsAPI.getDashboard();
      const dashboardData = dashboardResponse.data?.data || dashboardResponse.data;

      if (dashboardData) {
        // Map theo API response format mới
        const statusCounts = dashboardData.productsByStatus || {};
        setStats({
          total: dashboardData.totalProducts || 0,
          available: statusCounts.in_stock || 0,
          hold: statusCounts.on_hold || 0,
          warranty: statusCounts.warranty || 0,
          totalValue: dashboardData.totalInventoryValue || 0,
        });
        setRecentProducts(dashboardData.recentProducts || []);
      }
    } catch (err) {
      console.error("Error fetching dashboard:", err);
      setError("Khong the ket noi den server. Vui long kiem tra backend dang chay.");
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value, currency = "VND") => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency,
    }).format(value);
  };

  const quickActions = [
    {
      icon: ScanLine,
      label: "Quet ma",
      path: ROUTES.INVENTORY_SCANNER,
      color: "#bc224c",
    },
    {
      icon: Plus,
      label: "Them san pham",
      path: ROUTES.INVENTORY_ADD_PRODUCT,
      color: "#10b981",
    },
    {
      icon: List,
      label: "Danh sach",
      path: ROUTES.INVENTORY_PRODUCTS,
      color: "#3b82f6",
    },
  ];

  const statCards = [
    {
      icon: Package,
      label: "Tong san pham",
      value: stats.total,
      color: "#6366f1",
    },
    {
      icon: CheckCircle,
      label: "Con hang",
      value: stats.available,
      color: "#10b981",
    },
    {
      icon: Clock,
      label: "Dang giu",
      value: stats.hold,
      color: "#f59e0b",
    },
    {
      icon: Wrench,
      label: "Bao hanh",
      value: stats.warranty,
      color: "#ef4444",
    },
  ];

  if (loading) {
    return (
      <div className="inventory-dashboard">
        <div className="dashboard-loading">Dang tai...</div>
      </div>
    );
  }

  return (
    <div className="inventory-dashboard">
      <div className="dashboard-header">
        <h1>Tong quan kho hang</h1>
        <p>Quan ly san pham cua hang trang suc</p>
      </div>

      {/* Error Message */}
      {error && (
        <div className="dashboard-error">
          <AlertCircle size={20} />
          <span>{error}</span>
          <button onClick={fetchData}>Thu lai</button>
        </div>
      )}

      {/* Quick Actions */}
      <section className="dashboard-section">
        <h2>Truy cap nhanh</h2>
        <div className="quick-actions">
          {quickActions.map((action) => (
            <button
              key={action.path}
              className="quick-action-btn"
              style={{ "--action-color": action.color }}
              onClick={() => navigate(action.path)}
            >
              <action.icon size={24} />
              <span>{action.label}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Stats Cards */}
      <section className="dashboard-section">
        <h2>Thong ke</h2>
        <div className="stat-cards">
          {statCards.map((stat) => (
            <div
              key={stat.label}
              className="stat-card"
              style={{ "--stat-color": stat.color }}
            >
              <div className="stat-icon">
                <stat.icon size={24} />
              </div>
              <div className="stat-info">
                <span className="stat-value">{stat.value}</span>
                <span className="stat-label">{stat.label}</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Total Value */}
      <section className="dashboard-section">
        <div className="total-value-card">
          <div className="total-value-icon">
            <DollarSign size={32} />
          </div>
          <div className="total-value-info">
            <span className="total-value-label">Tong gia tri ton kho</span>
            <span className="total-value-amount">
              {formatCurrency(stats.totalValue)}
            </span>
          </div>
        </div>
      </section>

      {/* Recent Products */}
      <section className="dashboard-section">
        <h2>San pham moi them</h2>
        {recentProducts.length > 0 ? (
          <div className="recent-products">
            {recentProducts.map((product) => (
              <div
                key={product.id}
                className="recent-product-item"
                onClick={() =>
                  navigate(`/inventory/products/${product.id}`)
                }
              >
                <div className="recent-product-image">
                  {product.imageUrl ? (
                    <img src={product.imageUrl} alt={product.name} />
                  ) : (
                    <Package size={24} />
                  )}
                </div>
                <div className="recent-product-info">
                  <span className="recent-product-name">{product.name}</span>
                  <span className="recent-product-sku">{product.skuId || product.sku}</span>
                </div>
                <div className="recent-product-price">
                  {formatCurrency(product.price, product.currency)}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="no-data">Chua co san pham nao</p>
        )}
      </section>
    </div>
  );
};

export default Dashboard;
