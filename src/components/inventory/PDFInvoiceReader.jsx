import React, { useState, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  Upload,
  FileText,
  AlertCircle,
  X,
  RotateCcw,
  Printer,
  Trash2,
  Plus,
  Loader,
} from "lucide-react";
import { readPdfInvoice } from "../../services/pdfExtractApi";
import "./PDFInvoiceReader.css";

const PDFInvoiceReader = ({ onClose }) => {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const dropZoneRef = useRef(null);
  const [file, setFile] = useState(null);
  const [error, setError] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [progress, setProgress] = useState({ stage: "", progress: 0 });

  // Extracted invoice data (editable)
  const [invoiceData, setInvoiceData] = useState(null);

  // Handle file selection (PDF only)
  const handleFileSelect = useCallback((e) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      if (selectedFile.type !== "application/pdf") {
        setError("Vui long chon file PDF");
        return;
      }
      setFile(selectedFile);
      setError("");
      setInvoiceData(null);
    }
  }, []);

  // Handle drag and drop (PDF only)
  const handleDrop = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    const droppedFile = e.dataTransfer?.files?.[0];
    if (droppedFile) {
      if (droppedFile.type !== "application/pdf") {
        setError("Vui long chon file PDF");
        return;
      }
      setFile(droppedFile);
      setError("");
      setInvoiceData(null);
    }
  }, []);

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  // Read PDF and extract data
  const handleReadPdf = async () => {
    if (!file) return;

    setIsProcessing(true);
    setError("");

    try {
      const data = await readPdfInvoice(file, {
        onProgress: (p) => setProgress(p),
      });

      console.log("=== Extracted Invoice Data ===");
      console.log(data);
      console.log("==============================");

      setInvoiceData({
        invoiceCode: data.invoiceCode || "2C25MYY",
        invoiceNumber: data.invoiceNumber || "",
        invoiceDate: data.invoiceDate || new Date().toLocaleDateString("vi-VN"),
        paymentMethod: data.paymentMethod || "TM/CK",
        customerCompany: data.customerCompany || "Khach le khong lay hoa don",
        customerName: data.customerName || "Khach le khong lay hoa don",
        customerTaxCode: data.customerTaxCode || "",
        customerAddress: data.customerAddress || "",
        customerPhone: data.customerPhone || "",
        items: data.items || [],
        totalInWords: data.totalInWords || "",
        subtotal: data.subtotal || 0,
      });
    } catch (err) {
      console.error("PDF read error:", err);
      setError(`Loi doc PDF: ${err.message}`);
    } finally {
      setIsProcessing(false);
      setProgress({ stage: "", progress: 0 });
    }
  };

  // Update invoice field
  const updateField = (field, value) => {
    setInvoiceData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Update item field
  const updateItem = (index, field, value) => {
    setInvoiceData((prev) => ({
      ...prev,
      items: prev.items.map((item, i) =>
        i === index ? { ...item, [field]: value } : item
      ),
    }));
  };

  // Add new item
  const addItem = () => {
    setInvoiceData((prev) => ({
      ...prev,
      items: [
        ...(prev.items || []),
        {
          stt: (prev.items?.length || 0) + 1,
          name: "",
          unit: "cai",
          quantity: 1,
          unitPrice: 0,
          total: 0,
        },
      ],
    }));
  };

  // Remove item
  const removeItem = (index) => {
    setInvoiceData((prev) => ({
      ...prev,
      items: prev.items
        .filter((_, i) => i !== index)
        .map((item, i) => ({ ...item, stt: i + 1 })),
    }));
  };

  // Calculate totals
  const calculateTotal = () => {
    if (!invoiceData?.items) return 0;
    return invoiceData.items.reduce((sum, item) => sum + (item.total || 0), 0);
  };

  // Export to LuxuryInvoice
  const handleExportToInvoice = () => {
    if (!invoiceData) return;

    // Parse date if string
    let invoiceDate = new Date();
    if (invoiceData.invoiceDate) {
      const parts = invoiceData.invoiceDate.split("/");
      if (parts.length === 3) {
        invoiceDate = new Date(parts[2], parts[1] - 1, parts[0]);
      }
    }

    const orderData = {
      invoiceCode: invoiceData.invoiceCode || "2C25MYY",
      invoiceNumber: invoiceData.invoiceNumber || "<Chua cap so>",
      invoiceDate: invoiceDate,
      taxAuthCode: invoiceData.taxAuthCode || "",
      customerCompany: invoiceData.customerCompany || "Khach le khong lay hoa don",
      customerTaxCode: invoiceData.customerTaxCode || "",
      customerAddress: invoiceData.customerAddress || "",
      customerName: invoiceData.customerName || "Khach le khong lay hoa don",
      customerPhone: invoiceData.customerPhone || "",
      customerIdNumber: invoiceData.customerIdNumber || "",
      paymentMethod: invoiceData.paymentMethod || "TM/CK",
      items: (invoiceData.items || []).map((item) => ({
        name: item.name,
        quantity: item.quantity,
        price: item.unitPrice,
      })),
    };

    navigate("/inventory/invoice", { state: { orderData } });
    onClose();
  };

  // Reset
  const handleReset = () => {
    setFile(null);
    setInvoiceData(null);
    setError("");
    setIsProcessing(false);
    setProgress({ stage: "", progress: 0 });
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  // Format currency
  const formatCurrency = (value) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(value || 0);
  };

  // Get progress text
  const getProgressText = () => {
    switch (progress.stage) {
      case "reading":
        return "Dang doc file PDF...";
      case "extracting":
        return "Dang trich xuat van ban...";
      case "parsing":
        return "Dang phan tich hoa don...";
      case "done":
        return "Hoan thanh!";
      default:
        return "Dang xu ly...";
    }
  };

  return (
    <div className="pdf-reader-overlay">
      <div className="pdf-reader-modal large">
        {/* Header */}
        <div className="pdf-reader-header">
          <h2>
            <FileText size={24} />
            Doc hoa don PDF
          </h2>
          <button className="close-btn" onClick={onClose}>
            <X size={24} />
          </button>
        </div>

        {/* Content */}
        <div className="pdf-reader-content">
          {/* File Upload Section */}
          {!invoiceData && (
            <div className="upload-section">
              <div
                ref={dropZoneRef}
                className={`drop-zone ${file ? "has-file" : ""}`}
                onDrop={handleDrop}
                onDragOver={handleDragOver}
                onClick={() => fileInputRef.current?.click()}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf,application/pdf"
                  onChange={handleFileSelect}
                  style={{ display: "none" }}
                />
                {file ? (
                  <>
                    <FileText size={48} className="file-icon" />
                    <span className="file-name">{file.name}</span>
                    <span className="file-size">
                      {(file.size / 1024 / 1024).toFixed(2)} MB
                    </span>
                  </>
                ) : (
                  <>
                    <Upload size={48} />
                    <span>Keo tha file PDF vao day</span>
                    <span className="drop-hint">Hoac click de chon file</span>
                  </>
                )}
              </div>

              {/* Processing indicator */}
              {isProcessing && (
                <div className="processing-indicator">
                  <Loader size={24} className="spinner" />
                  <span>{getProgressText()}</span>
                  <div className="progress-bar">
                    <div
                      className="progress-fill"
                      style={{ width: `${progress.progress}%` }}
                    />
                  </div>
                </div>
              )}

              {file && !isProcessing && (
                <button className="process-btn" onClick={handleReadPdf}>
                  <FileText size={20} />
                  Doc hoa don
                </button>
              )}
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="error-message">
              <AlertCircle size={20} />
              <span>{error}</span>
            </div>
          )}

          {/* Invoice Data Form */}
          {invoiceData && (
            <div className="invoice-form-container single-panel">
              {/* Form Panel */}
              <div className="invoice-form">
                {/* Reset button */}
                <div className="form-header">
                  <span>Du lieu trich xuat tu PDF</span>
                  <button className="reset-btn" onClick={handleReset}>
                    <RotateCcw size={16} />
                    Chon file khac
                  </button>
                </div>

                {/* Invoice Info Section */}
                <div className="form-section">
                  <h3>Thong tin hoa don</h3>
                  <div className="form-grid">
                    <div className="form-group">
                      <label>Ky hieu</label>
                      <input
                        type="text"
                        value={invoiceData.invoiceCode || ""}
                        onChange={(e) => updateField("invoiceCode", e.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label>So hoa don</label>
                      <input
                        type="text"
                        value={invoiceData.invoiceNumber || ""}
                        onChange={(e) => updateField("invoiceNumber", e.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label>Ngay (DD/MM/YYYY)</label>
                      <input
                        type="text"
                        value={invoiceData.invoiceDate || ""}
                        onChange={(e) => updateField("invoiceDate", e.target.value)}
                        placeholder="25/12/2025"
                      />
                    </div>
                    <div className="form-group">
                      <label>Hinh thuc thanh toan</label>
                      <input
                        type="text"
                        value={invoiceData.paymentMethod || ""}
                        onChange={(e) => updateField("paymentMethod", e.target.value)}
                      />
                    </div>
                  </div>
                </div>

                {/* Customer Info Section */}
                <div className="form-section">
                  <h3>Thong tin khach hang</h3>
                  <div className="form-grid">
                    <div className="form-group full-width">
                      <label>Ten don vi</label>
                      <input
                        type="text"
                        value={invoiceData.customerCompany || ""}
                        onChange={(e) => updateField("customerCompany", e.target.value)}
                      />
                    </div>
                    <div className="form-group full-width">
                      <label>Ho va ten nguoi mua</label>
                      <input
                        type="text"
                        value={invoiceData.customerName || ""}
                        onChange={(e) => updateField("customerName", e.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label>Ma so thue / CCCD</label>
                      <input
                        type="text"
                        value={invoiceData.customerTaxCode || ""}
                        onChange={(e) => updateField("customerTaxCode", e.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label>Dien thoai</label>
                      <input
                        type="text"
                        value={invoiceData.customerPhone || ""}
                        onChange={(e) => updateField("customerPhone", e.target.value)}
                      />
                    </div>
                    <div className="form-group full-width">
                      <label>Dia chi</label>
                      <input
                        type="text"
                        value={invoiceData.customerAddress || ""}
                        onChange={(e) => updateField("customerAddress", e.target.value)}
                      />
                    </div>
                  </div>
                </div>

                {/* Items Section */}
                <div className="form-section">
                  <div className="section-header">
                    <h3>Danh sach san pham ({invoiceData.items?.length || 0})</h3>
                    <button className="add-item-btn" onClick={addItem}>
                      <Plus size={16} />
                      Them san pham
                    </button>
                  </div>

                  {invoiceData.items && invoiceData.items.length > 0 ? (
                    <div className="items-table-wrapper">
                      <table className="items-edit-table">
                        <thead>
                          <tr>
                            <th>STT</th>
                            <th>Ten san pham</th>
                            <th>DVT</th>
                            <th>SL</th>
                            <th>Don gia</th>
                            <th>Thanh tien</th>
                            <th></th>
                          </tr>
                        </thead>
                        <tbody>
                          {invoiceData.items.map((item, index) => (
                            <tr key={index}>
                              <td className="stt-cell">{item.stt}</td>
                              <td>
                                <input
                                  type="text"
                                  value={item.name || ""}
                                  onChange={(e) => updateItem(index, "name", e.target.value)}
                                  className="name-input"
                                />
                              </td>
                              <td>
                                <input
                                  type="text"
                                  value={item.unit || "cai"}
                                  onChange={(e) => updateItem(index, "unit", e.target.value)}
                                  className="unit-input"
                                />
                              </td>
                              <td>
                                <input
                                  type="number"
                                  value={item.quantity || 1}
                                  onChange={(e) => {
                                    const qty = parseInt(e.target.value) || 1;
                                    updateItem(index, "quantity", qty);
                                    updateItem(index, "total", qty * (item.unitPrice || 0));
                                  }}
                                  className="qty-input"
                                  min="1"
                                />
                              </td>
                              <td>
                                <input
                                  type="number"
                                  value={item.unitPrice || 0}
                                  onChange={(e) => {
                                    const price = parseFloat(e.target.value) || 0;
                                    updateItem(index, "unitPrice", price);
                                    updateItem(index, "total", (item.quantity || 1) * price);
                                  }}
                                  className="price-input"
                                />
                              </td>
                              <td className="total-cell">{formatCurrency(item.total)}</td>
                              <td>
                                <button
                                  className="remove-item-btn"
                                  onClick={() => removeItem(index)}
                                >
                                  <Trash2 size={16} />
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                        <tfoot>
                          <tr>
                            <td colSpan="5" className="total-label">
                              TONG CONG:
                            </td>
                            <td className="total-value" colSpan="2">
                              {formatCurrency(calculateTotal())}
                            </td>
                          </tr>
                        </tfoot>
                      </table>
                    </div>
                  ) : (
                    <div className="no-items">
                      <p>Chua co san pham nao</p>
                      <button className="add-item-btn" onClick={addItem}>
                        <Plus size={16} />
                        Them san pham
                      </button>
                    </div>
                  )}
                </div>

                {/* Total in Words */}
                {invoiceData.totalInWords && (
                  <div className="form-section">
                    <div className="form-group full-width">
                      <label>So tien viet bang chu</label>
                      <input
                        type="text"
                        value={invoiceData.totalInWords || ""}
                        onChange={(e) => updateField("totalInWords", e.target.value)}
                      />
                    </div>
                  </div>
                )}
              </div>
              {/* End Form Panel */}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="pdf-reader-footer">
          <button className="cancel-btn" onClick={onClose}>
            Huy
          </button>
          {invoiceData && (
            <button className="export-btn" onClick={handleExportToInvoice}>
              <Printer size={20} />
              Xuat hoa don
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default PDFInvoiceReader;
