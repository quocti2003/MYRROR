import React, { useState, useEffect } from 'react';

/**
 * ProductFulfillment - Fill product assets and data
 *
 * Step 3 of Product Workflow
 * Allows Product Ops to:
 * - Upload/manage product images
 * - Write product descriptions
 * - Add specifications and details
 * - Mark as ready for MISA SKU request
 */

const ProductFulfillment = ({ productId, onComplete, onCancel, initialData = {} }) => {
  const [formData, setFormData] = useState({
    description: initialData.description || '',
    shortDescription: initialData.shortDescription || '',
    imageUrls: initialData.imageUrls || [],
    specifications: initialData.specifications || {},
    tags: initialData.tags || [],
    category: initialData.category || '',
    collection: initialData.collection || '',
    model3dId: initialData.model3dId || ''
  });

  const [saving, setSaving] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  const [uploadingImage, setUploadingImage] = useState(false);

  // Category options
  const categories = [
    'Rings',
    'Necklaces',
    'Bracelets',
    'Earrings',
    'Pendants',
    'Bangles',
    'Chains',
    'Sets'
  ];

  // Collection options
  const collections = [
    'Classic',
    'Modern',
    'Vintage',
    'Luxury',
    'Bridal',
    'Everyday',
    'Statement',
    'Minimalist'
  ];

  /**
   * Handle form field changes
   */
  const handleChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));

    // Clear validation error for this field
    if (validationErrors[field]) {
      setValidationErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  /**
   * Handle specification changes
   */
  const handleSpecChange = (key, value) => {
    setFormData(prev => ({
      ...prev,
      specifications: {
        ...prev.specifications,
        [key]: value
      }
    }));
  };

  /**
   * Add image URL
   */
  const addImageUrl = () => {
    const url = prompt('Enter image URL:');
    if (url && url.trim()) {
      setFormData(prev => ({
        ...prev,
        imageUrls: [...prev.imageUrls, url.trim()]
      }));
    }
  };

  /**
   * Remove image URL
   */
  const removeImageUrl = (index) => {
    if (window.confirm('Remove this image?')) {
      setFormData(prev => ({
        ...prev,
        imageUrls: prev.imageUrls.filter((_, i) => i !== index)
      }));
    }
  };

  /**
   * Move image position
   */
  const moveImage = (index, direction) => {
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= formData.imageUrls.length) return;

    const newUrls = [...formData.imageUrls];
    [newUrls[index], newUrls[newIndex]] = [newUrls[newIndex], newUrls[index]];

    setFormData(prev => ({
      ...prev,
      imageUrls: newUrls
    }));
  };

  /**
   * Add tag
   */
  const addTag = () => {
    const tag = prompt('Enter tag:');
    if (tag && tag.trim() && !formData.tags.includes(tag.trim())) {
      setFormData(prev => ({
        ...prev,
        tags: [...prev.tags, tag.trim()]
      }));
    }
  };

  /**
   * Remove tag
   */
  const removeTag = (index) => {
    setFormData(prev => ({
      ...prev,
      tags: prev.tags.filter((_, i) => i !== index)
    }));
  };

  /**
   * Validate form
   */
  const validateForm = () => {
    const errors = {};

    if (!formData.description || formData.description.trim().length < 50) {
      errors.description = 'Description must be at least 50 characters';
    }

    if (!formData.shortDescription || formData.shortDescription.trim().length < 20) {
      errors.shortDescription = 'Short description must be at least 20 characters';
    }

    if (formData.imageUrls.length === 0) {
      errors.imageUrls = 'At least one image is required';
    }

    if (!formData.category) {
      errors.category = 'Category is required';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  /**
   * Handle save
   */
  const handleSave = async () => {
    if (!validateForm()) {
      return;
    }

    setSaving(true);

    try {
      // TODO: Implement actual API call
      // await productOpsAPI.updateProductData(productId, formData);

      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Call parent callback
      if (onComplete) {
        onComplete(formData);
      }
    } catch (error) {
      console.error('Error saving product data:', error);
      alert('Failed to save product data. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  /**
   * Handle cancel
   */
  const handleCancel = () => {
    if (onCancel) {
      onCancel();
    }
  };

  return (
    <div className="admin-card" style={{ padding: '1.5rem' }}>
      {/* Header */}
      <div style={{ marginBottom: '1.5rem' }}>
        <h2 style={{ margin: '0 0 0.25rem 0', fontSize: '0.9375rem', fontWeight: '600', color: '#0f172a' }}>
          📸 Fill Assets & Product Data
        </h2>
        <p style={{ margin: 0, fontSize: '0.8125rem', color: '#64748b' }}>
          Step 3: Upload images and write product descriptions
        </p>
      </div>

      {/* Form */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {/* Product Images */}
        <div>
          <label className="admin-label">
            Product Images <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <p style={{ margin: '0 0 0.75rem 0', fontSize: '0.75rem', color: '#64748b' }}>
            Upload product images. First image will be the primary image.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: '0.75rem', marginBottom: '0.75rem' }}>
            {formData.imageUrls.map((url, index) => (
              <div
                key={index}
                style={{
                  position: 'relative',
                  aspectRatio: '1',
                  borderRadius: '6px',
                  overflow: 'hidden',
                  border: index === 0 ? '2px solid #3b82f6' : '1px solid #e2e8f0',
                  backgroundColor: '#f1f5f9'
                }}
              >
                <img
                  src={url}
                  alt={`Product ${index + 1}`}
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
                {index === 0 && (
                  <div style={{
                    position: 'absolute',
                    top: '0.25rem',
                    left: '0.25rem',
                    fontSize: '0.6875rem',
                    padding: '0.125rem 0.375rem',
                    borderRadius: '4px',
                    backgroundColor: '#3b82f6',
                    color: '#ffffff',
                    fontWeight: '600'
                  }}>
                    Primary
                  </div>
                )}
                <div style={{
                  position: 'absolute',
                  top: '0.25rem',
                  right: '0.25rem',
                  display: 'flex',
                  gap: '0.25rem'
                }}>
                  {index > 0 && (
                    <button
                      type="button"
                      onClick={() => moveImage(index, 'up')}
                      style={{
                        padding: '0.25rem',
                        backgroundColor: 'rgba(0, 0, 0, 0.6)',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '0.75rem',
                        lineHeight: 1
                      }}
                    >
                      ▲
                    </button>
                  )}
                  {index < formData.imageUrls.length - 1 && (
                    <button
                      type="button"
                      onClick={() => moveImage(index, 'down')}
                      style={{
                        padding: '0.25rem',
                        backgroundColor: 'rgba(0, 0, 0, 0.6)',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '0.75rem',
                        lineHeight: 1
                      }}
                    >
                      ▼
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => removeImageUrl(index)}
                    style={{
                      padding: '0.25rem 0.375rem',
                      backgroundColor: 'rgba(239, 68, 68, 0.9)',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '0.75rem',
                      lineHeight: 1
                    }}
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}

            {/* Add image button */}
            <button
              type="button"
              onClick={addImageUrl}
              style={{
                aspectRatio: '1',
                borderRadius: '6px',
                border: '2px dashed #cbd5e1',
                backgroundColor: '#f8fafc',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.25rem',
                color: '#64748b',
                fontSize: '0.75rem',
                transition: 'all 0.2s ease'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = '#3b82f6';
                e.currentTarget.style.backgroundColor = '#f0f9ff';
                e.currentTarget.style.color = '#3b82f6';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = '#cbd5e1';
                e.currentTarget.style.backgroundColor = '#f8fafc';
                e.currentTarget.style.color = '#64748b';
              }}
            >
              <span style={{ fontSize: '1.5rem' }}>+</span>
              <span>Add Image</span>
            </button>
          </div>

          {validationErrors.imageUrls && (
            <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.75rem', color: '#ef4444' }}>
              {validationErrors.imageUrls}
            </p>
          )}
        </div>

        {/* Category & Collection */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div>
            <label className="admin-label">
              Category <span style={{ color: '#ef4444' }}>*</span>
            </label>
            <select
              value={formData.category}
              onChange={(e) => handleChange('category', e.target.value)}
              className="admin-select"
            >
              <option value="">Select category...</option>
              {categories.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
            {validationErrors.category && (
              <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.75rem', color: '#ef4444' }}>
                {validationErrors.category}
              </p>
            )}
          </div>

          <div>
            <label className="admin-label">Collection (Optional)</label>
            <select
              value={formData.collection}
              onChange={(e) => handleChange('collection', e.target.value)}
              className="admin-select"
            >
              <option value="">Select collection...</option>
              {collections.map(col => (
                <option key={col} value={col}>{col}</option>
              ))}
            </select>
          </div>
        </div>

        {/* 3D Model ID */}
        <div>
          <label className="admin-label">
            3D Model ID (iJewel Drive)
          </label>
          <p style={{ margin: '0 0 0.5rem 0', fontSize: '0.75rem', color: '#64748b' }}>
            Enter the iJewel Drive model ID for 3D viewer display
          </p>
          <input
            type="text"
            value={formData.model3dId}
            onChange={(e) => handleChange('model3dId', e.target.value)}
            placeholder="e.g., RUsrBi-vQey2vExitZOYig"
            maxLength="100"
            className="admin-input"
          />
          <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.75rem', color: '#64748b' }}>
            Find model ID from iJewel Drive URL or dashboard
          </p>
        </div>

        {/* Short Description */}
        <div>
          <label className="admin-label">
            Short Description <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <p style={{ margin: '0 0 0.5rem 0', fontSize: '0.75rem', color: '#64748b' }}>
            Brief one-line description (20-100 characters)
          </p>
          <input
            type="text"
            value={formData.shortDescription}
            onChange={(e) => handleChange('shortDescription', e.target.value)}
            placeholder="e.g., Elegant diamond solitaire ring in 18K white gold"
            maxLength="100"
            className="admin-input"
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.25rem' }}>
            {validationErrors.shortDescription ? (
              <p style={{ margin: 0, fontSize: '0.75rem', color: '#ef4444' }}>
                {validationErrors.shortDescription}
              </p>
            ) : (
              <span />
            )}
            <p style={{ margin: 0, fontSize: '0.75rem', color: '#64748b' }}>
              {formData.shortDescription.length}/100
            </p>
          </div>
        </div>

        {/* Full Description */}
        <div>
          <label className="admin-label">
            Full Description <span style={{ color: '#ef4444' }}>*</span>
          </label>
          <p style={{ margin: '0 0 0.5rem 0', fontSize: '0.75rem', color: '#64748b' }}>
            Detailed product description (minimum 50 characters)
          </p>
          <textarea
            value={formData.description}
            onChange={(e) => handleChange('description', e.target.value)}
            placeholder="Write a detailed description of the product, including materials, craftsmanship, design features, and what makes it special..."
            rows={6}
            className="admin-input"
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.25rem' }}>
            {validationErrors.description ? (
              <p style={{ margin: 0, fontSize: '0.75rem', color: '#ef4444' }}>
                {validationErrors.description}
              </p>
            ) : (
              <span />
            )}
            <p style={{ margin: 0, fontSize: '0.75rem', color: '#64748b' }}>
              {formData.description.length} characters
            </p>
          </div>
        </div>

        {/* Tags */}
        <div>
          <label className="admin-label">Tags (Optional)</label>
          <p style={{ margin: '0 0 0.75rem 0', fontSize: '0.75rem', color: '#64748b' }}>
            Add keywords for better searchability
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.75rem' }}>
            {formData.tags.map((tag, index) => (
              <span
                key={index}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.375rem 0.75rem',
                  backgroundColor: '#eff6ff',
                  color: '#1e40af',
                  borderRadius: '16px',
                  fontSize: '0.8125rem',
                  fontWeight: '500'
                }}
              >
                {tag}
                <button
                  type="button"
                  onClick={() => removeTag(index)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#1e40af',
                    cursor: 'pointer',
                    padding: 0,
                    fontSize: '0.875rem',
                    lineHeight: 1
                  }}
                >
                  ✕
                </button>
              </span>
            ))}
            <button
              type="button"
              onClick={addTag}
              style={{
                padding: '0.375rem 0.75rem',
                backgroundColor: '#ffffff',
                border: '1px dashed #cbd5e1',
                borderRadius: '16px',
                color: '#64748b',
                fontSize: '0.8125rem',
                cursor: 'pointer'
              }}
            >
              + Add Tag
            </button>
          </div>
        </div>

        {/* Specifications */}
        <div>
          <label className="admin-label">Specifications (Optional)</label>
          <p style={{ margin: '0 0 0.75rem 0', fontSize: '0.75rem', color: '#64748b' }}>
            Technical details and product specifications
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <input
                type="text"
                value={formData.specifications.metal || ''}
                onChange={(e) => handleSpecChange('metal', e.target.value)}
                placeholder="Metal (e.g., 18K White Gold)"
                className="admin-input"
              />
              <input
                type="text"
                value={formData.specifications.gemstone || ''}
                onChange={(e) => handleSpecChange('gemstone', e.target.value)}
                placeholder="Gemstone (e.g., Diamond)"
                className="admin-input"
              />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <input
                type="text"
                value={formData.specifications.weight || ''}
                onChange={(e) => handleSpecChange('weight', e.target.value)}
                placeholder="Weight (e.g., 3.5g)"
                className="admin-input"
              />
              <input
                type="text"
                value={formData.specifications.dimensions || ''}
                onChange={(e) => handleSpecChange('dimensions', e.target.value)}
                placeholder="Dimensions (e.g., 15mm x 10mm)"
                className="admin-input"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Info Box */}
      <div style={{
        marginTop: '1.5rem',
        padding: '1rem',
        backgroundColor: '#eff6ff',
        borderRadius: '6px',
        borderLeft: '3px solid #3b82f6'
      }}>
        <div style={{ fontSize: '0.8125rem', color: '#1e40af', lineHeight: '1.6' }}>
          <strong>💡 Tips for great product listings:</strong>
          <ul style={{ margin: '0.5rem 0 0 0', paddingLeft: '1.25rem' }}>
            <li>Use high-quality images with good lighting</li>
            <li>First image should show the product clearly</li>
            <li>Write descriptions that highlight unique features</li>
            <li>Include technical specifications for professional buyers</li>
          </ul>
        </div>
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
        <button
          onClick={handleCancel}
          className="admin-button admin-button-secondary"
          disabled={saving}
        >
          Cancel
        </button>
        <button
          onClick={handleSave}
          className="admin-button admin-button-primary"
          disabled={saving}
        >
          {saving ? 'Saving...' : '✓ Save & Continue'}
        </button>
      </div>
    </div>
  );
};

export default ProductFulfillment;
