import React, { useState, useRef, useEffect } from "react";
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
} from "lucide-react";
import "./CreateOrder.css";

// Mock data cho demo UI - sau này sẽ thay bằng API call
const MOCK_PRODUCTS = [
  {
    id: "PRD000001",
    name: "Lumina Diamond Ring",
    sku: "SKU-LDR-001",
    price: 22999999.99,
    currency: "VND",
    imageUrl: "https://mirror-storage.s3.ap-southeast-1.amazonaws.com/public/dd34bd94-b46d-4746-bd16-4c5b652ab2a3_2a5a8fd0404bf615af5a.jpg",
    stockQuantity: 15,
  },
  {
    id: "PRD000002",
    name: "Aurora Pendant",
    sku: "SKU-AP-001",
    price: 18000000,
    currency: "VND",
    imageUrl: "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=800",
    stockQuantity: 8,
  },
  {
    id: "PRD000003",
    name: "Solaris Gold Ring",
    sku: "SKU-SGR-001",
    price: 22000000,
    currency: "VND",
    imageUrl: "https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?w=800",
    stockQuantity: 12,
  },
  {
    id: "PRD000004",
    name: "Luna Sapphire Earrings",
    sku: "SKU-LSE-001",
    price: 32000000,
    currency: "VND",
    imageUrl: "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=800",
    stockQuantity: 6,
  },
  {
    id: "PRD000009",
    name: "Diamond Solitaire Engagement Ring",
    sku: "SKU-DSER-001",
    price: 25000000,
    currency: "VND",
    imageUrl: "https://mirror-storage.s3.ap-southeast-1.amazonaws.com/public/4c191c66-9f05-417c-aca8-6d0c1a5b1743_model_1.svg",
    stockQuantity: 1,
  },
  {
    id: "PRD000024",
    name: "Mirror Custom Ring",
    sku: "SKU-MCR-001",
    price: 15600,
    currency: "USD",
    imageUrl: "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=800",
    stockQuantity: 999,
  },
];

const CreateOrder = () => {
  const inputRef = useRef(null);
  const [skuInput, setSkuInput] = useState("");
  const [orderItems, setOrderItems] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);
  const [showSubmitConfirm, setShowSubmitConfirm] = useState(false);

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

  // Mock function to find product by SKU - sau này thay bằng API call
  const findProductBySku = (sku) => {
    return MOCK_PRODUCTS.find(
      (p) => p.sku.toLowerCase() === sku.toLowerCase()
    );
  };

  // Handle input change
  const handleInputChange = (e) => {
    setSkuInput(e.target.value);
    setError("");
  };

  // Handle key press - scanner sends Enter after scan
  const handleKeyPress = (e) => {
    if (e.key === "Enter" && skuInput.trim()) {
      handleAddProduct();
    }
  };

  // Add product to order
  const handleAddProduct = () => {
    const sku = skuInput.trim();
    if (!sku) {
      setError("Vui long nhap ma SKU");
      return;
    }

    // Find product
    const product = findProductBySku(sku);
    if (!product) {
      setError(`Khong tim thay san pham voi ma SKU: ${sku}`);
      setSkuInput("");
      inputRef.current?.focus();
      return;
    }

    // Check if product is out of stock
    if (product.stockQuantity <= 0) {
      setError(`San pham "${product.name}" da het hang`);
      setSkuInput("");
      inputRef.current?.focus();
      return;
    }

    // Check if product already in order
    const existingItem = orderItems.find((item) => item.id === product.id);

    if (existingItem) {
      // Check if can add more
      if (existingItem.quantity >= product.stockQuantity) {
        setError(`Chi con ${product.stockQuantity} san pham "${product.name}" trong kho`);
        setSkuInput("");
        inputRef.current?.focus();
        return;
      }

      // Increase quantity
      setOrderItems((prev) =>
        prev.map((item) =>
          item.id === product.id
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
  };

  // Update item quantity
  const handleQuantityChange = (itemId, newQuantity) => {
    const item = orderItems.find((i) => i.id === itemId);
    if (!item) return;

    if (newQuantity < 1) {
      // Remove item if quantity < 1
      handleRemoveItem(itemId);
      return;
    }

    if (newQuantity > item.stockQuantity) {
      setError(`Chi con ${item.stockQuantity} san pham "${item.name}" trong kho`);
      return;
    }

    setOrderItems((prev) =>
      prev.map((i) =>
        i.id === itemId ? { ...i, quantity: newQuantity } : i
      )
    );
  };

  // Remove item from order
  const handleRemoveItem = (itemId) => {
    const item = orderItems.find((i) => i.id === itemId);
    setOrderItems((prev) => prev.filter((i) => i.id !== itemId));
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
            placeholder="Quet hoac nhap ma SKU..."
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
          disabled={!skuInput.trim()}
        >
          <Plus size={20} />
          Them
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
                  <tr key={item.id}>
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
                      <span className="stock-info">
                        Ton kho: {item.stockQuantity}
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
                            handleQuantityChange(item.id, item.quantity - 1)
                          }
                        >
                          <Minus size={16} />
                        </button>
                        <input
                          type="number"
                          value={item.quantity}
                          onChange={(e) =>
                            handleQuantityChange(
                              item.id,
                              parseInt(e.target.value) || 0
                            )
                          }
                          min="1"
                          max={item.stockQuantity}
                        />
                        <button
                          className="qty-btn"
                          onClick={() =>
                            handleQuantityChange(item.id, item.quantity + 1)
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
                        onClick={() => handleRemoveItem(item.id)}
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
            <span>Quet ma SKU de them san pham</span>
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
          </ul>
        </div>
      )}
    </div>
  );
};

export default CreateOrder;
