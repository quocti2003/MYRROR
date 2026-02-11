import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  collectionPlanAPI,
  COLLECTION_PLAN_STATUS,
  PRODUCT_TYPES,
  getStatusConfig,
  isPlanEditable,
  validateCollectionPlan,
  formatCurrency,
} from '@services/collectionPlanService';
import { SkeletonTable } from '@components/admin-dashboard/Skeleton';
import '@components/production/production.css';

const StatusBadge = ({ status }) => {
  const config = getStatusConfig(status);
  return (
    <span
      className="production-status-badge"
      style={{ backgroundColor: config.bg, color: config.color }}
    >
      {config.label}
    </span>
  );
};

const emptyItem = {
  productName: '',
  productType: '',
  baseDesign: '',
  targetQuantity: '',
  estimatedUnitCost: '',
};

const CollectionPlanFormPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get('mode') || 'new';
  const planId = searchParams.get('id');
  const isViewMode = mode === 'view';
  const isEditMode = mode === 'edit';

  // State
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  // Form data
  const [formData, setFormData] = useState({
    name: '',
    deadline: '',
    estimatedDeliveryDate: '',
    status: COLLECTION_PLAN_STATUS.DRAFT,
  });

  // Items
  const [items, setItems] = useState([]);

  // Fetch existing plan for edit/view
  useEffect(() => {
    if (planId && (isEditMode || isViewMode)) {
      const fetchPlan = async () => {
        setLoading(true);
        try {
          const response = await collectionPlanAPI.getById(planId);
          const data = response.data;
          setFormData({
            name: data.name || '',
            deadline: data.deadline || '',
            estimatedDeliveryDate: data.estimatedDeliveryDate || '',
            status: data.status || COLLECTION_PLAN_STATUS.DRAFT,
          });
          setItems(
            (data.items || []).map((item) => ({
              id: item.id,
              productName: item.productName || '',
              productType: item.productType || '',
              baseDesign: item.baseDesign || '',
              targetQuantity: item.targetQuantity ?? '',
              estimatedUnitCost: item.estimatedUnitCost ?? '',
            }))
          );
        } catch (err) {
          console.error('Error fetching collection plan:', err);
          setError('Failed to load collection plan. Please try again.');
        } finally {
          setLoading(false);
        }
      };
      fetchPlan();
    }
  }, [planId, isEditMode, isViewMode]);

  // Handle form field changes
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (validationErrors[field]) {
      setValidationErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  // Item management
  const handleAddItem = () => {
    setItems((prev) => [...prev, { ...emptyItem }]);
  };

  const handleItemChange = (index, field, value) => {
    setItems((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const handleRemoveItem = (index) => {
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  // Computed values
  const totalQuantity = items.reduce(
    (sum, item) => sum + (parseInt(item.targetQuantity) || 0),
    0
  );

  const totalCost = items.reduce((sum, item) => {
    const qty = parseInt(item.targetQuantity) || 0;
    const cost = parseFloat(item.estimatedUnitCost) || 0;
    return sum + qty * cost;
  }, 0);

  // Validate
  const validate = useCallback(() => {
    const result = validateCollectionPlan(formData);
    setValidationErrors(result.errors);
    return result.isValid;
  }, [formData]);

  // Build payload
  const buildPayload = () => {
    const payload = {
      name: formData.name,
      deadline: formData.deadline || null,
      estimatedDeliveryDate: formData.estimatedDeliveryDate || null,
      items: items
        .filter((item) => item.productName && item.productType)
        .map((item) => ({
          productName: item.productName,
          productType: item.productType,
          baseDesign: item.baseDesign || null,
          targetQuantity: parseInt(item.targetQuantity) || 0,
          estimatedUnitCost: item.estimatedUnitCost ? parseFloat(item.estimatedUnitCost) : null,
        })),
    };
    return payload;
  };

  // Save as Draft
  const handleSaveDraft = async () => {
    if (!formData.name?.trim()) {
      setValidationErrors({ name: 'Plan name is required' });
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const payload = buildPayload();
      let response;
      if (isEditMode && planId) {
        response = await collectionPlanAPI.update(planId, payload);
      } else {
        response = await collectionPlanAPI.create(payload);
      }
      alert('Draft saved successfully!');
      if (!isEditMode) {
        navigate(
          `/dashboard/admin?tab=collection-plan-form&mode=edit&id=${response.data.id}`,
          { replace: true }
        );
      }
    } catch (err) {
      console.error('Error saving draft:', err);
      setError(err.response?.data || 'Failed to save draft.');
    } finally {
      setSaving(false);
    }
  };

  // Save & Approve
  const handleSaveAndApprove = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);
    try {
      const payload = buildPayload();
      let savedId = planId;

      if (isEditMode && planId) {
        await collectionPlanAPI.update(planId, payload);
      } else {
        const response = await collectionPlanAPI.create(payload);
        savedId = response.data.id;
      }

      // Transition to APPROVED
      await collectionPlanAPI.updateStatus(savedId, 'APPROVED');

      alert('Collection plan saved and approved!');
      navigate('/dashboard/admin?tab=collection-plans');
    } catch (err) {
      console.error('Error saving plan:', err);
      setError(err.response?.data || 'Failed to save plan.');
    } finally {
      setSaving(false);
    }
  };

  const handleBack = () => {
    navigate('/dashboard/admin?tab=collection-plans');
  };

  const canEdit = !isViewMode && isPlanEditable(formData.status);

  if (loading) {
    return (
      <div className="production-plan-form-page">
        <div className="admin-card admin-p-lg">
          <SkeletonTable rows={10} columns={2} />
        </div>
      </div>
    );
  }

  return (
    <div className="production-plan-form-page">
      {/* Header */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <div className="production-form-header">
          <div className="production-form-title">
            <button onClick={handleBack} className="production-back-link">
              Back to Collection Plans
            </button>
            <h1>
              {mode === 'new' && 'Create New Collection Plan'}
              {mode === 'edit' && `Edit Plan - ${formData.name || 'Draft'}`}
              {mode === 'view' && `View Plan - ${formData.name}`}
            </h1>
            {formData.status && <StatusBadge status={formData.status} />}
          </div>
        </div>
      </div>

      {/* Error Display */}
      {error && <div className="admin-error-state admin-mb-lg">{error}</div>}

      {/* Validation Errors */}
      {Object.keys(validationErrors).length > 0 && (
        <div className="production-validation-errors admin-mb-lg">
          <h4>Please fix the following errors:</h4>
          <ul>
            {Object.entries(validationErrors).map(([key, message]) => (
              <li key={key}>{message}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Plan Info */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <h3 className="production-section-title">Plan Information</h3>

        <div className="admin-grid admin-grid-2">
          <div className="admin-form-group">
            <label className="admin-form-label">
              Plan Name <span className="required">*</span>
            </label>
            <input
              type="text"
              className={`admin-input ${validationErrors.name ? 'admin-input-error' : ''}`}
              value={formData.name}
              onChange={(e) => handleChange('name', e.target.value)}
              placeholder="e.g., Spring 2025 Bridal Collection"
              disabled={!canEdit}
            />
            {validationErrors.name && (
              <span className="admin-error-message">{validationErrors.name}</span>
            )}
          </div>

          <div className="admin-form-group">
            <label className="admin-form-label">Total Quantity (auto-calculated)</label>
            <input
              type="text"
              className="admin-input"
              value={totalQuantity}
              disabled
            />
            <p className="admin-form-hint">
              Sum of all item quantities.
            </p>
          </div>
        </div>

        <div className="admin-grid admin-grid-2">
          <div className="admin-form-group">
            <label className="admin-form-label">Deadline</label>
            <input
              type="date"
              className="admin-input"
              value={formData.deadline}
              onChange={(e) => handleChange('deadline', e.target.value)}
              disabled={!canEdit}
            />
          </div>

          <div className="admin-form-group">
            <label className="admin-form-label">Estimated Delivery Date</label>
            <input
              type="date"
              className={`admin-input ${validationErrors.estimatedDeliveryDate ? 'admin-input-error' : ''}`}
              value={formData.estimatedDeliveryDate}
              onChange={(e) => handleChange('estimatedDeliveryDate', e.target.value)}
              disabled={!canEdit}
            />
            {validationErrors.estimatedDeliveryDate && (
              <span className="admin-error-message">
                {validationErrors.estimatedDeliveryDate}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Items Section */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 className="production-section-title" style={{ margin: 0 }}>
            Plan Items ({items.length})
          </h3>
          {canEdit && (
            <button
              type="button"
              className="admin-button admin-button-secondary"
              onClick={handleAddItem}
            >
              + Add Item
            </button>
          )}
        </div>

        {items.length === 0 ? (
          <div className="admin-empty-state" style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
            <p>No items added yet.</p>
            {canEdit && (
              <button
                type="button"
                className="admin-button admin-button-primary"
                onClick={handleAddItem}
                style={{ marginTop: '8px' }}
              >
                + Add First Item
              </button>
            )}
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="admin-table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th style={{ minWidth: '150px' }}>Product Name</th>
                  <th style={{ minWidth: '120px' }}>Type</th>
                  <th style={{ minWidth: '120px' }}>Base Design</th>
                  <th style={{ minWidth: '80px', textAlign: 'center' }}>Qty</th>
                  <th style={{ minWidth: '100px', textAlign: 'right' }}>Unit Cost</th>
                  <th style={{ minWidth: '100px', textAlign: 'right' }}>Subtotal</th>
                  {canEdit && <th style={{ width: '60px' }}>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {items.map((item, index) => {
                  const qty = parseInt(item.targetQuantity) || 0;
                  const cost = parseFloat(item.estimatedUnitCost) || 0;
                  const subtotal = qty * cost;

                  return (
                    <tr key={item.id || index}>
                      <td>
                        {canEdit ? (
                          <input
                            type="text"
                            className="admin-input"
                            value={item.productName}
                            onChange={(e) => handleItemChange(index, 'productName', e.target.value)}
                            placeholder="Product name"
                            style={{ fontSize: '13px', padding: '6px 8px' }}
                          />
                        ) : (
                          item.productName
                        )}
                      </td>
                      <td>
                        {canEdit ? (
                          <select
                            className="admin-select"
                            value={item.productType}
                            onChange={(e) => handleItemChange(index, 'productType', e.target.value)}
                            style={{ fontSize: '13px', padding: '6px 8px' }}
                          >
                            <option value="">Select type</option>
                            {PRODUCT_TYPES.map((pt) => (
                              <option key={pt.value} value={pt.value}>
                                {pt.label}
                              </option>
                            ))}
                          </select>
                        ) : (
                          PRODUCT_TYPES.find((pt) => pt.value === item.productType)?.label || item.productType
                        )}
                      </td>
                      <td>
                        {canEdit ? (
                          <input
                            type="text"
                            className="admin-input"
                            value={item.baseDesign}
                            onChange={(e) => handleItemChange(index, 'baseDesign', e.target.value)}
                            placeholder="Design ref"
                            style={{ fontSize: '13px', padding: '6px 8px' }}
                          />
                        ) : (
                          item.baseDesign || '-'
                        )}
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {canEdit ? (
                          <input
                            type="number"
                            className="admin-input"
                            value={item.targetQuantity}
                            onChange={(e) => handleItemChange(index, 'targetQuantity', e.target.value)}
                            placeholder="0"
                            min="0"
                            style={{ fontSize: '13px', padding: '6px 8px', width: '70px', textAlign: 'center' }}
                          />
                        ) : (
                          item.targetQuantity || 0
                        )}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {canEdit ? (
                          <input
                            type="number"
                            className="admin-input"
                            value={item.estimatedUnitCost}
                            onChange={(e) => handleItemChange(index, 'estimatedUnitCost', e.target.value)}
                            placeholder="0.00"
                            min="0"
                            step="0.01"
                            style={{ fontSize: '13px', padding: '6px 8px', width: '100px', textAlign: 'right' }}
                          />
                        ) : (
                          formatCurrency(item.estimatedUnitCost)
                        )}
                      </td>
                      <td style={{ textAlign: 'right', fontWeight: 500 }}>
                        {formatCurrency(subtotal)}
                      </td>
                      {canEdit && (
                        <td style={{ textAlign: 'center' }}>
                          <button
                            type="button"
                            onClick={() => handleRemoveItem(index)}
                            className="admin-button admin-button-danger"
                            style={{ fontSize: '12px', padding: '4px 8px' }}
                            title="Remove item"
                          >
                            X
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Cost Summary */}
      {items.length > 0 && (
        <div className="admin-card admin-p-lg admin-mb-lg">
          <h3 className="production-section-title">Cost Summary</h3>
          <div className="admin-grid admin-grid-2">
            <div>
              <p className="admin-form-label">Total Items</p>
              <p style={{ fontSize: '18px', fontWeight: 600 }}>{items.length}</p>
            </div>
            <div>
              <p className="admin-form-label">Total Quantity</p>
              <p style={{ fontSize: '18px', fontWeight: 600 }}>{totalQuantity}</p>
            </div>
            <div>
              <p className="admin-form-label">Estimated Total Cost</p>
              <p style={{ fontSize: '18px', fontWeight: 600, color: '#1e40af' }}>
                {formatCurrency(totalCost)}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Form Footer */}
      {canEdit && (
        <div className="admin-card admin-p-lg">
          <div className="production-form-footer">
            <button
              type="button"
              className="admin-button admin-button-secondary"
              onClick={handleBack}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="button"
              className="admin-button admin-button-secondary"
              onClick={handleSaveDraft}
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save as Draft'}
            </button>
            <button
              type="button"
              className="admin-button admin-button-primary"
              onClick={handleSaveAndApprove}
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save & Approve'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default CollectionPlanFormPage;
