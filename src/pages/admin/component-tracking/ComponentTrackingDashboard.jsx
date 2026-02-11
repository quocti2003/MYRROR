import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  componentOwnershipAPI,
  HANDOFF_STATUS,
  HANDOFF_TYPE,
  HANDOFF_TYPE_CONFIG,
  getHandoffStatusConfig,
  getHandoffTypeConfig,
  formatDateTime,
  formatDate,
  getArrivalStatusLabel,
  getHandoffStats,
} from '@services/componentOwnershipService';
import { productionOrderAPI } from '@services/productionOrderService';
import { vendorsAPI } from '@services/api';
import { FormModal, ConfirmDialog } from '@components/admin-dashboard/AdminModal';
import { SkeletonTable } from '@components/admin-dashboard/Skeleton';
import './component-tracking.css';

/**
 * ComponentTrackingDashboard - Visual pipeline showing component locations across partners
 * Features:
 * - Summary stats (pending, overdue, received)
 * - Filter by status, vendor
 * - Overdue items highlighted
 * - Drill-down to order details
 * - Initiate, ship, confirm, reject, cancel handoffs
 */
const ComponentTrackingDashboard = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // State
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState(searchParams.get('view') || 'overview');

  // Data
  const [overdueHandoffs, setOverdueHandoffs] = useState([]);
  const [pendingByStatus, setPendingByStatus] = useState({});
  const [stats, setStats] = useState({
    totalPending: 0,
    totalOverdue: 0,
    totalInTransit: 0,
    totalReceived: 0,
  });

  // Reference data for dropdowns
  const [vendors, setVendors] = useState([]);
  const [productionOrders, setProductionOrders] = useState([]);

  // Modal state
  const [showInitiateModal, setShowInitiateModal] = useState(false);
  const [showInTransitModal, setShowInTransitModal] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [showCancelDialog, setShowCancelDialog] = useState(false);
  const [selectedHandoff, setSelectedHandoff] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState(null);

  // Form data for initiate handoff
  const [initiateForm, setInitiateForm] = useState({
    productionOrderId: '',
    handoffType: '',
    fromVendorId: '',
    toVendorId: '',
    expectedArrivalDate: '',
    reason: '',
    notes: '',
  });

  // Form data for mark in-transit
  const [inTransitForm, setInTransitForm] = useState({
    trackingNumber: '',
    shippingCarrier: '',
    expectedArrivalDate: '',
    notes: '',
  });

  // Form data for reject
  const [rejectForm, setRejectForm] = useState({
    rejectionReason: '',
    notes: '',
  });

  // Form data for confirm receipt
  const [confirmForm, setConfirmForm] = useState({
    notes: '',
  });

  // Filters
  const [filters, setFilters] = useState({
    vendorId: searchParams.get('vendorId') || '',
    status: searchParams.get('status') || '',
  });

  // Fetch data on load
  useEffect(() => {
    fetchDashboardData();
    fetchReferenceData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      // Fetch overdue handoffs
      const overdueResponse = await componentOwnershipAPI.getOverdueHandoffs();
      setOverdueHandoffs(overdueResponse.data || []);

      // Fetch by status for stats
      const [initiatedRes, inTransitRes] = await Promise.all([
        componentOwnershipAPI.getByStatus(HANDOFF_STATUS.INITIATED, { size: 100 }),
        componentOwnershipAPI.getByStatus(HANDOFF_STATUS.IN_TRANSIT, { size: 100 }),
      ]);

      const initiated = initiatedRes.data?.content || [];
      const inTransit = inTransitRes.data?.content || [];

      setPendingByStatus({
        [HANDOFF_STATUS.INITIATED]: initiated,
        [HANDOFF_STATUS.IN_TRANSIT]: inTransit,
      });

      // Calculate stats
      setStats({
        totalPending: initiated.length + inTransit.length,
        totalOverdue: overdueResponse.data?.length || 0,
        totalInTransit: inTransit.length,
        totalInitiated: initiated.length,
      });
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError('Failed to load component tracking data');
    } finally {
      setLoading(false);
    }
  };

  const fetchReferenceData = async () => {
    try {
      const [vendorsRes, ordersRes] = await Promise.all([
        vendorsAPI.getAll(),
        productionOrderAPI.getAll({ size: 200 }),
      ]);
      setVendors(vendorsRes.data || []);
      setProductionOrders(ordersRes.data?.content || ordersRes.data || []);
    } catch (err) {
      console.error('Error fetching reference data:', err);
    }
  };

  const handleViewOrder = (orderId) => {
    navigate(`/dashboard/admin?tab=production-order-detail&id=${orderId}`);
  };

  const handleViewHandoff = (handoffId) => {
    navigate(`/dashboard/admin?tab=handoff-detail&id=${handoffId}`);
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    const newParams = new URLSearchParams(searchParams);
    newParams.set('view', tab);
    setSearchParams(newParams);
  };

  // ===== ACTION HANDLERS =====

  const handleInitiateHandoff = async () => {
    setActionLoading(true);
    setActionError(null);
    try {
      const payload = {
        productionOrderId: initiateForm.productionOrderId,
        handoffType: initiateForm.handoffType,
        ...(initiateForm.fromVendorId && { fromVendorId: initiateForm.fromVendorId }),
        ...(initiateForm.toVendorId && { toVendorId: initiateForm.toVendorId }),
        ...(initiateForm.expectedArrivalDate && { expectedArrivalDate: initiateForm.expectedArrivalDate }),
        ...(initiateForm.reason && { reason: initiateForm.reason }),
        ...(initiateForm.notes && { notes: initiateForm.notes }),
      };
      await componentOwnershipAPI.initiateHandoff(payload);
      setShowInitiateModal(false);
      resetInitiateForm();
      await fetchDashboardData();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to initiate handoff';
      setActionError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleMarkInTransit = async () => {
    if (!selectedHandoff) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const payload = {
        ...(inTransitForm.trackingNumber && { trackingNumber: inTransitForm.trackingNumber }),
        ...(inTransitForm.shippingCarrier && { shippingCarrier: inTransitForm.shippingCarrier }),
        ...(inTransitForm.expectedArrivalDate && { expectedArrivalDate: inTransitForm.expectedArrivalDate }),
        ...(inTransitForm.notes && { notes: inTransitForm.notes }),
      };
      await componentOwnershipAPI.markInTransit(selectedHandoff.id, payload);
      setShowInTransitModal(false);
      setSelectedHandoff(null);
      resetInTransitForm();
      await fetchDashboardData();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to mark in transit';
      setActionError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmReceipt = async () => {
    if (!selectedHandoff) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const payload = {
        ...(confirmForm.notes && { notes: confirmForm.notes }),
      };
      await componentOwnershipAPI.confirmReceipt(selectedHandoff.id, payload);
      setShowConfirmModal(false);
      setSelectedHandoff(null);
      setConfirmForm({ notes: '' });
      await fetchDashboardData();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to confirm receipt';
      setActionError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectHandoff = async () => {
    if (!selectedHandoff) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const payload = {
        rejectionReason: rejectForm.rejectionReason,
        ...(rejectForm.notes && { notes: rejectForm.notes }),
      };
      await componentOwnershipAPI.rejectHandoff(selectedHandoff.id, payload);
      setShowRejectModal(false);
      setSelectedHandoff(null);
      setRejectForm({ rejectionReason: '', notes: '' });
      await fetchDashboardData();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to reject handoff';
      setActionError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelHandoff = async () => {
    if (!selectedHandoff) return;
    setActionLoading(true);
    try {
      await componentOwnershipAPI.cancelHandoff(selectedHandoff.id);
      setShowCancelDialog(false);
      setSelectedHandoff(null);
      await fetchDashboardData();
    } catch (err) {
      console.error('Failed to cancel handoff:', err);
    } finally {
      setActionLoading(false);
    }
  };

  // ===== MODAL OPENERS =====

  const openShipModal = (handoff) => {
    setSelectedHandoff(handoff);
    setInTransitForm({
      trackingNumber: '',
      shippingCarrier: '',
      expectedArrivalDate: handoff.expectedArrivalDate ? handoff.expectedArrivalDate.split('T')[0] : '',
      notes: '',
    });
    setActionError(null);
    setShowInTransitModal(true);
  };

  const openConfirmModal = (handoff) => {
    setSelectedHandoff(handoff);
    setConfirmForm({ notes: '' });
    setActionError(null);
    setShowConfirmModal(true);
  };

  const openRejectModal = (handoff) => {
    setSelectedHandoff(handoff);
    setRejectForm({ rejectionReason: '', notes: '' });
    setActionError(null);
    setShowRejectModal(true);
  };

  const openCancelDialog = (handoff) => {
    setSelectedHandoff(handoff);
    setShowCancelDialog(true);
  };

  const openInitiateModal = () => {
    resetInitiateForm();
    setActionError(null);
    setShowInitiateModal(true);
  };

  const resetInitiateForm = () => {
    setInitiateForm({
      productionOrderId: '',
      handoffType: '',
      fromVendorId: '',
      toVendorId: '',
      expectedArrivalDate: '',
      reason: '',
      notes: '',
    });
  };

  const resetInTransitForm = () => {
    setInTransitForm({
      trackingNumber: '',
      shippingCarrier: '',
      expectedArrivalDate: '',
      notes: '',
    });
  };

  // Determine which vendor fields to show based on handoff type
  const showFromVendor = initiateForm.handoffType && initiateForm.handoffType !== 'INITIAL_ASSIGNMENT';
  const showToVendor = initiateForm.handoffType && initiateForm.handoffType !== 'RETURN_TO_MIRROR';

  // Status Badge Component
  const StatusBadge = ({ status }) => {
    const config = getHandoffStatusConfig(status);
    return (
      <span
        className="tracking-status-badge"
        style={{ backgroundColor: config.bg, color: config.color }}
      >
        {config.icon} {config.label}
      </span>
    );
  };

  // Overdue Badge Component
  const OverdueBadge = ({ expectedArrivalDate }) => {
    const arrivalStatus = getArrivalStatusLabel(expectedArrivalDate);
    if (!arrivalStatus) return null;
    return (
      <span className="arrival-status-badge" style={{ color: arrivalStatus.color }}>
        {arrivalStatus.label}
      </span>
    );
  };

  // Stats Cards Component
  const StatsCards = () => (
    <div className="tracking-stats-grid">
      <div className="tracking-stat-card">
        <div className="stat-icon pending">
          <span>📤</span>
        </div>
        <div className="stat-content">
          <div className="stat-value">{stats.totalInitiated || 0}</div>
          <div className="stat-label">Awaiting Shipment</div>
        </div>
      </div>

      <div className="tracking-stat-card">
        <div className="stat-icon transit">
          <span>🚚</span>
        </div>
        <div className="stat-content">
          <div className="stat-value">{stats.totalInTransit || 0}</div>
          <div className="stat-label">In Transit</div>
        </div>
      </div>

      <div className="tracking-stat-card warning">
        <div className="stat-icon overdue">
          <span>⚠️</span>
        </div>
        <div className="stat-content">
          <div className="stat-value">{stats.totalOverdue || 0}</div>
          <div className="stat-label">Overdue</div>
        </div>
      </div>

      <div className="tracking-stat-card">
        <div className="stat-icon total">
          <span>📦</span>
        </div>
        <div className="stat-content">
          <div className="stat-value">{stats.totalPending || 0}</div>
          <div className="stat-label">Total Pending</div>
        </div>
      </div>
    </div>
  );

  // Overdue Items Table
  const OverdueItemsTable = () => {
    if (overdueHandoffs.length === 0) {
      return (
        <div className="empty-state">
          <span className="empty-icon">✅</span>
          <p>No overdue handoffs</p>
        </div>
      );
    }

    return (
      <div className="tracking-table-container">
        <table className="tracking-table">
          <thead>
            <tr>
              <th>Order</th>
              <th>From</th>
              <th>To</th>
              <th>Type</th>
              <th>Status</th>
              <th>Expected</th>
              <th>Overdue</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {overdueHandoffs.map((handoff) => {
              const typeConfig = getHandoffTypeConfig(handoff.handoffType);
              return (
                <tr key={handoff.id} className="overdue-row">
                  <td>
                    <button
                      className="link-button"
                      onClick={() => handleViewOrder(handoff.productionOrderId)}
                    >
                      {handoff.productionOrderNumber || handoff.productionOrderId}
                    </button>
                  </td>
                  <td>{handoff.fromVendorName || 'MIRROR'}</td>
                  <td>{handoff.toVendorName || 'MIRROR'}</td>
                  <td>
                    <span title={typeConfig.description}>
                      {typeConfig.icon} {typeConfig.label}
                    </span>
                  </td>
                  <td>
                    <StatusBadge status={handoff.status} />
                  </td>
                  <td>{formatDate(handoff.expectedArrivalDate)}</td>
                  <td>
                    <OverdueBadge expectedArrivalDate={handoff.expectedArrivalDate} />
                  </td>
                  <td>
                    <div className="card-actions">
                      <button
                        className="action-btn small"
                        onClick={() => handleViewHandoff(handoff.id)}
                      >
                        View
                      </button>
                      {handoff.status === HANDOFF_STATUS.IN_TRANSIT && (
                        <>
                          <button
                            className="action-btn small primary"
                            onClick={() => openConfirmModal(handoff)}
                          >
                            Confirm
                          </button>
                          <button
                            className="action-btn small danger"
                            onClick={() => openRejectModal(handoff)}
                          >
                            Reject
                          </button>
                        </>
                      )}
                      {handoff.status === HANDOFF_STATUS.INITIATED && (
                        <>
                          <button
                            className="action-btn small primary"
                            onClick={() => openShipModal(handoff)}
                          >
                            Ship
                          </button>
                          <button
                            className="action-btn small danger"
                            onClick={() => openCancelDialog(handoff)}
                          >
                            Cancel
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  };

  // Pending Items by Status
  const PendingItemsList = ({ status }) => {
    const items = pendingByStatus[status] || [];
    const statusConfig = getHandoffStatusConfig(status);

    if (items.length === 0) {
      return (
        <div className="empty-state small">
          <p>No items {statusConfig.label.toLowerCase()}</p>
        </div>
      );
    }

    return (
      <div className="pending-items-list">
        {items.slice(0, 10).map((handoff) => (
          <div key={handoff.id} className="pending-item-card">
            <div className="pending-item-header">
              <button
                className="link-button"
                onClick={() => handleViewOrder(handoff.productionOrderId)}
              >
                {handoff.productionOrderNumber || 'Order'}
              </button>
              <StatusBadge status={handoff.status} />
            </div>
            <div className="pending-item-details">
              <span className="transfer-arrow">
                {handoff.fromVendorName || 'MIRROR'} → {handoff.toVendorName || 'MIRROR'}
              </span>
              {handoff.expectedArrivalDate && (
                <OverdueBadge expectedArrivalDate={handoff.expectedArrivalDate} />
              )}
            </div>
            <div className="pending-item-time">
              Initiated: {formatDateTime(handoff.initiatedAt)}
            </div>
            {/* Action buttons based on status */}
            <div className="card-actions">
              {status === HANDOFF_STATUS.INITIATED && (
                <>
                  <button
                    className="action-btn small primary"
                    onClick={() => openShipModal(handoff)}
                  >
                    Ship
                  </button>
                  <button
                    className="action-btn small danger"
                    onClick={() => openCancelDialog(handoff)}
                  >
                    Cancel
                  </button>
                </>
              )}
              {status === HANDOFF_STATUS.IN_TRANSIT && (
                <>
                  <button
                    className="action-btn small primary"
                    onClick={() => openConfirmModal(handoff)}
                  >
                    Confirm Receipt
                  </button>
                  <button
                    className="action-btn small danger"
                    onClick={() => openRejectModal(handoff)}
                  >
                    Reject
                  </button>
                </>
              )}
            </div>
          </div>
        ))}
        {items.length > 10 && (
          <div className="more-items">
            +{items.length - 10} more items
          </div>
        )}
      </div>
    );
  };

  // Pipeline View
  const PipelineView = () => (
    <div className="pipeline-container">
      <div className="pipeline-stage">
        <div className="pipeline-stage-header">
          <span className="stage-icon">📤</span>
          <h3>Initiated</h3>
          <span className="stage-count">{pendingByStatus[HANDOFF_STATUS.INITIATED]?.length || 0}</span>
        </div>
        <PendingItemsList status={HANDOFF_STATUS.INITIATED} />
      </div>

      <div className="pipeline-arrow">→</div>

      <div className="pipeline-stage">
        <div className="pipeline-stage-header">
          <span className="stage-icon">🚚</span>
          <h3>In Transit</h3>
          <span className="stage-count">{pendingByStatus[HANDOFF_STATUS.IN_TRANSIT]?.length || 0}</span>
        </div>
        <PendingItemsList status={HANDOFF_STATUS.IN_TRANSIT} />
      </div>

      <div className="pipeline-arrow">→</div>

      <div className="pipeline-stage completed">
        <div className="pipeline-stage-header">
          <span className="stage-icon">✅</span>
          <h3>Received</h3>
          <span className="stage-count">-</span>
        </div>
        <div className="empty-state small">
          <p>Completed handoffs archived</p>
        </div>
      </div>
    </div>
  );

  if (loading) {
    return (
      <div className="tracking-dashboard">
        <div className="page-header">
          <h1>Component Tracking</h1>
        </div>
        <SkeletonTable rows={5} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="tracking-dashboard">
        <div className="page-header">
          <h1>Component Tracking</h1>
        </div>
        <div className="error-state">
          <p>{error}</p>
          <button className="btn-primary" onClick={fetchDashboardData}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="tracking-dashboard">
      <div className="page-header">
        <div className="header-content">
          <h1>Component Tracking</h1>
          <p className="header-subtitle">Track component locations across production partners</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={openInitiateModal}>
            + New Handoff
          </button>
          <button className="btn-secondary" onClick={fetchDashboardData}>
            Refresh
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <StatsCards />

      {/* Tab Navigation */}
      <div className="tracking-tabs">
        <button
          className={`tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => handleTabChange('overview')}
        >
          Pipeline View
        </button>
        <button
          className={`tab-btn ${activeTab === 'overdue' ? 'active' : ''}`}
          onClick={() => handleTabChange('overdue')}
        >
          Overdue Items
          {stats.totalOverdue > 0 && (
            <span className="tab-badge danger">{stats.totalOverdue}</span>
          )}
        </button>
      </div>

      {/* Tab Content */}
      <div className="tracking-content">
        {activeTab === 'overview' && <PipelineView />}
        {activeTab === 'overdue' && (
          <div className="overdue-section">
            <h2>Overdue Handoffs</h2>
            <p className="section-subtitle">
              Items that have passed their expected arrival date
            </p>
            <OverdueItemsTable />
          </div>
        )}
      </div>

      {/* ===== MODALS ===== */}

      {/* Initiate Handoff Modal */}
      <FormModal
        isOpen={showInitiateModal}
        onClose={() => { setShowInitiateModal(false); setActionError(null); }}
        onSubmit={handleInitiateHandoff}
        title="New Handoff"
        submitText="Initiate Handoff"
        loading={actionLoading}
        error={actionError}
      >
        <div className="admin-form-group">
          <label className="admin-form-label">Production Order *</label>
          <select
            className="tracking-form-select"
            value={initiateForm.productionOrderId}
            onChange={(e) => setInitiateForm({ ...initiateForm, productionOrderId: e.target.value })}
            required
          >
            <option value="">Select a production order...</option>
            {productionOrders.map((order) => (
              <option key={order.id} value={order.id}>
                {order.orderNumber || order.id} {order.productName ? `- ${order.productName}` : ''}
              </option>
            ))}
          </select>
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">Handoff Type *</label>
          <select
            className="tracking-form-select"
            value={initiateForm.handoffType}
            onChange={(e) => setInitiateForm({
              ...initiateForm,
              handoffType: e.target.value,
              fromVendorId: e.target.value === 'INITIAL_ASSIGNMENT' ? '' : initiateForm.fromVendorId,
              toVendorId: e.target.value === 'RETURN_TO_MIRROR' ? '' : initiateForm.toVendorId,
            })}
            required
          >
            <option value="">Select handoff type...</option>
            {Object.entries(HANDOFF_TYPE_CONFIG).map(([key, config]) => (
              <option key={key} value={key}>
                {config.icon} {config.label}
              </option>
            ))}
          </select>
          {initiateForm.handoffType && HANDOFF_TYPE_CONFIG[initiateForm.handoffType] && (
            <span className="tracking-form-hint">
              {HANDOFF_TYPE_CONFIG[initiateForm.handoffType].description}
            </span>
          )}
        </div>

        {showFromVendor && (
          <div className="admin-form-group">
            <label className="admin-form-label">From Vendor</label>
            <select
              className="tracking-form-select"
              value={initiateForm.fromVendorId}
              onChange={(e) => setInitiateForm({ ...initiateForm, fromVendorId: e.target.value })}
            >
              <option value="">MIRROR (default)</option>
              {vendors.map((vendor) => (
                <option key={vendor.id} value={vendor.id}>
                  {vendor.companyName || vendor.name}
                </option>
              ))}
            </select>
          </div>
        )}

        {showToVendor && (
          <div className="admin-form-group">
            <label className="admin-form-label">To Vendor</label>
            <select
              className="tracking-form-select"
              value={initiateForm.toVendorId}
              onChange={(e) => setInitiateForm({ ...initiateForm, toVendorId: e.target.value })}
            >
              <option value="">MIRROR (default)</option>
              {vendors.map((vendor) => (
                <option key={vendor.id} value={vendor.id}>
                  {vendor.companyName || vendor.name}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="admin-form-group">
          <label className="admin-form-label">Expected Arrival Date</label>
          <input
            type="date"
            className="tracking-form-input"
            value={initiateForm.expectedArrivalDate}
            onChange={(e) => setInitiateForm({ ...initiateForm, expectedArrivalDate: e.target.value })}
          />
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">Reason</label>
          <input
            type="text"
            className="tracking-form-input"
            value={initiateForm.reason}
            onChange={(e) => setInitiateForm({ ...initiateForm, reason: e.target.value })}
            placeholder="Reason for handoff"
          />
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">Notes</label>
          <textarea
            className="tracking-form-textarea"
            value={initiateForm.notes}
            onChange={(e) => setInitiateForm({ ...initiateForm, notes: e.target.value })}
            placeholder="Additional notes..."
            rows={3}
          />
        </div>
      </FormModal>

      {/* Mark In-Transit Modal */}
      <FormModal
        isOpen={showInTransitModal}
        onClose={() => { setShowInTransitModal(false); setActionError(null); }}
        onSubmit={handleMarkInTransit}
        title="Mark as Shipped"
        submitText="Mark In Transit"
        loading={actionLoading}
        error={actionError}
      >
        {selectedHandoff && (
          <div className="tracking-modal-context">
            <strong>{selectedHandoff.productionOrderNumber || 'Order'}</strong>
            <span className="transfer-arrow">
              {selectedHandoff.fromVendorName || 'MIRROR'} → {selectedHandoff.toVendorName || 'MIRROR'}
            </span>
          </div>
        )}

        <div className="admin-form-group">
          <label className="admin-form-label">Tracking Number</label>
          <input
            type="text"
            className="tracking-form-input"
            value={inTransitForm.trackingNumber}
            onChange={(e) => setInTransitForm({ ...inTransitForm, trackingNumber: e.target.value })}
            placeholder="e.g. 1Z999AA10123456784"
          />
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">Shipping Carrier</label>
          <input
            type="text"
            className="tracking-form-input"
            value={inTransitForm.shippingCarrier}
            onChange={(e) => setInTransitForm({ ...inTransitForm, shippingCarrier: e.target.value })}
            placeholder="e.g. DHL, FedEx, VNPost"
          />
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">Expected Arrival Date</label>
          <input
            type="date"
            className="tracking-form-input"
            value={inTransitForm.expectedArrivalDate}
            onChange={(e) => setInTransitForm({ ...inTransitForm, expectedArrivalDate: e.target.value })}
          />
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">Notes</label>
          <textarea
            className="tracking-form-textarea"
            value={inTransitForm.notes}
            onChange={(e) => setInTransitForm({ ...inTransitForm, notes: e.target.value })}
            placeholder="Shipping notes..."
            rows={2}
          />
        </div>
      </FormModal>

      {/* Confirm Receipt Modal */}
      <FormModal
        isOpen={showConfirmModal}
        onClose={() => { setShowConfirmModal(false); setActionError(null); }}
        onSubmit={handleConfirmReceipt}
        title="Confirm Receipt"
        submitText="Confirm Receipt"
        loading={actionLoading}
        error={actionError}
        size="sm"
      >
        {selectedHandoff && (
          <div className="tracking-modal-context">
            <strong>{selectedHandoff.productionOrderNumber || 'Order'}</strong>
            <span className="transfer-arrow">
              {selectedHandoff.fromVendorName || 'MIRROR'} → {selectedHandoff.toVendorName || 'MIRROR'}
            </span>
          </div>
        )}

        <p className="tracking-modal-message">
          Confirm that this component has been received?
        </p>

        <div className="admin-form-group">
          <label className="admin-form-label">Notes</label>
          <textarea
            className="tracking-form-textarea"
            value={confirmForm.notes}
            onChange={(e) => setConfirmForm({ ...confirmForm, notes: e.target.value })}
            placeholder="Optional notes..."
            rows={2}
          />
        </div>
      </FormModal>

      {/* Reject Handoff Modal */}
      <FormModal
        isOpen={showRejectModal}
        onClose={() => { setShowRejectModal(false); setActionError(null); }}
        onSubmit={handleRejectHandoff}
        title="Reject Handoff"
        submitText="Reject Handoff"
        loading={actionLoading}
        error={actionError}
        size="sm"
      >
        {selectedHandoff && (
          <div className="tracking-modal-context">
            <strong>{selectedHandoff.productionOrderNumber || 'Order'}</strong>
            <span className="transfer-arrow">
              {selectedHandoff.fromVendorName || 'MIRROR'} → {selectedHandoff.toVendorName || 'MIRROR'}
            </span>
          </div>
        )}

        <div className="admin-form-group">
          <label className="admin-form-label">Rejection Reason *</label>
          <input
            type="text"
            className="tracking-form-input"
            value={rejectForm.rejectionReason}
            onChange={(e) => setRejectForm({ ...rejectForm, rejectionReason: e.target.value })}
            placeholder="Reason for rejection"
            required
          />
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">Notes</label>
          <textarea
            className="tracking-form-textarea"
            value={rejectForm.notes}
            onChange={(e) => setRejectForm({ ...rejectForm, notes: e.target.value })}
            placeholder="Additional details..."
            rows={2}
          />
        </div>
      </FormModal>

      {/* Cancel Handoff Dialog */}
      <ConfirmDialog
        isOpen={showCancelDialog}
        onClose={() => { setShowCancelDialog(false); setSelectedHandoff(null); }}
        onConfirm={handleCancelHandoff}
        title="Cancel Handoff"
        message={
          selectedHandoff
            ? `Cancel the handoff for ${selectedHandoff.productionOrderNumber || 'this order'}? This action cannot be undone.`
            : 'Cancel this handoff?'
        }
        confirmText="Cancel Handoff"
        variant="danger"
        loading={actionLoading}
      />
    </div>
  );
};

export default ComponentTrackingDashboard;
