import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  misaProductStockAPI,
  getStockStatusColor,
  getStockStatusText,
} from "@/services/inventoryApi";
import {
  ScanLine,
  Package,
  Trash2,
  Plus,
  Minus,
  ShoppingCart,
  X,
  AlertCircle,
  CheckCircle,
  RefreshCw,
  Loader2,
  FileText,
  Upload,
} from "lucide-react";
import PDFInvoiceReader from "./PDFInvoiceReader";
import "./CreateOrder.css";

const CreateOrder = () => {
  const navigate = useNavigate();
  const inputRef = useRef(null);
  const [skuInput, setSkuInput] = useState("");
  const [orderItems, setOrderItems] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);
  const [showSubmitConfirm, setShowSubmitConfirm] = useState(false);
  const [showPdfReader, setShowPdfReader] = useState(false);

  // Auto focus input on mount
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // Clear messages after 3 seconds
  useEffect(() => {
    if (error || success) {
      const timer = setTimeout(() => {
        setError("");
        setSuccess("");
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [error, success]);

  // Find product by SKU/Barcode using MISA API
  const findProductByCode = async (code) => {
    try {
      const data = await misaProductStockAPI.getByCode(code);
      if (data && data.found) {
        return {
          id: data.productId,
          name: data.name,
          sku: data.code,
          barcode: data.barcode,
          price: data.sellingPrice,
          currency: "VND",
          imageUrl: data.picture,
          stockQuantity: data.availableQuantity, // Số lượng có sẵn để bán
          totalOnHand: data.totalOnHand,
          stockStatus: data.stockStatus,
          branchStocks: data.branchStocks,
        };
      }
      return null;
    } catch (err) {
      console.error("Error finding product:", err);
      return null;
    }
  };

  // Handle input change
  const handleInputChange = (e) => {
    setSkuInput(e.target.value);
    setError("");
  };

  // Handle key press - scanner sends Enter after scan
  const handleKeyPress = (e) => {
    if (e.key === "Enter" && skuInput.trim() && !loading) {
      handleAddProduct();
    }
  };

  // Add product to order - using MISA API
  const handleAddProduct = async () => {
    const code = skuInput.trim();
    if (!code) {
      setError("Vui long nhap ma SKU hoac barcode");
      return;
    }

    // Prevent duplicate requests
    if (loading) return;

    setLoading(true);

    try {
      // Find product using MISA API
      const product = await findProductByCode(code);

      if (!product) {
        setError(`Khong tim thay san pham voi ma: ${code}`);
        setSkuInput("");
        inputRef.current?.focus();
        return;
      }

      // Check if product is out of stock
      if (product.stockQuantity <= 0) {
        setError(`San pham "${product.name}" da het hang (${getStockStatusText(product.stockStatus)})`);
        setSkuInput("");
        inputRef.current?.focus();
        return;
      }

      // Check if product already in order
      const existingItem = orderItems.find((item) => item.id === product.id || item.sku === product.sku);

      if (existingItem) {
        // Check if can add more
        if (existingItem.quantity >= product.stockQuantity) {
          setError(`Chi con ${product.stockQuantity} san pham "${product.name}" co san de ban`);
          setSkuInput("");
          inputRef.current?.focus();
          return;
        }

        // Increase quantity
        setOrderItems((prev) =>
          prev.map((item) =>
            item.id === product.id || item.sku === product.sku
              ? { ...item, quantity: item.quantity + 1 }
              : item
          )
        );
        setSuccess(`Da tang so luong "${product.name}" len ${existingItem.quantity + 1}`);
      } else {
        // Add new item
        setOrderItems((prev) => [
          ...prev,
          {
            ...product,
            quantity: 1,
          },
        ]);
        setSuccess(`Da them "${product.name}" vao don hang`);
      }

      setSkuInput("");
      inputRef.current?.focus();
    } catch (err) {
      console.error("Add product error:", err);
      if (err.code === "ERR_NETWORK") {
        setError("Khong the ket noi den server. Vui long kiem tra mang.");
      } else {
        setError("Co loi xay ra khi tim san pham. Vui long thu lai.");
      }
      setSkuInput("");
      inputRef.current?.focus();
    } finally {
      setLoading(false);
    }
  };

  // Update item quantity - use SKU as key since MISA products may not have consistent IDs
  const handleQuantityChange = (itemSku, newQuantity) => {
    const item = orderItems.find((i) => i.sku === itemSku);
    if (!item) return;

    if (newQuantity < 1) {
      // Remove item if quantity < 1
      handleRemoveItem(itemSku);
      return;
    }

    if (newQuantity > item.stockQuantity) {
      setError(`Chi con ${item.stockQuantity} san pham "${item.name}" co san de ban`);
      return;
    }

    setOrderItems((prev) =>
      prev.map((i) =>
        i.sku === itemSku ? { ...i, quantity: newQuantity } : i
      )
    );
  };

  // Remove item from order
  const handleRemoveItem = (itemSku) => {
    const item = orderItems.find((i) => i.sku === itemSku);
    setOrderItems((prev) => prev.filter((i) => i.sku !== itemSku));
    if (item) {
      setSuccess(`Da xoa "${item.name}" khoi don hang`);
    }
    inputRef.current?.focus();
  };

  // Cancel order
  const handleCancelOrder = () => {
    setOrderItems([]);
    setShowCancelConfirm(false);
    setSuccess("Da huy don hang");
    inputRef.current?.focus();
  };

  // Submit order
  const handleSubmitOrder = () => {
    // TODO: Call API to update stock_quantity
    // For now, just reset the form
    setOrderItems([]);
    setShowSubmitConfirm(false);
    setSuccess("Don hang da duoc xac nhan thanh cong!");
    inputRef.current?.focus();
  };

  // Import items from PDF
  const handleImportFromPdf = (importedItems) => {
    // Add imported items to order
    const newItems = importedItems.map((item, index) => ({
      id: `pdf-import-${Date.now()}-${index}`,
      name: item.name,
      sku: `PDF-${Date.now()}-${index}`,
      barcode: "",
      price: item.price || item.unitPrice || 0,
      currency: "VND",
      imageUrl: "",
      stockQuantity: 999, // Unknown stock from PDF
      totalOnHand: 999,
      stockStatus: "UNKNOWN",
      branchStocks: [],
      quantity: item.quantity || 1,
    }));

    setOrderItems((prev) => [...prev, ...newItems]);
    setSuccess(`Da import ${importedItems.length} san pham tu PDF`);
    inputRef.current?.focus();
  };

  // Export invoice - navigate to invoice page with order data
  const handleExportInvoice = () => {
    const orderData = {
      invoiceCode: `MR${Date.now().toString().slice(-6)}`,
      invoiceNumber: "<Chưa cấp số>",
      invoiceDate: new Date(),
      customerCompany: "Khách lẻ không lấy hóa đơn",
      customerName: "Khách lẻ không lấy hóa đơn",
      paymentMethod: "TM/CK",
      items: orderItems.map(item => ({
        name: item.name,
        quantity: item.quantity,
        price: item.price,
      })),
    };

    navigate("/inventory/invoice", { state: { orderData } });
  };

  // Format currency
  const formatCurrency = (value, currency = "VND") => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency,
    }).format(value);
  };

  // Calculate totals
  const totalItems = orderItems.reduce((sum, item) => sum + item.quantity, 0);
  const totalAmount = orderItems.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );

  // Get primary currency (most common in order)
  const primaryCurrency = orderItems.length > 0
    ? orderItems[0].currency
    : "VND";

  return (
    <div className="create-order-page">
      {/* Header */}
      <div className="order-header">
        <div className="order-header-left">
          <ShoppingCart size={28} />
          <h1>Tao Don Hang</h1>
        </div>
        {orderItems.length > 0 && (
          <button
            className="cancel-order-btn"
            onClick={() => setShowCancelConfirm(true)}
          >
            <RefreshCw size={20} />
            Huy don hang
          </button>
        )}
      </div>

      {/* Error/Success Messages */}
      {error && (
        <div className="order-alert error">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}
      {success && (
        <div className="order-alert success">
          <CheckCircle size={20} />
          <span>{success}</span>
        </div>
      )}

      {/* SKU Input */}
      <div className="sku-input-section">
        <div className="sku-input-wrapper">
          <ScanLine size={24} className="scan-icon" />
          <input
            ref={inputRef}
            type="text"
            value={skuInput}
            onChange={handleInputChange}
            onKeyPress={handleKeyPress}
            placeholder="Quet ma vach hoac nhap ma SKU..."
            className="sku-input"
            autoFocus
          />
          {skuInput && (
            <button
              className="clear-input-btn"
              onClick={() => {
                setSkuInput("");
                inputRef.current?.focus();
              }}
            >
              <X size={20} />
            </button>
          )}
        </div>
        <button
          className="add-btn"
          onClick={handleAddProduct}
          disabled={!skuInput.trim() || loading}
        >
          {loading ? (
            <>
              <Loader2 size={20} className="spin" />
              Dang tim...
            </>
          ) : (
            <>
              <Plus size={20} />
              Them
            </>
          )}
        </button>
        <button
          className="import-pdf-btn"
          onClick={() => setShowPdfReader(true)}
          title="Import tu file PDF hoa don"
        >
          <Upload size={20} />
          PDF
        </button>
      </div>

      {/* Order Items Table */}
      <div className="order-items-section">
        <h2>Danh sach san pham ({orderItems.length} loai)</h2>

        {orderItems.length > 0 ? (
          <div className="order-table-container">
            <table className="order-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Hinh</th>
                  <th>Ten san pham</th>
                  <th>SKU</th>
                  <th>Don gia</th>
                  <th>So luong</th>
                  <th>Thanh tien</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {orderItems.map((item, index) => (
                  <tr key={item.sku}>
                    <td className="index-cell">{index + 1}</td>
                    <td>
                      <div className="item-image">
                        {item.imageUrl ? (
                          <img src={item.imageUrl} alt={item.name} />
                        ) : (
                          <Package size={24} />
                        )}
                      </div>
                    </td>
                    <td className="name-cell">
                      <span className="item-name">{item.name}</span>
                      <span
                        className="stock-info"
                        style={{ color: getStockStatusColor(item.stockStatus) }}
                      >
                        Co san: {item.stockQuantity} ({getStockStatusText(item.stockStatus)})
                      </span>
                    </td>
                    <td className="sku-cell">{item.sku}</td>
                    <td className="price-cell">
                      {formatCurrency(item.price, item.currency)}
                    </td>
                    <td className="quantity-cell">
                      <div className="quantity-control">
                        <button
                          className="qty-btn"
                          onClick={() =>
                            handleQuantityChange(item.sku, item.quantity - 1)
                          }
                        >
                          <Minus size={16} />
                        </button>
                        <input
                          type="number"
                          value={item.quantity}
                          onChange={(e) =>
                            handleQuantityChange(
                              item.sku,
                              parseInt(e.target.value) || 0
                            )
                          }
                          min="1"
                          max={item.stockQuantity}
                        />
                        <button
                          className="qty-btn"
                          onClick={() =>
                            handleQuantityChange(item.sku, item.quantity + 1)
                          }
                          disabled={item.quantity >= item.stockQuantity}
                        >
                          <Plus size={16} />
                        </button>
                      </div>
                      {item.quantity >= item.stockQuantity && (
                        <span className="max-qty-warning">Max</span>
                      )}
                    </td>
                    <td className="subtotal-cell">
                      {formatCurrency(item.price * item.quantity, item.currency)}
                    </td>
                    <td className="action-cell">
                      <button
                        className="remove-btn"
                        onClick={() => handleRemoveItem(item.sku)}
                        title="Xoa khoi don hang"
                      >
                        <Trash2 size={18} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-order">
            <ShoppingCart size={64} />
            <p>Chua co san pham nao trong don hang</p>
            <span>Quet ma vach hoac nhap SKU de them san pham</span>
          </div>
        )}
      </div>

      {/* Order Summary */}
      {orderItems.length > 0 && (
        <div className="order-summary">
          <div className="summary-row">
            <span>Tong so san pham:</span>
            <span className="summary-value">{totalItems} items</span>
          </div>
          <div className="summary-row total">
            <span>TONG TIEN:</span>
            <span className="summary-value total-amount">
              {formatCurrency(totalAmount, primaryCurrency)}
            </span>
          </div>
        </div>
      )}

      {/* Action Buttons */}
      {orderItems.length > 0 && (
        <div className="order-actions">
          <button
            className="action-btn cancel-btn"
            onClick={() => setShowCancelConfirm(true)}
          >
            <X size={20} />
            Huy don hang
          </button>
          <button
            className="action-btn invoice-btn"
            onClick={handleExportInvoice}
          >
            <FileText size={20} />
            Xuat hoa don
          </button>
          <button
            className="action-btn submit-btn"
            onClick={() => setShowSubmitConfirm(true)}
          >
            <CheckCircle size={20} />
            Xac nhan ban hang
          </button>
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {showCancelConfirm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Xac nhan huy don hang</h3>
            <p>Ban co chac chan muon huy don hang nay?</p>
            <p className="modal-warning">Tat ca san pham se bi xoa khoi don!</p>
            <div className="modal-actions">
              <button
                className="modal-btn cancel-btn"
                onClick={() => setShowCancelConfirm(false)}
              >
                Quay lai
              </button>
              <button
                className="modal-btn confirm-btn danger"
                onClick={handleCancelOrder}
              >
                Huy don hang
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Submit Confirmation Modal */}
      {showSubmitConfirm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Xac nhan ban hang</h3>
            <p>Ban co chac chan muon xac nhan don hang nay?</p>
            <div className="confirm-summary">
              <div className="confirm-row">
                <span>So san pham:</span>
                <span>{totalItems} items</span>
              </div>
              <div className="confirm-row total">
                <span>Tong tien:</span>
                <span>{formatCurrency(totalAmount, primaryCurrency)}</span>
              </div>
            </div>
            <p className="modal-info">
              So luong ton kho se duoc cap nhat sau khi xac nhan.
            </p>
            <div className="modal-actions">
              <button
                className="modal-btn cancel-btn"
                onClick={() => setShowSubmitConfirm(false)}
              >
                Quay lai
              </button>
              <button
                className="modal-btn confirm-btn success"
                onClick={handleSubmitOrder}
              >
                Xac nhan
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Instructions */}
      {orderItems.length === 0 && (
        <div className="order-instructions">
          <h3>Huong dan su dung</h3>
          <ul>
            <li>Su dung may quet de quet ma barcode/QR tren san pham</li>
            <li>Hoac nhap thu cong ma SKU vao o tim kiem</li>
            <li>San pham se tu dong duoc them vao don hang</li>
            <li>Quet lai san pham da co de tang so luong</li>
            <li>Click "Xac nhan ban hang" khi hoan tat</li>
            <li>Hoac click "PDF" de import tu file hoa don PDF</li>
          </ul>
        </div>
      )}

      {/* PDF Invoice Reader Modal */}
      {showPdfReader && (
        <PDFInvoiceReader
          onImportItems={handleImportFromPdf}
          onClose={() => setShowPdfReader(false)}
        />
      )}
    </div>
  );
};

export default CreateOrder;
