import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  productionPlanAPI,
  PRODUCTION_PLAN_STATUS,
  validateProductionPlan,
  isPlanEditable,
  formatDate,
} from '@services/productionPlanService';
import { workflowTemplateAPI, WORKFLOW_TEMPLATE_STATUS } from '@services/workflowTemplateService';
import api from '@services/api';
import { PlanStatusBadge, TemplatePreview } from '@components/production';
import { SkeletonTable } from '@components/admin-dashboard/Skeleton';
import '@components/production/production.css';

/**
 * ProductionPlanFormPage - Create/Edit/View form for production plans
 * Features: Collection selection, Template selection with preview, Date pickers
 */
const ProductionPlanFormPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get('mode') || 'new'; // new, edit, view
  const planId = searchParams.get('id');
  const isViewMode = mode === 'view';
  const isEditMode = mode === 'edit';

  // State
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  // Options from API
  const [collections, setCollections] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [selectedTemplate, setSelectedTemplate] = useState(null);

  // Form data
  const [formData, setFormData] = useState({
    name: '',
    collectionPlanId: '',
    workflowTemplateId: '',
    targetStartDate: '',
    targetEndDate: '',
    notes: '',
    status: PRODUCTION_PLAN_STATUS.DRAFT,
  });

  // Fetch collection plans and workflow templates
  useEffect(() => {
    const fetchOptions = async () => {
      try {
        // Fetch collection plans (production collection plans, not website collections)
        const collectionsResponse = await api.get('/api/v1/collection-plans', {
          params: { size: 100 }
        });
        const collectionsData = collectionsResponse.data?.content || [];
        setCollections(collectionsData);

        // Fetch active workflow templates
        const templatesResponse = await workflowTemplateAPI.getAll({ status: WORKFLOW_TEMPLATE_STATUS.ACTIVE });
        const templateData = templatesResponse.data?.content || templatesResponse.data || [];
        setTemplates(templateData);
      } catch (err) {
        console.error('Error fetching options:', err);
      }
    };
    fetchOptions();
  }, []);

  // Fetch existing plan for edit/view
  useEffect(() => {
    if (planId && (isEditMode || isViewMode)) {
      const fetchPlan = async () => {
        setLoading(true);
        try {
          const response = await productionPlanAPI.getById(planId);
          const data = response.data;
          setFormData({
            name: data.name || '',
            collectionPlanId: data.collectionPlanId || '',
            workflowTemplateId: data.workflowTemplateId || '',
            targetStartDate: data.targetStartDate?.split('T')[0] || '',
            targetEndDate: data.targetEndDate?.split('T')[0] || '',
            notes: data.notes || '',
            status: data.status || PRODUCTION_PLAN_STATUS.DRAFT,
          });

          // Set template preview if available
          if (data.workflowTemplate) {
            setSelectedTemplate(data.workflowTemplate);
          } else if (data.workflowTemplateId) {
            // Fetch template details
            try {
              const templateResponse = await workflowTemplateAPI.getById(data.workflowTemplateId);
              setSelectedTemplate(templateResponse.data);
            } catch (templateErr) {
              console.error('Error fetching template:', templateErr);
            }
          }
        } catch (err) {
          console.error('Error fetching production plan:', err);
          setError('Failed to load production plan. Please try again.');
        } finally {
          setLoading(false);
        }
      };
      fetchPlan();
    }
  }, [planId, isEditMode, isViewMode]);

  // Handle template selection change - update preview
  const handleTemplateChange = async (templateId) => {
    setFormData((prev) => ({ ...prev, workflowTemplateId: templateId }));

    if (templateId) {
      // Find template in already loaded templates
      const template = templates.find((t) => t.id === templateId);
      if (template) {
        setSelectedTemplate(template);
      } else {
        // Fetch if not in list
        try {
          const response = await workflowTemplateAPI.getById(templateId);
          setSelectedTemplate(response.data);
        } catch (err) {
          console.error('Error fetching template:', err);
          setSelectedTemplate(null);
        }
      }
    } else {
      setSelectedTemplate(null);
    }

    // Clear validation error
    if (validationErrors.workflowTemplateId) {
      setValidationErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors.workflowTemplateId;
        return newErrors;
      });
    }
  };

  // Handle form field changes
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // Clear validation error for this field
    if (validationErrors[field]) {
      setValidationErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  // Validate form
  const validate = useCallback(() => {
    const result = validateProductionPlan(formData);
    setValidationErrors(result.errors);
    return result.isValid;
  }, [formData]);

  // Handle save as draft
  const handleSaveDraft = async () => {
    setSaving(true);
    setError(null);

    try {
      let response;
      if (isEditMode && planId) {
        response = await productionPlanAPI.update(planId, {
          ...formData,
          status: PRODUCTION_PLAN_STATUS.DRAFT,
        });
      } else {
        response = await productionPlanAPI.create({
          ...formData,
          status: PRODUCTION_PLAN_STATUS.DRAFT,
        });
      }
      alert('Draft saved successfully!');
      // Navigate to edit mode with new ID
      if (!isEditMode) {
        navigate(`/dashboard/admin?tab=production-plan-form&mode=edit&id=${response.data.id}`, { replace: true });
      }
    } catch (err) {
      console.error('Error saving draft:', err);
      setError('Failed to save draft. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // Handle save as planned (validate and mark as planned)
  const handleSaveAsPlanned = async () => {
    if (!validate()) {
      return;
    }

    setSaving(true);
    setError(null);

    try {
      let planIdToUpdate = planId;

      // Save first if new
      if (!isEditMode || !planId) {
        const createResponse = await productionPlanAPI.create(formData);
        planIdToUpdate = createResponse.data.id;
      } else {
        await productionPlanAPI.update(planId, formData);
      }

      // Update status to PLANNED
      await productionPlanAPI.update(planIdToUpdate, {
        ...formData,
        status: PRODUCTION_PLAN_STATUS.PLANNED,
      });

      alert('Production plan saved and marked as planned!');
      navigate('/dashboard/admin?tab=production-plans');
    } catch (err) {
      console.error('Error saving plan:', err);
      setError('Failed to save plan. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // Handle back
  const handleBack = () => {
    navigate('/dashboard/admin?tab=production-plans');
  };

  // Check if form is editable
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
              Back to Production Plans
            </button>
            <h1>
              {mode === 'new' && 'Create New Production Plan'}
              {mode === 'edit' && `Edit Plan - ${formData.name || 'Draft'}`}
              {mode === 'view' && `View Plan - ${formData.name}`}
            </h1>
            {formData.status && <PlanStatusBadge status={formData.status} />}
          </div>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="admin-error-state admin-mb-lg">{error}</div>
      )}

      {/* Validation Errors Summary */}
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

      {/* Form Content */}
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
              placeholder="e.g., Spring 2025 Ring Production"
              disabled={!canEdit}
            />
            {validationErrors.name && (
              <span className="admin-error-message">{validationErrors.name}</span>
            )}
          </div>

          <div className="admin-form-group">
            <label className="admin-form-label">
              Collection Plan <span className="required">*</span>
            </label>
            <select
              className={`admin-select ${validationErrors.collectionPlanId ? 'admin-select-error' : ''}`}
              value={formData.collectionPlanId}
              onChange={(e) => handleChange('collectionPlanId', e.target.value)}
              disabled={!canEdit}
            >
              <option value="">Select Collection Plan</option>
              {collections.map((col) => (
                <option key={col.id} value={col.id}>
                  {col.name} {col.status ? `[${col.status}]` : ''}
                </option>
              ))}
            </select>
            {validationErrors.collectionPlanId && (
              <span className="admin-error-message">{validationErrors.collectionPlanId}</span>
            )}
          </div>
        </div>

        <div className="admin-form-group">
          <label className="admin-form-label">
            Workflow Template <span className="required">*</span>
          </label>
          <select
            className={`admin-select ${validationErrors.workflowTemplateId ? 'admin-select-error' : ''}`}
            value={formData.workflowTemplateId}
            onChange={(e) => handleTemplateChange(e.target.value)}
            disabled={!canEdit}
          >
            <option value="">Select Workflow Template</option>
            {templates.map((template) => (
              <option key={template.id} value={template.id}>
                {template.name} ({template.stages?.length || 0} stages)
              </option>
            ))}
          </select>
          {validationErrors.workflowTemplateId && (
            <span className="admin-error-message">{validationErrors.workflowTemplateId}</span>
          )}
          <p className="admin-form-hint">
            Only active workflow templates are shown.
          </p>
        </div>

        {/* Template Preview */}
        {selectedTemplate && (
          <TemplatePreview template={selectedTemplate} className="admin-mt-lg" />
        )}
      </div>

      {/* Target Dates Section */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <h3 className="production-section-title">Target Dates</h3>

        <div className="admin-grid admin-grid-2">
          <div className="admin-form-group">
            <label className="admin-form-label">Target Start Date</label>
            <input
              type="date"
              className="admin-input"
              value={formData.targetStartDate}
              onChange={(e) => handleChange('targetStartDate', e.target.value)}
              disabled={!canEdit}
            />
            <p className="admin-form-hint">
              When production should begin.
            </p>
          </div>

          <div className="admin-form-group">
            <label className="admin-form-label">Target End Date</label>
            <input
              type="date"
              className={`admin-input ${validationErrors.targetEndDate ? 'admin-input-error' : ''}`}
              value={formData.targetEndDate}
              onChange={(e) => handleChange('targetEndDate', e.target.value)}
              disabled={!canEdit}
            />
            {validationErrors.targetEndDate && (
              <span className="admin-error-message">{validationErrors.targetEndDate}</span>
            )}
            <p className="admin-form-hint">
              When production should complete.
            </p>
          </div>
        </div>

        {formData.targetStartDate && formData.targetEndDate && (
          <div className="production-date-summary">
            <strong>Duration:</strong>{' '}
            {Math.ceil(
              (new Date(formData.targetEndDate) - new Date(formData.targetStartDate)) /
                (1000 * 60 * 60 * 24)
            )}{' '}
            days
          </div>
        )}
      </div>

      {/* Notes Section */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <h3 className="production-section-title">Notes</h3>

        <div className="admin-form-group">
          <label className="admin-form-label">Production Notes</label>
          <textarea
            className="admin-input"
            rows={4}
            value={formData.notes}
            onChange={(e) => handleChange('notes', e.target.value)}
            placeholder="Any special instructions or notes for this production plan..."
            disabled={!canEdit}
          />
          <p className="admin-form-hint">
            Notes will be visible to production coordinators.
          </p>
        </div>
      </div>

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
              onClick={handleSaveAsPlanned}
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save & Mark as Planned'}
            </button>
          </div>
        </div>
      )}

      {/* View Mode - Show Plan Details */}
      {isViewMode && formData.status !== PRODUCTION_PLAN_STATUS.DRAFT && (
        <div className="admin-card admin-p-lg">
          <h3 className="production-section-title">Plan Status</h3>
          <div className="production-status-info">
            <div className="production-status-item">
              <span className="production-status-label">Current Status:</span>
              <PlanStatusBadge status={formData.status} />
            </div>
            {formData.targetStartDate && (
              <div className="production-status-item">
                <span className="production-status-label">Target Start:</span>
                <span>{formatDate(formData.targetStartDate)}</span>
              </div>
            )}
            {formData.targetEndDate && (
              <div className="production-status-item">
                <span className="production-status-label">Target End:</span>
                <span>{formatDate(formData.targetEndDate)}</span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductionPlanFormPage;
