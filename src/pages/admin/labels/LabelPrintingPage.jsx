import React, { useState, useEffect, useCallback } from 'react';
import {
  labelTemplateAPI,
  labelAPI,
  printerAPI,
  getLabelTypeConfig,
  checkPrintServerStatus,
} from '@/services/labelTemplateService';
import { productsAPI } from '@/services/api';
import './LabelPrintingPage.css';

const LabelPrintingPage = () => {
  // Templates
  const [templates, setTemplates] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [loadingTemplates, setLoadingTemplates] = useState(true);

  // Products
  const [products, setProducts] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [productSearch, setProductSearch] = useState('');
  const [productPage, setProductPage] = useState(0);
  const [productTotal, setProductTotal] = useState(0);

  // Printer
  const [printerStatus, setPrinterStatus] = useState({ online: false, checking: true });
  const [printers, setPrinters] = useState([]);
  const [selectedPrinter, setSelectedPrinter] = useState('');

  // Print options
  const [quantityPerProduct, setQuantityPerProduct] = useState(1);

  // Preview & Print
  const [previews, setPreviews] = useState([]);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [printing, setPrinting] = useState(false);
  const [printResult, setPrintResult] = useState(null);

  // Local print server URL
  const [printServerUrl, setPrintServerUrl] = useState('http://localhost:3001');

  // Load templates on mount
  useEffect(() => {
    loadTemplates();
    checkPrinterConnection();
  }, []);

  // Debounced product search
  useEffect(() => {
    const timer = setTimeout(() => {
      if (productSearch) {
        searchProducts();
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [productSearch]);

  const loadTemplates = async () => {
    try {
      const response = await labelTemplateAPI.getAll({ size: 100 });
      setTemplates(response.data.content || []);
    } catch (err) {
      console.error('Error loading templates:', err);
    } finally {
      setLoadingTemplates(false);
    }
  };

  const checkPrinterConnection = async () => {
    setPrinterStatus({ online: false, checking: true });
    try {
      printerAPI.setBaseUrl(printServerUrl);
      const status = await checkPrintServerStatus();
      setPrinterStatus({ ...status, checking: false });

      if (status.online) {
        const printersData = await printerAPI.getPrinters();
        setPrinters(printersData.printers || []);
        if (printersData.configuredPrinter) {
          setSelectedPrinter(printersData.configuredPrinter);
        }
      }
    } catch (err) {
      console.error('Printer check error:', err);
      setPrinterStatus({ online: false, checking: false, error: err.message });
    }
  };

  const searchProducts = async () => {
    setLoadingProducts(true);
    try {
      const response = await productsAPI.getAll({
        page: productPage,
        size: 20,
        search: productSearch,
      });
      const productList = response.data.content || response.data || [];
      // Debug: Log returned products
      console.log('=== SEARCH DEBUG ===');
      console.log('Search results:', productList.map(p => ({
        id: p.id,
        skuCode: p.skuCode,
        barcode: p.barcode,
        name: p.name || p.itemName
      })));
      setProducts(productList);
      setProductTotal(response.data.totalItems || response.data.length || 0);
    } catch (err) {
      console.error('Error searching products:', err);
    } finally {
      setLoadingProducts(false);
    }
  };

  const handleProductSelect = (product) => {
    // Debug: Log product being selected
    console.log('=== SELECT DEBUG ===');
    console.log('Product clicked:', { id: product.id, skuCode: product.skuCode, barcode: product.barcode });
    setSelectedProducts((prev) => {
      const exists = prev.find((p) => p.id === product.id);
      if (exists) {
        return prev.filter((p) => p.id !== product.id);
      }
      return [...prev, product];
    });
  };

  const handleSelectAll = () => {
    const allIds = new Set(selectedProducts.map((p) => p.id));
    const newProducts = products.filter((p) => !allIds.has(p.id));
    setSelectedProducts([...selectedProducts, ...newProducts]);
  };

  const handleClearSelection = () => {
    setSelectedProducts([]);
  };

  const generatePreviews = async () => {
    if (!selectedTemplate || selectedProducts.length === 0) return;

    setLoadingPreview(true);
    setPreviews([]);

    try {
      const response = await labelAPI.previewBatch({
        templateId: selectedTemplate.id,
        productIds: selectedProducts.map((p) => p.id),
        quantityPerProduct: 1,
      });

      setPreviews(response.data.previews || []);
    } catch (err) {
      console.error('Error generating previews:', err);
    } finally {
      setLoadingPreview(false);
    }
  };

  const handlePrint = async () => {
    if (!selectedTemplate || selectedProducts.length === 0) return;

    setPrinting(true);
    setPrintResult(null);

    try {
      // Debug: Log selected products before printing
      console.log('=== PRINT DEBUG ===');
      console.log('Selected products FULL:', JSON.stringify(selectedProducts.map(p => ({
        id: p.id,
        skuCode: p.skuCode,
        barcode: p.barcode,
        descriptiveCode: p.descriptiveCode,
        name: p.name || p.itemName
      })), null, 2));
      console.log('Product IDs being sent:', selectedProducts.map(p => p.id));

      // First, render the labels
      const renderResponse = await labelAPI.renderBatch({
        templateId: selectedTemplate.id,
        productIds: selectedProducts.map((p) => p.id),
        quantityPerProduct,
      });

      // Debug: Log the render response
      console.log('Render response products:', JSON.stringify(renderResponse.data.products, null, 2));
      console.log('Rendered ZPL:', renderResponse.data.renderedZpl);

      const renderedZpl = renderResponse.data.renderedZpl;

      // Then send to printer
      printerAPI.setBaseUrl(printServerUrl);
      const printResponse = await printerAPI.print(renderedZpl, {
        printer: selectedPrinter,
        copies: 1,
      });

      setPrintResult({
        success: true,
        message: `Successfully sent ${renderResponse.data.totalLabels} labels to printer`,
        jobId: printResponse.jobId,
      });
    } catch (err) {
      console.error('Print error:', err);
      setPrintResult({
        success: false,
        message: err.message || 'Print failed',
      });
    } finally {
      setPrinting(false);
    }
  };

  const handleTestPrint = async () => {
    setPrinting(true);
    setPrintResult(null);

    try {
      printerAPI.setBaseUrl(printServerUrl);
      const response = await printerAPI.testPrint(selectedPrinter);
      setPrintResult({
        success: true,
        message: 'Test label sent to printer',
        jobId: response.jobId,
      });
    } catch (err) {
      console.error('Test print error:', err);
      setPrintResult({
        success: false,
        message: err.message || 'Test print failed',
      });
    } finally {
      setPrinting(false);
    }
  };

  return (
    <div className="label-printing-page">
      {/* Header */}
      <div className="lpp-header">
        <h2>Label Printing</h2>
        <div className="lpp-printer-status">
          {printerStatus.checking ? (
            <span className="lpp-status-checking">Checking printer...</span>
          ) : printerStatus.online ? (
            <span className="lpp-status-online">Printer Online</span>
          ) : (
            <span className="lpp-status-offline">Printer Offline</span>
          )}
        </div>
      </div>

      <div className="lpp-layout">
        {/* Left Panel - Configuration */}
        <div className="lpp-panel">
          {/* Printer Settings */}
          <div className="lpp-section">
            <h3>Printer Settings</h3>

            <div className="lpp-field">
              <label>Print Server URL</label>
              <div className="lpp-input-group">
                <input
                  type="text"
                  value={printServerUrl}
                  onChange={(e) => setPrintServerUrl(e.target.value)}
                  placeholder="http://localhost:3001"
                />
                <button
                  className="lpp-btn lpp-btn-small"
                  onClick={checkPrinterConnection}
                  disabled={printerStatus.checking}
                >
                  Check
                </button>
              </div>
            </div>

            {printers.length > 0 && (
              <div className="lpp-field">
                <label>Printer</label>
                <select
                  value={selectedPrinter}
                  onChange={(e) => setSelectedPrinter(e.target.value)}
                >
                  {printers.map((p) => (
                    <option key={p.name} value={p.name}>
                      {p.name} {p.isDefault ? '(Default)' : ''}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <button
              className="lpp-btn lpp-btn-secondary"
              onClick={handleTestPrint}
              disabled={!printerStatus.online || printing}
            >
              Test Print
            </button>
          </div>

          {/* Template Selection */}
          <div className="lpp-section">
            <h3>Select Template</h3>

            {loadingTemplates ? (
              <div className="lpp-loading-small">Loading templates...</div>
            ) : templates.length === 0 ? (
              <div className="lpp-empty-small">No templates found</div>
            ) : (
              <div className="lpp-template-list">
                {templates.map((template) => {
                  const typeConfig = getLabelTypeConfig(template.labelType);
                  const isSelected = selectedTemplate?.id === template.id;
                  return (
                    <div
                      key={template.id}
                      className={`lpp-template-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => setSelectedTemplate(template)}
                    >
                      <div
                        className="lpp-template-icon"
                        style={{ backgroundColor: typeConfig.bg, color: typeConfig.color }}
                      >
                        {typeConfig.icon}
                      </div>
                      <div className="lpp-template-info">
                        <div className="lpp-template-name">{template.name}</div>
                        <div className="lpp-template-meta">
                          {template.widthMm}×{template.heightMm}mm • {template.dpi} DPI
                        </div>
                      </div>
                      {isSelected && <span className="lpp-check">✓</span>}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Print Options */}
          <div className="lpp-section">
            <h3>Print Options</h3>

            <div className="lpp-field">
              <label>Copies per Product</label>
              <input
                type="number"
                value={quantityPerProduct}
                onChange={(e) => setQuantityPerProduct(Math.max(1, parseInt(e.target.value) || 1))}
                min={1}
                max={100}
              />
            </div>

            <div className="lpp-summary">
              <div className="lpp-summary-item">
                <span>Selected Products:</span>
                <strong>{selectedProducts.length}</strong>
              </div>
              <div className="lpp-summary-item">
                <span>Total Labels:</span>
                <strong>{selectedProducts.length * quantityPerProduct}</strong>
              </div>
            </div>
          </div>
        </div>

        {/* Middle Panel - Product Selection */}
        <div className="lpp-panel lpp-panel-wide">
          <div className="lpp-section">
            <div className="lpp-section-header">
              <h3>Select Products</h3>
              <div className="lpp-section-actions">
                <button className="lpp-btn lpp-btn-small" onClick={handleSelectAll}>
                  Select All
                </button>
                <button className="lpp-btn lpp-btn-small" onClick={handleClearSelection}>
                  Clear
                </button>
              </div>
            </div>

            <div className="lpp-field">
              <input
                type="text"
                value={productSearch}
                onChange={(e) => setProductSearch(e.target.value)}
                placeholder="Search by name, SKU, or barcode..."
                className="lpp-search-input"
              />
            </div>

            {loadingProducts ? (
              <div className="lpp-loading-small">Searching...</div>
            ) : products.length === 0 ? (
              <div className="lpp-empty-small">
                {productSearch ? 'No products found' : 'Enter a search term'}
              </div>
            ) : (
              <div className="lpp-product-list">
                {products.map((product) => {
                  const isSelected = selectedProducts.some((p) => p.id === product.id);
                  return (
                    <div
                      key={product.id}
                      className={`lpp-product-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => handleProductSelect(product)}
                    >
                      <div className="lpp-product-checkbox">{isSelected ? '☑' : '☐'}</div>
                      {product.imageUrl && (
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="lpp-product-image"
                        />
                      )}
                      <div className="lpp-product-info">
                        <div className="lpp-product-name">{product.name}</div>
                        <div className="lpp-product-meta">
                          <span>SKU: {product.skuCode || '-'}</span>
                          {product.barcode && <span>Barcode: {product.barcode}</span>}
                        </div>
                      </div>
                      {product.price && (
                        <div className="lpp-product-price">
                          {new Intl.NumberFormat('vi-VN', {
                            style: 'currency',
                            currency: 'VND',
                          }).format(product.price)}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* Selected Products Summary */}
            {selectedProducts.length > 0 && (
              <div className="lpp-selected-summary">
                <h4>Selected Products ({selectedProducts.length})</h4>
                <div className="lpp-selected-list">
                  {selectedProducts.map((product) => (
                    <div key={product.id} className="lpp-selected-item">
                      <span>{product.name}</span>
                      <button
                        className="lpp-remove-btn"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleProductSelect(product);
                        }}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Panel - Preview & Print */}
        <div className="lpp-panel">
          <div className="lpp-section">
            <div className="lpp-section-header">
              <h3>Preview & Print</h3>
              <button
                className="lpp-btn lpp-btn-small"
                onClick={generatePreviews}
                disabled={!selectedTemplate || selectedProducts.length === 0 || loadingPreview}
              >
                {loadingPreview ? 'Loading...' : 'Generate Preview'}
              </button>
            </div>

            {previews.length > 0 ? (
              <div className="lpp-preview-grid">
                {previews.map((preview, index) => (
                  <div key={index} className="lpp-preview-item">
                    {preview.previewImageBase64 && (
                      <img
                        src={`data:image/png;base64,${preview.previewImageBase64}`}
                        alt={`Preview ${index + 1}`}
                        className="lpp-preview-image"
                      />
                    )}
                  </div>
                ))}
                {selectedProducts.length > previews.length && (
                  <div className="lpp-preview-more">
                    +{selectedProducts.length - previews.length} more
                  </div>
                )}
              </div>
            ) : (
              <div className="lpp-preview-placeholder">
                Select a template and products to preview labels
              </div>
            )}

            {/* Print Result */}
            {printResult && (
              <div
                className={`lpp-print-result ${printResult.success ? 'success' : 'error'}`}
              >
                {printResult.message}
                {printResult.jobId && <div className="lpp-job-id">Job ID: {printResult.jobId}</div>}
              </div>
            )}

            {/* Print Button */}
            <button
              className="lpp-btn lpp-btn-primary lpp-btn-print"
              onClick={handlePrint}
              disabled={
                !printerStatus.online ||
                !selectedTemplate ||
                selectedProducts.length === 0 ||
                printing
              }
            >
              {printing ? 'Printing...' : `Print ${selectedProducts.length * quantityPerProduct} Labels`}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LabelPrintingPage;
