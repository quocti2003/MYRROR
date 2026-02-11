import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { printJobApi, labelTemplateApi, labelGenerationApi } from "@/services/labelApi";
import { ROUTES } from "@/constants/routes";
import "@/components/pod-admin/PodAdminLayout.css";

// Product search API using product-ops endpoint
const searchProducts = async (query) => {
  try {
    const token = localStorage.getItem("accessToken");
    const response = await fetch(
      `/api/product-ops/products?search=${encodeURIComponent(query)}`,
      {
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      }
    );
    if (!response.ok) throw new Error("Search failed");
    const data = await response.json();
    // Map response to expected format
    return data.map(p => ({
      id: p.id,
      itemName: p.itemName || p.skuCode,
      skuCode: p.skuCode,
      barcode: p.barcode
    }));
  } catch (err) {
    console.error("Error searching products:", err);
    return [];
  }
};

export default function PrintJobCreate() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState([]);
  const [options, setOptions] = useState({
    includeRFID: true,
    copies: 1,
    darkness: 15,
    speed: 4,
  });
  const [printerName, setPrinterName] = useState("");
  const [printMethod, setPrintMethod] = useState("NETWORK");

  // Preview state
  const [previewMode, setPreviewMode] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [generatingPreview, setGeneratingPreview] = useState(false);

  useEffect(() => {
    fetchTemplates();
  }, []);

  const fetchTemplates = async () => {
    try {
      const response = await labelTemplateApi.getAll({ status: "ACTIVE", size: 100 });
      setTemplates(response.data.content || []);
      // Auto-select default template
      const defaultTemplate = response.data.content?.find((t) => t.isDefault);
      if (defaultTemplate) {
        setSelectedTemplate(defaultTemplate.id);
      }
    } catch (err) {
      console.error("Error fetching templates:", err);
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    try {
      const results = await searchProducts(searchQuery);
      setSearchResults(results);
    } catch (err) {
      console.error("Error searching:", err);
    }
  };

  const addProduct = (product) => {
    if (selectedProducts.find((p) => p.id === product.id)) return;
    setSelectedProducts([...selectedProducts, product]);
    setSearchResults([]);
    setSearchQuery("");
  };

  const removeProduct = (productId) => {
    setSelectedProducts(selectedProducts.filter((p) => p.id !== productId));
  };

  const generatePreview = async () => {
    if (!selectedTemplate || selectedProducts.length === 0) {
      alert("Please select a template and at least one product");
      return;
    }

    try {
      setGeneratingPreview(true);
      const response = await labelGenerationApi.generateZPL({
        templateId: selectedTemplate,
        productIds: selectedProducts.map((p) => p.id),
        options: {
          includeRFID: options.includeRFID,
        },
      });
      setPreviewData(response.data);
      setPreviewMode(true);
    } catch (err) {
      console.error("Error generating preview:", err);
      alert("Failed to generate preview");
    } finally {
      setGeneratingPreview(false);
    }
  };

  const handleSubmit = async () => {
    if (!selectedTemplate || selectedProducts.length === 0) {
      alert("Please select a template and at least one product");
      return;
    }

    try {
      setLoading(true);
      await printJobApi.create({
        templateId: selectedTemplate,
        productIds: selectedProducts.map((p) => p.id),
        printerName: printerName || null,
        printMethod: printMethod,
        options: {
          includeRFID: options.includeRFID,
          copies: options.copies,
          darkness: options.darkness,
          speed: options.speed,
        },
      });
      alert("Print job created successfully!");
      navigate(ROUTES.PRINT_JOBS);
    } catch (err) {
      console.error("Error creating print job:", err);
      alert("Failed to create print job: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="pod-page">
      <div className="pod-page-header">
        <h1 className="pod-page-title">Create Print Job</h1>
        <button className="pod-btn" onClick={() => navigate(ROUTES.PRINT_JOBS)}>
          Cancel
        </button>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 400px", gap: "1.5rem" }}>
        {/* Main Form */}
        <div>
          {/* Template Selection */}
          <div className="pod-card" style={{ marginBottom: "1rem" }}>
            <h3 style={{ marginBottom: "1rem" }}>1. Select Template</h3>
            <select
              className="pod-input"
              value={selectedTemplate}
              onChange={(e) => setSelectedTemplate(e.target.value)}
              style={{ width: "100%" }}
            >
              <option value="">-- Select a template --</option>
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name} ({template.labelWidth}x{template.labelHeight}mm)
                  {template.isDefault ? " (Default)" : ""}
                </option>
              ))}
            </select>
          </div>

          {/* Product Selection */}
          <div className="pod-card" style={{ marginBottom: "1rem" }}>
            <h3 style={{ marginBottom: "1rem" }}>2. Select Products</h3>

            {/* Search */}
            <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
              <input
                type="text"
                className="pod-input"
                placeholder="Search products by name or SKU..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                style={{ flex: 1 }}
              />
              <button className="pod-btn pod-btn-secondary" onClick={handleSearch}>
                Search
              </button>
            </div>

            {/* Search Results */}
            {searchResults.length > 0 && (
              <div
                style={{
                  border: "1px solid #e5e7eb",
                  borderRadius: "4px",
                  maxHeight: "200px",
                  overflow: "auto",
                  marginBottom: "1rem",
                }}
              >
                {searchResults.map((product) => (
                  <div
                    key={product.id}
                    style={{
                      padding: "0.5rem",
                      borderBottom: "1px solid #e5e7eb",
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      cursor: "pointer",
                    }}
                    onClick={() => addProduct(product)}
                  >
                    <div>
                      <div style={{ fontWeight: "500" }}>{product.itemName || product.name}</div>
                      <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                        SKU: {product.skuCode || product.sku}
                      </div>
                    </div>
                    <button className="pod-btn pod-btn-primary" style={{ padding: "0.25rem 0.5rem" }}>
                      + Add
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Selected Products */}
            <div>
              <h4 style={{ fontSize: "0.875rem", color: "#6b7280", marginBottom: "0.5rem" }}>
                Selected Products ({selectedProducts.length})
              </h4>
              {selectedProducts.length === 0 ? (
                <p style={{ color: "#9ca3af", fontSize: "0.875rem" }}>
                  No products selected. Search and add products above.
                </p>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                  {selectedProducts.map((product) => (
                    <div
                      key={product.id}
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        padding: "0.5rem",
                        background: "#f9fafb",
                        borderRadius: "4px",
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: "500" }}>{product.itemName || product.name}</div>
                        <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                          SKU: {product.skuCode || product.sku}
                        </div>
                      </div>
                      <button
                        className="pod-btn"
                        style={{ color: "#dc2626", padding: "0.25rem 0.5rem" }}
                        onClick={() => removeProduct(product.id)}
                      >
                        Remove
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Print Options */}
          <div className="pod-card">
            <h3 style={{ marginBottom: "1rem" }}>3. Print Options</h3>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
              <label style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                <span style={{ fontSize: "0.875rem", color: "#374151" }}>Copies per label</span>
                <input
                  type="number"
                  className="pod-input"
                  min={1}
                  max={100}
                  value={options.copies}
                  onChange={(e) => setOptions({ ...options, copies: Number(e.target.value) })}
                />
              </label>

              <label style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                <span style={{ fontSize: "0.875rem", color: "#374151" }}>Print Method</span>
                <select
                  className="pod-input"
                  value={printMethod}
                  onChange={(e) => setPrintMethod(e.target.value)}
                >
                  <option value="NETWORK">Network (TCP/IP)</option>
                  <option value="USB">USB</option>
                  <option value="SPOOLER">Print Spooler</option>
                </select>
              </label>

              <label style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                <span style={{ fontSize: "0.875rem", color: "#374151" }}>Printer Name/IP</span>
                <input
                  type="text"
                  className="pod-input"
                  placeholder="e.g., 192.168.1.100 or PrinterName"
                  value={printerName}
                  onChange={(e) => setPrinterName(e.target.value)}
                />
              </label>

              <label style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                <span style={{ fontSize: "0.875rem", color: "#374151" }}>Darkness (1-30)</span>
                <input
                  type="number"
                  className="pod-input"
                  min={1}
                  max={30}
                  value={options.darkness}
                  onChange={(e) => setOptions({ ...options, darkness: Number(e.target.value) })}
                />
              </label>
            </div>

            <div style={{ marginTop: "1rem" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                <input
                  type="checkbox"
                  checked={options.includeRFID}
                  onChange={(e) => setOptions({ ...options, includeRFID: e.target.checked })}
                />
                <span style={{ fontSize: "0.875rem", color: "#374151" }}>
                  Include RFID encoding (generate EPC tags)
                </span>
              </label>
            </div>
          </div>
        </div>

        {/* Preview Panel */}
        <div className="pod-card">
          <h3 style={{ marginBottom: "1rem" }}>Preview</h3>

          {previewMode && previewData ? (
            <div>
              <div style={{ marginBottom: "1rem", fontSize: "0.875rem", color: "#6b7280" }}>
                Total labels: {previewData.totalLabels}
              </div>

              {previewData.labels?.slice(0, 3).map((label, index) => (
                <div
                  key={index}
                  style={{
                    padding: "0.75rem",
                    background: "#f9fafb",
                    borderRadius: "4px",
                    marginBottom: "0.5rem",
                  }}
                >
                  <div style={{ fontWeight: "500", marginBottom: "0.25rem" }}>
                    {label.productName}
                  </div>
                  <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                    SKU: {label.sku}
                  </div>
                  {label.epc && (
                    <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                      EPC: <code>{label.epc}</code>
                    </div>
                  )}
                </div>
              ))}

              {previewData.labels?.length > 3 && (
                <p style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                  ...and {previewData.labels.length - 3} more labels
                </p>
              )}

              <button
                className="pod-btn pod-btn-secondary"
                style={{ width: "100%", marginTop: "1rem" }}
                onClick={() => setPreviewMode(false)}
              >
                Close Preview
              </button>
            </div>
          ) : (
            <div style={{ textAlign: "center", padding: "2rem", color: "#9ca3af" }}>
              <p>Select template and products, then click "Preview" to see generated labels</p>
              <button
                className="pod-btn pod-btn-secondary"
                style={{ marginTop: "1rem" }}
                onClick={generatePreview}
                disabled={generatingPreview || !selectedTemplate || selectedProducts.length === 0}
              >
                {generatingPreview ? "Generating..." : "Generate Preview"}
              </button>
            </div>
          )}

          <hr style={{ margin: "1.5rem 0", border: "none", borderTop: "1px solid #e5e7eb" }} />

          {/* Summary */}
          <div style={{ fontSize: "0.875rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "0.5rem" }}>
              <span style={{ color: "#6b7280" }}>Template:</span>
              <span>{templates.find((t) => t.id === selectedTemplate)?.name || "-"}</span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "0.5rem" }}>
              <span style={{ color: "#6b7280" }}>Products:</span>
              <span>{selectedProducts.length}</span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "0.5rem" }}>
              <span style={{ color: "#6b7280" }}>Copies:</span>
              <span>{options.copies}</span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "0.5rem" }}>
              <span style={{ color: "#6b7280" }}>Total Labels:</span>
              <span style={{ fontWeight: "600" }}>{selectedProducts.length * options.copies}</span>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span style={{ color: "#6b7280" }}>RFID:</span>
              <span>{options.includeRFID ? "Yes" : "No"}</span>
            </div>
          </div>

          <button
            className="pod-btn pod-btn-primary"
            style={{ width: "100%", marginTop: "1.5rem" }}
            onClick={handleSubmit}
            disabled={loading || !selectedTemplate || selectedProducts.length === 0}
          >
            {loading ? "Creating..." : "Create Print Job"}
          </button>
        </div>
      </div>
    </div>
  );
}
