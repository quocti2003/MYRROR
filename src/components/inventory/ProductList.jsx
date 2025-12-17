import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { inventoryProductsAPI } from "@/services/inventoryApi";
import { ROUTES, getInventoryProductDetailRoute } from "@/constants/routes";
import {
  Search,
  Filter,
  Package,
  ChevronLeft,
  ChevronRight,
  Plus,
  X,
  AlertCircle,
  Info,
} from "lucide-react";
import "./ProductList.css";

const STATUS_OPTIONS = [
  { value: "", label: "Tat ca trang thai" },
  { value: "in_stock", label: "Con hang" },
  { value: "on_hold", label: "Dang giu" },
  { value: "warranty", label: "Bao hanh" },
  { value: "sold", label: "Da ban" },
  { value: "DRAFT", label: "Nhap" },
];

const ITEMS_PER_PAGE = 10;

const ProductList = () => {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [priceRange, setPriceRange] = useState({ min: "", max: "" });
  const [currentPage, setCurrentPage] = useState(1);
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    fetchProducts();
  }, []);

  useEffect(() => {
    filterProducts();
  }, [products, searchTerm, statusFilter, priceRange]);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      setError("");
      const response = await inventoryProductsAPI.getAll();
      const data = response.data?.data || response.data || [];
      setProducts(data);
    } catch (err) {
      console.error("Error fetching products:", err);
      setError("Khong the ket noi den server. Vui long kiem tra backend dang chay.");
    } finally {
      setLoading(false);
    }
  };

  const filterProducts = () => {
    let filtered = [...products];

    // Search filter
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.name?.toLowerCase().includes(term) ||
          p.skuId?.toLowerCase().includes(term) ||
          p.sku?.toLowerCase().includes(term)
      );
    }

    // Status filter
    if (statusFilter) {
      filtered = filtered.filter((p) => p.status === statusFilter);
    }

    // Price range filter
    if (priceRange.min) {
      filtered = filtered.filter(
        (p) => parseFloat(p.price) >= parseFloat(priceRange.min)
      );
    }
    if (priceRange.max) {
      filtered = filtered.filter(
        (p) => parseFloat(p.price) <= parseFloat(priceRange.max)
      );
    }

    setFilteredProducts(filtered);
    setCurrentPage(1);
  };

  const clearFilters = () => {
    setSearchTerm("");
    setStatusFilter("");
    setPriceRange({ min: "", max: "" });
  };

  const formatCurrency = (value, currency = "VND") => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency,
    }).format(value);
  };

  const getStatusLabel = (status) => {
    const labels = {
      in_stock: "Con hang",
      on_hold: "Dang giu",
      warranty: "Bao hanh",
      sold: "Da ban",
      DRAFT: "Nhap",
    };
    return labels[status] || status;
  };

  const getStatusColor = (status) => {
    const colors = {
      in_stock: "#10b981",
      on_hold: "#f59e0b",
      warranty: "#ef4444",
      sold: "#6b7280",
      DRAFT: "#9ca3af",
    };
    return colors[status] || "#6b7280";
  };

  // Pagination
  const totalPages = Math.ceil(filteredProducts.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const paginatedProducts = filteredProducts.slice(
    startIndex,
    startIndex + ITEMS_PER_PAGE
  );

  const hasActiveFilters = searchTerm || statusFilter || priceRange.min || priceRange.max;

  if (loading) {
    return (
      <div className="product-list-page">
        <div className="list-loading">Dang tai...</div>
      </div>
    );
  }

  return (
    <div className="product-list-page">
      <div className="list-header">
        <div className="list-header-left">
          <h1>Danh sach san pham</h1>
          <span className="product-count">{filteredProducts.length} san pham</span>
        </div>
        <button
          className="add-product-btn disabled"
          onClick={() => navigate(ROUTES.INVENTORY_ADD_PRODUCT)}
          title="Chuc nang tam thoi bi vo hieu hoa"
        >
          <Plus size={20} />
          Them san pham
        </button>
      </div>

      {/* Info về chức năng thêm bị disable */}
      <div className="list-info">
        <Info size={16} />
        <span>Chuc nang them san pham tam thoi bi vo hieu hoa do rang buoc co so du lieu.</span>
      </div>

      {/* Error Message */}
      {error && (
        <div className="list-error">
          <AlertCircle size={20} />
          <span>{error}</span>
          <button onClick={fetchProducts}>Thu lai</button>
        </div>
      )}

      {/* Search & Filters */}
      <div className="list-controls">
        <div className="search-wrapper">
          <Search size={20} className="search-icon" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tim kiem theo ten hoac SKU..."
            className="search-input"
          />
          {searchTerm && (
            <button
              className="search-clear"
              onClick={() => setSearchTerm("")}
            >
              <X size={18} />
            </button>
          )}
        </div>

        <button
          className={`filter-toggle-btn ${showFilters ? "active" : ""}`}
          onClick={() => setShowFilters(!showFilters)}
        >
          <Filter size={20} />
          Bo loc
          {hasActiveFilters && <span className="filter-badge" />}
        </button>
      </div>

      {/* Filter Panel */}
      {showFilters && (
        <div className="filter-panel">
          <div className="filter-group">
            <label>Trang thai</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              {STATUS_OPTIONS.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label>Khoang gia</label>
            <div className="price-range-inputs">
              <input
                type="number"
                value={priceRange.min}
                onChange={(e) =>
                  setPriceRange((prev) => ({ ...prev, min: e.target.value }))
                }
                placeholder="Tu"
              />
              <span>-</span>
              <input
                type="number"
                value={priceRange.max}
                onChange={(e) =>
                  setPriceRange((prev) => ({ ...prev, max: e.target.value }))
                }
                placeholder="Den"
              />
            </div>
          </div>

          {hasActiveFilters && (
            <button className="clear-filters-btn" onClick={clearFilters}>
              Xoa bo loc
            </button>
          )}
        </div>
      )}

      {/* Product Table */}
      <div className="product-table-container">
        {paginatedProducts.length > 0 ? (
          <table className="product-table">
            <thead>
              <tr>
                <th>Hinh</th>
                <th>SKU</th>
                <th>Ten san pham</th>
                <th>Kim loai</th>
                <th>Gia</th>
                <th>Trang thai</th>
              </tr>
            </thead>
            <tbody>
              {paginatedProducts.map((product) => (
                <tr
                  key={product.id}
                  onClick={() =>
                    navigate(getInventoryProductDetailRoute(product.id))
                  }
                >
                  <td>
                    <div className="product-image">
                      {product.imageUrl ? (
                        <img src={product.imageUrl} alt={product.name} />
                      ) : (
                        <Package size={24} />
                      )}
                    </div>
                  </td>
                  <td className="sku-cell">{product.skuId || product.sku}</td>
                  <td className="name-cell">{product.name}</td>
                  <td className="metal-cell">
                    {product.metalType} {product.metalPurity}
                  </td>
                  <td className="price-cell">
                    {formatCurrency(product.price, product.currency)}
                  </td>
                  <td>
                    <span
                      className="status-badge"
                      style={{ backgroundColor: getStatusColor(product.status) }}
                    >
                      {getStatusLabel(product.status)}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="no-products">
            <Package size={48} />
            <p>Khong tim thay san pham nao</p>
            {hasActiveFilters && (
              <button onClick={clearFilters}>Xoa bo loc</button>
            )}
          </div>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button
            className="pagination-btn"
            onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
            disabled={currentPage === 1}
          >
            <ChevronLeft size={20} />
          </button>

          <div className="pagination-info">
            Trang {currentPage} / {totalPages}
          </div>

          <button
            className="pagination-btn"
            onClick={() =>
              setCurrentPage((prev) => Math.min(prev + 1, totalPages))
            }
            disabled={currentPage === totalPages}
          >
            <ChevronRight size={20} />
          </button>
        </div>
      )}
    </div>
  );
};

export default ProductList;
