import { useState, useEffect, useRef, useCallback } from "react";
import { rfidApi } from "@/services/labelApi";
import "./ScannerPage.css";

export default function ScannerPage() {
  const inputRef = useRef(null);
  const [epcInput, setEpcInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [product, setProduct] = useState(null);
  const [tagInfo, setTagInfo] = useState(null);
  const [scanHistory, setScanHistory] = useState(() => {
    try {
      const saved = localStorage.getItem("rfid-scan-history");
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });
  const [showHistory, setShowHistory] = useState(false);
  const [deviceId] = useState(() => {
    let id = localStorage.getItem("scanner-device-id");
    if (!id) {
      id = "PDA-" + Math.random().toString(36).substring(2, 8).toUpperCase();
      localStorage.setItem("scanner-device-id", id);
    }
    return id;
  });

  // Auto-focus input on load
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // Save history to localStorage
  useEffect(() => {
    try {
      localStorage.setItem("rfid-scan-history", JSON.stringify(scanHistory.slice(0, 100)));
    } catch {}
  }, [scanHistory]);

  // Detect if input is EPC (24 hex chars) or Barcode (numeric, 8-14 digits)
  const detectScanType = (input) => {
    const clean = input.trim().toUpperCase();
    // EPC: 24 hex characters (SGTIN-96)
    if (clean.length === 24 && /^[A-F0-9]+$/.test(clean)) {
      return "EPC";
    }
    // Barcode: 8-14 digits (EAN-8, EAN-13, UPC-A, etc.)
    if (/^\d{8,14}$/.test(clean)) {
      return "BARCODE";
    }
    // Default: try as EPC if hex, otherwise barcode
    if (/^[A-F0-9]+$/.test(clean)) {
      return "EPC";
    }
    return "BARCODE";
  };

  // Handle scan/lookup
  const handleScan = useCallback(async (input) => {
    if (!input || !input.trim()) return;

    const cleanInput = input.trim().toUpperCase();
    const scanType = detectScanType(cleanInput);

    setLoading(true);
    setError(null);
    setProduct(null);
    setTagInfo(null);

    try {
      let scanResponse;

      if (scanType === "EPC") {
        // Scan by RFID EPC
        scanResponse = await rfidApi.recordScan({
          epc: cleanInput,
          deviceId: deviceId,
          deviceName: "PDA Scanner",
          deviceType: "PDA",
          scanType: "LOOKUP",
          location: null,
        });
      } else {
        // Scan by Barcode
        scanResponse = await rfidApi.scanByBarcode(cleanInput);
      }

      if (scanResponse.data.tagFound) {
        setTagInfo(scanResponse.data.tagInfo);
        setProduct(scanResponse.data.product);

        // Add to history
        const historyItem = {
          id: Date.now(),
          epc: cleanInput,
          scanType: scanType,
          product: scanResponse.data.product,
          scannedAt: new Date().toISOString(),
          found: true,
        };
        setScanHistory((prev) => [historyItem, ...prev]);
      } else {
        setError(scanType === "EPC"
          ? "Tag RFID không tìm thấy trong hệ thống"
          : "Sản phẩm không tìm thấy với barcode này");
        // Still add to history as unknown
        const historyItem = {
          id: Date.now(),
          epc: cleanInput,
          scanType: scanType,
          product: null,
          scannedAt: new Date().toISOString(),
          found: false,
        };
        setScanHistory((prev) => [historyItem, ...prev]);
      }
    } catch (err) {
      console.error("Scan error:", err);
      if (err.response?.status === 404) {
        setError(scanType === "EPC"
          ? "Tag RFID không tìm thấy"
          : "Sản phẩm không tìm thấy");
      } else {
        setError("Lỗi kết nối. Vui lòng thử lại.");
      }
    } finally {
      setLoading(false);
      setEpcInput("");
      // Re-focus for next scan
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [deviceId]);

  // Handle input change with auto-submit
  const handleInputChange = (e) => {
    const value = e.target.value;
    setEpcInput(value);

    // Auto-submit if looks like complete EPC (24 hex chars for SGTIN-96)
    if (value.length === 24 && /^[A-Fa-f0-9]+$/.test(value)) {
      handleScan(value);
      return;
    }

    // Auto-submit if looks like complete barcode (13 digits for EAN-13)
    if (value.length === 13 && /^\d+$/.test(value)) {
      handleScan(value);
      return;
    }
  };

  // Handle Enter key
  const handleKeyDown = (e) => {
    if (e.key === "Enter" && epcInput.trim()) {
      e.preventDefault();
      handleScan(epcInput);
    }
  };

  // Format price
  const formatPrice = (price) => {
    if (!price) return "-";
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
    });
  };

  // Clear history
  const clearHistory = () => {
    if (confirm("Xóa toàn bộ lịch sử quét?")) {
      setScanHistory([]);
    }
  };

  // Reset for new scan
  const resetScan = () => {
    setProduct(null);
    setTagInfo(null);
    setError(null);
    setEpcInput("");
    inputRef.current?.focus();
  };

  return (
    <div className="scanner-app">
      {/* Header */}
      <header className="scanner-header">
        <h1>RFID Scanner</h1>
        <button
          className="history-btn"
          onClick={() => setShowHistory(!showHistory)}
        >
          {showHistory ? "Quét" : `Lịch sử (${scanHistory.length})`}
        </button>
      </header>

      {!showHistory ? (
        <main className="scanner-main">
          {/* Scan Input */}
          <div className="scan-input-container">
            <div className="scan-icon">📡</div>
            <input
              ref={inputRef}
              type="text"
              className="scan-input"
              placeholder="Quét EPC hoặc Barcode..."
              value={epcInput}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              autoComplete="off"
              autoCorrect="off"
              autoCapitalize="characters"
              spellCheck="false"
            />
            {epcInput && (
              <button className="clear-input-btn" onClick={() => setEpcInput("")}>
                ✕
              </button>
            )}
          </div>

          {/* Manual scan button */}
          {epcInput && (
            <button
              className="scan-btn"
              onClick={() => handleScan(epcInput)}
              disabled={loading}
            >
              {loading ? "Đang tìm..." : "Tìm kiếm"}
            </button>
          )}

          {/* Loading */}
          {loading && (
            <div className="loading-container">
              <div className="loading-spinner"></div>
              <p>Đang tra cứu...</p>
            </div>
          )}

          {/* Error */}
          {error && !loading && (
            <div className="error-container">
              <div className="error-icon">⚠️</div>
              <p>{error}</p>
              <button className="retry-btn" onClick={resetScan}>
                Quét lại
              </button>
            </div>
          )}

          {/* Product Result */}
          {product && !loading && (
            <div className="product-result">
              <div className="product-image">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} />
                ) : (
                  <div className="no-image">💎</div>
                )}
              </div>

              <div className="product-info">
                <h2 className="product-name">{product.name}</h2>

                <div className="product-details">
                  <div className="detail-row">
                    <span className="label">SKU:</span>
                    <span className="value">{product.sku || "-"}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Barcode:</span>
                    <span className="value">{product.barcode || "-"}</span>
                  </div>
                  <div className="detail-row highlight">
                    <span className="label">Giá:</span>
                    <span className="value price">{formatPrice(product.price)}</span>
                  </div>
                </div>

                {tagInfo && (
                  <div className="tag-info">
                    <div className="detail-row">
                      <span className="label">EPC:</span>
                      <span className="value mono">{tagInfo.epc}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">Trạng thái:</span>
                      <span className={`value status status-${tagInfo.status?.toLowerCase()}`}>
                        {tagInfo.status === "ACTIVE" ? "Hoạt động" : tagInfo.status}
                      </span>
                    </div>
                    <div className="detail-row">
                      <span className="label">Lần quét:</span>
                      <span className="value">{tagInfo.scanCount || 0}</span>
                    </div>
                  </div>
                )}
              </div>

              <button className="new-scan-btn" onClick={resetScan}>
                Quét tiếp
              </button>
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && !product && (
            <div className="empty-state">
              <div className="empty-icon">📱</div>
              <p>Hướng PDA vào tag RFID hoặc Barcode</p>
              <p className="hint">Hỗ trợ: EPC (24 ký tự) | Barcode (13 số)</p>
            </div>
          )}
        </main>
      ) : (
        /* History View */
        <main className="scanner-main history-view">
          <div className="history-header">
            <h2>Lịch sử quét</h2>
            {scanHistory.length > 0 && (
              <button className="clear-history-btn" onClick={clearHistory}>
                Xóa tất cả
              </button>
            )}
          </div>

          {scanHistory.length === 0 ? (
            <div className="empty-history">
              <p>Chưa có lịch sử quét</p>
            </div>
          ) : (
            <div className="history-list">
              {scanHistory.map((item) => (
                <div
                  key={item.id}
                  className={`history-item ${item.found ? "found" : "not-found"}`}
                  onClick={() => {
                    if (item.found && item.product) {
                      setProduct(item.product);
                      setTagInfo({ epc: item.epc });
                      setShowHistory(false);
                    }
                  }}
                >
                  <div className="history-item-main">
                    <div className="history-status">
                      {item.found ? "✓" : "✗"}
                    </div>
                    <div className="history-content">
                      <div className="history-product">
                        {item.found ? item.product?.name : "Không tìm thấy"}
                      </div>
                      <div className="history-epc">{item.epc}</div>
                    </div>
                  </div>
                  <div className="history-time">{formatDate(item.scannedAt)}</div>
                </div>
              ))}
            </div>
          )}
        </main>
      )}

      {/* Footer */}
      <footer className="scanner-footer">
        <span className="device-id">Device: {deviceId}</span>
        <span className="status online">● Online</span>
      </footer>
    </div>
  );
}
