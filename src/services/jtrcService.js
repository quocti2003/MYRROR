import api from './api';

/**
 * JTRC (Jewelry Technical Report Card) Service
 * Handles all API calls for JTRC management
 */

// ===== JTRC API =====
export const jtrcAPI = {
  // Get all JTRCs with optional filters
  getAll: (params = {}) => api.get('/api/v1/jtrc', { params }),

  // Get JTRC by ID
  getById: (id) => api.get(`/api/v1/jtrc/${id}`),

  // Get JTRC by report number
  getByReportNumber: (reportNumber) =>
    api.get(`/api/v1/jtrc/report-number/${encodeURIComponent(reportNumber)}`),

  // Search JTRCs
  search: (query, params = {}) =>
    api.get('/api/v1/jtrc/search', { params: { q: query, ...params } }),

  // Get JTRCs by collection
  getByCollection: (collectionId, params = {}) =>
    api.get(`/api/v1/jtrc/collection/${collectionId}`, { params }),

  // Get JTRCs by status
  getByStatus: (status, params = {}) =>
    api.get(`/api/v1/jtrc/status/${status}`, { params }),

  // Get JTRCs by category
  getByCategory: (category, params = {}) =>
    api.get(`/api/v1/jtrc/category/${encodeURIComponent(category)}`, { params }),

  // Get JTRCs by season
  getBySeason: (season, params = {}) =>
    api.get(`/api/v1/jtrc/season/${encodeURIComponent(season)}`, { params }),

  // Get filter options (collections, seasons, categories, statuses)
  getFilterOptions: () => api.get('/api/v1/jtrc/filter-options'),

  // Get statistics
  getStatistics: () => api.get('/api/v1/jtrc/statistics'),

  // CRUD operations
  create: (data) => api.post('/api/v1/jtrc', data),

  update: (id, data) => api.put(`/api/v1/jtrc/${id}`, data),

  delete: (id) => api.delete(`/api/v1/jtrc/${id}`),

  // Duplicate JTRC
  duplicate: (id) => api.post(`/api/v1/jtrc/${id}/duplicate`),

  // Status transitions
  submitForApproval: (id) => api.post(`/api/v1/jtrc/${id}/submit`),

  approve: (id) => api.post(`/api/v1/jtrc/${id}/approve`),

  reject: (id, reason) => api.post(`/api/v1/jtrc/${id}/reject`, { reason }),

  archive: (id) => api.post(`/api/v1/jtrc/${id}/archive`),

  unarchive: (id) => api.post(`/api/v1/jtrc/${id}/unarchive`),

  // Save as draft
  saveDraft: (id, data) => api.put(`/api/v1/jtrc/${id}/draft`, data),

  // Metal component operations
  updateMetalComponent: (jtrcId, metalData) =>
    api.put(`/api/v1/jtrc/${jtrcId}/metal`, metalData),

  // Stone component operations
  addStoneComponent: (jtrcId, stoneData) =>
    api.post(`/api/v1/jtrc/${jtrcId}/stones`, stoneData),

  updateStoneComponent: (jtrcId, stoneId, stoneData) =>
    api.put(`/api/v1/jtrc/${jtrcId}/stones/${stoneId}`, stoneData),

  removeStoneComponent: (jtrcId, stoneId) =>
    api.delete(`/api/v1/jtrc/${jtrcId}/stones/${stoneId}`),

  // Labor component operations
  addLaborComponent: (jtrcId, laborData) =>
    api.post(`/api/v1/jtrc/${jtrcId}/labor`, laborData),

  updateLaborComponent: (jtrcId, laborId, laborData) =>
    api.put(`/api/v1/jtrc/${jtrcId}/labor/${laborId}`, laborData),

  removeLaborComponent: (jtrcId, laborId) =>
    api.delete(`/api/v1/jtrc/${jtrcId}/labor/${laborId}`),

  // Visual assets operations
  uploadAsset: (jtrcId, assetType, file) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('assetType', assetType);
    return api.post(`/api/v1/jtrc/${jtrcId}/assets`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    });
  },

  removeAsset: (jtrcId, assetType) =>
    api.delete(`/api/v1/jtrc/${jtrcId}/assets/${assetType}`),

  // Get current gold price
  getGoldPrice: () => api.get('/api/v1/jtrc/gold-price'),

  // Refresh gold price
  refreshGoldPrice: () => api.post('/api/v1/jtrc/gold-price/refresh'),

  // Calculate costs (for preview before save)
  calculateCosts: (data) => api.post('/api/v1/jtrc/calculate-costs', data),
};

