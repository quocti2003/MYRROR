import React, { useState, useEffect, useCallback } from "react";
import JewelryCodeForm from "./sku-codes/JewelryCodeForm";
import PackagingCodeForm from "./sku-codes/PackagingCodeForm";
import DiamondCodeForm from "./sku-codes/DiamondCodeForm";
import CodeSearch from "./sku-codes/CodeSearch";
import { skuCodesAPI, diamondsAPI } from "@/services/api";
import "./SkuCodesManager.css";

const DEFAULT_PAGE_SIZE = 25;

const formatDateTime = (value) => {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return date.toLocaleString();
};

const formatCurrency = (value) => {
  if (value === null || value === undefined) {
    return "—";
  }

  const numberValue = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(numberValue)) {
    return "—";
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(numberValue);
};

const SkuCodesManager = () => {
  const [activeTab, setActiveTab] = useState("jewelry");
  const [generatedSkus, setGeneratedSkus] = useState([]);
  const [generatedMeta, setGeneratedMeta] = useState({
    page: 0,
    size: DEFAULT_PAGE_SIZE,
    totalPages: 0,
    totalElements: 0,
  });
  const [generatedLoading, setGeneratedLoading] = useState(false);
  const [generatedError, setGeneratedError] = useState(null);
  const [selectedSkuIds, setSelectedSkuIds] = useState(new Set());

  // Lab-Grown Diamonds state
  const [diamonds, setDiamonds] = useState([]);
  const [diamondsLoading, setDiamondsLoading] = useState(false);
  const [diamondsError, setDiamondsError] = useState(null);

  const fetchGeneratedSkus = useCallback(
    async ({ page, size } = {}) => {
      const nextPage = page ?? 0;
      const nextSize = size ?? generatedMeta.size;

      setGeneratedLoading(true);
      setGeneratedError(null);

      try {
        const response = await skuCodesAPI.getGeneratedSkus({
          page: nextPage,
          size: nextSize,
          sortBy: "createdAt",
          direction: "DESC",
        });

        const data = response.data || {};
        setGeneratedSkus(data.codes || []);
        setGeneratedMeta({
          page: data.page ?? nextPage,
          size: data.size ?? nextSize,
          totalPages: data.totalPages ?? 0,
          totalElements: data.totalElements ?? 0,
        });
      } catch (error) {
        setGeneratedError(
          error.response?.data?.message ||
            "Failed to load generated SKUs. Please try again."
        );
      } finally {
        setGeneratedLoading(false);
      }
    },
    [generatedMeta.size]
  );

  const fetchDiamonds = useCallback(async () => {
    setDiamondsLoading(true);
    setDiamondsError(null);

    try {
      const diamondsResponse = await diamondsAPI.getAllDiamonds();
      setDiamonds(diamondsResponse.data || []);
    } catch (error) {
      setDiamondsError(
        error.response?.data?.message ||
          "Failed to load diamonds. Please try again."
      );
    } finally {
      setDiamondsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === "history") {
      fetchGeneratedSkus({ page: 0 });
      fetchDiamonds();
    }
  }, [activeTab, fetchGeneratedSkus, fetchDiamonds]);

  const handleSkuGenerated = useCallback(() => {
    if (activeTab === "history") {
      fetchGeneratedSkus({ page: 0 });
      fetchDiamonds();
    }
  }, [activeTab, fetchGeneratedSkus, fetchDiamonds]);

  const handlePageSizeChange = (event) => {
    const newSize = parseInt(event.target.value, 10);
    if (Number.isNaN(newSize)) {
      return;
    }
    setSelectedSkuIds(new Set());
    fetchGeneratedSkus({ page: 0, size: newSize });
  };

  const goToPreviousPage = () => {
    if (generatedMeta.page === 0) {
      return;
    }
    setSelectedSkuIds(new Set());
    fetchGeneratedSkus({ page: generatedMeta.page - 1 });
  };

  const goToNextPage = () => {
    if (generatedMeta.page >= generatedMeta.totalPages - 1) {
      return;
    }
    setSelectedSkuIds(new Set());
    fetchGeneratedSkus({ page: generatedMeta.page + 1 });
  };

  const handleRefresh = () => {
    fetchGeneratedSkus({ page: generatedMeta.page });
  };

  const handleCopySku = (sku) => {
    if (!sku || !navigator?.clipboard) {
      return;
    }
    navigator.clipboard.writeText(sku).catch(() => {
      // Clipboard write failures are non-fatal; silently ignore.
    });
  };

  const handleCopyBarcode = (barcode) => {
    if (!barcode || !navigator?.clipboard) {
      return;
    }
    navigator.clipboard.writeText(barcode).catch(() => {
      // Clipboard write failures are non-fatal; silently ignore.
    });
  };

  const handleExportToMisa = async () => {
    try {
      setGeneratedLoading(true);
      setGeneratedError(null);

      const response = await skuCodesAPI.exportAllToMisa();

      // Extract filename from Content-Disposition header
      let filename = 'MISA_Import.xls';
      const contentDisposition = response.headers['content-disposition'];
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i);
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1];
        }
      }

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);

      // Trigger download
      document.body.appendChild(link);
      link.click();
      link.remove();

      // Clean up
      window.URL.revokeObjectURL(url);

    } catch (error) {
      setGeneratedError(
        error.response?.data?.message ||
          "Failed to export to MISA template. Please try again."
      );
    } finally {
      setGeneratedLoading(false);
    }
  };

  const handleExportSelectedToMisa = async () => {
    if (selectedSkuIds.size === 0) {
      return;
    }

    try {
      setGeneratedLoading(true);
      setGeneratedError(null);

      const response = await skuCodesAPI.exportByIdsToMisa(Array.from(selectedSkuIds));

      // Extract filename from Content-Disposition header
      let filename = 'MISA_Selected_Import.xls';
      const contentDisposition = response.headers['content-disposition'];
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?(.+)"?/i);
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1];
        }
      }

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);

      // Trigger download
      document.body.appendChild(link);
      link.click();
      link.remove();

      // Clean up
      window.URL.revokeObjectURL(url);

      // Clear selection after successful export
      setSelectedSkuIds(new Set());

    } catch (error) {
      setGeneratedError(
        error.response?.data?.message ||
          "Failed to export selected SKUs to MISA template. Please try again."
      );
    } finally {
      setGeneratedLoading(false);
    }
  };

  const handleSelectSku = (skuCode) => {
    setSelectedSkuIds((prev) => {
      const next = new Set(prev);
      if (next.has(skuCode)) {
        next.delete(skuCode);
      } else {
        next.add(skuCode);
      }
      return next;
    });
  };

  const handleSelectAll = () => {
    if (selectedSkuIds.size === generatedSkus.length && generatedSkus.length > 0) {
      // Deselect all
      setSelectedSkuIds(new Set());
    } else {
      // Select all on current page
      setSelectedSkuIds(new Set(generatedSkus.map((item) => item.skuCode)));
    }
  };

  const isAllSelected = generatedSkus.length > 0 && selectedSkuIds.size === generatedSkus.length;
  const isSomeSelected = selectedSkuIds.size > 0 && selectedSkuIds.size < generatedSkus.length;

  const tabs = [
    { id: "jewelry", label: "Generate Jewelry SKU" },
    { id: "packaging", label: "Generate Packaging SKU" },
    { id: "diamond", label: "Generate Diamond SKU" },
    { id: "history", label: "Generated SKUs" },
  ];

  const renderTabContent = () => {
    switch (activeTab) {
      case "jewelry":
        return <JewelryCodeForm onCodeGenerated={handleSkuGenerated} />;
      case "packaging":
        return <PackagingCodeForm onCodeGenerated={handleSkuGenerated} />;
      case "diamond":
        return <DiamondCodeForm onCodeGenerated={handleSkuGenerated} />;
      case "history":
        return (
          <div>
            <div style={{ marginBottom: "1.5rem" }}>
              <CodeSearch />
            </div>

            <div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "1.5rem" }}>
                <div>
                  <h3 style={{ fontSize: "0.875rem", fontWeight: "600", marginBottom: "0.25rem", color: "#0f172a" }}>Generated SKUs</h3>
                  <p style={{ fontSize: "0.875rem", color: "#64748b" }}>
                    Showing products and lab-grown diamonds (newest first)
                  </p>
                </div>

                <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
                  <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", fontSize: "0.875rem", color: "#64748b" }}>
                    <span>Results per page:</span>
                    <select
                      value={generatedMeta.size}
                      onChange={handlePageSizeChange}
                      disabled={generatedLoading}
                      className="admin-select"
                    >
                      <option value="10">10</option>
                      <option value="25">25</option>
                      <option value="50">50</option>
                      <option value="100">100</option>
                    </select>
                  </label>

                  <button
                    type="button"
                    className="admin-button admin-button-secondary"
                    onClick={handleRefresh}
                    disabled={generatedLoading}
                  >
                    {generatedLoading ? "Refreshing..." : "Refresh"}
                  </button>

                  <button
                    type="button"
                    className="admin-button admin-button-primary"
                    onClick={handleExportSelectedToMisa}
                    disabled={generatedLoading || selectedSkuIds.size === 0}
                    title="Export selected SKUs to MISA template"
                  >
                    Export Selected ({selectedSkuIds.size})
                  </button>

                  <button
                    type="button"
                    className="admin-button admin-button-secondary"
                    onClick={handleExportToMisa}
                    disabled={generatedLoading || generatedSkus.length === 0}
                    title="Export all SKUs to MISA template"
                  >
                    Export All
                  </button>
                </div>
              </div>

              {generatedError ? (
                <div className="admin-error-state">
                  <div>
                    <strong>Unable to load generated SKUs.</strong>
                    <p>{generatedError}</p>
                  </div>
                  <button type="button" onClick={handleRefresh} className="admin-button admin-button-secondary" style={{ marginTop: "1rem" }}>
                    Try Again
                  </button>
                </div>
              ) : (
                <>
                  <div style={{ overflowX: "auto" }}>
                    <table className="admin-table">
                      <thead>
                        <tr>
                          <th style={{ width: "40px", textAlign: "center" }}>
                            <input
                              type="checkbox"
                              checked={isAllSelected}
                              ref={(el) => {
                                if (el) el.indeterminate = isSomeSelected;
                              }}
                              onChange={handleSelectAll}
                              disabled={generatedLoading || generatedSkus.length === 0}
                              title={isAllSelected ? "Deselect all" : "Select all on this page"}
                              style={{ cursor: "pointer" }}
                            />
                          </th>
                          <th>SKU</th>
                          <th>Barcode</th>
                          <th>Item Name</th>
                          <th>Category</th>
                          <th>MISA Item Code</th>
                          <th>Qty</th>
                          <th>Price</th>
                          <th>Created</th>
                        </tr>
                      </thead>
                      <tbody>
                        {generatedLoading ? (
                          <tr>
                            <td colSpan="9" style={{ textAlign: "center", padding: "3rem 1rem", color: "#64748b" }}>
                              Loading generated SKUs...
                            </td>
                          </tr>
                        ) : generatedSkus.length === 0 ? (
                          <tr>
                            <td colSpan="9" style={{ textAlign: "center", padding: "3rem 1rem", color: "#64748b" }}>
                              No generated SKUs found.
                            </td>
                          </tr>
                        ) : (
                          generatedSkus.map((item) => (
                            <tr key={item.skuCode} style={{ backgroundColor: selectedSkuIds.has(item.skuCode) ? "#f0f9ff" : undefined }}>
                              <td style={{ textAlign: "center" }}>
                                <input
                                  type="checkbox"
                                  checked={selectedSkuIds.has(item.skuCode)}
                                  onChange={() => handleSelectSku(item.skuCode)}
                                  style={{ cursor: "pointer" }}
                                />
                              </td>
                              <td>
                                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                                  <code style={{ background: "#dbeafe", padding: "2px 8px", borderRadius: "4px", fontSize: "0.8125rem", fontFamily: "monospace", color: "#1e40af" }}>{item.skuCode}</code>
                                  <button
                                    type="button"
                                    onClick={() => handleCopySku(item.skuCode)}
                                    title="Copy SKU Code"
                                    style={{
                                      background: "none",
                                      border: "1px solid #e2e8f0",
                                      borderRadius: "4px",
                                      padding: "2px 6px",
                                      cursor: "pointer",
                                      fontSize: "0.75rem",
                                      lineHeight: "1"
                                    }}
                                  >
                                    📋
                                  </button>
                                </div>
                              </td>
                              <td>
                                {item.descriptiveCode ? (
                                  <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                                    <code style={{ background: "#f1f5f9", padding: "2px 8px", borderRadius: "4px", fontSize: "0.8125rem", fontFamily: "monospace" }}>{item.descriptiveCode}</code>
                                    <button
                                      type="button"
                                      onClick={() => handleCopyBarcode(item.descriptiveCode)}
                                      title="Copy Descriptive Code"
                                      style={{
                                        background: "none",
                                        border: "1px solid #e2e8f0",
                                        borderRadius: "4px",
                                        padding: "2px 6px",
                                        cursor: "pointer",
                                        fontSize: "0.75rem",
                                        lineHeight: "1"
                                      }}
                                    >
                                      📋
                                    </button>
                                  </div>
                                ) : (
                                  "—"
                                )}
                              </td>
                              <td>{item.itemName || "—"}</td>
                              <td>{item.category || "—"}</td>
                              <td>{item.misaItemCode || "—"}</td>
                              <td>{
                                item.currentQuantity !== null && item.currentQuantity !== undefined && item.currentQuantity > 0
                                  ? item.currentQuantity
                                  : "—"
                              }</td>
                              <td>{formatCurrency(item.price)}</td>
                              <td>{formatDateTime(item.createdAt)}</td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>

                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "1.5rem", paddingTop: "1.5rem", borderTop: "1px solid #e2e8f0" }}>
                    <button
                      type="button"
                      className="admin-button admin-button-secondary"
                      onClick={goToPreviousPage}
                      disabled={generatedLoading || generatedMeta.page === 0}
                    >
                      Previous
                    </button>

                    <span style={{ fontSize: "0.875rem", color: "#64748b" }}>
                      Page {generatedMeta.totalPages === 0 ? 0 : generatedMeta.page + 1} of {generatedMeta.totalPages}
                      <span style={{ marginLeft: "0.5rem" }}>
                        ({generatedMeta.totalElements} total)
                      </span>
                    </span>

                    <button
                      type="button"
                      className="admin-button admin-button-secondary"
                      onClick={goToNextPage}
                      disabled={
                        generatedLoading ||
                        generatedMeta.totalPages === 0 ||
                        generatedMeta.page >= generatedMeta.totalPages - 1
                      }
                    >
                      Next
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Lab-Grown Diamonds Section */}
            <div style={{ marginTop: "3rem", paddingTop: "2rem", borderTop: "2px solid #e2e8f0" }}>
              <div style={{ marginBottom: "1.5rem" }}>
                <h3 style={{ fontSize: "0.875rem", fontWeight: "600", marginBottom: "0.25rem", color: "#0f172a" }}>Lab-Grown Diamonds</h3>
                <p style={{ fontSize: "0.875rem", color: "#64748b" }}>
                  Diamond SKUs: LGD-[COLOR][CLARITY]-[CARAT]-[SHAPE]-[ORIGIN]-[MFR]-[CERT]
                </p>
              </div>

              {diamondsError ? (
                <div className="admin-error-state">
                  <div>
                    <strong>Unable to load diamonds.</strong>
                    <p>{diamondsError}</p>
                  </div>
                </div>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>SKU Code</th>
                        <th>Type</th>
                        <th>Color/Clarity</th>
                        <th>Carat</th>
                        <th>Shape</th>
                        <th>Cert #</th>
                        <th>Manufacturer</th>
                        <th>Status</th>
                        <th>Price</th>
                        <th>Created</th>
                      </tr>
                    </thead>
                    <tbody>
                      {diamondsLoading ? (
                        <tr>
                          <td colSpan="10" style={{ textAlign: "center", padding: "3rem 1rem", color: "#64748b" }}>
                            Loading diamonds...
                          </td>
                        </tr>
                      ) : diamonds.length === 0 ? (
                        <tr>
                          <td colSpan="10" style={{ textAlign: "center", padding: "3rem 1rem", color: "#64748b" }}>
                            No diamonds found.
                          </td>
                        </tr>
                      ) : (
                        diamonds.map((diamond) => (
                          <tr key={diamond.id}>
                            <td>
                              <code style={{ background: "#f1f5f9", padding: "2px 8px", borderRadius: "4px", fontSize: "0.8125rem", fontFamily: "monospace" }}>{diamond.skuCode}</code>
                            </td>
                            <td>
                              <span style={{ padding: "2px 8px", borderRadius: "4px", fontSize: "0.75rem", fontWeight: "500", background: "#dbeafe", color: "#1e40af" }}>
                                Diamond
                              </span>
                            </td>
                            <td>
                              <span style={{ fontWeight: "500" }}>{diamond.color} {diamond.clarity}</span>
                            </td>
                            <td>{diamond.caratWeight?.toFixed(2)} ct</td>
                            <td>{diamond.shape}</td>
                            <td><code style={{ fontSize: "0.8125rem" }}>{diamond.certNumber}</code></td>
                            <td>{diamond.manufacturerName || diamond.manufacturerCode || "—"}</td>
                            <td>
                              <span style={{
                                padding: "2px 8px",
                                borderRadius: "4px",
                                fontSize: "0.75rem",
                                fontWeight: "500",
                                background: diamond.status === "IN_STOCK" ? "#dcfce7" : "#f3f4f6",
                                color: diamond.status === "IN_STOCK" ? "#166534" : "#374151"
                              }}>
                                {diamond.status}
                              </span>
                            </td>
                            <td>{formatCurrency(diamond.totalPriceUsd)}</td>
                            <td>{formatDateTime(diamond.createdAt)}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="admin-card" style={{ padding: "1.5rem" }}>
      <div style={{ marginBottom: "1.5rem" }}>
        <h2 style={{ fontSize: "1rem", fontWeight: "600", marginBottom: "0.5rem", color: "#0f172a" }}>SKU Generator & Search</h2>
        <p style={{ fontSize: "0.875rem", color: "#64748b" }}>
          Generate standardized product SKUs for jewelry and packaging, then browse or search the catalog of existing SKUs.
        </p>
      </div>

      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1.5rem", borderBottom: "1px solid #e2e8f0" }}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            style={{
              padding: "0.75rem 1rem",
              background: "none",
              border: "none",
              borderBottom: activeTab === tab.id ? "2px solid #0f172a" : "2px solid transparent",
              color: activeTab === tab.id ? "#0f172a" : "#64748b",
              fontWeight: activeTab === tab.id ? "500" : "400",
              fontSize: "0.875rem",
              cursor: "pointer",
              transition: "all 0.2s"
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div>{renderTabContent()}</div>
    </div>
  );
};

export default SkuCodesManager;
