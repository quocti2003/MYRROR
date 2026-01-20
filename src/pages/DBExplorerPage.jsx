/**
 * =============================================================================
 * TRANG: DB Explorer
 * =============================================================================
 *
 * MỤC ĐÍCH:
 * - Cho phép user chọn Database → Table → Columns
 * - Thêm Computed Columns (ví dụ: qr_url = prefix + sku)
 * - Preview data
 * - Export ra CSV hoặc XLSX
 *
 * FLOW:
 * 1. Load danh sách databases từ API
 * 2. User chọn database → Load danh sách tables
 * 3. User chọn table → Load danh sách columns
 * 4. User tick chọn columns muốn export
 * 5. User thêm computed columns (optional)
 * 6. Preview data
 * 7. Export CSV/XLSX
 *
 * =============================================================================
 */

import React, { useState, useEffect } from "react";
import * as XLSX from "xlsx";
import "./DBExplorerPage.css";

// URL base của API (sẽ tự động dùng domain hiện tại khi deploy)
const API_BASE = "";

export default function DBExplorerPage() {
  // ======================== STATE ========================

  // Danh sách từ API
  const [databases, setDatabases] = useState([]);
  const [tables, setTables] = useState([]);
  const [columns, setColumns] = useState([]);

  // Giá trị đã chọn
  const [selectedDb, setSelectedDb] = useState("");
  const [selectedTable, setSelectedTable] = useState("");
  const [selectedColumns, setSelectedColumns] = useState([]); // Array of column names

  // Computed columns
  // Format: [{ name: "qr_url", prefix: "https://...", sourceColumn: "sku" }, ...]
  const [computedColumns, setComputedColumns] = useState([]);
  const [newComputedName, setNewComputedName] = useState("");
  const [newComputedPrefix, setNewComputedPrefix] = useState("https://test-beryl-five-56.vercel.app/");
  const [newComputedSource, setNewComputedSource] = useState("");

  // Data và Preview
  const [previewData, setPreviewData] = useState([]);
  const [totalRows, setTotalRows] = useState(0);

  // Loading states
  const [loadingDbs, setLoadingDbs] = useState(false);
  const [loadingTables, setLoadingTables] = useState(false);
  const [loadingColumns, setLoadingColumns] = useState(false);
  const [loadingData, setLoadingData] = useState(false);
  const [exporting, setExporting] = useState(false);

  // Error state
  const [error, setError] = useState(null);

  // Export format
  const [exportFormat, setExportFormat] = useState("xlsx");

  // ======================== EFFECTS ========================

  // Load databases khi component mount
  useEffect(() => {
    loadDatabases();
  }, []);

  // Load tables khi chọn database
  useEffect(() => {
    if (selectedDb) {
      loadTables(selectedDb);
      setSelectedTable("");
      setColumns([]);
      setSelectedColumns([]);
      setPreviewData([]);
    }
  }, [selectedDb]);

  // Load columns khi chọn table
  useEffect(() => {
    if (selectedDb && selectedTable) {
      loadColumns(selectedDb, selectedTable);
      setSelectedColumns([]);
      setPreviewData([]);
    }
  }, [selectedTable]);

  // ======================== API CALLS ========================

  /**
   * Load danh sách databases
   */
  const loadDatabases = async () => {
    setLoadingDbs(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/db/databases`);
      const data = await res.json();
      if (data.success) {
        setDatabases(data.databases);
      } else {
        setError(data.error || "Không thể load databases");
      }
    } catch (err) {
      setError("Lỗi kết nối: " + err.message);
    } finally {
      setLoadingDbs(false);
    }
  };

  /**
   * Load danh sách tables trong database
   */
  const loadTables = async (db) => {
    setLoadingTables(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/db/tables?db=${encodeURIComponent(db)}`);
      const data = await res.json();
      if (data.success) {
        setTables(data.tables);
      } else {
        setError(data.error || "Không thể load tables");
      }
    } catch (err) {
      setError("Lỗi kết nối: " + err.message);
    } finally {
      setLoadingTables(false);
    }
  };

  /**
   * Load danh sách columns của table
   */
  const loadColumns = async (db, table) => {
    setLoadingColumns(true);
    setError(null);
    try {
      const res = await fetch(
        `${API_BASE}/api/db/columns?db=${encodeURIComponent(db)}&table=${encodeURIComponent(table)}`
      );
      const data = await res.json();
      if (data.success) {
        setColumns(data.columns);
        // Mặc định chọn tất cả columns
        setSelectedColumns(data.columns.map((c) => c.name));
      } else {
        setError(data.error || "Không thể load columns");
      }
    } catch (err) {
      setError("Lỗi kết nối: " + err.message);
    } finally {
      setLoadingColumns(false);
    }
  };

  /**
   * Load preview data (10 rows)
   */
  const loadPreview = async () => {
    if (!selectedDb || !selectedTable || selectedColumns.length === 0) {
      setError("Vui lòng chọn database, table và ít nhất 1 column");
      return;
    }

    setLoadingData(true);
    setError(null);
    try {
      const columnsParam = selectedColumns.join(",");
      const res = await fetch(
        `${API_BASE}/api/db/data?db=${encodeURIComponent(selectedDb)}&table=${encodeURIComponent(
          selectedTable
        )}&columns=${encodeURIComponent(columnsParam)}&limit=10`
      );
      const data = await res.json();
      if (data.success) {
        // Thêm computed columns vào data
        const dataWithComputed = addComputedColumns(data.data);
        setPreviewData(dataWithComputed);
        setTotalRows(data.rowCount);
      } else {
        setError(data.error || "Không thể load data");
      }
    } catch (err) {
      setError("Lỗi kết nối: " + err.message);
    } finally {
      setLoadingData(false);
    }
  };

  // ======================== COMPUTED COLUMNS ========================

  /**
   * Thêm computed columns vào mỗi row của data
   * Ví dụ: qr_url = "https://..." + row.sku
   */
  const addComputedColumns = (data) => {
    if (computedColumns.length === 0) return data;

    return data.map((row) => {
      const newRow = { ...row };
      computedColumns.forEach((cc) => {
        // Lấy giá trị từ source column và nối với prefix
        const sourceValue = row[cc.sourceColumn] || "";
        newRow[cc.name] = cc.prefix + sourceValue;
      });
      return newRow;
    });
  };

  /**
   * Thêm computed column mới
   */
  const addComputedColumn = () => {
    if (!newComputedName.trim()) {
      setError("Vui lòng nhập tên cho computed column");
      return;
    }
    if (!newComputedSource) {
      setError("Vui lòng chọn source column");
      return;
    }

    // Kiểm tra tên đã tồn tại chưa
    if (
      computedColumns.some((cc) => cc.name === newComputedName) ||
      columns.some((c) => c.name === newComputedName)
    ) {
      setError("Tên column đã tồn tại");
      return;
    }

    setComputedColumns([
      ...computedColumns,
      {
        name: newComputedName.trim(),
        prefix: newComputedPrefix,
        sourceColumn: newComputedSource,
      },
    ]);

    // Reset form
    setNewComputedName("");
    setError(null);
  };

  /**
   * Xóa computed column
   */
  const removeComputedColumn = (name) => {
    setComputedColumns(computedColumns.filter((cc) => cc.name !== name));
  };

  // ======================== COLUMN SELECTION ========================

  /**
   * Toggle chọn/bỏ chọn 1 column
   */
  const toggleColumn = (columnName) => {
    if (selectedColumns.includes(columnName)) {
      setSelectedColumns(selectedColumns.filter((c) => c !== columnName));
    } else {
      setSelectedColumns([...selectedColumns, columnName]);
    }
  };

  /**
   * Chọn tất cả / Bỏ chọn tất cả
   */
  const toggleSelectAll = () => {
    if (selectedColumns.length === columns.length) {
      setSelectedColumns([]);
    } else {
      setSelectedColumns(columns.map((c) => c.name));
    }
  };

  // ======================== EXPORT ========================

  /**
   * Export data ra file CSV hoặc XLSX
   */
  const exportData = async () => {
    if (!selectedDb || !selectedTable || selectedColumns.length === 0) {
      setError("Vui lòng chọn database, table và ít nhất 1 column");
      return;
    }

    setExporting(true);
    setError(null);

    try {
      // Lấy toàn bộ data (không limit)
      const columnsParam = selectedColumns.join(",");
      const res = await fetch(
        `${API_BASE}/api/db/data?db=${encodeURIComponent(selectedDb)}&table=${encodeURIComponent(
          selectedTable
        )}&columns=${encodeURIComponent(columnsParam)}`
      );
      const data = await res.json();

      if (!data.success) {
        setError(data.error || "Không thể lấy data");
        return;
      }

      // Thêm computed columns
      const dataWithComputed = addComputedColumns(data.data);

      // Tạo headers (columns gốc + computed columns)
      const headers = [...selectedColumns, ...computedColumns.map((cc) => cc.name)];

      // Tạo workbook
      const worksheet = XLSX.utils.json_to_sheet(dataWithComputed, { header: headers });
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, selectedTable);

      // Tên file
      const filename = `${selectedDb}_${selectedTable}_${new Date().toISOString().split("T")[0]}`;

      // Export theo format đã chọn
      if (exportFormat === "csv") {
        XLSX.writeFile(workbook, `${filename}.csv`, { bookType: "csv" });
      } else {
        XLSX.writeFile(workbook, `${filename}.xlsx`, { bookType: "xlsx" });
      }
    } catch (err) {
      setError("Lỗi export: " + err.message);
    } finally {
      setExporting(false);
    }
  };

  // ======================== RENDER ========================

  return (
    <div className="db-explorer-page">
      <div className="db-explorer-container">
        {/* Header */}
        <div className="db-explorer-header">
          <h1>DB Explorer</h1>
          <p>Chọn Database → Table → Columns → Export CSV/XLSX</p>
        </div>

        {/* Error message */}
        {error && (
          <div className="db-explorer-error">
            <span>{error}</span>
            <button onClick={() => setError(null)}>×</button>
          </div>
        )}

        {/* Selection Section */}
        <div className="db-explorer-selection">
          {/* Database Dropdown */}
          <div className="db-explorer-field">
            <label>Database:</label>
            <select
              value={selectedDb}
              onChange={(e) => setSelectedDb(e.target.value)}
              disabled={loadingDbs}
            >
              <option value="">-- Chọn Database --</option>
              {databases.map((db) => (
                <option key={db} value={db}>
                  {db}
                </option>
              ))}
            </select>
            {loadingDbs && <span className="loading-indicator">Loading...</span>}
          </div>

          {/* Table Dropdown */}
          <div className="db-explorer-field">
            <label>Table:</label>
            <select
              value={selectedTable}
              onChange={(e) => setSelectedTable(e.target.value)}
              disabled={!selectedDb || loadingTables}
            >
              <option value="">-- Chọn Table --</option>
              {tables.map((table) => (
                <option key={table} value={table}>
                  {table}
                </option>
              ))}
            </select>
            {loadingTables && <span className="loading-indicator">Loading...</span>}
          </div>
        </div>

        {/* Columns Section */}
        {columns.length > 0 && (
          <div className="db-explorer-columns">
            <div className="columns-header">
              <h3>Columns</h3>
              <label className="select-all">
                <input
                  type="checkbox"
                  checked={selectedColumns.length === columns.length}
                  onChange={toggleSelectAll}
                />
                Chọn tất cả
              </label>
            </div>
            {loadingColumns ? (
              <p>Loading columns...</p>
            ) : (
              <div className="columns-list">
                {columns.map((col) => (
                  <label key={col.name} className="column-item">
                    <input
                      type="checkbox"
                      checked={selectedColumns.includes(col.name)}
                      onChange={() => toggleColumn(col.name)}
                    />
                    <span className="column-name">{col.name}</span>
                    <span className="column-type">{col.type}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Computed Columns Section */}
        {columns.length > 0 && (
          <div className="db-explorer-computed">
            <h3>Computed Columns</h3>
            <p className="computed-hint">
              Tạo cột mới bằng cách nối prefix với giá trị của cột có sẵn
            </p>

            {/* Form thêm computed column */}
            <div className="computed-form">
              <input
                type="text"
                placeholder="Tên cột mới (vd: qr_url)"
                value={newComputedName}
                onChange={(e) => setNewComputedName(e.target.value)}
              />
              <input
                type="text"
                placeholder="Prefix (vd: https://...)"
                value={newComputedPrefix}
                onChange={(e) => setNewComputedPrefix(e.target.value)}
              />
              <span className="computed-plus">+</span>
              <select
                value={newComputedSource}
                onChange={(e) => setNewComputedSource(e.target.value)}
              >
                <option value="">-- Chọn cột --</option>
                {columns.map((col) => (
                  <option key={col.name} value={col.name}>
                    {col.name}
                  </option>
                ))}
              </select>
              <button onClick={addComputedColumn} className="btn-add">
                Thêm
              </button>
            </div>

            {/* Danh sách computed columns đã tạo */}
            {computedColumns.length > 0 && (
              <div className="computed-list">
                <table>
                  <thead>
                    <tr>
                      <th>Tên cột</th>
                      <th>Công thức</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {computedColumns.map((cc) => (
                      <tr key={cc.name}>
                        <td>{cc.name}</td>
                        <td>
                          "{cc.prefix}" + {cc.sourceColumn}
                        </td>
                        <td>
                          <button
                            onClick={() => removeComputedColumn(cc.name)}
                            className="btn-remove"
                          >
                            Xóa
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Preview Section */}
        {selectedColumns.length > 0 && (
          <div className="db-explorer-preview">
            <div className="preview-header">
              <h3>Preview</h3>
              <button onClick={loadPreview} disabled={loadingData} className="btn-preview">
                {loadingData ? "Loading..." : "Load Preview (10 rows)"}
              </button>
            </div>

            {previewData.length > 0 && (
              <>
                <p className="preview-info">
                  Hiển thị {previewData.length} / {totalRows} rows
                </p>
                <div className="preview-table-wrapper">
                  <table className="preview-table">
                    <thead>
                      <tr>
                        {selectedColumns.map((col) => (
                          <th key={col}>{col}</th>
                        ))}
                        {computedColumns.map((cc) => (
                          <th key={cc.name} className="computed-header">
                            {cc.name}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {previewData.map((row, idx) => (
                        <tr key={idx}>
                          {selectedColumns.map((col) => (
                            <td key={col}>{String(row[col] ?? "")}</td>
                          ))}
                          {computedColumns.map((cc) => (
                            <td key={cc.name} className="computed-cell">
                              {row[cc.name]}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        )}

        {/* Export Section */}
        {selectedColumns.length > 0 && (
          <div className="db-explorer-export">
            <h3>Export</h3>
            <div className="export-options">
              <label>
                <input
                  type="radio"
                  name="format"
                  value="xlsx"
                  checked={exportFormat === "xlsx"}
                  onChange={(e) => setExportFormat(e.target.value)}
                />
                XLSX (Excel)
              </label>
              <label>
                <input
                  type="radio"
                  name="format"
                  value="csv"
                  checked={exportFormat === "csv"}
                  onChange={(e) => setExportFormat(e.target.value)}
                />
                CSV
              </label>
            </div>
            <button onClick={exportData} disabled={exporting} className="btn-export">
              {exporting ? "Exporting..." : `Export ${exportFormat.toUpperCase()}`}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
