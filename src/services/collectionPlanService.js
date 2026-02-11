import api from './api';

// ===== COLLECTION PLAN API =====
export const collectionPlanAPI = {
  getAll: (params = {}) => api.get('/api/v1/collection-plans', { params }),
  getById: (id) => api.get(`/api/v1/collection-plans/${id}`),
  create: (data) => api.post('/api/v1/collection-plans', data),
  update: (id, data) => api.put(`/api/v1/collection-plans/${id}`, data),
  delete: (id) => api.delete(`/api/v1/collection-plans/${id}`),
  updateStatus: (id, status) => api.patch(`/api/v1/collection-plans/${id}/status`, { status }),
  getStats: () => api.get('/api/v1/collection-plans/stats'),

  // Item operations
  addItem: (planId, data) => api.post(`/api/v1/collection-plans/${planId}/items`, data),
  updateItem: (planId, itemId, data) => api.put(`/api/v1/collection-plans/${planId}/items/${itemId}`, data),
  removeItem: (planId, itemId) => api.delete(`/api/v1/collection-plans/${planId}/items/${itemId}`),
};

// ===== CONSTANTS =====
export const COLLECTION_PLAN_STATUS = {
  DRAFT: 'DRAFT',
  APPROVED: 'APPROVED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
};

export const STATUS_CONFIG = {
  DRAFT: { bg: '#f1f5f9', color: '#475569', label: 'Draft' },
  APPROVED: { bg: '#dbeafe', color: '#1e40af', label: 'Approved' },
  IN_PROGRESS: { bg: '#fef3c7', color: '#854d0e', label: 'In Progress' },
  COMPLETED: { bg: '#dcfce7', color: '#166534', label: 'Completed' },
  CANCELLED: { bg: '#fee2e2', color: '#991b1b', label: 'Cancelled' },
};

export const PRODUCT_TYPES = [
  { value: 'RING', label: 'Ring' },
  { value: 'NECKLACE', label: 'Necklace' },
  { value: 'EARRING', label: 'Earring' },
  { value: 'BRACELET', label: 'Bracelet' },
  { value: 'PENDANT', label: 'Pendant' },
  { value: 'BROOCH', label: 'Brooch' },
  { value: 'OTHER', label: 'Other' },
];

// ===== HELPERS =====

export const getStatusConfig = (status) => {
  return STATUS_CONFIG[status] || STATUS_CONFIG.DRAFT;
};

export const isPlanEditable = (status) => {
  return [COLLECTION_PLAN_STATUS.DRAFT, COLLECTION_PLAN_STATUS.APPROVED].includes(status);
};

export const validateCollectionPlan = (plan) => {
  const errors = {};

  if (!plan.name?.trim()) {
    errors.name = 'Plan name is required';
  }

  if (plan.deadline && plan.estimatedDeliveryDate) {
    const deadline = new Date(plan.deadline);
    const delivery = new Date(plan.estimatedDeliveryDate);
    if (delivery.getTime() < deadline.getTime()) {
      errors.estimatedDeliveryDate = 'Delivery date should not be before deadline';
    }
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
};

export const calculateTotalCost = (items) => {
  if (!items || items.length === 0) return 0;
  return items.reduce((sum, item) => {
    const qty = item.targetQuantity || 0;
    const cost = parseFloat(item.estimatedUnitCost) || 0;
    return sum + qty * cost;
  }, 0);
};

export const formatCurrency = (amount) => {
  if (amount == null) return '-';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount);
};

export default collectionPlanAPI;