// ===== JTRC CONSTANTS =====
export const JTRC_STATUS = {
  DRAFT: 'DRAFT',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  ARCHIVED: 'ARCHIVED',
};

export const JTRC_STATUS_CONFIG = {
  DRAFT: { bg: '#fef3c7', color: '#854d0e', label: 'Draft' },
  PENDING_APPROVAL: { bg: '#dbeafe', color: '#1e40af', label: 'Pending Approval' },
  APPROVED: { bg: '#dcfce7', color: '#166534', label: 'Approved' },
  REJECTED: { bg: '#fee2e2', color: '#991b1b', label: 'Rejected' },
  ARCHIVED: { bg: '#f1f5f9', color: '#475569', label: 'Archived' },
};

// Color categories: local constant (3 values, drives conditional UI rendering)
export const COLOR_CATEGORIES = [
  { value: 'COLORLESS', label: 'Colorless' },
  { value: 'FANCY_COLOR', label: 'Fancy Color' },
  { value: 'COLOR_STONE', label: 'Color Stone' },
];

export const ASSET_TYPES = {
  RENDER_3D: 'render_3d',
  STONE_MAP: 'stone_map',
  TECHNICAL_DRAWING: 'technical_drawing',
};

// Sources: local constant (not a dropdown table, static list)
export const SOURCES = [
  { value: 'FACTORY', label: 'Factory' },
  { value: 'DESIGNER', label: 'Designer' },
  { value: 'CLIENT_REQUEST', label: 'Client Request' },
  { value: 'COLLECTION', label: 'Collection' },
];

// ===== HELPER FUNCTIONS =====

/**
 * Calculate metal cost based on weight, purity, and price
 */
export const calculateMetalCost = (weight, pricePerGram, lossRate = 0) => {
  if (!weight || !pricePerGram) return { metalCost: 0, lossCost: 0, totalMetalCost: 0 };

  const metalCost = weight * pricePerGram;
  const lossCost = metalCost * (lossRate / 100);
  const totalMetalCost = metalCost + lossCost;

  return {
    metalCost: Math.round(metalCost),
    lossCost: Math.round(lossCost),
    totalMetalCost: Math.round(totalMetalCost),
  };
};

/**
 * Calculate stone total price
 */
export const calculateStonePrice = (quantity, unitPrice) => {
  if (!quantity || !unitPrice) return 0;
  return Math.round(quantity * unitPrice);
};

/**
 * Calculate total stone cost from array of stones
 */
export const calculateTotalStoneCost = (stones = []) => {
  return stones.reduce((sum, stone) => {
    return sum + calculateStonePrice(stone.quantity, stone.unitPrice);
  }, 0);
};

/**
 * Calculate total labor cost from array of labor items
 */
export const calculateTotalLaborCost = (laborItems = []) => {
  return laborItems.reduce((sum, item) => sum + (item.cost || 0), 0);
};

/**
 * Calculate total COGS
 */
export const calculateTotalCOGS = (metalCost, stoneCost, laborCost) => {
  return (metalCost || 0) + (stoneCost || 0) + (laborCost || 0);
};

/**
 * Convert VND to USD
 */
export const convertVNDtoUSD = (vnd, exchangeRate) => {
  if (!vnd || !exchangeRate) return 0;
  return Math.round((vnd / exchangeRate) * 100) / 100;
};

/**
 * Format currency in VND
 */
export const formatVND = (amount) => {
  if (!amount) return '0 VND';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
};

/**
 * Format currency in USD
 */
export const formatUSD = (amount) => {
  if (!amount) return '$0.00';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

/**
 * Generate JTRC report number
 */
export const generateReportNumber = (year, season, sequence) => {
  const seasonCode = season?.substring(0, 3).toUpperCase() || 'XXX';
  const seq = String(sequence).padStart(3, '0');
  return `JTRC-${year}-${seasonCode}-${seq}`;
};

export default jtrcAPI;
