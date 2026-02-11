import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  jtrcAPI,
  JTRC_STATUS,
  SOURCES,
  calculateTotalStoneCost,
  calculateTotalLaborCost,
  calculateTotalCOGS,
  formatVND,
} from '@services/jtrcService';
import { collectionPlanAPI } from '@services/collectionPlanService';
import {
  JTRCStatusBadge,
  CostSummaryPanel,
  MetalComponentForm,
  StoneComponentForm,
  LaborComponentForm,
} from '@components/jtrc';
import { useDropdownOptions } from '@hooks/useDropdownOptions';
import { SkeletonTable } from '@components/admin-dashboard/Skeleton';
import '@components/jtrc/jtrc.css';

/**
 * JTRCFormPage - Create/Edit/View form for JTRC
 * Multi-tab form: Header, Metal, Stones, Labor, Assets, Summary
 */
const JTRCFormPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get('mode') || 'new'; // new, edit, view
  const jtrcId = searchParams.get('id');
  const isViewMode = mode === 'view';
  const isEditMode = mode === 'edit';

  // Dropdown options from database
  const { options: dropdownOptions, loading: dropdownLoading } = useDropdownOptions();

  // State
  const [activeTab, setActiveTab] = useState('header');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  // Collections for dropdown
  const [collections, setCollections] = useState([]);

  // Gold price
  const [goldPrice, setGoldPrice] = useState({
    pricePerGram: 0,
    exchangeRate: 24500,
    lastUpdated: null,
  });
  const [refreshingPrice, setRefreshingPrice] = useState(false);

  // Form data
  const [formData, setFormData] = useState({
    // Header
    collectionId: '',
    collectionName: '',
    season: '',
    projectId: '',
    category: '',
    source: '',
    entryDate: new Date().toISOString().split('T')[0],
    goldPricePerGram: 0,
    exchangeRate: 24500,

    // Metal
    metalComponent: {
      metalType: '',
      metalPurity: '',
      weight: '',
      lossRate: 3,
      pricePerGram: 0,
      metalCost: 0,
      lossCost: 0,
      totalMetalCost: 0,
    },

    // Stones
    stoneComponents: [],

    // Labor
    laborComponents: [],

    // Assets
    assets: {
      render3d: null,
      stoneMap: null,
      technicalDrawing: null,
    },

    // Summary
    difficultyLevel: 3,
    estimatedLeadTime: 14,
    castingStatus: 'NOT_STARTED',
    productionNotes: '',

    // Calculated
    totalMetalCost: 0,
    totalStoneCost: 0,
    totalLaborCost: 0,
    totalCOGS: 0,

    // Status
    status: JTRC_STATUS.DRAFT,
    reportNumber: '',
  });

  // Fetch collection plans (production-side, not commercial collections)
  useEffect(() => {
    const fetchCollections = async () => {
      try {
        const response = await collectionPlanAPI.getAll({ size: 100 });
        setCollections(response.data?.content || []);
      } catch (err) {
        console.error('Error fetching collection plans:', err);
      }
    };
    fetchCollections();
  }, []);

  // Fetch gold price
  useEffect(() => {
    const fetchGoldPrice = async () => {
      try {
        const response = await jtrcAPI.getGoldPrice();
        const data = response.data;
        setGoldPrice({
          pricePerGram: data.pricePerGram || 1850000,
          exchangeRate: data.exchangeRate || 24500,
          lastUpdated: data.lastUpdated,
        });
        // Update form with gold price
        setFormData((prev) => ({
          ...prev,
          goldPricePerGram: data.pricePerGram || prev.goldPricePerGram,
          exchangeRate: data.exchangeRate || prev.exchangeRate,
          metalComponent: {
            ...prev.metalComponent,
            pricePerGram: data.pricePerGram || prev.metalComponent.pricePerGram,
          },
        }));
      } catch (err) {
        console.error('Error fetching gold price:', err);
        // Use defaults
        setGoldPrice({
          pricePerGram: 1850000,
          exchangeRate: 24500,
          lastUpdated: null,
        });
      }
    };
    fetchGoldPrice();
  }, []);

  // Fetch existing JTRC for edit/view
  useEffect(() => {
    if (jtrcId && (isEditMode || isViewMode)) {
      const fetchJTRC = async () => {
        setLoading(true);
        try {
          const response = await jtrcAPI.getById(jtrcId);
          const data = response.data;
          setFormData({
            ...data,
            entryDate: data.entryDate?.split('T')[0] || new Date().toISOString().split('T')[0],
            metalComponent: data.metalComponent || formData.metalComponent,
            stoneComponents: data.stoneComponents || [],
            laborComponents: data.laborComponents || [],
            assets: data.assets || formData.assets,
          });
        } catch (err) {
          console.error('Error fetching JTRC:', err);
          setError('Failed to load JTRC. Please try again.');
        } finally {
          setLoading(false);
        }
      };
      fetchJTRC();
    }
  }, [jtrcId, isEditMode, isViewMode]);

  // Recalculate totals when components change
  useEffect(() => {
    const metalCost = formData.metalComponent?.totalMetalCost || 0;
    const stoneCost = calculateTotalStoneCost(formData.stoneComponents);
    const laborCost = calculateTotalLaborCost(formData.laborComponents);
    const totalCOGS = calculateTotalCOGS(metalCost, stoneCost, laborCost);

    setFormData((prev) => ({
      ...prev,
      totalMetalCost: metalCost,
      totalStoneCost: stoneCost,
      totalLaborCost: laborCost,
      totalCOGS,
    }));
  }, [formData.metalComponent, formData.stoneComponents, formData.laborComponents]);

  // Handle refresh gold price
  const handleRefreshGoldPrice = async () => {
    setRefreshingPrice(true);
    try {
      const response = await jtrcAPI.refreshGoldPrice();
      const data = response.data;
      setGoldPrice({
        pricePerGram: data.pricePerGram,
        exchangeRate: data.exchangeRate,
        lastUpdated: data.lastUpdated,
      });
      setFormData((prev) => ({
        ...prev,
        goldPricePerGram: data.pricePerGram,
        exchangeRate: data.exchangeRate,
        metalComponent: {
          ...prev.metalComponent,
          pricePerGram: data.pricePerGram,
        },
      }));
      alert('Gold price updated successfully!');
    } catch (err) {
      console.error('Error refreshing gold price:', err);
      alert('Failed to refresh gold price. Please try again.');
    } finally {
      setRefreshingPrice(false);
    }
  };

  // Validate form
  const validateForm = useCallback(() => {
    const errors = {};

    // Header validation
    if (!formData.collectionId) {
      errors.collectionId = 'Collection is required';
    }
    if (!formData.season) {
      errors.season = 'Season is required';
    }
    if (!formData.category) {
      errors.category = 'Category is required';
    }

    // Metal validation
    if (!formData.metalComponent?.metalType) {
      errors['metalComponent.metalType'] = 'Metal type is required';
    }
    if (!formData.metalComponent?.metalPurity) {
      errors['metalComponent.metalPurity'] = 'Metal purity is required';
    }
    if (!formData.metalComponent?.weight || parseFloat(formData.metalComponent.weight) <= 0) {
      errors['metalComponent.weight'] = 'Weight must be greater than 0';
    }

    // Stones validation - at least one for approval
    if (formData.stoneComponents.length === 0) {
      // Only warn, not block
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  }, [formData]);

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

  // Handle collection change
  const handleCollectionChange = (collectionId) => {
    const collection = collections.find((c) => String(c.id) === String(collectionId));
    setFormData((prev) => ({
      ...prev,
      collectionId,
      collectionName: collection?.name || '',
    }));
  };

  // Transform frontend formData to backend API payload format
  const buildApiPayload = (data) => {
    const payload = {
      collection: data.collectionName,
      season: data.season,
      projectId: data.projectId,
      category: data.category,
      source: data.source,
      entryDate: data.entryDate,
      goldPricePerGram: data.goldPricePerGram,
      exchangeRateUsd: data.exchangeRate,
      productionDifficulty: data.productionDifficulty,
      estimatedLeadTimeDays: data.estimatedLeadTimeDays,
      castingStatus: data.castingStatus,
      productionNotes: data.productionNotes,
      render3dUrl: data.assets?.render3d,
      stoneMapUrl: data.assets?.stoneMap,
      technicalDrawingUrl: data.assets?.technicalDrawing,
    };

    // Metal component - map frontend field names to backend DTO
    if (data.metalComponent?.metalType) {
      payload.metalComponent = {
        metalType: data.metalComponent.metalType,
        metalPurity: data.metalComponent.metalPurity,
        weightGrams: data.metalComponent.weight ? parseFloat(data.metalComponent.weight) : null,
        lossRatePercent: data.metalComponent.lossRate != null ? parseFloat(data.metalComponent.lossRate) : null,
        pricePerGram: data.metalComponent.pricePerGram,
      };
    }

    // Stone components - map 'role' to 'stoneRole'
    if (data.stoneComponents?.length > 0) {
      payload.stoneComponents = data.stoneComponents.map((stone) => ({
        stoneRole: stone.role || stone.stoneRole,
        stoneType: stone.stoneType,
        stoneDetail: stone.stoneDetail,
        shape: stone.shape,
        colorCategory: stone.colorCategory,
        colorGrade: stone.colorGrade,
        colorIntensity: stone.colorIntensity,
        colorName: stone.colorName,
        clarity: stone.clarity,
        sizeMm: stone.sizeMm,
        weightCarat: stone.weightCarat ? parseFloat(stone.weightCarat) : null,
        quantity: stone.quantity || 1,
        unitPrice: stone.unitPrice,
        notes: stone.notes,
      }));
    }

    // Labor components
    if (data.laborComponents?.length > 0) {
      payload.laborComponents = data.laborComponents.map((labor) => ({
        laborType: labor.laborType,
        description: labor.description,
        cost: labor.cost,
        notes: labor.notes,
      }));
    }

    return payload;
  };

  // Handle save draft
  const handleSaveDraft = async () => {
    setSaving(true);
    setError(null);

    try {
      let response;
      const apiPayload = buildApiPayload(formData);
      if (isEditMode && jtrcId) {
        response = await jtrcAPI.saveDraft(jtrcId, apiPayload);
      } else {
        response = await jtrcAPI.create({ ...apiPayload, status: JTRC_STATUS.DRAFT });
      }
      alert('Draft saved successfully!');
      // Navigate to edit mode with new ID
      if (!isEditMode) {
        navigate(`/dashboard/admin?tab=jtrc-form&mode=edit&id=${response.data.id}`, { replace: true });
      }
    } catch (err) {
      console.error('Error saving draft:', err);
      setError('Failed to save draft. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // Handle submit for approval
  const handleSubmitForApproval = async () => {
    if (!validateForm()) {
      setActiveTab('header');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      let jtrcIdToSubmit = jtrcId;

      // Save first if new
      const apiPayload = buildApiPayload(formData);
      if (!isEditMode || !jtrcId) {
        const createResponse = await jtrcAPI.create(apiPayload);
        jtrcIdToSubmit = createResponse.data.id;
      } else {
        await jtrcAPI.update(jtrcId, apiPayload);
      }

      // Submit for approval
      await jtrcAPI.submitForApproval(jtrcIdToSubmit);
      alert('JTRC submitted for approval successfully!');
      navigate('/dashboard/admin?tab=jtrc');
    } catch (err) {
      console.error('Error submitting for approval:', err);
      setError('Failed to submit for approval. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // Handle back
  const handleBack = () => {
    navigate('/dashboard/admin?tab=jtrc');
  };

  // Tabs configuration
  const tabs = [
    { id: 'header', label: 'Header' },
    { id: 'metal', label: 'Metal' },
    { id: 'stones', label: 'Stones' },
    { id: 'labor', label: 'Labor' },
    { id: 'assets', label: 'Assets' },
    { id: 'summary', label: 'Summary' },
  ];

  // Season options
  const seasonOptions = [
    { value: 'SS25', label: 'SS25 - Spring/Summer 2025' },
    { value: 'FW25', label: 'FW25 - Fall/Winter 2025' },
    { value: 'SS26', label: 'SS26 - Spring/Summer 2026' },
    { value: 'FW26', label: 'FW26 - Fall/Winter 2026' },
    { value: 'SS27', label: 'SS27 - Spring/Summer 2027' },
  ];

  if (loading) {
    return (
      <div className="jtrc-form-page">
        <div className="admin-card admin-p-lg">
          <SkeletonTable rows={10} columns={2} />
        </div>
      </div>
    );
  }

  return (
    <div className="jtrc-form-page">
      {/* Header */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <div className="jtrc-form-header">
          <div className="jtrc-form-title">
            <button onClick={handleBack} className="jtrc-back-link">
              ← Back to JTRC List
            </button>
            <h1>
              {mode === 'new' && 'Create New JTRC'}
              {mode === 'edit' && `Edit JTRC - ${formData.reportNumber || 'Draft'}`}
              {mode === 'view' && `View JTRC - ${formData.reportNumber}`}
            </h1>
            {formData.status && <JTRCStatusBadge status={formData.status} />}
          </div>
        </div>

        {/* Tabs */}
        <div className="jtrc-tabs" role="tablist" aria-label="JTRC sections">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              role="tab"
              aria-selected={activeTab === tab.id}
              aria-controls={`panel-${tab.id}`}
              className={`jtrc-tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="admin-error-state admin-mb-lg">{error}</div>
      )}

      {/* Tab Panels */}
      <div className="admin-card admin-p-lg">
        {/* Tab 1: Header */}
        {activeTab === 'header' && (
          <div className="jtrc-tab-panel" role="tabpanel" id="panel-header">
            <h3 className="jtrc-section-title">Admin Header</h3>

            <div className="admin-grid admin-grid-3">
              <div className="admin-form-group">
                <label className="admin-form-label">
                  Collection <span className="required">*</span>
                </label>
                <select
                  className={`admin-select ${validationErrors.collectionId ? 'admin-select-error' : ''}`}
                  value={formData.collectionId}
                  onChange={(e) => handleCollectionChange(e.target.value)}
                  disabled={isViewMode}
                >
                  <option value="">Select Collection Plan</option>
                  {collections.map((col) => (
                    <option key={col.id} value={col.id}>
                      {col.name} {col.status ? `[${col.status}]` : ''}
                    </option>
                  ))}
                </select>
                {validationErrors.collectionId && (
                  <span className="admin-error-message">{validationErrors.collectionId}</span>
                )}
              </div>

              <div className="admin-form-group">
                <label className="admin-form-label">
                  Season <span className="required">*</span>
                </label>
                <select
                  className={`admin-select ${validationErrors.season ? 'admin-select-error' : ''}`}
                  value={formData.season}
                  onChange={(e) => handleChange('season', e.target.value)}
                  disabled={isViewMode}
                >
                  <option value="">Select Season</option>
                  {seasonOptions.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
                {validationErrors.season && (
                  <span className="admin-error-message">{validationErrors.season}</span>
                )}
              </div>

              <div className="admin-form-group">
                <label className="admin-form-label">Project ID</label>
                <input
                  type="text"
                  className="admin-input"
                  value={formData.projectId}
                  onChange={(e) => handleChange('projectId', e.target.value)}
                  placeholder="PRJ-001"
                  disabled={isViewMode}
                />
              </div>
            </div>

            <div className="admin-grid admin-grid-3">
              <div className="admin-form-group">
                <label className="admin-form-label">
                  Category <span className="required">*</span>
                </label>
                <select
                  className={`admin-select ${validationErrors.category ? 'admin-select-error' : ''}`}
                  value={formData.category}
                  onChange={(e) => handleChange('category', e.target.value)}
                  disabled={isViewMode}
                >
                  <option value="">Select Category</option>
                  {dropdownOptions.prefixes.map((cat) => (
                    <option key={cat.code} value={cat.code}>
                      {cat.name}
                    </option>
                  ))}
                </select>
                {validationErrors.category && (
                  <span className="admin-error-message">{validationErrors.category}</span>
                )}
              </div>

              <div className="admin-form-group">
                <label className="admin-form-label">Source</label>
                <select
                  className="admin-select"
                  value={formData.source}
                  onChange={(e) => handleChange('source', e.target.value)}
                  disabled={isViewMode}
                >
                  <option value="">Select Source</option>
                  {SOURCES.map((src) => (
                    <option key={src.value} value={src.value}>
                      {src.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="admin-form-group">
                <label className="admin-form-label">Entry Date</label>
                <input
                  type="date"
                  className="admin-input"
                  value={formData.entryDate}
                  onChange={(e) => handleChange('entryDate', e.target.value)}
                  disabled={isViewMode}
                />
              </div>
            </div>

            {/* Pricing Snapshot */}
            <div style={{ marginTop: '2rem', padding: '1rem', background: 'var(--mirror-gray-50)', borderRadius: '8px' }}>
              <h4 className="jtrc-section-title">Pricing Snapshot</h4>
              <div className="admin-grid admin-grid-3">
                <div className="admin-form-group">
                  <label className="admin-form-label">Gold Price (per gram)</label>
                  <input
                    type="text"
                    className="admin-input"
                    value={formatVND(goldPrice.pricePerGram)}
                    readOnly
                    disabled
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Exchange Rate (USD)</label>
                  <input
                    type="text"
                    className="admin-input"
                    value={formatVND(goldPrice.exchangeRate)}
                    readOnly
                    disabled
                  />
                </div>
                <div className="admin-form-group" style={{ display: 'flex', alignItems: 'flex-end' }}>
                  <button
                    type="button"
                    className="admin-button admin-button-secondary"
                    onClick={handleRefreshGoldPrice}
                    disabled={refreshingPrice || isViewMode}
                  >
                    {refreshingPrice ? 'Refreshing...' : 'Refresh Prices'}
                  </button>
                </div>
              </div>
              {goldPrice.lastUpdated && (
                <p className="admin-form-hint" style={{ marginTop: '0.5rem' }}>
                  Last updated: {new Date(goldPrice.lastUpdated).toLocaleString()}
                </p>
              )}
            </div>
          </div>
        )}

        {/* Tab 2: Metal */}
        {activeTab === 'metal' && (
          <div className="jtrc-tab-panel" role="tabpanel" id="panel-metal">
            <MetalComponentForm
              data={formData.metalComponent}
              onChange={(metalData) => handleChange('metalComponent', metalData)}
              goldPricePerGram={goldPrice.pricePerGram}
              errors={validationErrors}
              disabled={isViewMode}
              metalTypeOptions={dropdownOptions.metalTypes}
              metalPurityOptions={dropdownOptions.metalPurities}
            />
          </div>
        )}

        {/* Tab 3: Stones */}
        {activeTab === 'stones' && (
          <div className="jtrc-tab-panel" role="tabpanel" id="panel-stones">
            <StoneComponentForm
              stones={formData.stoneComponents}
              onChange={(stones) => handleChange('stoneComponents', stones)}
              errors={validationErrors}
              disabled={isViewMode}
              stoneRoleOptions={dropdownOptions.stoneRoles}
              stoneTypeOptions={dropdownOptions.stoneTypes}
              stoneShapeOptions={dropdownOptions.stoneShapes}
              colorGradeOptions={dropdownOptions.colorGrades}
              clarityGradeOptions={dropdownOptions.clarityGrades}
            />
          </div>
        )}

        {/* Tab 4: Labor */}
        {activeTab === 'labor' && (
          <div className="jtrc-tab-panel" role="tabpanel" id="panel-labor">
            <LaborComponentForm
              laborItems={formData.laborComponents}
              onChange={(items) => handleChange('laborComponents', items)}
              errors={validationErrors}
              disabled={isViewMode}
              laborTypeOptions={dropdownOptions.laborTypes}
            />
          </div>
        )}

        {/* Tab 5: Assets */}
        {activeTab === 'assets' && (
          <div className="jtrc-tab-panel" role="tabpanel" id="panel-assets">
            <div className="visual-assets-form">
              <h3 className="jtrc-section-title">Visual Assets</h3>
              <div className="visual-assets-grid">
                {/* 3D Render */}
                <div className={`asset-upload-card ${formData.assets?.render3d ? 'has-file' : ''}`}>
                  <h4 className="asset-upload-title">3D Render</h4>
                  {formData.assets?.render3d ? (
                    <>
                      <img
                        src={formData.assets.render3d}
                        alt="3D Render"
                        className="asset-preview"
                      />
                      {!isViewMode && (
                        <div className="asset-actions">
                          <button
                            type="button"
                            className="admin-button admin-button-secondary admin-button-sm"
                            onClick={() =>
                              handleChange('assets', { ...formData.assets, render3d: null })
                            }
                          >
                            Remove
                          </button>
                        </div>
                      )}
                    </>
                  ) : (
                    <>
                      <p className="asset-upload-hint">PNG, JPG, WebP - Max 10MB</p>
                      {!isViewMode && (
                        <button
                          type="button"
                          className="admin-button admin-button-secondary admin-button-sm asset-upload-btn"
                          onClick={() => {
                            // Trigger file input
                            document.getElementById('upload-render3d')?.click();
                          }}
                        >
                          + Upload
                        </button>
                      )}
                      <input
                        type="file"
                        id="upload-render3d"
                        className="asset-upload-input"
                        accept="image/png,image/jpeg,image/webp"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            // For now, just store URL preview
                            const url = URL.createObjectURL(file);
                            handleChange('assets', { ...formData.assets, render3d: url });
                          }
                        }}
                      />
                    </>
                  )}
                </div>

                {/* Stone Map */}
                <div className={`asset-upload-card ${formData.assets?.stoneMap ? 'has-file' : ''}`}>
                  <h4 className="asset-upload-title">Stone Map</h4>
                  {formData.assets?.stoneMap ? (
                    <>
                      <img
                        src={formData.assets.stoneMap}
                        alt="Stone Map"
                        className="asset-preview"
                      />
                      {!isViewMode && (
                        <div className="asset-actions">
                          <button
                            type="button"
                            className="admin-button admin-button-secondary admin-button-sm"
                            onClick={() =>
                              handleChange('assets', { ...formData.assets, stoneMap: null })
                            }
                          >
                            Remove
                          </button>
                        </div>
                      )}
                    </>
                  ) : (
                    <>
                      <p className="asset-upload-hint">PNG, JPG, WebP - Max 10MB</p>
                      {!isViewMode && (
                        <button
                          type="button"
                          className="admin-button admin-button-secondary admin-button-sm asset-upload-btn"
                          onClick={() => {
                            document.getElementById('upload-stoneMap')?.click();
                          }}
                        >
                          + Upload
                        </button>
                      )}
                      <input
                        type="file"
                        id="upload-stoneMap"
                        className="asset-upload-input"
                        accept="image/png,image/jpeg,image/webp"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            const url = URL.createObjectURL(file);
                            handleChange('assets', { ...formData.assets, stoneMap: url });
                          }
                        }}
                      />
                    </>
                  )}
                </div>

                {/* Technical Drawing */}
                <div className={`asset-upload-card ${formData.assets?.technicalDrawing ? 'has-file' : ''}`}>
                  <h4 className="asset-upload-title">Technical Drawing</h4>
                  {formData.assets?.technicalDrawing ? (
                    <>
                      <img
                        src={formData.assets.technicalDrawing}
                        alt="Technical Drawing"
                        className="asset-preview"
                      />
                      {!isViewMode && (
                        <div className="asset-actions">
                          <button
                            type="button"
                            className="admin-button admin-button-secondary admin-button-sm"
                            onClick={() =>
                              handleChange('assets', { ...formData.assets, technicalDrawing: null })
                            }
                          >
                            Remove
                          </button>
                        </div>
                      )}
                    </>
                  ) : (
                    <>
                      <p className="asset-upload-hint">PDF, PNG, JPG - Max 10MB</p>
                      {!isViewMode && (
                        <button
                          type="button"
                          className="admin-button admin-button-secondary admin-button-sm asset-upload-btn"
                          onClick={() => {
                            document.getElementById('upload-technicalDrawing')?.click();
                          }}
                        >
                          + Upload
                        </button>
                      )}
                      <input
                        type="file"
                        id="upload-technicalDrawing"
                        className="asset-upload-input"
                        accept="application/pdf,image/png,image/jpeg"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) {
                            const url = URL.createObjectURL(file);
                            handleChange('assets', { ...formData.assets, technicalDrawing: url });
                          }
                        }}
                      />
                    </>
                  )}
                </div>
              </div>
              <div className="visual-assets-note">
                Assets will be visible to assigned production partners.
              </div>
            </div>
          </div>
        )}

        {/* Tab 6: Summary */}
        {activeTab === 'summary' && (
          <div className="jtrc-tab-panel" role="tabpanel" id="panel-summary">
            <CostSummaryPanel
              metalCost={formData.totalMetalCost}
              stoneCost={formData.totalStoneCost}
              laborCost={formData.totalLaborCost}
              exchangeRate={goldPrice.exchangeRate}
              goldPricePerGram={goldPrice.pricePerGram}
              goldPriceUpdatedAt={goldPrice.lastUpdated}
              difficultyLevel={formData.difficultyLevel}
              estimatedLeadTime={formData.estimatedLeadTime}
              castingStatus={formData.castingStatus}
              productionNotes={formData.productionNotes}
            />

            {/* Editable fields for summary */}
            {!isViewMode && (
              <div style={{ marginTop: '1.5rem', padding: '1rem', background: 'var(--mirror-gray-50)', borderRadius: '8px' }}>
                <h4 className="jtrc-section-title">Production Details</h4>
                <div className="admin-grid admin-grid-3">
                  <div className="admin-form-group">
                    <label className="admin-form-label">Difficulty Level (1-5)</label>
                    <select
                      className="admin-select"
                      value={formData.difficultyLevel}
                      onChange={(e) => handleChange('difficultyLevel', parseInt(e.target.value))}
                    >
                      <option value={1}>1 - Simple</option>
                      <option value={2}>2 - Easy</option>
                      <option value={3}>3 - Moderate</option>
                      <option value={4}>4 - Complex</option>
                      <option value={5}>5 - Very Complex</option>
                    </select>
                  </div>
                  <div className="admin-form-group">
                    <label className="admin-form-label">Estimated Lead Time (days)</label>
                    <input
                      type="number"
                      className="admin-input"
                      value={formData.estimatedLeadTime}
                      onChange={(e) => handleChange('estimatedLeadTime', parseInt(e.target.value))}
                      min={1}
                    />
                  </div>
                  <div className="admin-form-group">
                    <label className="admin-form-label">Casting Status</label>
                    <select
                      className="admin-select"
                      value={formData.castingStatus}
                      onChange={(e) => handleChange('castingStatus', e.target.value)}
                    >
                      <option value="NOT_STARTED">Not Started</option>
                      <option value="IN_PROGRESS">In Progress</option>
                      <option value="COMPLETED">Completed</option>
                    </select>
                  </div>
                </div>
                <div className="admin-form-group" style={{ marginTop: '1rem' }}>
                  <label className="admin-form-label">Production Notes</label>
                  <textarea
                    className="admin-input"
                    rows={4}
                    value={formData.productionNotes}
                    onChange={(e) => handleChange('productionNotes', e.target.value)}
                    placeholder="Special instructions, notes for production partners..."
                  />
                </div>
              </div>
            )}
          </div>
        )}

        {/* Form Footer */}
        {!isViewMode && (
          <div className="jtrc-form-footer">
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
              {saving ? 'Saving...' : 'Save Draft'}
            </button>
            <button
              type="button"
              className="admin-button admin-button-primary"
              onClick={handleSubmitForApproval}
              disabled={saving}
            >
              {saving ? 'Submitting...' : 'Save & Approve'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default JTRCFormPage;
