import React, { useState } from "react";
import { skuCodesAPI } from "@/services/api";
import { useDropdownOptions } from "@/hooks/useDropdownOptions";
import CodeGenerationHelpModal from "./CodeGenerationHelpModal";
import "./CodeForms.css";

const PackagingCodeForm = ({ onCodeGenerated }) => {
  const [formData, setFormData] = useState({
    prefix: "",
    material: "",
    size: "",
    color: "",
    type: "",
    finish: "",
    countryOfOrigin: "",
    itemName: "",
    notes: "",
    // Non-serialized inventory fields (packaging uses shared barcode, quantity-based)
    stockQuantity: 0,
    stockLocation: "",
  });

  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showHelp, setShowHelp] = useState(false);
  const [bulkFile, setBulkFile] = useState(null);
  const [bulkLoading, setBulkLoading] = useState(false);
  const [bulkError, setBulkError] = useState(null);
  const [bulkResult, setBulkResult] = useState(null);

  // Fetch dropdown options
  const { options, loading: optionsLoading, error: optionsError } = useDropdownOptions();

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    // Don't uppercase dropdown values (prefix, countryOfOrigin)
    if (name === "prefix" || name === "countryOfOrigin") {
      setFormData((prev) => ({ ...prev, [name]: value }));
    } else {
      setFormData((prev) => ({ ...prev, [name]: value.toUpperCase() }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await skuCodesAPI.generatePackagingSku(formData);
      setResult(response.data);
      if (onCodeGenerated) {
        onCodeGenerated({
          ...response.data,
          type: "packaging",
          inputs: formData,
        });
      }
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to generate SKU. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setFormData({
      prefix: "",
      material: "",
      size: "",
      color: "",
      type: "",
      finish: "",
      countryOfOrigin: "",
      itemName: "",
      notes: "",
      stockQuantity: 0,
      stockLocation: "",
    });
    setResult(null);
    setError(null);
  };

  const handleBulkFileChange = (event) => {
    const file = event.target.files && event.target.files[0];
    setBulkFile(file || null);
    setBulkError(null);
    setBulkResult(null);
  };

  const handleBulkGenerate = async () => {
    if (!bulkFile) {
      setBulkError("Please attach a CSV file with required columns including prefix and country_of_origin.");
      return;
    }

    setBulkLoading(true);
    setBulkError(null);

    try {
      const response = await skuCodesAPI.importPackagingSkus(bulkFile);
      setBulkResult(response.data);
      if (onCodeGenerated) {
        onCodeGenerated();
      }
    } catch (bulkException) {
      setBulkResult(null);
      setBulkError(
        bulkException.response?.data?.message ||
          "Failed to process the CSV file. Please verify the format."
      );
    } finally {
      setBulkLoading(false);
    }
  };

  const helpSections = [
    {
      heading: "Required fields",
      items: [
        "Prefix and Country of Origin are required for all packaging SKUs.",
        "Prefix is selected from pre-configured options (BOX, BAG, TAG, etc.).",
        "Country of Origin options include China (CN), India (IN), Thailand (TH), and Hongkong (HK).",
      ],
    },
    {
      heading: "Optional details are flexible",
      items: [
        "Material, size, color, type, finish, and notes are optional—include only what matters.",
        "Inputs are automatically uppercased and stripped of spaces or special characters.",
        "Common packaging terms are abbreviated (LEATHERETTE → LTHRTTE, MICROFIBER → MF).",
      ],
    },
    {
      heading: "What happens after generation",
      items: [
        "Each product gets a unique barcode that serves as its SKU code.",
        "The descriptive code (e.g., BOX-MF-LG-NVY) is stored for easy identification.",
        "Products are saved to the catalog and ready for MISA export.",
      ],
    },
    {
      heading: "Length management",
      items: [
        "Descriptive codes cannot exceed 30 characters. The generator trims trailing segments if needed.",
        "If trimming is required, you will see the \"Truncated\" warning in the result card.",
      ],
    },
  ];

  if (optionsLoading) {
    return (
      <div className="code-form-container">
        <div className="loading-state">
          <p>Loading dropdown options...</p>
        </div>
      </div>
    );
  }

  if (optionsError) {
    return (
      <div className="code-form-container">
        <div className="error-state">
          <p>{optionsError}</p>
          <button onClick={() => window.location.reload()}>Reload Page</button>
        </div>
      </div>
    );
  }

  return (
    <div className="code-form-container">
      <div className="form-header-actions">
        <button
          type="button"
          className="code-help-button"
          onClick={() => setShowHelp(true)}
        >
          How packaging SKU generation works
        </button>
      </div>

      <form onSubmit={handleSubmit} className="code-form">
        <div className="form-section">
          <h3>Required Fields</h3>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="prefix">
                Prefix <span className="required">*</span>
              </label>
              <select
                id="prefix"
                name="prefix"
                value={formData.prefix}
                onChange={handleInputChange}
                required
              >
                <option value="">Select prefix...</option>
                {options.prefixes.map((opt) => (
                  <option key={opt.id} value={opt.code}>
                    {opt.name} ({opt.code})
                  </option>
                ))}
              </select>
              <span className="hint">Packaging category (e.g., BOX, BAG)</span>
            </div>

            <div className="form-group">
              <label htmlFor="countryOfOrigin">
                Country of Origin <span className="required">*</span>
              </label>
              <select
                id="countryOfOrigin"
                name="countryOfOrigin"
                value={formData.countryOfOrigin}
                onChange={handleInputChange}
                required
              >
                <option value="">Select country...</option>
                {options.countries.map((opt) => (
                  <option key={opt.id} value={opt.code}>
                    {opt.name} ({opt.code})
                  </option>
                ))}
              </select>
              <span className="hint">Manufacturing country</span>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Optional Fields</h3>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="material">Material</label>
              <input
                type="text"
                id="material"
                name="material"
                value={formData.material}
                onChange={handleInputChange}
                placeholder="MICROFIBER, LEATHERETTE"
              />
              <span className="hint">Packaging material</span>
            </div>

            <div className="form-group">
              <label htmlFor="size">Size</label>
              <input
                type="text"
                id="size"
                name="size"
                value={formData.size}
                onChange={handleInputChange}
                placeholder="LARGE, SMALL, MEDIUM"
              />
              <span className="hint">Package size</span>
            </div>

            <div className="form-group">
              <label htmlFor="color">Color</label>
              <input
                type="text"
                id="color"
                name="color"
                value={formData.color}
                onChange={handleInputChange}
                placeholder="NAVY, RED, BRIGHTSILVER"
              />
              <span className="hint">Package color</span>
            </div>

            <div className="form-group">
              <label htmlFor="type">Type</label>
              <input
                type="text"
                id="type"
                name="type"
                value={formData.type}
                onChange={handleInputChange}
                placeholder="RING, NECKLACE, BRACELET"
              />
              <span className="hint">Intended product type</span>
            </div>

            <div className="form-group">
              <label htmlFor="finish">Finish</label>
              <input
                type="text"
                id="finish"
                name="finish"
                value={formData.finish}
                onChange={handleInputChange}
                placeholder="MATTE, GLOSSY"
              />
              <span className="hint">Surface finish</span>
            </div>

            <div className="form-group">
              <label htmlFor="itemName">
                Item Name <span className="required">*</span>
              </label>
              <input
                type="text"
                id="itemName"
                name="itemName"
                value={formData.itemName}
                onChange={handleInputChange}
                placeholder="e.g., Ring Box"
                required
              />
              <span className="hint">Descriptive product name</span>
            </div>

            <div className="form-group">
              <label htmlFor="notes">Notes</label>
              <input
                type="text"
                id="notes"
                name="notes"
                value={formData.notes}
                onChange={handleInputChange}
                placeholder="Additional specifications"
              />
              <span className="hint">Extra details</span>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Product Details (Optional)</h3>
          <p className="section-description" style={{ fontSize: "0.875rem", color: "#666", marginBottom: "1rem" }}>
            Set initial stock quantity and storage location for this product.
          </p>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="stockQuantity">Initial Stock Quantity</label>
              <input
                type="number"
                id="stockQuantity"
                name="stockQuantity"
                value={formData.stockQuantity}
                onChange={(e) => setFormData((prev) => ({ ...prev, stockQuantity: parseInt(e.target.value) || 0 }))}
                min="0"
                max="100000"
                placeholder="0"
              />
              <span className="hint">Number of units in stock</span>
            </div>

            <div className="form-group">
              <label htmlFor="stockLocation">Storage Location</label>
              <input
                type="text"
                id="stockLocation"
                name="stockLocation"
                value={formData.stockLocation}
                onChange={handleInputChange}
                placeholder="e.g., Warehouse A, Shelf B3"
              />
              <span className="hint">Where items are stored</span>
            </div>
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? "Generating..." : "Generate SKU"}
          </button>
          <button type="button" className="btn-secondary" onClick={handleReset}>
            Reset Form
          </button>
        </div>
      </form>

      {error && (
        <div className="code-result error">
          <div className="result-header">
            <span className="result-icon">❌</span>
            <h4>Error</h4>
          </div>
          <p>{error}</p>
        </div>
      )}

      {result && (
        <div className="code-result success">
          <div className="result-header">
            <span className="result-icon">✅</span>
            <h4>Product Created</h4>
          </div>

          {/* Descriptive Code (human-readable specs) */}
          <div className="result-code-display">
            <div style={{ fontSize: "0.75rem", color: "#64748b", marginBottom: "4px" }}>Descriptive Code:</div>
            <code className="generated-code">{result.code}</code>
            <button
              className="btn-copy"
              onClick={() => navigator.clipboard.writeText(result.code)}
            >
              Copy
            </button>
          </div>

          {/* SKU/Barcode (unique identifier) */}
          {result.barcode && (
            <div className="result-code-display" style={{ marginTop: "0.5rem" }}>
              <div style={{ fontSize: "0.75rem", color: "#64748b", marginBottom: "4px" }}>SKU Code (Barcode):</div>
              <code className="generated-barcode">{result.barcode}</code>
              <button
                className="btn-copy"
                onClick={() => navigator.clipboard.writeText(result.barcode)}
              >
                Copy
              </button>
            </div>
          )}

          <div className="result-details">
            <div className="result-detail-item">
              <span className="detail-label">Code Length:</span>
              <span className="detail-value">
                {result.length} / 30 characters
              </span>
            </div>
            {result.truncated && (
              <div className="result-detail-item">
                <span className="detail-label">Truncated:</span>
                <span className="detail-value warning">Yes</span>
              </div>
            )}
          </div>

          {/* Product saved info */}
          <div style={{ marginTop: "1rem", padding: "1rem", background: "#f0fdf4", borderRadius: "8px", border: "1px solid #bbf7d0" }}>
            <p style={{ fontSize: "0.875rem", color: "#16a34a", margin: 0, fontWeight: "500" }}>
              Product saved with unique barcode identifier.
              {formData.stockQuantity > 0 && (
                <span style={{ display: "block", marginTop: "0.5rem", color: "#666", fontWeight: "400" }}>
                  Initial stock: {formData.stockQuantity} unit(s)
                  {formData.stockLocation && ` at ${formData.stockLocation}`}
                </span>
              )}
            </p>
          </div>
        </div>
      )}

      <CodeGenerationHelpModal
        open={showHelp}
        onClose={() => setShowHelp(false)}
        title="Packaging SKU Rules"
        sections={helpSections}
        footer="Prefix and Country of Origin are now required for all packaging SKUs."
      />

      <div className="bulk-upload-section">
        <div className="bulk-upload-header">
          <div>
            <h3>Bulk Generate from CSV</h3>
            <p>
              Upload a CSV file with required columns: prefix, country_of_origin, <strong>item_name</strong>, and optional columns: material, size, color, type, finish, notes.{" "}
              <a
                href="/api/skus/templates/packaging"
                download="packaging_items_template.csv"
                style={{color: '#007bff', textDecoration: 'underline'}}
              >
                Download CSV Template
              </a>
            </p>
          </div>
        </div>
        <div className="bulk-upload-controls">
          <input
            type="file"
            accept=".csv"
            onChange={handleBulkFileChange}
            className="bulk-upload-input"
          />
          <button
            type="button"
            className="btn-primary"
            onClick={handleBulkGenerate}
            disabled={bulkLoading}
          >
            {bulkLoading ? "Processing..." : "Generate SKUs from CSV"}
          </button>
        </div>

        {bulkError && <div className="bulk-upload-error">{bulkError}</div>}

        {bulkResult && (
          <div className="bulk-upload-summary">
            <p>
              Generated {bulkResult.successCount} of {bulkResult.totalRows} rows.
              {bulkResult.failureCount > 0 && " Some rows could not be processed."}
            </p>
            {Array.isArray(bulkResult.generatedCodes) &&
              bulkResult.generatedCodes.length > 0 && (
                <div className="bulk-upload-codes">
                  <h4>Sample SKUs</h4>
                  <ul>
                    {bulkResult.generatedCodes.slice(0, 5).map((item) => (
                      <li key={item.skuCode}>
                        <code>{item.skuCode}</code>
                        {item.barcode && <span className="barcode-preview"> • Barcode: {item.barcode}</span>}
                        {item.itemName ? ` – ${item.itemName}` : ""}
                      </li>
                    ))}
                  </ul>
                  {bulkResult.generatedCodes.length > 5 && (
                    <p className="bulk-upload-note">
                      Showing first 5 of {bulkResult.generatedCodes.length} SKUs.
                    </p>
                  )}
                  <p className="bulk-upload-note">
                    All products with unique barcodes are saved in the Generated SKUs tab.
                  </p>
                </div>
              )}
            {bulkResult.errors && bulkResult.errors.length > 0 && (
              <details className="bulk-upload-errors">
                <summary>View {bulkResult.errors.length} import notices</summary>
                <ul>
                  {bulkResult.errors.map((message, index) => (
                    <li key={index}>{message}</li>
                  ))}
                </ul>
              </details>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default PackagingCodeForm;
