import React, { useState, useEffect } from "react";
import { inventoryProductsAPI } from "@/services/inventoryApi";
import {
  Printer,
  Copy,
  CheckCircle,
  Package,
  Search,
  X,
  FileJson,
  FileSpreadsheet,
  Check,
} from "lucide-react";
import "./PrintLabel.css";

const PrintLabel = () => {
  const [products, setProducts] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [copied, setCopied] = useState(false);
  const [exportFormat, setExportFormat] = useState("json");
  const [labelOptions, setLabelOptions] = useState({
    includeSku: true,
    includeName: true,
    includePrice: true,
  });

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      const response = await inventoryProductsAPI.getAll();
      const data = response.data?.data || response.data || [];
      setProducts(data);
    } catch (error) {
      console.error("Error fetching products:", error);
    } finally {
      setLoading(false);
    }
  };

  const filteredProducts = products.filter((p) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      p.name?.toLowerCase().includes(term) ||
      p.sku?.toLowerCase().includes(term)
    );
  });

  const toggleProduct = (product) => {
    setSelectedProducts((prev) => {
      const exists = prev.find((p) => p.id === product.id);
      if (exists) {
        return prev.filter((p) => p.id !== product.id);
      }
      return [...prev, product];
    });
  };

  const selectAll = () => {
    setSelectedProducts(filteredProducts);
  };

  const deselectAll = () => {
    setSelectedProducts([]);
  };

  const isSelected = (productId) => {
    return selectedProducts.some((p) => p.id === productId);
  };

  const generateExportData = () => {
    return selectedProducts.map((p) => {
      const data = {};
      if (labelOptions.includeSku) data.sku = p.sku;
      if (labelOptions.includeName) data.name = p.name;
      if (labelOptions.includePrice) {
        data.price = p.price;
        data.currency = p.currency;
      }
      return data;
    });
  };

  const handleCopyJSON = () => {
    const data = generateExportData();
    navigator.clipboard.writeText(JSON.stringify(data, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleCopyCSV = () => {
    const data = generateExportData();
    if (data.length === 0) return;

    const headers = Object.keys(data[0]);
    const csvRows = [
      headers.join(","),
      ...data.map((row) =>
        headers.map((h) => `"${row[h] || ""}"`).join(",")
      ),
    ];
    const csvString = csvRows.join("\n");

    navigator.clipboard.writeText(csvString);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleExport = () => {
    if (exportFormat === "json") {
      handleCopyJSON();
    } else {
      handleCopyCSV();
    }
  };

  const formatCurrency = (value, currency = "VND") => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency,
    }).format(value);
  };

  if (loading) {
    return (
      <div className="print-label-page">
        <div className="print-loading">Dang tai...</div>
      </div>
    );
  }

  return (
    <div className="print-label-page">
      <div className="print-header">
        <h1>In Label</h1>
        <p>Chon san pham va tao du lieu de in label</p>
      </div>

      <div className="print-layout">
        {/* Left: Product Selection */}
        <div className="print-products-section">
          <div className="section-header">
            <h2>Chon san pham ({selectedProducts.length} da chon)</h2>
            <div className="selection-actions">
              <button onClick={selectAll}>Chon tat ca</button>
              <button onClick={deselectAll}>Bo chon</button>
            </div>
          </div>

          <div className="search-wrapper">
            <Search size={20} className="search-icon" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tim kiem san pham..."
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

          <div className="products-list">
            {filteredProducts.length > 0 ? (
              filteredProducts.map((product) => (
                <div
                  key={product.id}
                  className={`product-item ${
                    isSelected(product.id) ? "selected" : ""
                  }`}
                  onClick={() => toggleProduct(product)}
                >
                  <div className="product-checkbox">
                    {isSelected(product.id) && <Check size={16} />}
                  </div>
                  <div className="product-image">
                    {product.imageUrl ? (
                      <img src={product.imageUrl} alt={product.name} />
                    ) : (
                      <Package size={20} />
                    )}
                  </div>
                  <div className="product-info">
                    <span className="product-name">{product.name}</span>
                    <span className="product-sku">{product.sku}</span>
                  </div>
                  <div className="product-price">
                    {formatCurrency(product.price, product.currency)}
                  </div>
                </div>
              ))
            ) : (
              <div className="no-products">Khong tim thay san pham</div>
            )}
          </div>
        </div>

        {/* Right: Export Options */}
        <div className="print-export-section">
          <div className="export-card">
            <h2>Tuy chon Label</h2>

            <div className="label-options">
              <label className="checkbox-option">
                <input
                  type="checkbox"
                  checked={labelOptions.includeSku}
                  onChange={(e) =>
                    setLabelOptions((prev) => ({
                      ...prev,
                      includeSku: e.target.checked,
                    }))
                  }
                />
                <span>Ma SKU</span>
              </label>
              <label className="checkbox-option">
                <input
                  type="checkbox"
                  checked={labelOptions.includeName}
                  onChange={(e) =>
                    setLabelOptions((prev) => ({
                      ...prev,
                      includeName: e.target.checked,
                    }))
                  }
                />
                <span>Ten san pham</span>
              </label>
              <label className="checkbox-option">
                <input
                  type="checkbox"
                  checked={labelOptions.includePrice}
                  onChange={(e) =>
                    setLabelOptions((prev) => ({
                      ...prev,
                      includePrice: e.target.checked,
                    }))
                  }
                />
                <span>Gia</span>
              </label>
            </div>

            <div className="export-format">
              <h3>Dinh dang xuat</h3>
              <div className="format-options">
                <button
                  className={`format-btn ${
                    exportFormat === "json" ? "active" : ""
                  }`}
                  onClick={() => setExportFormat("json")}
                >
                  <FileJson size={20} />
                  JSON
                </button>
                <button
                  className={`format-btn ${
                    exportFormat === "csv" ? "active" : ""
                  }`}
                  onClick={() => setExportFormat("csv")}
                >
                  <FileSpreadsheet size={20} />
                  CSV
                </button>
              </div>
            </div>

            <button
              className="export-btn"
              onClick={handleExport}
              disabled={selectedProducts.length === 0}
            >
              {copied ? (
                <>
                  <CheckCircle size={20} />
                  Da copy!
                </>
              ) : (
                <>
                  <Copy size={20} />
                  Copy du lieu ({selectedProducts.length})
                </>
              )}
            </button>
          </div>

          {/* Preview */}
          {selectedProducts.length > 0 && (
            <div className="preview-card">
              <h2>Xem truoc</h2>
              <div className="preview-content">
                <pre>
                  {exportFormat === "json"
                    ? JSON.stringify(generateExportData(), null, 2)
                    : (() => {
                        const data = generateExportData();
                        if (data.length === 0) return "";
                        const headers = Object.keys(data[0]);
                        return [
                          headers.join(","),
                          ...data.map((row) =>
                            headers.map((h) => row[h] || "").join(",")
                          ),
                        ].join("\n");
                      })()}
                </pre>
              </div>
            </div>
          )}

          {/* Instructions */}
          <div className="instructions-card">
            <h3>Huong dan</h3>
            <ol>
              <li>Chon cac san pham can in label</li>
              <li>Chon thong tin muon hien thi tren label</li>
              <li>Chon dinh dang xuat (JSON hoac CSV)</li>
              <li>Click "Copy du lieu" de sao chep</li>
              <li>Dan vao phan mem in label cua ban</li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PrintLabel;
