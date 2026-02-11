import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  productionOrderAPI,
  stageAPI,
  getOrderStatusConfig,
  getStageStatusConfig,
  getCapabilityConfig,
  calculateOrderProgress,
  formatDate,
  formatCurrency,
  canStartStage,
  canCompleteStage,
  canSkipStage,
} from '@services/productionOrderService';
import { SkeletonTable } from '@components/admin-dashboard/Skeleton';
import '@components/production/production.css';

const ProductionOrderDetailPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('id');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [order, setOrder] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  useEffect(() => {
    if (orderId) {
      fetchOrder();
    } else {
      setError('No order ID provided');
      setLoading(false);
    }
  }, [orderId]);

  const fetchOrder = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await productionOrderAPI.getById(orderId);
      setOrder(response.data);
    } catch (err) {
      console.error('Error fetching order:', err);
      setError('Failed to load production order');
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    navigate('/dashboard/admin?tab=production-orders');
  };

  const handleAssignPartners = () => {
    navigate(`/dashboard/admin?tab=partner-assignment&orderId=${orderId}`);
  };

  const handleStartStage = async (stageId) => {
    setActionLoading(stageId);
    try {
      await stageAPI.start(orderId, stageId);
      await fetchOrder();
    } catch (err) {
      console.error('Error starting stage:', err);
      setError('Failed to start stage');
    } finally {
      setActionLoading(null);
    }
  };

  const handleCompleteStage = async (stageId) => {
    setActionLoading(stageId);
    try {
      await stageAPI.complete(orderId, stageId);
      await fetchOrder();
    } catch (err) {
      console.error('Error completing stage:', err);
      setError('Failed to complete stage');
    } finally {
      setActionLoading(null);
    }
  };

  const handleSkipStage = async (stageId) => {
    setActionLoading(stageId);
    try {
      await stageAPI.skip(orderId, stageId, 'Skipped by admin');
      await fetchOrder();
    } catch (err) {
      console.error('Error skipping stage:', err);
      setError('Failed to skip stage');
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) {
    return (
      <div className="production-order-detail-page">
        <div className="admin-card admin-p-lg">
          <SkeletonTable rows={8} columns={5} />
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="production-order-detail-page">
        <div className="admin-card admin-p-lg">
          <div className="admin-error-state">{error || 'Order not found'}</div>
          <button className="admin-button admin-button-secondary" onClick={handleBack} style={{ marginTop: '16px' }}>
            Back to Production Orders
          </button>
        </div>
      </div>
    );
  }

  const statusConfig = getOrderStatusConfig(order.status);
  const progress = order.progressPercentage || calculateOrderProgress(order.stages);

  return (
    <div className="production-order-detail-page">
      {/* Header */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <button onClick={handleBack} className="production-back-link">
          Back to Production Orders
        </button>
        <div className="order-detail-header">
          <div className="order-detail-header-left">
            <h1 style={{ margin: '8px 0 4px' }}>{order.orderNumber}</h1>
            <span
              className="production-status-badge"
              style={{ backgroundColor: statusConfig.bg, color: statusConfig.color, fontSize: '14px', padding: '4px 12px' }}
            >
              {statusConfig.label}
            </span>
          </div>
          <div className="order-detail-header-right">
            <button className="admin-button admin-button-primary" onClick={handleAssignPartners}>
              Assign Partners
            </button>
          </div>
        </div>
      </div>

      {error && <div className="admin-error-state admin-mb-lg">{error}</div>}

      {/* Order Info */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <h3 className="production-section-title">Order Information</h3>
        <div className="order-detail-grid">
          <div className="order-detail-item">
            <span className="detail-label">Production Plan</span>
            <span className="detail-value">{order.productionPlanName || '-'}</span>
          </div>
          <div className="order-detail-item">
            <span className="detail-label">JTRC Reference</span>
            <span className="detail-value">{order.jtrcReportNumber || '-'}</span>
          </div>
          <div className="order-detail-item">
            <span className="detail-label">Quantity</span>
            <span className="detail-value">{order.quantity || '-'}</span>
          </div>
          <div className="order-detail-item">
            <span className="detail-label">Current Stage</span>
            <span className="detail-value">{order.currentStageName || '-'}</span>
          </div>
          <div className="order-detail-item">
            <span className="detail-label">Est. Completion</span>
            <span className="detail-value">{formatDate(order.estimatedCompletionDate)}</span>
          </div>
          <div className="order-detail-item">
            <span className="detail-label">Created</span>
            <span className="detail-value">{formatDate(order.createdAt)}</span>
          </div>
        </div>
        {order.notes && (
          <div style={{ marginTop: '16px' }}>
            <span className="detail-label">Notes</span>
            <p style={{ margin: '4px 0 0', color: '#475569' }}>{order.notes}</p>
          </div>
        )}
      </div>

      {/* Progress */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <h3 className="production-section-title">Production Progress</h3>
        <div className="order-progress-bar-container">
          <div className="order-progress-bar">
            <div
              className="order-progress-fill"
              style={{
                width: `${progress}%`,
                backgroundColor: progress === 100 ? '#10b981' : '#4f8cff',
              }}
            />
          </div>
          <span className="order-progress-text">{progress}% Complete</span>
        </div>
        <div className="order-progress-stats">
          <span>{order.completedStages || 0} of {order.totalStages || 0} stages completed</span>
        </div>
      </div>

      {/* Stages Timeline */}
      <div className="admin-card admin-p-lg">
        <h3 className="production-section-title">Production Stages</h3>
        <div className="admin-table-wrapper">
          <table className="admin-table stages-table">
            <thead>
              <tr>
                <th style={{ width: '60px' }}>#</th>
                <th>Stage Name</th>
                <th>Status</th>
                <th>Assigned Vendor</th>
                <th>Started</th>
                <th>Completed</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {order.stages?.map((stage) => {
                const stageStatusConfig = getStageStatusConfig(stage.status);
                const isLoading = actionLoading === stage.id;

                return (
                  <tr key={stage.id} className={stage.status === 'COMPLETED' || stage.status === 'SKIPPED' ? 'stage-row-disabled' : ''}>
                    <td>
                      <div className="stage-order-badge">{stage.stageOrder}</div>
                    </td>
                    <td>
                      <strong>{stage.stageName}</strong>
                      {stage.isFinalStage && (
                        <span style={{ fontSize: '11px', color: '#6b7280', marginLeft: '8px' }}>Final Stage</span>
                      )}
                    </td>
                    <td>
                      <span
                        className="stage-status-badge"
                        style={{ backgroundColor: stageStatusConfig.bg, color: stageStatusConfig.color }}
                      >
                        {stageStatusConfig.label}
                      </span>
                    </td>
                    <td>{stage.assignedVendorName || <span className="text-muted">Not assigned</span>}</td>
                    <td>{formatDate(stage.actualStartDate)}</td>
                    <td>{formatDate(stage.actualEndDate || stage.completedAt)}</td>
                    <td>
                      <div className="action-buttons">
                        {stage.canStart && (
                          <button
                            className="admin-button admin-button-sm admin-button-primary"
                            onClick={() => handleStartStage(stage.id)}
                            disabled={isLoading}
                          >
                            {isLoading ? '...' : 'Start'}
                          </button>
                        )}
                        {stage.canComplete && (
                          <button
                            className="admin-button admin-button-sm admin-button-primary"
                            onClick={() => handleCompleteStage(stage.id)}
                            disabled={isLoading}
                          >
                            {isLoading ? '...' : 'Complete'}
                          </button>
                        )}
                        {stage.canSkip && (
                          <button
                            className="admin-button admin-button-sm admin-button-secondary"
                            onClick={() => handleSkipStage(stage.id)}
                            disabled={isLoading}
                          >
                            {isLoading ? '...' : 'Skip'}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ProductionOrderDetailPage;
