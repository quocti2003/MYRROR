import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { inventoryProductsAPI } from "@/services/inventoryApi";
import { getInventoryProductDetailRoute, getInventoryProductEditRoute } from "@/constants/routes";
import {
  ScanLine,
  Search,
  Package,
  Edit,
  Trash2,
  Printer,
  X,
  AlertCircle,
  CheckCircle,
} from "lucide-react";
import "./Scanner.css";

const Scanner = () => {
  const navigate = useNavigate();
  const inputRef = useRef(null);
  const [skuInput, setSkuInput] = useState("");
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Auto focus input on mount
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // Handle input change
  const handleInputChange = (e) => {
    setSkuInput(e.target.value);
    setError("");
  };

  // Handle key press - scanner typically sends Enter after scan
  const handleKeyPress = (e) => {
    if (e.key === "Enter" && skuInput.trim()) {
      handleSearch();
    }
  };

  // Search product by SKU
  const handleSearch = async () => {
    if (!skuInput.trim()) {
      setError("Vui long nhap ma SKU");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setProduct(null);

      const response = await inventoryProductsAPI.getBySku(skuInput.trim());
      const productData = response.data?.data || response.data;

      if (productData) {
        setProduct(productData);
      } else {
        setError("Khong tim thay san pham voi ma SKU nay");
      }
    } catch (err) {
      console.error("Search error:", err);
      if (err.response?.status === 404) {
        setError("Khong tim thay san pham voi ma SKU nay");
      } else {
        setError("Co loi xay ra khi tim kiem. Vui long kiem tra ket noi server.");
      }
    } finally {
      setLoading(false);
    }
  };

  // Clear search
  const handleClear = () => {
    setSkuInput("");
    setProduct(null);
    setError("");
    inputRef.current?.focus();
  };

  // Delete product
  const handleDelete = async () => {
    if (!product) return;

    try {
      setDeleteLoading(true);
      await inventoryProductsAPI.delete(product.id);
      setShowDeleteConfirm(false);
      setProduct(null);
      setSkuInput("");
      inputRef.current?.focus();
    } catch (err) {
      console.error("Delete error:", err);
      setError("Co loi xay ra khi xoa san pham");
    } finally {
      setDeleteLoading(false);
    }
  };

  // Copy product data for printing
  const handleCopyForPrint = () => {
    if (!product) return;

    const printData = {
      sku: product.sku,
      name: product.name,
      price: product.price,
      currency: product.currency,
    };

    navigator.clipboard.writeText(JSON.stringify(printData, null, 2));
    alert("Da copy du lieu de in!");
  };

  const formatCurrency = (value, currency = "VND") => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency,
    }).format(value);
  };

  const getStatusLabel = (status) => {
    const labels = {
      available: "Con hang",
      hold: "Dang giu",
      warranty: "Bao hanh",
      sold: "Da ban",
    };
    return labels[status] || status;
  };

  const getStatusColor = (status) => {
    const colors = {
      available: "#10b981",
      hold: "#f59e0b",
      warranty: "#ef4444",
      sold: "#6b7280",
    };
    return colors[status] || "#6b7280";
  };

  return (
    <div className="inventory-scanner">
      <div className="scanner-header">
        <h1>Quet ma san pham</h1>
        <p>Su dung may quet hoac nhap thu cong ma SKU</p>
      </div>

      {/* Scanner Input */}
      <div className="scanner-input-section">
        <div className="scanner-input-wrapper">
          <ScanLine size={24} className="scanner-icon" />
          <input
            ref={inputRef}
            type="text"
            value={skuInput}
            onChange={handleInputChange}
            onKeyPress={handleKeyPress}
            placeholder="Quet hoac nhap ma SKU..."
            className="scanner-input"
            autoFocus
          />
          {skuInput && (
            <button className="scanner-clear-btn" onClick={handleClear}>
              <X size={20} />
            </button>
          )}
        </div>
        <button
          className="scanner-search-btn"
          onClick={handleSearch}
          disabled={loading || !skuInput.trim()}
        >
          {loading ? "Dang tim..." : "Tim kiem"}
          <Search size={20} />
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="scanner-error">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Product Result */}
      {product && (
        <div className="scanner-result">
          <div className="scanner-result-header">
            <CheckCircle size={24} color="#10b981" />
            <span>Tim thay san pham!</span>
          </div>

          <div className="product-card">
            <div className="product-card-image">
              {product.imageUrl ? (
                <img src={product.imageUrl} alt={product.name} />
              ) : (
                <Package size={48} />
              )}
            </div>

            <div className="product-card-info">
              <h3 className="product-card-name">{product.name}</h3>
              <p className="product-card-sku">SKU: {product.sku}</p>

              <div className="product-card-details">
                <div className="detail-row">
                  <span className="detail-label">Gia:</span>
                  <span className="detail-value">
                    {formatCurrency(product.price, product.currency)}
                  </span>
                </div>
                {product.metalType && (
                  <div className="detail-row">
                    <span className="detail-label">Kim loai:</span>
                    <span className="detail-value">
                      {product.metalType} {product.metalPurity}
                    </span>
                  </div>
                )}
                {product.stoneType && (
                  <div className="detail-row">
                    <span className="detail-label">Loai da:</span>
                    <span className="detail-value">{product.stoneType}</span>
                  </div>
                )}
                {product.weightGrams && (
                  <div className="detail-row">
                    <span className="detail-label">Trong luong:</span>
                    <span className="detail-value">{product.weightGrams}g</span>
                  </div>
                )}
                <div className="detail-row">
                  <span className="detail-label">Trang thai:</span>
                  <span
                    className="status-badge"
                    style={{ backgroundColor: getStatusColor(product.status) }}
                  >
                    {getStatusLabel(product.status)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="product-actions">
            <button
              className="action-btn view-btn"
              onClick={() => navigate(getInventoryProductDetailRoute(product.id))}
            >
              <Package size={20} />
              Xem chi tiet
            </button>
            <button
              className="action-btn edit-btn"
              onClick={() => navigate(getInventoryProductEditRoute(product.id))}
            >
              <Edit size={20} />
              Sua
            </button>
            <button
              className="action-btn delete-btn"
              onClick={() => setShowDeleteConfirm(true)}
            >
              <Trash2 size={20} />
              Xoa
            </button>
            <button className="action-btn print-btn" onClick={handleCopyForPrint}>
              <Printer size={20} />
              Copy de in
            </button>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Xac nhan xoa</h3>
            <p>Ban co chac chan muon xoa san pham "{product?.name}"?</p>
            <div className="modal-actions">
              <button
                className="modal-btn cancel-btn"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={deleteLoading}
              >
                Huy
              </button>
              <button
                className="modal-btn confirm-btn"
                onClick={handleDelete}
                disabled={deleteLoading}
              >
                {deleteLoading ? "Dang xoa..." : "Xoa"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Instructions */}
      {!product && !error && (
        <div className="scanner-instructions">
          <h3>Huong dan su dung</h3>
          <ul>
            <li>Su dung may quet de quet ma QR/barcode tren san pham</li>
            <li>Hoac nhap thu cong ma SKU vao o tim kiem</li>
            <li>Nhan Enter hoac click "Tim kiem" de tra cuu</li>
          </ul>
        </div>
      )}
    </div>
  );
};

export default Scanner;
