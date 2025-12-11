import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { inventoryProductsAPI } from "@/services/inventoryApi";
import { ROUTES, getInventoryProductEditRoute } from "@/constants/routes";
import {
  ArrowLeft,
  Edit,
  Trash2,
  Printer,
  Package,
  Copy,
  CheckCircle,
  AlertCircle,
} from "lucide-react";
import "./ProductDetail.css";

const ProductDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    fetchProduct();
  }, [id]);

  const fetchProduct = async () => {
    try {
      setLoading(true);
      setError("");
      const response = await inventoryProductsAPI.getById(id);
      const data = response.data?.data || response.data;

      if (data) {
        setProduct(data);
      } else {
        setError("Khong tim thay san pham");
      }
    } catch (err) {
      console.error("Error fetching product:", err);
      if (err.response?.status === 404) {
        setError("Khong tim thay san pham voi ID nay");
      } else {
        setError("Khong the ket noi den server. Vui long kiem tra backend dang chay.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    try {
      setDeleteLoading(true);
      await inventoryProductsAPI.delete(id);
      navigate(ROUTES.INVENTORY_PRODUCTS);
    } catch (err) {
      console.error("Delete error:", err);
      setError("Co loi xay ra khi xoa san pham");
      setShowDeleteConfirm(false);
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleCopyForPrint = () => {
    if (!product) return;

    const printData = {
      sku: product.sku,
      name: product.name,
      price: product.price,
      currency: product.currency,
    };

    navigator.clipboard.writeText(JSON.stringify(printData, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const formatCurrency = (value, currency = "VND") => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency,
    }).format(value);
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleString("vi-VN");
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

  if (loading) {
    return (
      <div className="product-detail-page">
        <div className="detail-loading">Dang tai...</div>
      </div>
    );
  }

  if (error && !product) {
    return (
      <div className="product-detail-page">
        <div className="detail-error">
          <AlertCircle size={48} />
          <p>{error}</p>
          <button onClick={() => navigate(ROUTES.INVENTORY_PRODUCTS)}>
            Quay lai danh sach
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="product-detail-page">
      {/* Header */}
      <div className="detail-header">
        <button className="back-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={20} />
          Quay lai
        </button>

        <div className="detail-actions">
          <button
            className="action-btn print-btn"
            onClick={handleCopyForPrint}
          >
            {copied ? <CheckCircle size={20} /> : <Copy size={20} />}
            {copied ? "Da copy!" : "Copy de in"}
          </button>
          <button
            className="action-btn edit-btn"
            onClick={() => navigate(getInventoryProductEditRoute(id))}
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
        </div>
      </div>

      {error && (
        <div className="detail-alert error">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Main Content */}
      <div className="detail-content">
        {/* Image Section */}
        <div className="detail-image-section">
          <div className="product-main-image">
            {product.imageUrl ? (
              <img src={product.imageUrl} alt={product.name} />
            ) : (
              <div className="no-image">
                <Package size={64} />
                <span>Khong co hinh</span>
              </div>
            )}
          </div>
        </div>

        {/* Info Section */}
        <div className="detail-info-section">
          <div className="product-title-row">
            <h1>{product.name}</h1>
            <span
              className="status-badge"
              style={{ backgroundColor: getStatusColor(product.status) }}
            >
              {getStatusLabel(product.status)}
            </span>
          </div>

          <p className="product-sku">SKU: {product.sku}</p>

          <div className="product-price">
            {formatCurrency(product.price, product.currency)}
          </div>

          {product.description && (
            <div className="product-description">
              <h3>Mo ta</h3>
              <p>{product.description}</p>
            </div>
          )}

          {/* Details Grid */}
          <div className="details-grid">
            <div className="detail-card">
              <h3>Thong tin chat lieu</h3>
              <div className="detail-rows">
                <div className="detail-row">
                  <span className="label">Loai kim loai:</span>
                  <span className="value">{product.metalType || "-"}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Do tinh khiet:</span>
                  <span className="value">{product.metalPurity || "-"}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Loai da:</span>
                  <span className="value">{product.stoneType || "-"}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Trong luong:</span>
                  <span className="value">
                    {product.weightGrams ? `${product.weightGrams}g` : "-"}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="label">Kich thuoc:</span>
                  <span className="value">{product.dimensions || "-"}</span>
                </div>
              </div>
            </div>

            <div className="detail-card">
              <h3>Thong tin he thong</h3>
              <div className="detail-rows">
                <div className="detail-row">
                  <span className="label">ID:</span>
                  <span className="value mono">{product.id}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Ngay tao:</span>
                  <span className="value">{formatDate(product.createdAt)}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Cap nhat:</span>
                  <span className="value">{formatDate(product.updatedAt)}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Trang thai:</span>
                  <span className="value">
                    {product.isActive !== false ? "Hoat dong" : "Khong hoat dong"}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Xac nhan xoa</h3>
            <p>Ban co chac chan muon xoa san pham "{product.name}"?</p>
            <p className="modal-warning">Hanh dong nay khong the hoan tac!</p>
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
    </div>
  );
};

export default ProductDetail;
