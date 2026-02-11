import React, { useState } from 'react';
import {
  COLOR_CATEGORIES,
  calculateStonePrice,
  calculateTotalStoneCost,
  formatVND,
} from '@services/jtrcService';
import './jtrc.css';

/**
 * StoneComponentForm - Form for entering stone specifications (multiple stones)
 *
 * @param {Object} props
 * @param {Array} props.stones - Array of stone components
 * @param {Function} props.onChange - Handler for data changes
 * @param {Object} props.errors - Validation errors
 * @param {boolean} props.disabled - Disable form inputs
 * @param {Array} props.stoneRoleOptions - Stone role options from API [{code, name}]
 * @param {Array} props.stoneTypeOptions - Stone type options from API [{code, name}]
 * @param {Array} props.stoneShapeOptions - Stone shape options from API [{code, name}]
 * @param {Array} props.colorGradeOptions - Color grade options from API [{code, name}]
 * @param {Array} props.clarityGradeOptions - Clarity grade options from API [{code, name}]
 */
const StoneComponentForm = ({
  stones = [],
  onChange,
  errors = {},
  disabled = false,
  stoneRoleOptions = [],
  stoneTypeOptions = [],
  stoneShapeOptions = [],
  colorGradeOptions = [],
  clarityGradeOptions = [],
}) => {
  const [expandedStones, setExpandedStones] = useState({});

  const toggleStoneExpand = (index) => {
    setExpandedStones((prev) => ({
      ...prev,
      [index]: !prev[index],
    }));
  };

  const addStone = () => {
    const newStone = {
      id: `stone-${Date.now()}`,
      role: '',
      stoneType: '',
      stoneDetail: '',
      shape: '',
      colorCategory: '',
      colorGrade: '',
      colorIntensity: '',
      colorName: '',
      clarity: '',
      sizeMm: '',
      caratWeight: '',
      quantity: 1,
      unitPrice: 0,
      totalPrice: 0,
    };
    const newStones = [...stones, newStone];
    onChange(newStones);
    // Expand the new stone
    setExpandedStones((prev) => ({
      ...prev,
      [newStones.length - 1]: true,
    }));
  };

  const removeStone = (index) => {
    if (!window.confirm('Remove this stone component?')) return;
    const newStones = stones.filter((_, i) => i !== index);
    onChange(newStones);
  };

  const updateStone = (index, field, value) => {
    const newStones = [...stones];
    newStones[index] = { ...newStones[index], [field]: value };

    // Auto-calculate total price when quantity or unit price changes
    if (field === 'quantity' || field === 'unitPrice') {
      newStones[index].totalPrice = calculateStonePrice(
        newStones[index].quantity,
        newStones[index].unitPrice
      );
    }

    onChange(newStones);
  };

  const totalStoneCost = calculateTotalStoneCost(stones);

  const getRoleLabel = (role) => {
    const found = stoneRoleOptions.find((r) => r.code === role);
    return found?.name || role || 'Stone';
  };

  const getStoneError = (index, field) => {
    const key = `stones[${index}].${field}`;
    return errors[key];
  };

  return (
    <div className="stone-component-form">
      <div className="stone-header">
        <h3 className="jtrc-section-title">Stone Components</h3>
        <button
          type="button"
          className="admin-button admin-button-primary admin-button-sm"
          onClick={addStone}
          disabled={disabled}
        >
          + Add Stone
        </button>
      </div>

      {stones.length === 0 ? (
        <div className="stone-empty-state">
          <p>No stone components added yet.</p>
          <p className="admin-form-hint">Click "Add Stone" to add a stone component.</p>
        </div>
      ) : (
        <div className="stone-list">
          {stones.map((stone, index) => (
            <div key={stone.id || index} className="stone-card">
              <div
                className="stone-card-header"
                onClick={() => toggleStoneExpand(index)}
              >
                <div className="stone-card-title">
                  <span className="stone-role-badge">
                    {getRoleLabel(stone.role) || `Stone ${index + 1}`}
                  </span>
                  {stone.stoneType && (
                    <span className="stone-type-label">
                      {stoneTypeOptions.find((t) => t.code === stone.stoneType)?.name || stone.stoneType}
                    </span>
                  )}
                  {stone.caratWeight && (
                    <span className="stone-carat-label">{stone.caratWeight}ct</span>
                  )}
                </div>
                <div className="stone-card-actions">
                  <span className="stone-price">{formatVND(stone.totalPrice)}</span>
                  <button
                    type="button"
                    className="stone-remove-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      removeStone(index);
                    }}
                    disabled={disabled}
                    title="Remove stone"
                  >
                    x
                  </button>
                  <span className="stone-expand-icon">
                    {expandedStones[index] ? '−' : '+'}
                  </span>
                </div>
              </div>

              {(expandedStones[index] || expandedStones[index] === undefined) && (
                <div className="stone-card-body">
                  <div className="admin-grid admin-grid-3">
                    {/* Stone Role */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">
                        Stone Role <span className="required">*</span>
                      </label>
                      <select
                        className={`admin-select ${getStoneError(index, 'role') ? 'admin-select-error' : ''}`}
                        value={stone.role}
                        onChange={(e) => updateStone(index, 'role', e.target.value)}
                        disabled={disabled}
                      >
                        <option value="">Select Role</option>
                        {stoneRoleOptions.map((role) => (
                          <option key={role.code} value={role.code}>
                            {role.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Stone Type */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">
                        Stone Type <span className="required">*</span>
                      </label>
                      <select
                        className={`admin-select ${getStoneError(index, 'stoneType') ? 'admin-select-error' : ''}`}
                        value={stone.stoneType}
                        onChange={(e) => updateStone(index, 'stoneType', e.target.value)}
                        disabled={disabled}
                      >
                        <option value="">Select Type</option>
                        {stoneTypeOptions.map((type) => (
                          <option key={type.code} value={type.code}>
                            {type.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Stone Detail */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Stone Detail</label>
                      <input
                        type="text"
                        className="admin-input"
                        value={stone.stoneDetail}
                        onChange={(e) => updateStone(index, 'stoneDetail', e.target.value)}
                        placeholder="e.g., CVD Hearts & Arrows"
                        disabled={disabled}
                      />
                    </div>
                  </div>

                  <div className="admin-grid admin-grid-3">
                    {/* Shape */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Shape</label>
                      <select
                        className="admin-select"
                        value={stone.shape}
                        onChange={(e) => updateStone(index, 'shape', e.target.value)}
                        disabled={disabled}
                      >
                        <option value="">Select Shape</option>
                        {stoneShapeOptions.map((shape) => (
                          <option key={shape.code} value={shape.code}>
                            {shape.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Color Category */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Color Category</label>
                      <select
                        className="admin-select"
                        value={stone.colorCategory}
                        onChange={(e) => updateStone(index, 'colorCategory', e.target.value)}
                        disabled={disabled}
                      >
                        <option value="">Select Category</option>
                        {COLOR_CATEGORIES.map((cat) => (
                          <option key={cat.value} value={cat.value}>
                            {cat.label}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Conditional: Color Grade (for Colorless) */}
                    {stone.colorCategory === 'COLORLESS' && (
                      <div className="admin-form-group">
                        <label className="admin-form-label">Color Grade</label>
                        <select
                          className="admin-select"
                          value={stone.colorGrade}
                          onChange={(e) => updateStone(index, 'colorGrade', e.target.value)}
                          disabled={disabled}
                        >
                          <option value="">Select Grade</option>
                          {colorGradeOptions.map((grade) => (
                            <option key={grade.code} value={grade.code}>
                              {grade.name}
                            </option>
                          ))}
                        </select>
                      </div>
                    )}

                    {/* Conditional: Color Intensity + Name (for Fancy Color) */}
                    {stone.colorCategory === 'FANCY_COLOR' && (
                      <>
                        <div className="admin-form-group">
                          <label className="admin-form-label">Color Intensity</label>
                          <input
                            type="text"
                            className="admin-input"
                            value={stone.colorIntensity}
                            onChange={(e) => updateStone(index, 'colorIntensity', e.target.value)}
                            placeholder="e.g., Fancy Vivid"
                            disabled={disabled}
                          />
                        </div>
                        <div className="admin-form-group">
                          <label className="admin-form-label">Color Name</label>
                          <input
                            type="text"
                            className="admin-input"
                            value={stone.colorName}
                            onChange={(e) => updateStone(index, 'colorName', e.target.value)}
                            placeholder="e.g., Blue"
                            disabled={disabled}
                          />
                        </div>
                      </>
                    )}

                    {/* Conditional: Color Stone Type (for Color Stone) */}
                    {stone.colorCategory === 'COLOR_STONE' && (
                      <div className="admin-form-group">
                        <label className="admin-form-label">Stone Color</label>
                        <input
                          type="text"
                          className="admin-input"
                          value={stone.colorName}
                          onChange={(e) => updateStone(index, 'colorName', e.target.value)}
                          placeholder="e.g., Pigeon Blood Red"
                          disabled={disabled}
                        />
                      </div>
                    )}
                  </div>

                  <div className="admin-grid admin-grid-3">
                    {/* Clarity */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Clarity</label>
                      <select
                        className="admin-select"
                        value={stone.clarity}
                        onChange={(e) => updateStone(index, 'clarity', e.target.value)}
                        disabled={disabled}
                      >
                        <option value="">Select Clarity</option>
                        {clarityGradeOptions.map((grade) => (
                          <option key={grade.code} value={grade.code}>
                            {grade.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Size (mm) */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Size (mm)</label>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        className="admin-input"
                        value={stone.sizeMm}
                        onChange={(e) => updateStone(index, 'sizeMm', e.target.value)}
                        placeholder="0.00"
                        disabled={disabled}
                      />
                    </div>

                    {/* Carat Weight */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Carat Weight</label>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        className="admin-input"
                        value={stone.caratWeight}
                        onChange={(e) => updateStone(index, 'caratWeight', e.target.value)}
                        placeholder="0.00"
                        disabled={disabled}
                      />
                    </div>
                  </div>

                  <div className="admin-grid admin-grid-3">
                    {/* Quantity */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Quantity</label>
                      <input
                        type="number"
                        step="1"
                        min="1"
                        className="admin-input"
                        value={stone.quantity}
                        onChange={(e) => updateStone(index, 'quantity', parseInt(e.target.value) || 1)}
                        placeholder="1"
                        disabled={disabled}
                      />
                    </div>

                    {/* Unit Price */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Unit Price (VND)</label>
                      <input
                        type="number"
                        step="1000"
                        min="0"
                        className="admin-input"
                        value={stone.unitPrice}
                        onChange={(e) => updateStone(index, 'unitPrice', parseFloat(e.target.value) || 0)}
                        placeholder="0"
                        disabled={disabled}
                      />
                    </div>

                    {/* Total Price (Read-only) */}
                    <div className="admin-form-group">
                      <label className="admin-form-label">Total Price</label>
                      <input
                        type="text"
                        className="admin-input cost-display-field"
                        value={formatVND(stone.totalPrice)}
                        readOnly
                        disabled
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Total Stone Cost */}
      <div className="stone-total-cost">
        <span className="stone-total-label">Total Stone Cost:</span>
        <span className="stone-total-value">{formatVND(totalStoneCost)}</span>
      </div>
    </div>
  );
};

export default StoneComponentForm;
