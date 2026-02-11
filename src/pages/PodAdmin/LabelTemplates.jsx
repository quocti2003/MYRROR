import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { labelTemplateApi } from "@/services/labelApi";
import { ROUTES } from "@/constants/routes";
import "@/components/pod-admin/PodAdminLayout.css";

export default function LabelTemplates() {
  const navigate = useNavigate();
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchTemplates();
  }, [currentPage, statusFilter]);

  const fetchTemplates = async () => {
    try {
      setLoading(true);
      const response = await labelTemplateApi.getAll({
        page: currentPage,
        size: 20,
        status: statusFilter || undefined,
        search: searchQuery || undefined,
      });
      setTemplates(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setError(null);
    } catch (err) {
      console.error("Error fetching templates:", err);
      setError("Failed to load label templates");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setCurrentPage(0);
    fetchTemplates();
  };

  const handleDelete = async (id, name) => {
    if (!confirm(`Are you sure you want to delete template "${name}"?`)) return;

    try {
      await labelTemplateApi.delete(id);
      fetchTemplates();
    } catch (err) {
      console.error("Error deleting template:", err);
      alert("Failed to delete template");
    }
  };

  const handleDuplicate = async (id) => {
    try {
      await labelTemplateApi.duplicate(id);
      fetchTemplates();
    } catch (err) {
      console.error("Error duplicating template:", err);
      alert("Failed to duplicate template");
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  if (loading && templates.length === 0) {
    return (
      <div className="pod-page">
        <div className="pod-loading">
          <div className="pod-loading-spinner" />
          <p>Loading templates...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="pod-page">
      <div className="pod-page-header">
        <h1 className="pod-page-title">Label Templates</h1>
        <button
          className="pod-btn pod-btn-primary"
          onClick={() => navigate(ROUTES.LABEL_DESIGNER_NEW)}
        >
          + Create Template
        </button>
      </div>

      {error && (
        <div className="pod-card" style={{ background: "#fef2f2", borderColor: "#fecaca" }}>
          <p style={{ color: "#dc2626" }}>{error}</p>
        </div>
      )}

      {/* Filters */}
      <div className="pod-card" style={{ marginBottom: "1rem" }}>
        <form onSubmit={handleSearch} style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
          <input
            type="text"
            className="pod-input"
            placeholder="Search templates..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ flex: 1, minWidth: "200px" }}
          />
          <select
            className="pod-input"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setCurrentPage(0);
            }}
            style={{ width: "150px" }}
          >
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <button type="submit" className="pod-btn pod-btn-secondary">
            Search
          </button>
        </form>
      </div>

      {/* Templates Grid */}
      <div style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
        gap: "1rem"
      }}>
        {templates.map((template) => (
          <div key={template.id} className="pod-card" style={{ position: "relative" }}>
            {template.isDefault && (
              <span style={{
                position: "absolute",
                top: "0.5rem",
                right: "0.5rem",
                background: "#3b82f6",
                color: "white",
                padding: "0.25rem 0.5rem",
                borderRadius: "4px",
                fontSize: "0.75rem",
              }}>
                Default
              </span>
            )}

            {/* Preview Image */}
            <div style={{
              width: "100%",
              height: "150px",
              background: "#f3f4f6",
              borderRadius: "4px",
              marginBottom: "1rem",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              overflow: "hidden",
            }}>
              {template.previewUrl ? (
                <img
                  src={template.previewUrl}
                  alt={template.name}
                  style={{ maxWidth: "100%", maxHeight: "100%", objectFit: "contain" }}
                />
              ) : (
                <span style={{ color: "#9ca3af" }}>No Preview</span>
              )}
            </div>

            {/* Template Info */}
            <h3 style={{ marginBottom: "0.5rem" }}>{template.name}</h3>
            <p style={{ color: "#6b7280", fontSize: "0.875rem", marginBottom: "0.5rem" }}>
              {template.description || "No description"}
            </p>
            <div style={{ fontSize: "0.75rem", color: "#9ca3af", marginBottom: "1rem" }}>
              <div>{template.labelWidth}mm x {template.labelHeight}mm @ {template.dpi}DPI</div>
              <div>Created: {formatDate(template.createdAt)}</div>
            </div>

            {/* Status Badge */}
            <span style={{
              display: "inline-block",
              padding: "0.25rem 0.5rem",
              borderRadius: "4px",
              fontSize: "0.75rem",
              background: template.status === "ACTIVE" ? "#dcfce7" : "#f3f4f6",
              color: template.status === "ACTIVE" ? "#166534" : "#6b7280",
              marginBottom: "1rem",
            }}>
              {template.status}
            </span>

            {/* Actions */}
            <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
              <button
                className="pod-btn pod-btn-primary"
                style={{ flex: 1 }}
                onClick={() => navigate(ROUTES.LABEL_DESIGNER_EDIT.replace(":id", template.id))}
              >
                Edit
              </button>
              <button
                className="pod-btn pod-btn-secondary"
                onClick={() => handleDuplicate(template.id)}
              >
                Duplicate
              </button>
              <button
                className="pod-btn"
                style={{ color: "#dc2626" }}
                onClick={() => handleDelete(template.id, template.name)}
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      {templates.length === 0 && !loading && (
        <div className="pod-card" style={{ textAlign: "center", padding: "2rem" }}>
          <p style={{ color: "#6b7280" }}>No templates found</p>
          <button
            className="pod-btn pod-btn-primary"
            style={{ marginTop: "1rem" }}
            onClick={() => navigate(ROUTES.LABEL_DESIGNER_NEW)}
          >
            Create your first template
          </button>
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
    </div>
  );
}
