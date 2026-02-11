import { useState, useEffect } from "react";
import { rfidApi } from "@/services/labelApi";
import "@/components/pod-admin/PodAdminLayout.css";

const STATUS_COLORS = {
  ACTIVE: { bg: "#dcfce7", color: "#166534" },
  INACTIVE: { bg: "#fef3c7", color: "#92400e" },
  VOIDED: { bg: "#fee2e2", color: "#dc2626" },
};

export default function RFIDTags() {
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [statusFilter, setStatusFilter] = useState("");
  const [searchEPC, setSearchEPC] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Detail modal
  const [selectedTag, setSelectedTag] = useState(null);
  const [scanHistory, setScanHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Void modal
  const [voidModal, setVoidModal] = useState({ open: false, epc: null });
  const [voidReason, setVoidReason] = useState("");

  useEffect(() => {
    fetchTags();
  }, [currentPage, statusFilter]);

  const fetchTags = async () => {
    try {
      setLoading(true);
      const response = await rfidApi.getAllTags({
        page: currentPage,
        size: 20,
        status: statusFilter || undefined,
      });
      setTags(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setError(null);
    } catch (err) {
      console.error("Error fetching RFID tags:", err);
      setError("Failed to load RFID tags");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchEPC.trim()) {
      fetchTags();
      return;
    }

    try {
      setLoading(true);
      const response = await rfidApi.getTagByEPC(searchEPC.trim());
      setTags([response.data]);
      setTotalPages(1);
      setError(null);
    } catch (err) {
      console.error("Error searching tag:", err);
      if (err.response?.status === 404) {
        setTags([]);
        setError("No tag found with this EPC");
      } else {
        setError("Failed to search tag");
      }
    } finally {
      setLoading(false);
    }
  };

  const viewTagDetails = async (tag) => {
    setSelectedTag(tag);
    setLoadingHistory(true);
    try {
      const response = await rfidApi.getScanHistory(tag.epc, { size: 50 });
      setScanHistory(response.data.content || []);
    } catch (err) {
      console.error("Error fetching scan history:", err);
      setScanHistory([]);
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleVoid = async () => {
    if (!voidReason.trim()) {
      alert("Please enter a reason for voiding");
      return;
    }

    try {
      await rfidApi.voidTag(voidModal.epc, voidReason);
      setVoidModal({ open: false, epc: null });
      setVoidReason("");
      fetchTags();
      if (selectedTag?.epc === voidModal.epc) {
        setSelectedTag(null);
      }
    } catch (err) {
      console.error("Error voiding tag:", err);
      alert("Failed to void tag");
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

  if (loading && tags.length === 0) {
    return (
      <div className="pod-page">
        <div className="pod-loading">
          <div className="pod-loading-spinner" />
          <p>Loading RFID tags...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="pod-page">
      <div className="pod-page-header">
        <h1 className="pod-page-title">RFID Tags</h1>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button className="pod-btn pod-btn-secondary" onClick={fetchTags}>
            Refresh
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
        <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", alignItems: "center" }}>
          <input
            type="text"
            className="pod-input"
            placeholder="Search by EPC..."
            value={searchEPC}
            onChange={(e) => setSearchEPC(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            style={{ width: "300px" }}
          />
          <button className="pod-btn pod-btn-secondary" onClick={handleSearch}>
            Search
          </button>
          <select
            className="pod-input"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setSearchEPC("");
              setCurrentPage(0);
            }}
            style={{ width: "150px" }}
          >
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="VOIDED">Voided</option>
          </select>
          {searchEPC && (
            <button
              className="pod-btn"
              onClick={() => {
                setSearchEPC("");
                fetchTags();
              }}
            >
              Clear Search
            </button>
          )}
        </div>
      </div>

      {/* Tags Table */}
      <div className="pod-card" style={{ padding: 0, overflow: "hidden" }}>
        <table className="pod-table">
          <thead>
            <tr>
              <th>EPC</th>
              <th>Product</th>
              <th>Status</th>
              <th>Scan Count</th>
              <th>Last Scanned</th>
              <th>Encoded At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {tags.map((tag) => (
              <tr key={tag.id}>
                <td>
                  <code style={{ fontSize: "0.75rem", wordBreak: "break-all" }}>
                    {tag.epc}
                  </code>
                </td>
                <td>
                  {tag.product ? (
                    <div>
                      <div style={{ fontWeight: "500" }}>{tag.product.name}</div>
                      <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                        SKU: {tag.product.sku}
                      </div>
                    </div>
                  ) : (
                    <span style={{ color: "#9ca3af" }}>-</span>
                  )}
                </td>
                <td>
                  <span
                    style={{
                      display: "inline-block",
                      padding: "0.25rem 0.5rem",
                      borderRadius: "4px",
                      fontSize: "0.75rem",
                      background: STATUS_COLORS[tag.status]?.bg || "#f3f4f6",
                      color: STATUS_COLORS[tag.status]?.color || "#374151",
                    }}
                  >
                    {tag.status}
                  </span>
                </td>
                <td style={{ textAlign: "center" }}>{tag.scanCount || 0}</td>
                <td style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                  {formatDate(tag.lastScannedAt)}
                </td>
                <td style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                  {formatDate(tag.encodedAt)}
                </td>
                <td>
                  <div style={{ display: "flex", gap: "0.25rem" }}>
                    <button
                      className="pod-btn pod-btn-secondary"
                      style={{ padding: "0.25rem 0.5rem", fontSize: "0.75rem" }}
                      onClick={() => viewTagDetails(tag)}
                    >
                      Details
                    </button>
                    {tag.status === "ACTIVE" && (
                      <button
                        className="pod-btn"
                        style={{ padding: "0.25rem 0.5rem", fontSize: "0.75rem", color: "#dc2626" }}
                        onClick={() => setVoidModal({ open: true, epc: tag.epc })}
                      >
                        Void
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {tags.length === 0 && !loading && (
        <div className="pod-card" style={{ textAlign: "center", padding: "2rem" }}>
          <p style={{ color: "#6b7280" }}>No RFID tags found</p>
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

      {/* Tag Details Modal */}
      {selectedTag && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          onClick={() => setSelectedTag(null)}
        >
          <div
            className="pod-card"
            style={{ width: "600px", maxHeight: "80vh", overflow: "auto" }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "1rem" }}>
              <h2 style={{ margin: 0 }}>Tag Details</h2>
              <button className="pod-btn" onClick={() => setSelectedTag(null)}>
                &times;
              </button>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1.5rem" }}>
              <div>
                <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>EPC</label>
                <p style={{ margin: "0.25rem 0", wordBreak: "break-all" }}>
                  <code>{selectedTag.epc}</code>
                </p>
              </div>
              <div>
                <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>Status</label>
                <p style={{ margin: "0.25rem 0" }}>
                  <span
                    style={{
                      padding: "0.25rem 0.5rem",
                      borderRadius: "4px",
                      fontSize: "0.875rem",
                      background: STATUS_COLORS[selectedTag.status]?.bg,
                      color: STATUS_COLORS[selectedTag.status]?.color,
                    }}
                  >
                    {selectedTag.status}
                  </span>
                </p>
              </div>
              <div>
                <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>Product</label>
                <p style={{ margin: "0.25rem 0" }}>
                  {selectedTag.product?.name || "-"}
                  {selectedTag.product?.sku && (
                    <span style={{ color: "#6b7280" }}> ({selectedTag.product.sku})</span>
                  )}
                </p>
              </div>
              <div>
                <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>Scan Count</label>
                <p style={{ margin: "0.25rem 0" }}>{selectedTag.scanCount || 0}</p>
              </div>
              <div>
                <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>Encoded At</label>
                <p style={{ margin: "0.25rem 0" }}>{formatDate(selectedTag.encodedAt)}</p>
              </div>
              <div>
                <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>Last Scanned</label>
                <p style={{ margin: "0.25rem 0" }}>{formatDate(selectedTag.lastScannedAt)}</p>
              </div>
              {selectedTag.voidedAt && (
                <>
                  <div>
                    <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>Voided At</label>
                    <p style={{ margin: "0.25rem 0", color: "#dc2626" }}>
                      {formatDate(selectedTag.voidedAt)}
                    </p>
                  </div>
                  <div>
                    <label style={{ fontSize: "0.75rem", color: "#6b7280" }}>Void Reason</label>
                    <p style={{ margin: "0.25rem 0", color: "#dc2626" }}>
                      {selectedTag.voidedReason || "-"}
                    </p>
                  </div>
                </>
              )}
            </div>

            <h3 style={{ marginBottom: "0.5rem" }}>Scan History</h3>
            {loadingHistory ? (
              <p style={{ color: "#6b7280" }}>Loading...</p>
            ) : scanHistory.length === 0 ? (
              <p style={{ color: "#9ca3af" }}>No scan history</p>
            ) : (
              <div style={{ maxHeight: "200px", overflow: "auto" }}>
                <table style={{ width: "100%", fontSize: "0.75rem" }}>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Device</th>
                      <th>Location</th>
                      <th>Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {scanHistory.map((scan) => (
                      <tr key={scan.id}>
                        <td>{formatDate(scan.scannedAt)}</td>
                        <td>{scan.deviceName || scan.deviceId || "-"}</td>
                        <td>{scan.location || "-"}</td>
                        <td>{scan.scanType || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Void Modal */}
      {voidModal.open && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          onClick={() => setVoidModal({ open: false, epc: null })}
        >
          <div
            className="pod-card"
            style={{ width: "400px" }}
            onClick={(e) => e.stopPropagation()}
          >
            <h2 style={{ marginBottom: "1rem" }}>Void RFID Tag</h2>
            <p style={{ marginBottom: "1rem", color: "#6b7280" }}>
              Are you sure you want to void tag <code>{voidModal.epc}</code>?
              This action cannot be undone.
            </p>
            <label style={{ display: "block", marginBottom: "1rem" }}>
              <span style={{ fontSize: "0.875rem", color: "#374151" }}>Reason *</span>
              <textarea
                className="pod-input"
                rows={3}
                value={voidReason}
                onChange={(e) => setVoidReason(e.target.value)}
                placeholder="Enter reason for voiding this tag..."
                style={{ width: "100%", marginTop: "0.25rem" }}
              />
            </label>
            <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
              <button
                className="pod-btn pod-btn-secondary"
                onClick={() => setVoidModal({ open: false, epc: null })}
              >
                Cancel
              </button>
              <button
                className="pod-btn"
                style={{ background: "#dc2626", color: "white" }}
                onClick={handleVoid}
              >
                Void Tag
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
