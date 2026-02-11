import React, { useEffect } from 'react';
import {
  calculateMetalCost,
  formatVND,
} from '@services/jtrcService';
import './jtrc.css';

/**
 * MetalComponentForm - Form for entering metal specifications
 *
 * @param {Object} props
 * @param {Object} props.data - Metal component data
 * @param {Function} props.onChange - Handler for data changes
 * @param {number} props.goldPricePerGram - Current gold price per gram
 * @param {Object} props.errors - Validation errors
 * @param {boolean} props.disabled - Disable form inputs
 * @param {Array} props.metalTypeOptions - Metal type options from API [{code, name}]
 * @param {Array} props.metalPurityOptions - Metal purity options from API [{code, name}]
 */
const MetalComponentForm = ({
  data = {},
  onChange,
  goldPricePerGram = 0,
  errors = {},
  disabled = false,
  metalTypeOptions = [],
  metalPurityOptions = [],
}) => {
  const {
    metalType = '',
    metalPurity = '',
    weight = '',
    lossRate = 3,
    pricePerGram = goldPricePerGram,
    metalCost = 0,
    lossCost = 0,
    totalMetalCost = 0,
  } = data;

  // Auto-calculate costs when inputs change
  useEffect(() => {
    if (weight && pricePerGram) {
      const costs = calculateMetalCost(parseFloat(weight), parseFloat(pricePerGram), parseFloat(lossRate));
      onChange({
        ...data,
        metalCost: costs.metalCost,
        lossCost: costs.lossCost,
        totalMetalCost: costs.totalMetalCost,
      });
    }
  }, [weight, pricePerGram, lossRate]);

  // Update price per gram when gold price changes
  useEffect(() => {
    if (goldPricePerGram && !data.pricePerGram) {
      onChange({ ...data, pricePerGram: goldPricePerGram });
    }
  }, [goldPricePerGram]);

  const handleChange = (field, value) => {
    onChange({ ...data, [field]: value });
  };

  return (
    <div className="metal-component-form">
      <h3 className="jtrc-section-title">Metal Specification</h3>

      <div className="admin-grid admin-grid-3">
        {/* Metal Type */}
        <div className="admin-form-group">
          <label className="admin-form-label">
            Metal Type <span className="required">*</span>
          </label>
          <select
            className={`admin-select ${errors.metalType ? 'admin-select-error' : ''}`}
            value={metalType}
            onChange={(e) => handleChange('metalType', e.target.value)}
            disabled={disabled}
          >
            <option value="">Select Metal Type</option>
            {metalTypeOptions.map((type) => (
              <option key={type.code} value={type.code}>
                {type.name}
              </option>
            ))}
          </select>
          {errors.metalType && (
            <span className="admin-error-message">{errors.metalType}</span>
          )}
        </div>

        {/* Metal Purity */}
        <div className="admin-form-group">
          <label className="admin-form-label">
            Metal Purity <span className="required">*</span>
          </label>
          <select
            className={`admin-select ${errors.metalPurity ? 'admin-select-error' : ''}`}
            value={metalPurity}
            onChange={(e) => handleChange('metalPurity', e.target.value)}
            disabled={disabled}
          >
            <option value="">Select Purity</option>
            {metalPurityOptions.map((purity) => (
              <option key={purity.code} value={purity.code}>
                {purity.name}
              </option>
            ))}
          </select>
          {errors.metalPurity && (
            <span className="admin-error-message">{errors.metalPurity}</span>
          )}
        </div>

        {/* Weight */}
        <div className="admin-form-group">
          <label className="admin-form-label">
            Weight (grams) <span className="required">*</span>
          </label>
          <input
            type="number"
            step="0.01"
            min="0"
            className={`admin-input ${errors.weight ? 'admin-input-error' : ''}`}
            value={weight}
            onChange={(e) => handleChange('weight', e.target.value)}
            placeholder="0.00"
            disabled={disabled}
          />
          {errors.weight && (
            <span className="admin-error-message">{errors.weight}</span>
          )}
        </div>
      </div>

      <div className="admin-grid admin-grid-2">
        {/* Loss Rate */}
        <div className="admin-form-group">
          <label className="admin-form-label">Loss Rate (%)</label>
          <input
            type="number"
            step="0.1"
            min="0"
            max="100"
            className="admin-input"
            value={lossRate}
            onChange={(e) => handleChange('lossRate', e.target.value)}
            placeholder="3.0"
            disabled={disabled}
          />
          <span className="admin-form-hint">
            Typical loss rate during production (default: 3%)
          </span>
        </div>

        {/* Price per Gram */}
        <div className="admin-form-group">
          <label className="admin-form-label">Price per Gram (VND)</label>
          <input
            type="number"
            step="1000"
            min="0"
            className="admin-input"
            value={pricePerGram}
            onChange={(e) => handleChange('pricePerGram', e.target.value)}
            placeholder="1,850,000"
            disabled={disabled}
          />
          <span className="admin-form-hint">
            Auto-filled from header. Override if needed.
          </span>
        </div>
      </div>

      {/* Cost Calculation (Read-only) */}
      <div className="metal-cost-calculation">
        <h4 className="cost-calculation-title">Cost Calculation (Auto)</h4>
        <div className="cost-calculation-grid">
          <div className="cost-calculation-item">
            <span className="cost-label">Metal Cost</span>
            <span className="cost-value">{formatVND(metalCost)}</span>
          </div>
          <div className="cost-calculation-item">
            <span className="cost-label">Loss Cost</span>
            <span className="cost-value">{formatVND(lossCost)}</span>
          </div>
          <div className="cost-calculation-item cost-calculation-total">
            <span className="cost-label">Total Metal Cost</span>
            <span className="cost-value">{formatVND(totalMetalCost)}</span>
          </div>
        </div>
        <p className="cost-calculation-note">
          Calculation: ({weight || 0}g x {formatVND(pricePerGram)}) + {lossRate || 0}% loss
        </p>
      </div>
    </div>
  );
};

export default MetalComponentForm;
