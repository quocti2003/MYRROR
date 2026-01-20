import React, { useState, useEffect, useCallback } from "react";
import { warehouseAPI, handleAPIError } from "@services/api";
import "./WarehouseManagement.css";

const WarehouseManagement = () => {
  // View state
  const [activeView, setActiveView] = useState("warehouses");

  // Data state
  const [warehouses, setWarehouses] = useState([]);
  const [selectedWarehouse, setSelectedWarehouse] = useState(null);
  const [racks, setRacks] = useState([]);
  const [selectedRack, setSelectedRack] = useState(null);
  const [slots, setSlots] = useState([]);
  const [pendingPositions, setPendingPositions] = useState([]);
  const [stats, setStats] = useState(null);

  // UI state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");

  // Modal state
  const [warehouseModal, setWarehouseModal] = useState({ open: false, editing: null });
  const [rackModal, setRackModal] = useState({ open: false, editing: null });
  const [slotBatchModal, setSlotBatchModal] = useState({ open: false });
  const [assignModal, setAssignModal] = useState({ open: false, position: null });

  // Form state
  const [warehouseForm, setWarehouseForm] = useState({
    name: "", address: "", city: "", capacity: 1000, status: "ACTIVE"
  });
  const [rackForm, setRackForm] = useState({
    rackCode: "", rackType: "STANDARD", maxSlots: 50, description: ""
  });
  const [slotBatchForm, setSlotBatchForm] = useState({
    prefix: "S", count: 10
  });
  const [assignForm, setAssignForm] = useState({
    warehouseId: "", rackId: "", slotId: ""
  });

  // Fetch warehouses
  const fetchWarehouses = useCallback(async () => {
    setLoading(true);
    try {
      const response = await warehouseAPI.getAll();
      setWarehouses(response.data || []);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load warehouses");
      setError(errorInfo.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch racks for a warehouse
  const fetchRacks = useCallback(async (warehouseId) => {
    if (!warehouseId) {
      setRacks([]);
      return;
    }
    try {
      const response = await warehouseAPI.getRacks(warehouseId);
      setRacks(response.data || []);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load racks");
      setError(errorInfo.message);
    }
  }, []);

  // Fetch slots for a rack
  const fetchSlots = useCallback(async (rackId) => {
    if (!rackId) {
      setSlots([]);
      return;
    }
    try {
      const response = await warehouseAPI.getSlots(rackId);
      setSlots(response.data || []);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load slots");
      setError(errorInfo.message);
    }
  }, []);

  // Fetch pending positions
  const fetchPendingPositions = useCallback(async () => {
    try {
      const response = await warehouseAPI.getPending({});
      // Handle both array response and paginated response with .content
      const data = response.data;
      if (Array.isArray(data)) {
        setPendingPositions(data);
      } else if (data && Array.isArray(data.content)) {
        setPendingPositions(data.content);
      } else {
        setPendingPositions([]);
      }
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load pending positions");
      setError(errorInfo.message);
      setPendingPositions([]);
    }
  }, []);

  // Fetch stats
  const fetchStats = useCallback(async () => {
    try {
      const response = await warehouseAPI.getStats();
      setStats(response.data);
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to load stats");
      setError(errorInfo.message);
    }
  }, []);

  // Initial load
  useEffect(() => {
    fetchWarehouses();
  }, [fetchWarehouses]);

  // Load racks when warehouse is selected
  useEffect(() => {
    if (selectedWarehouse) {
      fetchRacks(selectedWarehouse.id);
    } else {
      setRacks([]);
      setSelectedRack(null);
      setSlots([]);
    }
  }, [selectedWarehouse, fetchRacks]);

  // Load slots when rack is selected
  useEffect(() => {
    if (selectedRack) {
      fetchSlots(selectedRack.id);
    } else {
      setSlots([]);
    }
  }, [selectedRack, fetchSlots]);

  // Load data based on active view
  useEffect(() => {
    if (activeView === "positions") {
      fetchPendingPositions();
    } else if (activeView === "stats") {
      fetchStats();
    }
  }, [activeView, fetchPendingPositions, fetchStats]);

  // Warehouse CRUD
  const handleWarehouseSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      if (warehouseModal.editing) {
        await warehouseAPI.update(warehouseModal.editing.id, warehouseForm);
      } else {
        await warehouseAPI.create(warehouseForm);
      }
      await fetchWarehouses();
      setWarehouseModal({ open: false, editing: null });
      resetWarehouseForm();
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to save warehouse");
      setError(errorInfo.message);
    }
  };

  const handleDeleteWarehouse = async (id) => {
    if (!window.confirm("Are you sure you want to delete this warehouse?")) return;
    try {
      await warehouseAPI.delete(id);
      await fetchWarehouses();
      if (selectedWarehouse?.id === id) {
        setSelectedWarehouse(null);
      }
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to delete warehouse");
      setError(errorInfo.message);
    }
  };

  const resetWarehouseForm = () => {
    setWarehouseForm({ name: "", address: "", city: "", capacity: 1000, status: "ACTIVE" });
  };

  // Rack CRUD
  const handleRackSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      if (rackModal.editing) {
        await warehouseAPI.updateRack(rackModal.editing.id, rackForm);
      } else {
        await warehouseAPI.createRack(selectedWarehouse.id, rackForm);
      }
      await fetchRacks(selectedWarehouse.id);
      setRackModal({ open: false, editing: null });
      resetRackForm();
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to save rack");
      setError(errorInfo.message);
    }
  };

  const handleDeleteRack = async (id) => {
    if (!window.confirm("Are you sure you want to delete this rack?")) return;
    try {
      await warehouseAPI.deleteRack(id);
      await fetchRacks(selectedWarehouse.id);
      if (selectedRack?.id === id) {
        setSelectedRack(null);
      }
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to delete rack");
      setError(errorInfo.message);
    }
  };

  const resetRackForm = () => {
    setRackForm({ rackCode: "", rackType: "STANDARD", maxSlots: 50, description: "" });
  };

  // Slot batch creation
  const handleSlotBatchSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await warehouseAPI.createSlotsBatch(selectedRack.id, slotBatchForm);
      await fetchSlots(selectedRack.id);
      await fetchRacks(selectedWarehouse.id); // Refresh rack to update slot count
      setSlotBatchModal({ open: false });
      setSlotBatchForm({ prefix: "S", count: 10 });
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to create slots");
      setError(errorInfo.message);
    }
  };

  // Position assignment
  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await warehouseAPI.assignPosition({
        productId: assignModal.position.productId,
        slotId: parseInt(assignForm.slotId)
      });
      await fetchPendingPositions();
      setAssignModal({ open: false, position: null });
      setAssignForm({ warehouseId: "", rackId: "", slotId: "" });
    } catch (err) {
      const errorInfo = handleAPIError(err, "Failed to assign position");
      setError(errorInfo.message);
    }
  };

  // Filter warehouses
  const filteredWarehouses = warehouses.filter(w =>
    w.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    w.address?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    w.city?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Render Views
  const renderWarehousesView = () => (
    <div>
      {/* Controls */}
      <div className="wm-controls">
        <input
          type="text"
          placeholder="Search warehouses..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="admin-input"
          style={{ flex: 1, maxWidth: 400 }}
        />
        <button
          onClick={() => {
            resetWarehouseForm();
            setWarehouseModal({ open: true, editing: null });
          }}
          className="admin-button admin-button-primary"
        >
          Add Warehouse
        </button>
      </div>

      {/* Warehouse Grid */}
      <div className="wm-grid">
        {filteredWarehouses.map(warehouse => (
          <div key={warehouse.id} className="wm-card">
            <div className="wm-card-header">
              <h3>{warehouse.name}</h3>
              <span className={`status-pill status-${warehouse.status?.toLowerCase() || 'active'}`}>
                {warehouse.status || 'ACTIVE'}
              </span>
            </div>
            <div className="wm-card-body">
              <div className="wm-detail">
                <span className="wm-label">Address:</span>
                <span>{warehouse.address}, {warehouse.city}</span>
              </div>
              <div className="wm-detail">
                <span className="wm-label">Capacity:</span>
                <span>{warehouse.capacity?.toLocaleString() || 0} items</span>
              </div>
              <div className="wm-stats-row">
                <div className="wm-stat">
                  <span className="wm-stat-value">{warehouse.rackCount || 0}</span>
                  <span className="wm-stat-label">Racks</span>
                </div>
                <div className="wm-stat">
                  <span className="wm-stat-value">{warehouse.productCount || 0}</span>
                  <span className="wm-stat-label">Products</span>
                </div>
              </div>
            </div>
            <div className="wm-card-actions">
              <button
                onClick={() => {
                  setWarehouseForm({
                    name: warehouse.name || "",
                    address: warehouse.address || "",
                    city: warehouse.city || "",
                    capacity: warehouse.capacity || 1000,
                    status: warehouse.status || "ACTIVE"
                  });
                  setWarehouseModal({ open: true, editing: warehouse });
                }}
                className="admin-button admin-button-outline"
              >
                Edit
              </button>
              <button
                onClick={() => {
                  setSelectedWarehouse(warehouse);
                  setActiveView("racks");
                }}
                className="admin-button admin-button-secondary"
              >
                Manage Racks
              </button>
              <button
                onClick={() => handleDeleteWarehouse(warehouse.id)}
                className="admin-button admin-button-danger"
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      {filteredWarehouses.length === 0 && (
        <div className="admin-empty-state">
          No warehouses found. {searchTerm ? "Try adjusting your search." : "Add your first warehouse to get started."}
        </div>
      )}
    </div>
  );

  const renderRacksView = () => (
    <div>
      {/* Breadcrumb */}
      <div className="wm-breadcrumb">
        <button onClick={() => { setActiveView("warehouses"); setSelectedWarehouse(null); }} className="wm-breadcrumb-link">
          Warehouses
        </button>
        <span className="wm-breadcrumb-sep">/</span>
        <span>{selectedWarehouse?.name || "Select Warehouse"}</span>
      </div>

      {!selectedWarehouse ? (
        <div className="admin-empty-state">
          <p>Select a warehouse to manage its racks and slots.</p>
          <select
            className="admin-select"
            value=""
            onChange={(e) => {
              const wh = warehouses.find(w => w.id === parseInt(e.target.value));
              setSelectedWarehouse(wh);
            }}
          >
            <option value="">-- Select Warehouse --</option>
            {warehouses.map(w => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>
        </div>
      ) : (
        <>
          {/* Controls */}
          <div className="wm-controls">
            <select
              className="admin-select"
              value={selectedWarehouse?.id || ""}
              onChange={(e) => {
                const wh = warehouses.find(w => w.id === parseInt(e.target.value));
                setSelectedWarehouse(wh);
                setSelectedRack(null);
              }}
              style={{ width: 250 }}
            >
              {warehouses.map(w => (
                <option key={w.id} value={w.id}>{w.name}</option>
              ))}
            </select>
            <button
              onClick={() => {
                resetRackForm();
                setRackModal({ open: true, editing: null });
              }}
              className="admin-button admin-button-primary"
            >
              Add Rack
            </button>
          </div>

          {/* Racks Table */}
          <div className="wm-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Rack Code</th>
                  <th>Type</th>
                  <th>Slots</th>
                  <th>Description</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {racks.map(rack => (
                  <React.Fragment key={rack.id}>
                    <tr className={selectedRack?.id === rack.id ? "selected" : ""}>
                      <td>
                        <button
                          className="wm-rack-expand"
                          onClick={() => setSelectedRack(selectedRack?.id === rack.id ? null : rack)}
                        >
                          {selectedRack?.id === rack.id ? "▼" : "▶"} {rack.rackCode}
                        </button>
                      </td>
                      <td>{rack.rackType}</td>
                      <td>{rack.currentSlotCount || 0} / {rack.maxSlots}</td>
                      <td>{rack.description || "-"}</td>
                      <td>
                        <div className="wm-action-buttons">
                          <button
                            onClick={() => {
                              setRackForm({
                                rackCode: rack.rackCode || "",
                                rackType: rack.rackType || "STANDARD",
                                maxSlots: rack.maxSlots || 50,
                                description: rack.description || ""
                              });
                              setRackModal({ open: true, editing: rack });
                            }}
                            className="admin-button admin-button-outline admin-button-sm"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteRack(rack.id)}
                            className="admin-button admin-button-danger admin-button-sm"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                    {/* Expanded Slots */}
                    {selectedRack?.id === rack.id && (
                      <tr className="wm-slots-row">
                        <td colSpan="5">
                          <div className="wm-slots-container">
                            <div className="wm-slots-header">
                              <strong>Slots in {rack.rackCode}</strong>
                              <button
                                onClick={() => setSlotBatchModal({ open: true })}
                                className="admin-button admin-button-secondary admin-button-sm"
                              >
                                + Add Slots
                              </button>
                            </div>
                            <div className="wm-slots-grid">
                              {slots.length === 0 ? (
                                <p className="wm-empty-slots">No slots yet. Add slots to this rack.</p>
                              ) : (
                                slots.map(slot => (
                                  <div
                                    key={slot.id}
                                    className={`wm-slot ${slot.isOccupied ? 'occupied' : 'available'}`}
                                    title={slot.isOccupied ? `Occupied by: ${slot.productSku || 'Unknown'}` : 'Available'}
                                  >
                                    {slot.slotCode}
                                  </div>
                                ))
                              )}
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
            {racks.length === 0 && (
              <div className="admin-empty-state">
                No racks in this warehouse. Add your first rack.
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );

  const renderPositionsView = () => (
    <div>
      <div className="wm-controls">
        <h3 style={{ margin: 0 }}>Pending Position Assignments</h3>
        <button onClick={fetchPendingPositions} className="admin-button admin-button-outline">
          Refresh
        </button>
      </div>

      <div className="wm-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Product SKU</th>
              <th>Product Name</th>
              <th>Current Location</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {pendingPositions.map(pos => (
              <tr key={pos.id}>
                <td><code>{pos.productSku}</code></td>
                <td>{pos.productName || "-"}</td>
                <td>{pos.currentLocation || "Unassigned"}</td>
                <td>
                  <button
                    onClick={() => {
                      setAssignForm({ warehouseId: "", rackId: "", slotId: "" });
                      setAssignModal({ open: true, position: pos });
                    }}
                    className="admin-button admin-button-primary admin-button-sm"
                  >
                    Assign Location
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {pendingPositions.length === 0 && (
          <div className="admin-empty-state">
            No pending position assignments. All products have assigned locations.
          </div>
        )}
      </div>
    </div>
  );

  const renderStatsView = () => (
    <div>
      <div className="wm-controls">
        <h3 style={{ margin: 0 }}>Warehouse Statistics</h3>
        <button onClick={fetchStats} className="admin-button admin-button-outline">
          Refresh
        </button>
      </div>

      {stats ? (
        <div className="wm-stats-grid">
          <div className="wm-stats-card">
            <div className="wm-stats-card-value">{stats.totalWarehouses || 0}</div>
            <div className="wm-stats-card-label">Total Warehouses</div>
          </div>
          <div className="wm-stats-card">
            <div className="wm-stats-card-value">{stats.totalRacks || 0}</div>
            <div className="wm-stats-card-label">Total Racks</div>
          </div>
          <div className="wm-stats-card">
            <div className="wm-stats-card-value">{stats.totalSlots || 0}</div>
            <div className="wm-stats-card-label">Total Slots</div>
          </div>
          <div className="wm-stats-card">
            <div className="wm-stats-card-value">{stats.occupiedSlots || 0}</div>
            <div className="wm-stats-card-label">Occupied Slots</div>
          </div>
          <div className="wm-stats-card">
            <div className="wm-stats-card-value">
              {stats.totalSlots > 0 ? Math.round((stats.occupiedSlots / stats.totalSlots) * 100) : 0}%
            </div>
            <div className="wm-stats-card-label">Utilization</div>
          </div>
          <div className="wm-stats-card">
            <div className="wm-stats-card-value">{stats.pendingPlacements || 0}</div>
            <div className="wm-stats-card-label">Pending Placements</div>
          </div>
        </div>
      ) : (
        <div className="admin-empty-state">Loading statistics...</div>
      )}
    </div>
  );

  // Loading state
  if (loading && warehouses.length === 0) {
    return (
      <div className="admin-empty-state">
        <div>Loading warehouse data...</div>
      </div>
    );
  }

  return (
    <div className="admin-card wm-container">
      {error && (
        <div className="admin-error-state" style={{ marginBottom: "1rem" }}>
          {error}
          <button onClick={() => setError(null)} style={{ marginLeft: "1rem" }}>Dismiss</button>
        </div>
      )}

      {/* View Tabs */}
      <div className="wm-tabs">
        <button
          className={`wm-tab ${activeView === "warehouses" ? "active" : ""}`}
          onClick={() => setActiveView("warehouses")}
        >
          Warehouses
        </button>
        <button
          className={`wm-tab ${activeView === "racks" ? "active" : ""}`}
          onClick={() => setActiveView("racks")}
        >
          Racks & Slots
        </button>
        <button
          className={`wm-tab ${activeView === "positions" ? "active" : ""}`}
          onClick={() => setActiveView("positions")}
        >
          Positions
        </button>
        <button
          className={`wm-tab ${activeView === "stats" ? "active" : ""}`}
          onClick={() => setActiveView("stats")}
        >
          Stats
        </button>
      </div>

      {/* View Content */}
      <div className="wm-content">
        {activeView === "warehouses" && renderWarehousesView()}
        {activeView === "racks" && renderRacksView()}
        {activeView === "positions" && renderPositionsView()}
        {activeView === "stats" && renderStatsView()}
      </div>

      {/* Warehouse Modal */}
      {warehouseModal.open && (
        <div className="admin-modal-overlay" onClick={() => setWarehouseModal({ open: false, editing: null })}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: "1.5rem" }}>
              <h2 className="wm-modal-title">
                {warehouseModal.editing ? "Edit Warehouse" : "Add New Warehouse"}
              </h2>
              <form onSubmit={handleWarehouseSubmit}>
                <div className="wm-form-grid">
                  <div className="wm-form-field">
                    <label>Warehouse Name *</label>
                    <input
                      type="text"
                      value={warehouseForm.name}
                      onChange={(e) => setWarehouseForm({ ...warehouseForm, name: e.target.value })}
                      className="admin-input"
                      required
                    />
                  </div>
                  <div className="wm-form-field">
                    <label>Status</label>
                    <select
                      value={warehouseForm.status}
                      onChange={(e) => setWarehouseForm({ ...warehouseForm, status: e.target.value })}
                      className="admin-select"
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                    </select>
                  </div>
                  <div className="wm-form-field wm-form-full">
                    <label>Address *</label>
                    <input
                      type="text"
                      value={warehouseForm.address}
                      onChange={(e) => setWarehouseForm({ ...warehouseForm, address: e.target.value })}
                      className="admin-input"
                      required
                    />
                  </div>
                  <div className="wm-form-field">
                    <label>City</label>
                    <input
                      type="text"
                      value={warehouseForm.city}
                      onChange={(e) => setWarehouseForm({ ...warehouseForm, city: e.target.value })}
                      className="admin-input"
                    />
                  </div>
                  <div className="wm-form-field">
                    <label>Capacity</label>
                    <input
                      type="number"
                      value={warehouseForm.capacity}
                      onChange={(e) => setWarehouseForm({ ...warehouseForm, capacity: parseInt(e.target.value) || 0 })}
                      className="admin-input"
                      min="0"
                    />
                  </div>
                </div>
                <div className="wm-modal-actions">
                  <button
                    type="button"
                    onClick={() => setWarehouseModal({ open: false, editing: null })}
                    className="admin-button admin-button-secondary"
                  >
                    Cancel
                  </button>
                  <button type="submit" className="admin-button admin-button-primary">
                    {warehouseModal.editing ? "Update" : "Create"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Rack Modal */}
      {rackModal.open && (
        <div className="admin-modal-overlay" onClick={() => setRackModal({ open: false, editing: null })}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: "1.5rem" }}>
              <h2 className="wm-modal-title">
                {rackModal.editing ? "Edit Rack" : "Add New Rack"}
              </h2>
              <form onSubmit={handleRackSubmit}>
                <div className="wm-form-grid">
                  <div className="wm-form-field">
                    <label>Rack Code *</label>
                    <input
                      type="text"
                      value={rackForm.rackCode}
                      onChange={(e) => setRackForm({ ...rackForm, rackCode: e.target.value })}
                      className="admin-input"
                      placeholder="e.g., R-001"
                      required
                    />
                  </div>
                  <div className="wm-form-field">
                    <label>Rack Type</label>
                    <select
                      value={rackForm.rackType}
                      onChange={(e) => setRackForm({ ...rackForm, rackType: e.target.value })}
                      className="admin-select"
                    >
                      <option value="STANDARD">Standard</option>
                      <option value="HIGH_VALUE">High Value</option>
                      <option value="COLD_STORAGE">Cold Storage</option>
                    </select>
                  </div>
                  <div className="wm-form-field">
                    <label>Max Slots</label>
                    <input
                      type="number"
                      value={rackForm.maxSlots}
                      onChange={(e) => setRackForm({ ...rackForm, maxSlots: parseInt(e.target.value) || 0 })}
                      className="admin-input"
                      min="1"
                    />
                  </div>
                  <div className="wm-form-field">
                    <label>Description</label>
                    <input
                      type="text"
                      value={rackForm.description}
                      onChange={(e) => setRackForm({ ...rackForm, description: e.target.value })}
                      className="admin-input"
                    />
                  </div>
                </div>
                <div className="wm-modal-actions">
                  <button
                    type="button"
                    onClick={() => setRackModal({ open: false, editing: null })}
                    className="admin-button admin-button-secondary"
                  >
                    Cancel
                  </button>
                  <button type="submit" className="admin-button admin-button-primary">
                    {rackModal.editing ? "Update" : "Create"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Slot Batch Modal */}
      {slotBatchModal.open && (
        <div className="admin-modal-overlay" onClick={() => setSlotBatchModal({ open: false })}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: "1.5rem" }}>
              <h2 className="wm-modal-title">Create Slots</h2>
              <form onSubmit={handleSlotBatchSubmit}>
                <div className="wm-form-grid">
                  <div className="wm-form-field">
                    <label>Prefix *</label>
                    <input
                      type="text"
                      value={slotBatchForm.prefix}
                      onChange={(e) => setSlotBatchForm({ ...slotBatchForm, prefix: e.target.value })}
                      className="admin-input"
                      placeholder="e.g., S"
                      required
                    />
                    <small className="wm-form-help">Slots will be named: {slotBatchForm.prefix}-001, {slotBatchForm.prefix}-002, etc.</small>
                  </div>
                  <div className="wm-form-field">
                    <label>Number of Slots *</label>
                    <input
                      type="number"
                      value={slotBatchForm.count}
                      onChange={(e) => setSlotBatchForm({ ...slotBatchForm, count: parseInt(e.target.value) || 1 })}
                      className="admin-input"
                      min="1"
                      max="100"
                      required
                    />
                  </div>
                </div>
                <div className="wm-modal-actions">
                  <button
                    type="button"
                    onClick={() => setSlotBatchModal({ open: false })}
                    className="admin-button admin-button-secondary"
                  >
                    Cancel
                  </button>
                  <button type="submit" className="admin-button admin-button-primary">
                    Create {slotBatchForm.count} Slots
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Assign Position Modal */}
      {assignModal.open && (
        <AssignPositionModal
          position={assignModal.position}
          warehouses={warehouses}
          onClose={() => setAssignModal({ open: false, position: null })}
          onAssign={handleAssignSubmit}
          assignForm={assignForm}
          setAssignForm={setAssignForm}
        />
      )}
    </div>
  );
};

// Separate component for assign modal to handle cascading dropdowns
const AssignPositionModal = ({ position, warehouses, onClose, onAssign, assignForm, setAssignForm }) => {
  const [racks, setRacks] = useState([]);
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(false);

  // Fetch racks when warehouse changes
  useEffect(() => {
    if (assignForm.warehouseId) {
      setLoading(true);
      warehouseAPI.getRacks(assignForm.warehouseId)
        .then(res => {
          setRacks(res.data || []);
          setAssignForm(f => ({ ...f, rackId: "", slotId: "" }));
        })
        .catch(() => setRacks([]))
        .finally(() => setLoading(false));
    } else {
      setRacks([]);
      setSlots([]);
    }
  }, [assignForm.warehouseId, setAssignForm]);

  // Fetch slots when rack changes
  useEffect(() => {
    if (assignForm.rackId) {
      setLoading(true);
      warehouseAPI.getSlots(assignForm.rackId)
        .then(res => {
          setSlots((res.data || []).filter(s => !s.isOccupied));
          setAssignForm(f => ({ ...f, slotId: "" }));
        })
        .catch(() => setSlots([]))
        .finally(() => setLoading(false));
    } else {
      setSlots([]);
    }
  }, [assignForm.rackId, setAssignForm]);

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <div style={{ padding: "1.5rem" }}>
          <h2 className="wm-modal-title">Assign Location</h2>
          <p style={{ marginBottom: "1rem", color: "#64748b" }}>
            Assigning location for: <strong>{position?.productSku}</strong>
          </p>
          <form onSubmit={onAssign}>
            <div className="wm-form-grid">
              <div className="wm-form-field wm-form-full">
                <label>Warehouse *</label>
                <select
                  value={assignForm.warehouseId}
                  onChange={(e) => setAssignForm({ ...assignForm, warehouseId: e.target.value })}
                  className="admin-select"
                  required
                >
                  <option value="">-- Select Warehouse --</option>
                  {warehouses.map(w => (
                    <option key={w.id} value={w.id}>{w.name}</option>
                  ))}
                </select>
              </div>
              <div className="wm-form-field wm-form-full">
                <label>Rack *</label>
                <select
                  value={assignForm.rackId}
                  onChange={(e) => setAssignForm({ ...assignForm, rackId: e.target.value })}
                  className="admin-select"
                  required
                  disabled={!assignForm.warehouseId || loading}
                >
                  <option value="">-- Select Rack --</option>
                  {racks.map(r => (
                    <option key={r.id} value={r.id}>{r.rackCode}</option>
                  ))}
                </select>
              </div>
              <div className="wm-form-field wm-form-full">
                <label>Slot *</label>
                <select
                  value={assignForm.slotId}
                  onChange={(e) => setAssignForm({ ...assignForm, slotId: e.target.value })}
                  className="admin-select"
                  required
                  disabled={!assignForm.rackId || loading}
                >
                  <option value="">-- Select Slot --</option>
                  {slots.map(s => (
                    <option key={s.id} value={s.id}>{s.slotCode}</option>
                  ))}
                </select>
                {slots.length === 0 && assignForm.rackId && !loading && (
                  <small className="wm-form-help wm-error">No available slots in this rack.</small>
                )}
              </div>
            </div>
            <div className="wm-modal-actions">
              <button type="button" onClick={onClose} className="admin-button admin-button-secondary">
                Cancel
              </button>
              <button
                type="submit"
                className="admin-button admin-button-primary"
                disabled={!assignForm.slotId}
              >
                Assign
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default WarehouseManagement;
