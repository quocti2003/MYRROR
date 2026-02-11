import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { printJobApi, printerApi } from "@/services/labelApi";
import { getAllProductsWithWorkflow } from "@/services/productOpsApi";
import { ROUTES } from "@/constants/routes";
import useWebUsbPrinter from "@/hooks/useWebUsbPrinter";
import "@/components/pod-admin/PodAdminLayout.css";

const STATUS_COLORS = {
  PENDING: { bg: "#fef3c7", color: "#92400e" },
  PRINTING: { bg: "#dbeafe", color: "#1d4ed8" },
  COMPLETED: { bg: "#dcfce7", color: "#166534" },
  FAILED: { bg: "#fee2e2", color: "#dc2626" },
  CANCELLED: { bg: "#f3f4f6", color: "#6b7280" },
};

export default function PrintJobs() {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [expandedJob, setExpandedJob] = useState(null);
  const [jobItems, setJobItems] = useState({});

  // Printer modal state
  const [showPrintModal, setShowPrintModal] = useState(false);
  const [selectedJobId, setSelectedJobId] = useState(null);
  const [printMethod, setPrintMethod] = useState("NETWORK");
  const [printerAddress, setPrinterAddress] = useState("");
  const [availablePrinters, setAvailablePrinters] = useState([]);
  const [printing, setPrinting] = useState(false);
  const [testingConnection, setTestingConnection] = useState(false);

  // Jewelry label modal state
  const [showJewelryModal, setShowJewelryModal] = useState(false);
  const [jewelryPrice, setJewelryPrice] = useState("10.000.000");
  const [jewelryProductName, setJewelryProductName] = useState("");
  const [jewelryBarcode, setJewelryBarcode] = useState("");
  const [jewelryIncludeRFID, setJewelryIncludeRFID] = useState(false);
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [productSearch, setProductSearch] = useState("");
  const [showProductDropdown, setShowProductDropdown] = useState(false);

  // WebUSB printer hook
  const {
    isSupported: webUsbSupported,
    isConnected: webUsbConnected,
    deviceInfo: webUsbDeviceInfo,
    connect: connectWebUsb,
    disconnect: disconnectWebUsb,
    printZPL: printWebUsb,
    error: webUsbError,
    isPrinting: webUsbPrinting,
  } = useWebUsbPrinter();

  useEffect(() => {
    fetchJobs();
  }, [currentPage, statusFilter]);

  const fetchJobs = async () => {
    try {
      setLoading(true);
      const response = await printJobApi.getAll({
        page: currentPage,
        size: 20,
        status: statusFilter || undefined,
      });
      setJobs(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setError(null);
    } catch (err) {
      console.error("Error fetching print jobs:", err);
      setError("Failed to load print jobs");
    } finally {
      setLoading(false);
    }
  };

  const fetchJobDetails = async (jobId) => {
    if (jobItems[jobId]) {
      setExpandedJob(expandedJob === jobId ? null : jobId);
      return;
    }

    try {
      const response = await printJobApi.getById(jobId, true);
      setJobItems({ ...jobItems, [jobId]: response.data.items || [] });
      setExpandedJob(jobId);
    } catch (err) {
      console.error("Error fetching job details:", err);
      alert("Failed to load job details");
    }
  };

  const handleCancel = async (id) => {
    if (!confirm("Are you sure you want to cancel this print job?")) return;

    try {
      await printJobApi.cancel(id);
      fetchJobs();
    } catch (err) {
      console.error("Error cancelling job:", err);
      alert("Failed to cancel job");
    }
  };

  const handleRetry = async (id) => {
    try {
      await printJobApi.retry(id);
      fetchJobs();
    } catch (err) {
      console.error("Error retrying job:", err);
      alert("Failed to retry job");
    }
  };

  // Open print modal
  const openPrintModal = async (jobId) => {
    setSelectedJobId(jobId);
    setShowPrintModal(true);

    // Load saved printer settings from localStorage
    const savedMethod = localStorage.getItem("printer-method") || "NETWORK";
    const savedAddress = localStorage.getItem("printer-address") || "";
    setPrintMethod(savedMethod);
    setPrinterAddress(savedAddress);

    // Fetch available USB/Spooler printers
    try {
      const response = await printerApi.getAvailablePrinters();
      setAvailablePrinters(response.data.printers || []);
    } catch (err) {
      console.error("Error fetching printers:", err);
    }
  };

  // Test printer connection
  const handleTestConnection = async () => {
    if (!printerAddress.trim()) {
      alert("Please enter printer address");
      return;
    }

    setTestingConnection(true);
    try {
      const response = await printerApi.testConnection({
        printMethod,
        printerAddress,
      });
      alert(response.data.success ? "Connection OK!" : "Connection failed: " + response.data.message);
    } catch (err) {
      alert("Connection failed: " + (err.response?.data?.message || err.message));
    } finally {
      setTestingConnection(false);
    }
  };

  // Print job
  const handlePrint = async () => {
    if (!printerAddress.trim()) {
      alert("Please enter printer address");
      return;
    }

    // Save settings
    localStorage.setItem("printer-method", printMethod);
    localStorage.setItem("printer-address", printerAddress);

    setPrinting(true);
    try {
      const response = await printerApi.printJob(selectedJobId, {
        printMethod,
        printerAddress,
      });

      if (response.data.success) {
        alert(`Print job sent! ${response.data.totalLabels} labels sent to printer.`);
        setShowPrintModal(false);
        fetchJobs();
      } else {
        alert("Print failed: " + response.data.message);
      }
    } catch (err) {
      alert("Print failed: " + (err.response?.data?.message || err.message));
    } finally {
      setPrinting(false);
    }
  };

  // Print test label
  const handlePrintTestLabel = async () => {
    if (!printerAddress.trim()) {
      alert("Please enter printer address");
      return;
    }

    setPrinting(true);
    try {
      const response = await printerApi.printTestLabel({
        printMethod,
        printerAddress,
      });

      if (response.data.success) {
        alert("Test label sent to printer!");
      } else {
        alert("Failed: " + response.data.message);
      }
    } catch (err) {
      alert("Failed: " + (err.response?.data?.message || err.message));
    } finally {
      setPrinting(false);
    }
  };

  // Open jewelry label modal
  const openJewelryModal = async () => {
    setShowJewelryModal(true);
    setSelectedProduct(null);
    setProductSearch("");

    // Load saved printer settings - prefer WebUSB if supported
    const savedMethod = localStorage.getItem("printer-method");
    const savedAddress = localStorage.getItem("printer-address") || "";
    const savedIncludeRFID = localStorage.getItem("jewelry-include-rfid") === "true";

    // Default to WebUSB if supported, otherwise use saved or SPOOLER
    if (webUsbSupported && (!savedMethod || savedMethod === "WEBUSB")) {
      setPrintMethod("WEBUSB");
    } else {
      setPrintMethod(savedMethod || "SPOOLER");
    }
    setPrinterAddress(savedAddress);
    setJewelryIncludeRFID(savedIncludeRFID);

    // Fetch products (always needed) and printers (only for non-WebUSB)
    try {
      const productsRes = await getAllProductsWithWorkflow({ size: 100 });
      const productList = productsRes.data?.content || productsRes.content || productsRes || [];
      setProducts(Array.isArray(productList) ? productList : []);

      // Only fetch printers if not using WebUSB (avoid backend dependency)
      if (!webUsbSupported) {
        const printersRes = await printerApi.getAvailablePrinters();
        setAvailablePrinters(printersRes.data.printers || []);
      }
    } catch (err) {
      console.error("Error fetching data:", err);
    }
  };

  // Handle product selection
  const handleProductSelect = (product) => {
    setSelectedProduct(product);
    setJewelryProductName(product.itemName || product.name || "");
    setJewelryPrice(product.price ? Number(product.price).toLocaleString("vi-VN") : "0");
    setJewelryBarcode(product.barcode || "");
    setProductSearch("");
  };

  // Print jewelry label - support both backend and WebUSB
  const handlePrintJewelryLabel = async () => {
    // Validate based on print method
    if (printMethod !== "WEBUSB" && !printerAddress.trim()) {
      alert("Vui lòng chọn máy in");
      return;
    }
    if (printMethod === "WEBUSB" && !webUsbConnected) {
      alert("Vui lòng kết nối máy in qua WebUSB trước");
      return;
    }
    if (!jewelryBarcode.trim()) {
      alert("Vui lòng nhập barcode");
      return;
    }

    // Save printer settings
    localStorage.setItem("printer-method", printMethod);
    if (printMethod !== "WEBUSB") {
      localStorage.setItem("printer-address", printerAddress);
    }
    localStorage.setItem("jewelry-include-rfid", jewelryIncludeRFID);

    setPrinting(true);
    try {
      if (printMethod === "WEBUSB") {
        // WebUSB printing: Generate ZPL from backend preview endpoint, then print locally
        const params = new URLSearchParams();
        params.append("price", jewelryPrice);
        params.append("productName", jewelryProductName);
        params.append("barcode", jewelryBarcode);
        params.append("includeRFID", jewelryIncludeRFID);
        if (selectedProduct?.id) params.append("productId", selectedProduct.id);
        if (selectedProduct?.skuCode) params.append("skuCode", selectedProduct.skuCode);

        // Get ZPL from backend preview endpoint
        const zplResponse = await fetch(`/api/v1/admin/printer/preview-jewelry-label?${params}`, {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
          },
        });

        if (!zplResponse.ok) {
          throw new Error("Không thể tạo ZPL từ server");
        }

        const zplData = await zplResponse.json();
        const zpl = zplData.zpl;

        // Print via WebUSB
        const result = await printWebUsb(zpl);
        if (result.success) {
          const rfidMsg = jewelryIncludeRFID && zplData.epc ? ` (EPC: ${zplData.epc})` : "";
          alert(`✅ In thành công qua WebUSB!${rfidMsg}`);
          setShowJewelryModal(false);
        } else {
          alert("❌ Lỗi in WebUSB: " + result.error);
        }
      } else {
        // Backend printing (NETWORK/SPOOLER)
        const response = await printerApi.printJewelryLabel(
          jewelryPrice,
          jewelryProductName,
          jewelryBarcode,
          { printMethod, printerAddress },
          jewelryIncludeRFID,
          selectedProduct?.id || null,
          selectedProduct?.skuCode || null
        );

        if (response.data.success) {
          const rfidMsg = jewelryIncludeRFID ? ` (EPC: ${response.data.epc})` : "";
          alert(`✅ Jewelry label sent to printer!${rfidMsg}`);
          setShowJewelryModal(false);
        } else {
          alert("❌ Failed: " + response.data.message);
        }
      }
    } catch (err) {
      alert("❌ Failed: " + (err.response?.data?.message || err.message));
    } finally {
      setPrinting(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleString("vi-VN", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  if (loading && jobs.length === 0) {
    return (
      <div className="pod-page">
        <div className="pod-loading">
          <div className="pod-loading-spinner" />
          <p>Loading print jobs...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="pod-page">
      <div className="pod-page-header">
        <h1 className="pod-page-title">Print Jobs</h1>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button
            className="pod-btn pod-btn-secondary"
            onClick={openJewelryModal}
            style={{ background: "#fef3c7", borderColor: "#f59e0b", color: "#92400e" }}
          >
            💎 In Jewelry Label
          </button>
          <button
            className="pod-btn pod-btn-primary"
            onClick={() => navigate(ROUTES.PRINT_JOB_CREATE)}
          >
            + New Print Job
          </button>
        </div>
      </div>

      {error && (
        <div className="pod-card" style={{ background: "#fef2f2", borderColor: "#fecaca" }}>
          <p style={{ color: "#dc2626" }}>{error}</p>
        </div>
      )}

      {/* Filters */}
      <div className="pod-card" style={{ marginBottom: "1rem" }}>
        <div style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
          <select
            className="pod-input"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(0);
            }}
            style={{ width: "180px" }}
          >
            <option value="">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="PRINTING">Printing</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button className="pod-btn pod-btn-secondary" onClick={fetchJobs}>
            Refresh
          </button>
        </div>
      </div>

      {/* Jobs Table */}
      <div className="pod-card" style={{ padding: 0, overflow: "hidden" }}>
        <table className="pod-table">
          <thead>
            <tr>
              <th style={{ width: "50px" }}></th>
              <th>Job ID</th>
              <th>Status</th>
              <th>Progress</th>
              <th>Printer</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <React.Fragment key={job.id}>
                <tr>
                  <td>
                    <button
                      className="pod-btn"
                      style={{ padding: "0.25rem 0.5rem", fontSize: "0.75rem" }}
                      onClick={() => fetchJobDetails(job.id)}
                    >
                      {expandedJob === job.id ? "▼" : "▶"}
                    </button>
                  </td>
                  <td>
                    <code style={{ fontSize: "0.75rem" }}>{job.id}</code>
                  </td>
                  <td>
                    <span
                      style={{
                        display: "inline-block",
                        padding: "0.25rem 0.5rem",
                        borderRadius: "4px",
                        fontSize: "0.75rem",
                        background: STATUS_COLORS[job.status]?.bg || "#f3f4f6",
                        color: STATUS_COLORS[job.status]?.color || "#374151",
                      }}
                    >
                      {job.status}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                      <div
                        style={{
                          flex: 1,
                          height: "8px",
                          background: "#e5e7eb",
                          borderRadius: "4px",
                          overflow: "hidden",
                        }}
                      >
                        <div
                          style={{
                            width: `${job.totalLabels > 0 ? (job.printedLabels / job.totalLabels) * 100 : 0}%`,
                            height: "100%",
                            background: job.failedLabels > 0 ? "#ef4444" : "#22c55e",
                          }}
                        />
                      </div>
                      <span style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                        {job.printedLabels}/{job.totalLabels}
                        {job.failedLabels > 0 && (
                          <span style={{ color: "#ef4444" }}> ({job.failedLabels} failed)</span>
                        )}
                      </span>
                    </div>
                  </td>
                  <td style={{ fontSize: "0.875rem" }}>{job.printerName || "-"}</td>
                  <td style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                    {formatDate(job.createdAt)}
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: "0.25rem" }}>
                      {(job.status === "PENDING" || job.status === "FAILED") && (
                        <button
                          className="pod-btn pod-btn-primary"
                          style={{ padding: "0.25rem 0.5rem", fontSize: "0.75rem" }}
                          onClick={() => openPrintModal(job.id)}
                        >
                          🖨️ Print
                        </button>
                      )}
                      {job.status === "PENDING" && (
                        <button
                          className="pod-btn"
                          style={{ padding: "0.25rem 0.5rem", fontSize: "0.75rem", color: "#dc2626" }}
                          onClick={() => handleCancel(job.id)}
                        >
                          Cancel
                        </button>
                      )}
                      {job.status === "FAILED" && (
                        <button
                          className="pod-btn pod-btn-secondary"
                          style={{ padding: "0.25rem 0.5rem", fontSize: "0.75rem" }}
                          onClick={() => handleRetry(job.id)}
                        >
                          Retry
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
                {expandedJob === job.id && jobItems[job.id] && (
                  <tr>
                    <td colSpan={7} style={{ background: "#f9fafb", padding: "1rem" }}>
                      <h4 style={{ marginBottom: "0.5rem", fontSize: "0.875rem" }}>
                        Print Items ({jobItems[job.id].length})
                      </h4>
                      <div style={{ maxHeight: "200px", overflow: "auto" }}>
                        <table style={{ width: "100%", fontSize: "0.75rem" }}>
                          <thead>
                            <tr>
                              <th>Product</th>
                              <th>SKU</th>
                              <th>EPC</th>
                              <th>Status</th>
                              <th>Error</th>
                            </tr>
                          </thead>
                          <tbody>
                            {jobItems[job.id].map((item) => (
                              <tr key={item.id}>
                                <td>{item.productName || item.productId}</td>
                                <td><code>{item.sku}</code></td>
                                <td><code>{item.epc || "-"}</code></td>
                                <td>
                                  <span
                                    style={{
                                      padding: "0.125rem 0.25rem",
                                      borderRadius: "2px",
                                      fontSize: "0.625rem",
                                      background: item.status === "PRINTED" ? "#dcfce7" : item.status === "FAILED" ? "#fee2e2" : "#f3f4f6",
                                      color: item.status === "PRINTED" ? "#166534" : item.status === "FAILED" ? "#dc2626" : "#374151",
                                    }}
                                  >
                                    {item.status}
                                  </span>
                                </td>
                                <td style={{ color: "#dc2626" }}>{item.errorMessage || "-"}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>

      {jobs.length === 0 && !loading && (
        <div className="pod-card" style={{ textAlign: "center", padding: "2rem" }}>
          <p style={{ color: "#6b7280" }}>No print jobs found</p>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: "flex", justifyContent: "center", gap: "0.5rem", marginTop: "1rem" }}>
          <button
            className="pod-btn pod-btn-secondary"
            disabled={currentPage === 0}
            onClick={() => setCurrentPage(currentPage - 1)}
          >
            Previous
          </button>
          <span style={{ padding: "0.5rem 1rem" }}>
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            className="pod-btn pod-btn-secondary"
            disabled={currentPage >= totalPages - 1}
            onClick={() => setCurrentPage(currentPage + 1)}
          >
            Next
          </button>
        </div>
      )}

      {/* Print Modal */}
      {showPrintModal && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          onClick={() => setShowPrintModal(false)}
        >
          <div
            className="pod-card"
            style={{ width: "450px", maxWidth: "90vw" }}
            onClick={(e) => e.stopPropagation()}
          >
            <h2 style={{ marginBottom: "1.5rem" }}>🖨️ Print Job</h2>

            {/* Print Method */}
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Print Method
              </label>
              <select
                className="pod-input"
                value={printMethod}
                onChange={(e) => {
                  setPrintMethod(e.target.value);
                  setPrinterAddress("");
                }}
                style={{ width: "100%" }}
              >
                <option value="NETWORK">Network (LAN/WiFi) - TCP/IP</option>
                <option value="USB">USB - Windows Spooler</option>
                <option value="SPOOLER">Print Spooler</option>
              </select>
            </div>

            {/* Printer Address */}
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                {printMethod === "NETWORK" ? "Printer IP Address" : "Printer Name"}
              </label>
              {printMethod === "NETWORK" ? (
                <input
                  type="text"
                  className="pod-input"
                  placeholder="e.g., 192.168.1.100 or 192.168.1.100:9100"
                  value={printerAddress}
                  onChange={(e) => setPrinterAddress(e.target.value)}
                  style={{ width: "100%" }}
                />
              ) : (
                <>
                  <select
                    className="pod-input"
                    value={printerAddress}
                    onChange={(e) => setPrinterAddress(e.target.value)}
                    style={{ width: "100%", marginBottom: "0.5rem" }}
                  >
                    <option value="">-- Select Printer --</option>
                    {availablePrinters.map((printer) => (
                      <option key={printer} value={printer}>
                        {printer}
                      </option>
                    ))}
                  </select>
                  <input
                    type="text"
                    className="pod-input"
                    placeholder="Or enter printer name manually"
                    value={printerAddress}
                    onChange={(e) => setPrinterAddress(e.target.value)}
                    style={{ width: "100%" }}
                  />
                </>
              )}
            </div>

            {/* Help text */}
            <div style={{ marginBottom: "1.5rem", padding: "0.75rem", background: "#f3f4f6", borderRadius: "4px", fontSize: "0.75rem", color: "#6b7280" }}>
              {printMethod === "NETWORK" ? (
                <>
                  <strong>Network Print:</strong> Enter the IP address of your Zebra/TSC printer.
                  Default port is 9100. Make sure the printer is connected to the same network.
                </>
              ) : (
                <>
                  <strong>USB/Spooler Print:</strong> Select or enter the printer name as shown in
                  Windows Devices and Printers. The printer driver must be installed.
                </>
              )}
            </div>

            {/* Action buttons */}
            <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
              <button
                className="pod-btn pod-btn-secondary"
                onClick={handleTestConnection}
                disabled={testingConnection || !printerAddress}
              >
                {testingConnection ? "Testing..." : "Test Connection"}
              </button>
              <button
                className="pod-btn pod-btn-secondary"
                onClick={handlePrintTestLabel}
                disabled={printing || !printerAddress}
              >
                Print Test Label
              </button>
              <div style={{ flex: 1 }} />
              <button
                className="pod-btn"
                onClick={() => setShowPrintModal(false)}
              >
                Cancel
              </button>
              <button
                className="pod-btn pod-btn-primary"
                onClick={handlePrint}
                disabled={printing || !printerAddress}
              >
                {printing ? "Printing..." : "🖨️ Print Now"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Jewelry Label Modal */}
      {showJewelryModal && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          onClick={() => setShowJewelryModal(false)}
        >
          <div
            className="pod-card"
            style={{ width: "500px", maxWidth: "90vw" }}
            onClick={(e) => e.stopPropagation()}
          >
            <h2 style={{ marginBottom: "1.5rem" }}>💎 In Jewelry Label (30x70mm)</h2>

            {/* Label Data */}
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Giá (Price)
              </label>
              <input
                type="text"
                className="pod-input"
                placeholder="VD: 15.000.000"
                value={jewelryPrice}
                onChange={(e) => setJewelryPrice(e.target.value)}
                style={{ width: "100%" }}
              />
            </div>

            <div style={{ marginBottom: "1rem", position: "relative" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Chọn sản phẩm ({products.length} sản phẩm)
              </label>
              <div style={{ position: "relative" }}>
                <input
                  type="text"
                  className="pod-input"
                  placeholder="Click để chọn hoặc gõ để tìm kiếm..."
                  value={productSearch}
                  onChange={(e) => setProductSearch(e.target.value)}
                  onFocus={() => setShowProductDropdown(true)}
                  onBlur={() => setTimeout(() => setShowProductDropdown(false), 200)}
                  style={{ width: "100%", paddingRight: "2rem" }}
                />
                <span style={{
                  position: "absolute",
                  right: "0.75rem",
                  top: "50%",
                  transform: `translateY(-50%) rotate(${showProductDropdown ? '180deg' : '0deg'})`,
                  transition: "transform 0.2s",
                  pointerEvents: "none",
                  color: "#6b7280"
                }}>
                  ▼
                </span>
              </div>
              {showProductDropdown && products.length > 0 && (
                <div style={{
                  position: "absolute",
                  top: "100%",
                  left: 0,
                  right: 0,
                  maxHeight: "200px",
                  overflowY: "auto",
                  background: "white",
                  border: "1px solid #e5e7eb",
                  borderRadius: "4px",
                  zIndex: 10,
                  boxShadow: "0 4px 6px rgba(0,0,0,0.1)"
                }}>
                  {products
                    .filter(p =>
                      !productSearch ||
                      (p.itemName || p.name || "").toLowerCase().includes(productSearch.toLowerCase()) ||
                      (p.skuCode || "").toLowerCase().includes(productSearch.toLowerCase())
                    )
                    .slice(0, 15)
                    .map(product => (
                      <div
                        key={product.id}
                        onClick={() => {
                          handleProductSelect(product);
                          setShowProductDropdown(false);
                        }}
                        style={{
                          padding: "0.5rem 0.75rem",
                          cursor: "pointer",
                          borderBottom: "1px solid #f3f4f6",
                          fontSize: "0.875rem"
                        }}
                        onMouseEnter={(e) => e.target.style.background = "#f3f4f6"}
                        onMouseLeave={(e) => e.target.style.background = "white"}
                      >
                        <strong>{product.itemName || product.name}</strong>
                        <br />
                        <span style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                          SKU: {product.skuCode} | Barcode: {product.barcode || "N/A"}
                        </span>
                      </div>
                    ))}
                  {products.filter(p =>
                    !productSearch ||
                    (p.itemName || p.name || "").toLowerCase().includes(productSearch.toLowerCase()) ||
                    (p.skuCode || "").toLowerCase().includes(productSearch.toLowerCase())
                  ).length === 0 && (
                    <div style={{ padding: "0.5rem 0.75rem", color: "#6b7280", fontSize: "0.875rem" }}>
                      Không tìm thấy sản phẩm
                    </div>
                  )}
                </div>
              )}
              {selectedProduct && (
                <div style={{
                  marginTop: "0.5rem",
                  padding: "0.5rem",
                  background: "#eff6ff",
                  borderRadius: "4px",
                  fontSize: "0.875rem"
                }}>
                  ✅ Đã chọn: <strong>{selectedProduct.itemName || selectedProduct.name}</strong>
                </div>
              )}
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Tên sản phẩm (hiển thị trên nhãn)
              </label>
              <input
                type="text"
                className="pod-input"
                placeholder="VD: Nhẫn Kim Cương"
                value={jewelryProductName}
                onChange={(e) => setJewelryProductName(e.target.value)}
                style={{ width: "100%" }}
              />
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Barcode (EAN-13) <span style={{ color: "#dc2626" }}>*</span>
              </label>
              <input
                type="text"
                className="pod-input"
                placeholder="VD: 123456789012 (12-13 số)"
                value={jewelryBarcode}
                onChange={(e) => setJewelryBarcode(e.target.value)}
                style={{ width: "100%" }}
              />
            </div>

            {/* RFID Option */}
            <div style={{ marginBottom: "1rem" }}>
              <label style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                cursor: "pointer",
                padding: "0.75rem",
                background: jewelryIncludeRFID ? "#dbeafe" : "#f3f4f6",
                borderRadius: "6px",
                border: jewelryIncludeRFID ? "1px solid #3b82f6" : "1px solid #e5e7eb",
                transition: "all 0.2s"
              }}>
                <input
                  type="checkbox"
                  checked={jewelryIncludeRFID}
                  onChange={(e) => setJewelryIncludeRFID(e.target.checked)}
                  style={{ width: "18px", height: "18px", cursor: "pointer" }}
                />
                <div>
                  <span style={{ fontWeight: "500" }}>📡 Ghi thông tin vào chip RFID</span>
                  <p style={{ margin: 0, fontSize: "0.75rem", color: "#6b7280" }}>
                    Mã EPC sẽ được tự động tạo từ thông tin sản phẩm
                  </p>
                </div>
              </label>
            </div>

            <hr style={{ margin: "1.5rem 0", border: "none", borderTop: "1px solid #e5e7eb" }} />

            {/* Printer Settings */}
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Phương thức in
              </label>
              <select
                className="pod-input"
                value={printMethod}
                onChange={async (e) => {
                  const newMethod = e.target.value;
                  setPrintMethod(newMethod);
                  setPrinterAddress("");

                  // Fetch printers when switching to SPOOLER
                  if (newMethod === "SPOOLER" && availablePrinters.length === 0) {
                    try {
                      const printersRes = await printerApi.getAvailablePrinters();
                      setAvailablePrinters(printersRes.data.printers || []);
                    } catch (err) {
                      console.error("Error fetching printers:", err);
                    }
                  }
                }}
                style={{ width: "100%" }}
              >
                {webUsbSupported && (
                  <option value="WEBUSB">🔌 WebUSB - Kết nối trực tiếp (Recommended)</option>
                )}
                <option value="SPOOLER">USB - Windows Spooler</option>
                <option value="NETWORK">Network (LAN/WiFi)</option>
              </select>
            </div>

            {/* WebUSB Printer Connection */}
            {printMethod === "WEBUSB" && (
              <div style={{ marginBottom: "1rem" }}>
                <div style={{
                  padding: "1rem",
                  background: webUsbConnected ? "#dcfce7" : "#f3f4f6",
                  borderRadius: "8px",
                  border: webUsbConnected ? "1px solid #86efac" : "1px solid #e5e7eb"
                }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "0.75rem" }}>
                    <div style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "50%",
                      background: webUsbConnected ? "#22c55e" : "#9ca3af"
                    }} />
                    <span style={{ fontWeight: "500" }}>
                      {webUsbConnected
                        ? `✅ Đã kết nối: ${webUsbDeviceInfo?.productName || "Máy in"}`
                        : "Chưa kết nối máy in"}
                    </span>
                  </div>

                  {webUsbError && (
                    <div style={{
                      padding: "0.5rem",
                      background: "#fef2f2",
                      borderRadius: "4px",
                      color: "#dc2626",
                      fontSize: "0.875rem",
                      marginBottom: "0.75rem"
                    }}>
                      ⚠️ {webUsbError}
                    </div>
                  )}

                  <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                    {!webUsbConnected ? (
                      <>
                        <button
                          type="button"
                          className="pod-btn pod-btn-primary"
                          onClick={() => connectWebUsb(false)}
                          style={{ flex: 1 }}
                        >
                          🔌 Kết nối máy in
                        </button>
                        <button
                          type="button"
                          className="pod-btn pod-btn-secondary"
                          onClick={() => connectWebUsb(true)}
                          title="Hiện tất cả thiết bị USB (dùng khi máy in không hiện trong danh sách)"
                        >
                          📋 Tất cả USB
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className="pod-btn pod-btn-secondary"
                        onClick={disconnectWebUsb}
                      >
                        Ngắt kết nối
                      </button>
                    )}
                  </div>

                  <p style={{ margin: "0.75rem 0 0", fontSize: "0.75rem", color: "#6b7280" }}>
                    WebUSB cho phép in trực tiếp từ trình duyệt. Nếu máy in không hiện, bấm "Tất cả USB".
                    Hỗ trợ: Zebra, TSC, HPRT.
                  </p>
                </div>
              </div>
            )}

            {/* Traditional printer selection (NETWORK/SPOOLER) */}
            {printMethod !== "WEBUSB" && (
              <div style={{ marginBottom: "1rem" }}>
                <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                  {printMethod === "NETWORK" ? "IP máy in" : "Chọn máy in"}
                </label>
                {printMethod === "NETWORK" ? (
                  <input
                    type="text"
                    className="pod-input"
                    placeholder="VD: 192.168.1.100"
                    value={printerAddress}
                    onChange={(e) => setPrinterAddress(e.target.value)}
                    style={{ width: "100%" }}
                  />
                ) : (
                  <select
                    className="pod-input"
                    value={printerAddress}
                    onChange={(e) => setPrinterAddress(e.target.value)}
                    style={{ width: "100%" }}
                  >
                    <option value="">-- Chọn máy in --</option>
                    {availablePrinters.map((printer) => (
                      <option key={printer} value={printer}>
                        {printer}
                      </option>
                    ))}
                  </select>
                )}
              </div>
            )}

            {/* Data Preview */}
            <div style={{
              marginBottom: "1rem",
              padding: "0.75rem",
              background: jewelryIncludeRFID ? "#eff6ff" : "#f0fdf4",
              borderRadius: "4px",
              fontSize: "0.875rem",
              border: jewelryIncludeRFID ? "1px solid #93c5fd" : "1px solid #86efac"
            }}>
              <strong>Dữ liệu sẽ in:</strong><br />
              💰 {jewelryPrice || "(trống)"} VND<br />
              🏷️ {jewelryProductName || "(trống)"}<br />
              📊 {jewelryBarcode || "(nhập barcode)"}<br />
              {jewelryIncludeRFID && (
                <>
                  📡 <strong style={{ color: "#2563eb" }}>RFID: Có</strong>
                  {selectedProduct && (
                    <span style={{ color: "#6b7280" }}> (ID: {selectedProduct.id})</span>
                  )}
                </>
              )}
            </div>

            {/* Actions */}
            <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
              <button
                className="pod-btn"
                onClick={() => setShowJewelryModal(false)}
              >
                Hủy
              </button>
              <button
                className="pod-btn pod-btn-primary"
                onClick={handlePrintJewelryLabel}
                disabled={
                  printing ||
                  webUsbPrinting ||
                  !jewelryBarcode ||
                  (printMethod === "WEBUSB" ? !webUsbConnected : !printerAddress)
                }
                style={{ background: "#f59e0b", borderColor: "#f59e0b" }}
              >
                {(printing || webUsbPrinting) ? "Đang in..." : "🖨️ In Label"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
