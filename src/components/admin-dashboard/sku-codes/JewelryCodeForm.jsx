import React, { useState } from "react";
import { skuCodesAPI } from "@/services/api";
import { useDropdownOptions } from "@/hooks/useDropdownOptions";
import CodeGenerationHelpModal from "./CodeGenerationHelpModal";
import "./CodeForms.css";

const JewelryCodeForm = ({ onCodeGenerated }) => {
  const [formData, setFormData] = useState({
    prefix: "",
    material: "",
    materialColor: "",
    materialWeight: "",
    isCoated: false,
    coatingMaterial: "",
    origin: "",
    shape: "",
    weight: "",
    sideStones: "",
    countryOfOrigin: "",
    itemName: "",
    variant: "",
    // Product details
    unitLocation: "",
    unitCostPrice: "",
    unitSalePrice: "",
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
    const { name, value, type, checked } = e.target;

    if (type === "checkbox") {
      setFormData((prev) => ({ ...prev, [name]: checked }));
    } else {
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await skuCodesAPI.generateJewelrySku(formData);
      setResult(response.data);
      if (onCodeGenerated) {
        onCodeGenerated({
          ...response.data,
          type: "jewelry",
          inputs: formData,
        });
      }
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to generate SKU. Please check all required fields and try again."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setFormData({
      prefix: "",
      material: "",
      materialColor: "",
      materialWeight: "",
      isCoated: false,
      coatingMaterial: "",
      origin: "",
      shape: "",
      weight: "",
      sideStones: "",
      countryOfOrigin: "",
      itemName: "",
      variant: "",
      unitLocation: "",
      unitCostPrice: "",
      unitSalePrice: "",
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
      setBulkError("Please attach a CSV file with the required columns including country_of_origin.");
      return;
    }

    setBulkLoading(true);
    setBulkError(null);

    try {
      const response = await skuCodesAPI.importJewelrySkus(bulkFile);
      setBulkResult(response.data);
      if (onCodeGenerated) {
        onCodeGenerated();
      }
    } catch (bulkException) {
      setBulkResult(null);
      setBulkError(
        bulkException.response?.data?.message ||
          "Failed to process the CSV file. Please verify the format and required columns."
      );
    } finally {
      setBulkLoading(false);
    }
  };

  const helpSections = [
    {
      heading: "Required fields for SKU generation",
      items: [
        "Prefix, Material, Material Color, Material Weight, Stone Origin, Stone Shape, Stone Weight, Side Stones, Country of Origin, and Item Name are required.",
        "Is Coated checkbox - if checked, Coating Material becomes required.",
        "Variant/Notes is the only optional field and won't be used in SKU code generation.",
      ],
    },
    {
      heading: "SKU code format",
      items: [
        "Format: PREFIX-MATERIAL-COLOR-WEIGHT-[COATING]-ORIGIN-SHAPE-CARATS-SIDESTONES",
        "Example: RNG-18KWG-W-2.09-LG-RD-1.29-N (Ring, 18K White Gold, White, 2.09g, Lab Grown, Round, 1.29ct, No side stones)",
        "Abbreviations are applied automatically (e.g., WHITE -> W, ROUND -> RD, NONE -> N).",
      ],
    },
    {
      heading: "Barcode generation",
      items: [
        "A unique 13-character barcode is auto-generated: MIR + timestamp + random digits.",
        "Barcode format is optimized for label printing and scanning.",
        "System checks database to ensure no duplicate barcodes exist.",
      ],
    },
    {
      heading: "What happens after generation",
      items: [
        "Every successful result is saved with SKU code, barcode, and item details.",
        "You can copy the SKU and barcode from the confirmation card.",
        "All generated SKUs are available in the Generated SKUs tab for MISA export.",
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
          How jewelry SKU generation works
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
              <span className="hint">Product category (e.g., Ring, Necklace)</span>
            </div>

            <div className="form-group">
              <label htmlFor="material">
                Material <span className="required">*</span>
              </label>
              <select
                id="material"
                name="material"
                value={formData.material}
                onChange={handleInputChange}
                required
              >
                <option value="">Select material...</option>
                {options.materials.map((opt) => (
                  <option key={opt.id} value={opt.code}>
                    {opt.name}
                  </option>
                ))}
              </select>
              <span className="hint">Metal type</span>
            </div>

            <div className="form-group">
              <label htmlFor="materialColor">
                Material Color <span className="required">*</span>
              </label>
              <select
                id="materialColor"
                name="materialColor"
                value={formData.materialColor}
                onChange={handleInputChange}
                required
              >
                <option value="">Select material color...</option>
                {options.materialColors.map((opt) => (
                  <option key={opt.id} value={opt.code}>
                    {opt.name}
                  </option>
                ))}
              </select>
              <span className="hint">Metal color</span>
            </div>

            <div className="form-group">
              <label htmlFor="materialWeight">
                Material Weight <span className="required">*</span>
              </label>
              <input
                type="text"
                id="materialWeight"
                name="materialWeight"
                value={formData.materialWeight}
                onChange={handleInputChange}
                placeholder="e.g., 3.5G, 5.2G"
                required
              />
              <span className="hint">Metal weight in grams (e.g., 3.5G)</span>
            </div>

            <div className="form-group">
              <label htmlFor="isCoated">
                Is Coated <span className="required">*</span>
              </label>
              <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                <input
                  type="checkbox"
                  id="isCoated"
                  name="isCoated"
                  checked={formData.isCoated}
                  onChange={handleInputChange}
                />
                <label htmlFor="isCoated" style={{margin: 0}}>Coated (e.g., Rhodium plating for silver)</label>
              </div>
              <span className="hint">Check if the material is coated</span>
            </div>

            {formData.isCoated && (
              <div className="form-group">
                <label htmlFor="coatingMaterial">
                  Coating Material <span className="required">*</span>
                </label>
                <input
                  type="text"
                  id="coatingMaterial"
                  name="coatingMaterial"
                  value={formData.coatingMaterial}
                  onChange={handleInputChange}
                  placeholder="e.g., RHODIUM, GOLD"
                  required={formData.isCoated}
                />
                <span className="hint">Coating material (e.g., RHODIUM, GOLD)</span>
              </div>
            )}

            <div className="form-group">
              <label htmlFor="origin">
                Stone Origin <span className="required">*</span>
              </label>
              <select
                id="origin"
                name="origin"
                value={formData.origin}
                onChange={handleInputChange}
                required
              >
                <option value="">Select origin...</option>
                {options.stoneOrigins.map((opt) => (
                  <option key={opt.id} value={opt.code}>
                    {opt.name}
                  </option>
                ))}
              </select>
              <span className="hint">Diamond origin</span>
            </div>

            <div className="form-group">
              <label htmlFor="shape">
                Stone Shape <span className="required">*</span>
              </label>
              <select
                id="shape"
                name="shape"
                value={formData.shape}
                onChange={handleInputChange}
                required
              >
                <option value="">Select shape...</option>
                {options.stoneShapes.map((opt) => (
                  <option key={opt.id} value={opt.code}>
                    {opt.name}
                  </option>
                ))}
              </select>
              <span className="hint">Main stone shape</span>
            </div>

            <div className="form-group">
              <label htmlFor="weight">
                Stone Weight <span className="required">*</span>
              </label>
              <input
                type="text"
                id="weight"
                name="weight"
                value={formData.weight}
                onChange={handleInputChange}
                placeholder="e.g., 1.29"
                required
              />
              <span className="hint">Carat weight (e.g., 1.29, 0.5)</span>
            </div>

            <div className="form-group">
              <label htmlFor="sideStones">
                Side Stones <span className="required">*</span>
              </label>
              <select
                id="sideStones"
                name="sideStones"
                value={formData.sideStones}
                onChange={handleInputChange}
                required
              >
                <option value="">Select side stones...</option>
                {options.sideStones.map((opt) => (
                  <option key={opt.id} value={opt.code}>
                    {opt.name}
                  </option>
                ))}
              </select>
              <span className="hint">Side stone type</span>
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
                placeholder="e.g., Diamond Solitaire Ring"
                required
              />
              <span className="hint">Descriptive product name</span>
            </div>
          </div>
        </div>

        <div className="form-section">
          <h3>Optional Field</h3>
          <div className="form-group">
            <label htmlFor="variant">Variant / Notes</label>
            <input
              type="text"
              id="variant"
              name="variant"
              value={formData.variant}
              onChange={handleInputChange}
              placeholder="e.g., HALO, MICROPAVE"
            />
            <span className="hint">Additional notes (not used in SKU generation)</span>
          </div>
        </div>

        <div className="form-section">
          <h3>Product Details (Optional)</h3>
          <p style={{ fontSize: "0.875rem", color: "#64748b", marginBottom: "1rem" }}>
            Each product gets a unique barcode as its SKU identifier.
          </p>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="unitLocation">Storage Location</label>
              <input
                type="text"
                id="unitLocation"
                name="unitLocation"
                value={formData.unitLocation}
                onChange={handleInputChange}
                placeholder="e.g., Showroom A, Safe-1"
              />
              <span className="hint">Where product is stored</span>
            </div>

            <div className="form-group">
              <label htmlFor="unitCostPrice">Cost Price (USD)</label>
              <input
                type="number"
                id="unitCostPrice"
                name="unitCostPrice"
                value={formData.unitCostPrice}
                onChange={handleInputChange}
                step="0.01"
                min="0"
                placeholder="e.g., 1500.00"
              />
              <span className="hint">Product cost</span>
            </div>

            <div className="form-group">
              <label htmlFor="unitSalePrice">Sale Price (USD)</label>
              <input
                type="number"
                id="unitSalePrice"
                name="unitSalePrice"
                value={formData.unitSalePrice}
                onChange={handleInputChange}
                step="0.01"
                min="0"
                placeholder="e.g., 2500.00"
              />
              <span className="hint">Product sale price</span>
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
              📋 Copy
            </button>
          </div>

          {/* SKU/Barcode (unique identifier) */}
          {result.barcode && (
            <div className="result-code-display" style={{ marginTop: "0.5rem" }}>
              <div style={{ fontSize: "0.75rem", color: "#64748b", marginBottom: "4px" }}>SKU Code (Barcode):</div>
              <code className="generated-barcode" style={{ background: "#e0f2fe", color: "#0369a1" }}>{result.barcode}</code>
              <button
                className="btn-copy"
                onClick={() => navigator.clipboard.writeText(result.barcode)}
              >
                📋 Copy
              </button>
            </div>
          )}

          <div className="result-details">
            <div className="result-detail-item">
              <span className="detail-label">Descriptive Code Length:</span>
              <span className="detail-value">
                {result.length} characters
              </span>
            </div>
            <div className="result-detail-item">
              <span className="detail-label">Description:</span>
              <span className="detail-value">{result.description}</span>
            </div>
          </div>

          {/* Info box explaining the new model */}
          <div style={{ marginTop: "1rem", padding: "1rem", background: "#f0fdf4", borderRadius: "8px", border: "1px solid #bbf7d0" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.5rem" }}>
              <span style={{ background: "#22c55e", color: "white", padding: "2px 8px", borderRadius: "4px", fontSize: "0.75rem", fontWeight: "600" }}>
                Unique Product
              </span>
            </div>
            <p style={{ fontSize: "0.875rem", color: "#666", margin: 0 }}>
              The SKU code (barcode) is the unique identifier for this product.
              The descriptive code shows the product specifications for easy reference.
            </p>
          </div>
        </div>
      )}

      <CodeGenerationHelpModal
        open={showHelp}
        onClose={() => setShowHelp(false)}
        title="Jewelry SKU Rules"
        sections={helpSections}
        footer="All fields except Variant are now required for jewelry SKUs."
      />

      <div className="bulk-upload-section">
        <div className="bulk-upload-header">
          <div>
            <h3>Bulk Generate from CSV</h3>
            <p>
              Upload a CSV file with required columns: prefix, material, <strong>material_color</strong>, <strong>material_weight</strong>, <strong>is_coated</strong>, coating_material (if coated), stone_origin, main_shape, main_weight, side_stones, country_of_origin, <strong>item_name</strong>, and optional variant.{" "}
              <a
                href="/api/skus/templates/jewelry"
                download="jewelry_items_template.csv"
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
                    All generated SKUs with unique barcodes are saved in the Generated SKUs tab.
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

export default JewelryCodeForm;
