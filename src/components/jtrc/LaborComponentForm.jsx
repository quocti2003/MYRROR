import React from 'react';
import {
  calculateTotalLaborCost,
  formatVND,
} from '@services/jtrcService';
import './jtrc.css';

/**
 * LaborComponentForm - Form for entering labor costs (multiple items)
 *
 * @param {Object} props
 * @param {Array} props.laborItems - Array of labor items
 * @param {Function} props.onChange - Handler for data changes
 * @param {Object} props.errors - Validation errors
 * @param {boolean} props.disabled - Disable form inputs
 * @param {Array} props.laborTypeOptions - Labor type options from API [{code, name}]
 */
const LaborComponentForm = ({
  laborItems = [],
  onChange,
  errors = {},
  disabled = false,
  laborTypeOptions = [],
}) => {
  const addLaborItem = () => {
    const newItem = {
      id: `labor-${Date.now()}`,
      laborType: '',
      description: '',
      cost: 0,
    };
    onChange([...laborItems, newItem]);
  };

  const removeLaborItem = (index) => {
    if (!window.confirm('Remove this labor item?')) return;
    const newItems = laborItems.filter((_, i) => i !== index);
    onChange(newItems);
  };

  const updateLaborItem = (index, field, value) => {
    const newItems = [...laborItems];
    newItems[index] = { ...newItems[index], [field]: value };
    onChange(newItems);
  };

  const totalLaborCost = calculateTotalLaborCost(laborItems);

  const getLaborError = (index, field) => {
    const key = `labor[${index}].${field}`;
    return errors[key];
  };

  return (
    <div className="labor-component-form">
      <div className="labor-header">
        <h3 className="jtrc-section-title">Labor Costs</h3>
        <button
          type="button"
          className="admin-button admin-button-primary admin-button-sm"
          onClick={addLaborItem}
          disabled={disabled}
        >
          + Add Labor
        </button>
      </div>

      {laborItems.length === 0 ? (
        <div className="labor-empty-state">
          <p>No labor items added yet.</p>
          <p className="admin-form-hint">Click "Add Labor" to add a labor cost item.</p>
        </div>
      ) : (
        <div className="labor-table-wrapper">
          <table className="labor-table admin-table">
            <thead>
              <tr>
                <th style={{ width: '200px' }}>Type</th>
                <th>Description</th>
                <th style={{ width: '180px', textAlign: 'right' }}>Cost (VND)</th>
                <th style={{ width: '60px', textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {laborItems.map((item, index) => (
                <tr key={item.id || index}>
                  <td>
                    <select
                      className={`admin-select labor-select ${getLaborError(index, 'laborType') ? 'admin-select-error' : ''}`}
                      value={item.laborType}
                      onChange={(e) => updateLaborItem(index, 'laborType', e.target.value)}
                      disabled={disabled}
                    >
                      <option value="">Select Type</option>
                      {laborTypeOptions.map((type) => (
                        <option key={type.code} value={type.code}>
                          {type.name}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <input
                      type="text"
                      className="admin-input labor-input"
                      value={item.description}
                      onChange={(e) => updateLaborItem(index, 'description', e.target.value)}
                      placeholder="Description of work"
                      disabled={disabled}
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      step="1000"
                      min="0"
                      className={`admin-input labor-input labor-cost-input ${getLaborError(index, 'cost') ? 'admin-input-error' : ''}`}
                      value={item.cost}
                      onChange={(e) => updateLaborItem(index, 'cost', parseFloat(e.target.value) || 0)}
                      placeholder="0"
                      disabled={disabled}
                    />
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <button
                      type="button"
                      className="labor-remove-btn"
                      onClick={() => removeLaborItem(index)}
                      disabled={disabled}
                      title="Remove labor item"
                    >
                      x
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Total Labor Cost */}
      <div className="labor-total-cost">
        <span className="labor-total-label">Total Labor Cost:</span>
        <span className="labor-total-value">{formatVND(totalLaborCost)}</span>
      </div>
    </div>
  );
};

export default LaborComponentForm;
