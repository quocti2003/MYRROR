import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  inventoryProductsAPI,
  misaProductStockAPI,
  getStockStatusColor,
  getStockStatusText,
} from "@/services/inventoryApi";
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
  Warehouse,
  MapPin,
  TrendingDown,
  ShoppingBag,
} from "lucide-react";
import "./Scanner.css";

const Scanner = () => {
  const navigate = useNavigate();
  const inputRef = useRef(null);
  const [skuInput, setSkuInput] = useState("");
  const [product, setProduct] = useState(null);
  const [stockInfo, setStockInfo] = useState(null); // MISA stock info
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [showBranchDetails, setShowBranchDetails] = useState(false);

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

  // Search product by SKU/Barcode using MISA API
  const handleSearch = async () => {
    if (!skuInput.trim()) {
      setError("Vui long nhap ma SKU hoac barcode");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setProduct(null);
      setStockInfo(null);
      setShowBranchDetails(false);

      // Gọi MISA API để lấy thông tin tồn kho real-time
      const misaData = await misaProductStockAPI.getByCode(skuInput.trim());

      if (misaData && misaData.found) {
        // Mapping MISA response to product format for display
        const productData = {
          id: misaData.productId,
          sku: misaData.code,
          name: misaData.name,
          barcode: misaData.barcode,
          price: misaData.sellingPrice,
          costPrice: misaData.costPrice,
          currency: "VND",
          imageUrl: misaData.picture,
          imageUrls: misaData.pictureUrls,
          description: misaData.description,
          categoryName: misaData.categoryName,
          color: misaData.color,
          size: misaData.size,
          material: misaData.material,
          unitName: misaData.unitName,
        };

        // Lưu stock info riêng biệt
        const stockData = {
          totalOnHand: misaData.totalOnHand,
          totalOrdered: misaData.totalOrdered,
          totalPreOrdered: misaData.totalPreOrdered,
          availableQuantity: misaData.availableQuantity,
          stockStatus: misaData.stockStatus,
          branchStocks: misaData.branchStocks || [],
        };

        setProduct(productData);
        setStockInfo(stockData);
      } else {
        // Không tìm thấy trong MISA
        setError(misaData?.errorMessage || "Khong tim thay san pham voi ma SKU/barcode nay");
      }
    } catch (err) {
      console.error("Search error:", err);
      if (err.response?.status === 404) {
        setError("Khong tim thay san pham voi ma SKU/barcode nay");
      } else if (err.code === "ERR_NETWORK") {
        setError("Khong the ket noi den server. Vui long kiem tra mang.");
      } else {
        setError("Co loi xay ra khi tim kiem. Vui long thu lai sau.");
      }
    } finally {
      setLoading(false);
    }
  };

  // Clear search
  const handleClear = () => {
    setSkuInput("");
    setProduct(null);
    setStockInfo(null);
    setError("");
    setShowBranchDetails(false);
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
      sku: product.sku || product.skuId,
      name: product.name,
      price: product.price,
      currency: product.currency,
    };

    navigator.clipboard.writeText(JSON.stringify(printData, null, 2));
    alert("Da copy du lieu de in!");
  };

  const formatCurrency = (value, currency = "VND") => {
    if (!value && value !== 0) return "Chua co gia";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency,
    }).format(value);
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
            placeholder="Quet ma vach hoac nhap ma SKU..."
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
              <p className="product-card-sku">SKU: {product.sku || product.skuId}</p>
              {product.barcode && (
                <p className="product-card-barcode">Barcode: {product.barcode}</p>
              )}

              <div className="product-card-details">
                <div className="detail-row">
                  <span className="detail-label">Gia ban:</span>
                  <span className="detail-value price">
                    {formatCurrency(product.price, product.currency)}
                  </span>
                </div>
                {product.categoryName && (
                  <div className="detail-row">
                    <span className="detail-label">Danh muc:</span>
                    <span className="detail-value">{product.categoryName}</span>
                  </div>
                )}
                {product.color && (
                  <div className="detail-row">
                    <span className="detail-label">Mau:</span>
                    <span className="detail-value">{product.color}</span>
                  </div>
                )}
                {product.size && (
                  <div className="detail-row">
                    <span className="detail-label">Size:</span>
                    <span className="detail-value">{product.size}</span>
                  </div>
                )}
                {product.material && (
                  <div className="detail-row">
                    <span className="detail-label">Chat lieu:</span>
                    <span className="detail-value">{product.material}</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Stock Status Section - MISA */}
          {stockInfo && (
            <div
              className="stock-status-section"
              style={{ backgroundColor: getStockStatusColor(stockInfo.stockStatus) }}
            >
              <div className="stock-status-header">
                <Warehouse size={20} />
                <span className="stock-status-badge">
                  {getStockStatusText(stockInfo.stockStatus)}
                </span>
              </div>
              <div className="stock-numbers">
                <div className="stock-item">
                  <span className="stock-label">Ton kho:</span>
                  <span className="stock-value">{stockInfo.totalOnHand}</span>
                </div>
                <div className="stock-item">
                  <span className="stock-label">Co san:</span>
                  <span className="stock-value">{stockInfo.availableQuantity}</span>
                </div>
                {stockInfo.totalOrdered > 0 && (
                  <div className="stock-item">
                    <ShoppingBag size={16} />
                    <span className="stock-label">Dang dat:</span>
                    <span className="stock-value">{stockInfo.totalOrdered}</span>
                  </div>
                )}
              </div>
              {stockInfo.branchStocks && stockInfo.branchStocks.length > 0 && (
                <button
                  className="toggle-branch-btn"
                  onClick={() => setShowBranchDetails(!showBranchDetails)}
                >
                  <MapPin size={16} />
                  {showBranchDetails ? "An chi nhanh" : `Xem ${stockInfo.branchStocks.length} chi nhanh`}
                </button>
              )}
            </div>
          )}

          {/* Branch Details Table */}
          {showBranchDetails && stockInfo?.branchStocks?.length > 0 && (
            <div className="branch-stocks-table">
              <h4>Chi tiet theo chi nhanh:</h4>
              <table>
                <thead>
                  <tr>
                    <th>Chi nhanh</th>
                    <th>Ton kho</th>
                    <th>Dang dat</th>
                  </tr>
                </thead>
                <tbody>
                  {stockInfo.branchStocks.map((branch, index) => (
                    <tr key={branch.branchId || index}>
                      <td>{branch.branchName}</td>
                      <td className={branch.onHand <= 0 ? "out-of-stock" : ""}>
                        {branch.onHand}
                      </td>
                      <td>{branch.ordered}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Low Stock Warning */}
          {stockInfo && stockInfo.stockStatus === "LOW_STOCK" && (
            <div className="stock-warning">
              <TrendingDown size={18} />
              <span>Canh bao: San pham sap het hang!</span>
            </div>
          )}

          {/* Action Buttons */}
          <div className="product-actions">
            {product.id && (
              <>
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
              </>
            )}
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
