import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  collectionPlanAPI,
  COLLECTION_PLAN_STATUS,
  STATUS_CONFIG,
  getStatusConfig,
  formatCurrency,
} from '@services/collectionPlanService';
import AdminTable, { TableActions, ActionButton } from '@components/admin-dashboard/AdminTable';
import { SkeletonStatsGrid } from '@components/admin-dashboard/Skeleton';
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

const CollectionPlanListPage = () => {
  const navigate = useNavigate();

  // State
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalCount, setTotalCount] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 20;

  // Filter state
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Stats
  const [stats, setStats] = useState({
    total: 0,
    DRAFT: 0,
    APPROVED: 0,
    IN_PROGRESS: 0,
    COMPLETED: 0,
  });

  // Fetch plans
  const fetchPlans = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page: currentPage,
        size: pageSize,
        ...(searchQuery && { search: searchQuery }),
        ...(statusFilter && { status: statusFilter }),
      };

      const response = await collectionPlanAPI.getAll(params);
      const data = response.data;

      if (data.content) {
        setPlans(data.content);
        setTotalCount(data.totalItems || data.content.length);
      } else if (Array.isArray(data)) {
        setPlans(data);
        setTotalCount(data.length);
      } else {
        setPlans([]);
        setTotalCount(0);
      }
    } catch (err) {
      console.error('Error fetching collection plans:', err);
      setError('Failed to load collection plans. Please try again.');
      setPlans([]);
    } finally {
      setLoading(false);
    }
  }, [currentPage, searchQuery, statusFilter]);

  // Fetch stats
  const fetchStats = useCallback(async () => {
    try {
      const response = await collectionPlanAPI.getStats();
      setStats(response.data);
    } catch (err) {
      console.error('Error fetching stats:', err);
    }
  }, []);

  useEffect(() => {
    fetchPlans();
    fetchStats();
  }, [fetchPlans, fetchStats]);

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchQuery(searchInput);
      setCurrentPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // Handlers
  const handleCreateNew = () => {
    navigate('/dashboard/admin?tab=collection-plan-form&mode=new');
  };

  const handleView = (plan) => {
    navigate(`/dashboard/admin?tab=collection-plan-form&mode=view&id=${plan.id}`);
  };

  const handleEdit = (plan) => {
    navigate(`/dashboard/admin?tab=collection-plan-form&mode=edit&id=${plan.id}`);
  };

  const handleDelete = async (plan) => {
    if (!window.confirm(`Cancel collection plan "${plan.name}"? This will set its status to Cancelled.`)) {
      return;
    }
    try {
      await collectionPlanAPI.delete(plan.id);
      fetchPlans();
      fetchStats();
    } catch (err) {
      console.error('Error deleting collection plan:', err);
      alert(err.response?.data || 'Failed to cancel collection plan.');
    }
  };

  const handlePageChange = (newPage) => {
    setCurrentPage(newPage);
  };

  // Table columns
  const columns = useMemo(
    () => [
      {
        key: 'name',
        header: 'Plan Name',
        sortable: true,
        render: (value) => (
          <div className="admin-table-primary">{value}</div>
        ),
      },
      {
        key: 'itemCount',
        header: 'Items',
        sortable: false,
        align: 'center',
        render: (value) => value || 0,
      },
      {
        key: 'totalQuantity',
        header: 'Total Qty',
        sortable: true,
        align: 'center',
        render: (value) => value || 0,
      },
      {
        key: 'deadline',
        header: 'Deadline',
        sortable: true,
        render: (value) =>
          value ? new Date(value).toLocaleDateString() : '-',
      },
      {
        key: 'status',
        header: 'Status',
        sortable: true,
        render: (value) => <StatusBadge status={value} />,
      },
      {
        key: 'totalCost',
        header: 'Est. Cost',
        sortable: true,
        align: 'right',
        render: (value) => formatCurrency(value),
      },
      {
        key: 'createdAt',
        header: 'Created',
        sortable: true,
        render: (value) =>
          value ? new Date(value).toLocaleDateString() : '-',
      },
      {
        key: 'actions',
        header: 'Actions',
        sortable: false,
        render: (_, row) => (
          <TableActions>
            <ActionButton onClick={() => handleView(row)} title="View">
              View
            </ActionButton>
            {row.status !== COLLECTION_PLAN_STATUS.CANCELLED &&
              row.status !== COLLECTION_PLAN_STATUS.COMPLETED && (
                <ActionButton onClick={() => handleEdit(row)} title="Edit">
                  Edit
                </ActionButton>
              )}
            {row.status !== COLLECTION_PLAN_STATUS.CANCELLED &&
              row.status !== COLLECTION_PLAN_STATUS.COMPLETED && (
                <ActionButton
                  onClick={() => handleDelete(row)}
                  variant="danger"
                  title="Cancel"
                >
                  Cancel
                </ActionButton>
              )}
          </TableActions>
        ),
      },
    ],
    []
  );

  const statusOptions = [
    { value: '', label: 'All Statuses' },
    { value: COLLECTION_PLAN_STATUS.DRAFT, label: 'Draft' },
    { value: COLLECTION_PLAN_STATUS.APPROVED, label: 'Approved' },
    { value: COLLECTION_PLAN_STATUS.IN_PROGRESS, label: 'In Progress' },
    { value: COLLECTION_PLAN_STATUS.COMPLETED, label: 'Completed' },
  ];

  return (
    <div className="workflow-template-list-page">
      {/* Filters & Actions */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <div className="workflow-list-header">
          <div className="workflow-filters">
            <input
              type="text"
              className="admin-input"
              placeholder="Search by plan name..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              style={{ maxWidth: '300px' }}
            />
            <select
              className="admin-select"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setCurrentPage(0);
              }}
              style={{ minWidth: '150px' }}
            >
              {statusOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <button
            onClick={handleCreateNew}
            className="admin-button admin-button-primary"
          >
            + Create New Plan
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      {loading ? (
        <div className="admin-card admin-p-lg admin-mb-lg">
          <SkeletonStatsGrid count={5} />
        </div>
      ) : (
        <div className="admin-stats-grid admin-mb-lg">
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Total</p>
            <p className="admin-stat-value">{stats.total || 0}</p>
          </div>
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Draft</p>
            <p className="admin-stat-value" style={{ color: '#475569' }}>
              {stats.DRAFT || 0}
            </p>
          </div>
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Approved</p>
            <p className="admin-stat-value" style={{ color: '#1e40af' }}>
              {stats.APPROVED || 0}
            </p>
          </div>
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">In Progress</p>
            <p className="admin-stat-value" style={{ color: '#854d0e' }}>
              {stats.IN_PROGRESS || 0}
            </p>
          </div>
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Completed</p>
            <p className="admin-stat-value" style={{ color: '#166534' }}>
              {stats.COMPLETED || 0}
            </p>
          </div>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="admin-error-state admin-mb-lg">{error}</div>
      )}

      {/* Table */}
      <div className="admin-card">
        <div className="admin-section-header">
          <h2>Collection Plans ({totalCount})</h2>
        </div>

        <AdminTable
          columns={columns}
          data={plans}
          loading={loading}
          emptyMessage="No collection plans found"
          emptySubtext="Create your first collection plan to start planning production items."
          rowKey="id"
          paginated
          pageSize={pageSize}
          totalCount={totalCount}
          currentPage={currentPage}
          onPageChange={handlePageChange}
          onRowClick={handleView}
          hoverable
        />
      </div>
    </div>
  );
};

export default CollectionPlanListPage;
