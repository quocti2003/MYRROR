import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  jtrcAPI,
  JTRC_STATUS,
  formatVND,
} from '@services/jtrcService';
import { useDropdownOptions } from '@hooks/useDropdownOptions';
import { JTRCStatusBadge } from '@components/jtrc';
import AdminTable, { TableActions, ActionButton } from '@components/admin-dashboard/AdminTable';
import { SkeletonStatsGrid } from '@components/admin-dashboard/Skeleton';
import '@components/jtrc/jtrc.css';

/**
 * JTRCListPage - List page for JTRC management
 * Features: Table, filters, pagination, status badges, actions
 */
const JTRCListPage = () => {
  const navigate = useNavigate();
  const { options: dropdownOptions } = useDropdownOptions();

  // State
  const [jtrcList, setJtrcList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalCount, setTotalCount] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 20;

  // Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [collectionFilter, setCollectionFilter] = useState('');
  const [seasonFilter, setSeasonFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Filter options (loaded from API or static)
  const [filterOptions, setFilterOptions] = useState({
    collections: [],
    seasons: [],
  });

  // Statistics
  const [stats, setStats] = useState({
    total: 0,
    draft: 0,
    approved: 0,
    pending: 0,
  });

  // Fetch filter options
  useEffect(() => {
    const fetchFilterOptions = async () => {
      try {
        const response = await jtrcAPI.getFilterOptions();
        setFilterOptions(response.data || { collections: [], seasons: [] });
      } catch (err) {
        console.error('Error fetching filter options:', err);
        // Use empty arrays if API fails
        setFilterOptions({ collections: [], seasons: [] });
      }
    };
    fetchFilterOptions();
  }, []);

  // Fetch JTRC list
  const fetchJTRCs = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const params = {
        page: currentPage,
        size: pageSize,
        ...(searchQuery && { q: searchQuery }),
        ...(collectionFilter && { collectionId: collectionFilter }),
        ...(seasonFilter && { season: seasonFilter }),
        ...(categoryFilter && { category: categoryFilter }),
        ...(statusFilter && { status: statusFilter }),
      };

      const response = await jtrcAPI.getAll(params);
      const data = response.data;

      // Handle paginated response
      if (data.content) {
        setJtrcList(data.content);
        setTotalCount(data.totalElements || data.content.length);
      } else if (Array.isArray(data)) {
        setJtrcList(data);
        setTotalCount(data.length);
      } else {
        setJtrcList([]);
        setTotalCount(0);
      }

      // Calculate stats
      const allItems = data.content || data || [];
      setStats({
        total: allItems.length,
        draft: allItems.filter((j) => j.status === JTRC_STATUS.DRAFT).length,
        approved: allItems.filter((j) => j.status === JTRC_STATUS.APPROVED).length,
        pending: allItems.filter((j) => j.status === JTRC_STATUS.PENDING_APPROVAL).length,
      });
    } catch (err) {
      console.error('Error fetching JTRCs:', err);
      setError('Failed to load JTRC records. Please try again.');
      setJtrcList([]);
    } finally {
      setLoading(false);
    }
  }, [currentPage, searchQuery, collectionFilter, seasonFilter, categoryFilter, statusFilter]);

  useEffect(() => {
    fetchJTRCs();
  }, [fetchJTRCs]);

  // Handle search with debounce
  const [searchInput, setSearchInput] = useState('');
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchQuery(searchInput);
      setCurrentPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // Handlers
  const handleCreateNew = () => {
    navigate('/dashboard/admin?tab=jtrc-form&mode=new');
  };

  const handleView = (jtrc) => {
    navigate(`/dashboard/admin?tab=jtrc-form&mode=view&id=${jtrc.id}`);
  };

  const handleEdit = (jtrc) => {
    navigate(`/dashboard/admin?tab=jtrc-form&mode=edit&id=${jtrc.id}`);
  };

  const handleDuplicate = async (jtrc) => {
    if (!window.confirm(`Duplicate JTRC "${jtrc.reportNumber}"?`)) return;

    try {
      const response = await jtrcAPI.duplicate(jtrc.id);
      fetchJTRCs();
      alert(`Duplicated successfully! New report: ${response.data.reportNumber}`);
    } catch (err) {
      console.error('Error duplicating JTRC:', err);
      alert('Failed to duplicate JTRC. Please try again.');
    }
  };

  const handleDelete = async (jtrc) => {
    if (!window.confirm(`Delete JTRC "${jtrc.reportNumber}"? This action cannot be undone.`)) {
      return;
    }

    try {
      await jtrcAPI.delete(jtrc.id);
      fetchJTRCs();
    } catch (err) {
      console.error('Error deleting JTRC:', err);
      alert('Failed to delete JTRC. Please try again.');
    }
  };

  const handlePageChange = (newPage) => {
    setCurrentPage(newPage);
  };

  const handleFilterChange = (filterType, value) => {
    switch (filterType) {
      case 'collection':
        setCollectionFilter(value);
        break;
      case 'season':
        setSeasonFilter(value);
        break;
      case 'category':
        setCategoryFilter(value);
        break;
      case 'status':
        setStatusFilter(value);
        break;
      default:
        break;
    }
    setCurrentPage(0);
  };

  // Table columns
  const columns = useMemo(
    () => [
      {
        key: 'reportNumber',
        header: 'Report Number',
        sortable: true,
        render: (value, row) => (
          <div>
            <div className="admin-table-primary">{value}</div>
            {row.projectId && (
              <div className="admin-table-secondary">
                Project: <code className="admin-code">{row.projectId}</code>
              </div>
            )}
          </div>
        ),
      },
      {
        key: 'collectionName',
        header: 'Collection',
        sortable: true,
        render: (value) => value || '-',
      },
      {
        key: 'season',
        header: 'Season',
        sortable: true,
        render: (value) => value || '-',
      },
      {
        key: 'category',
        header: 'Category',
        sortable: true,
        render: (value) => {
          const cat = dropdownOptions.prefixes?.find((c) => c.code === value);
          return cat?.name || value || '-';
        },
      },
      {
        key: 'status',
        header: 'Status',
        sortable: true,
        render: (value) => <JTRCStatusBadge status={value} size="sm" />,
      },
      {
        key: 'totalCOGS',
        header: 'Total COGS',
        sortable: true,
        align: 'right',
        render: (value) => (
          <span style={{ fontFamily: 'var(--font-mono)' }}>
            {formatVND(value)}
          </span>
        ),
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
            <ActionButton onClick={() => handleEdit(row)} title="Edit">
              Edit
            </ActionButton>
            <ActionButton onClick={() => handleDuplicate(row)} title="Duplicate">
              Copy
            </ActionButton>
            <ActionButton
              onClick={() => handleDelete(row)}
              variant="danger"
              title="Delete"
            >
              Delete
            </ActionButton>
          </TableActions>
        ),
      },
    ],
    [dropdownOptions.prefixes]
  );

  // Status options for filter
  const statusOptions = [
    { value: '', label: 'All Statuses' },
    { value: JTRC_STATUS.DRAFT, label: 'Draft' },
    { value: JTRC_STATUS.PENDING_APPROVAL, label: 'Pending Approval' },
    { value: JTRC_STATUS.APPROVED, label: 'Approved' },
    { value: JTRC_STATUS.REJECTED, label: 'Rejected' },
    { value: JTRC_STATUS.ARCHIVED, label: 'Archived' },
  ];

  return (
    <div className="jtrc-list-page">
      {/* Header */}
      <div className="admin-card admin-p-lg admin-mb-lg">
        <div className="jtrc-list-header">
          <div>
            <h1>Jewelry Technical Report Cards</h1>
            <p>
              Manage JTRC specifications for production. Track metal, stone, and labor costs.
            </p>
          </div>
          <button
            onClick={handleCreateNew}
            className="admin-button admin-button-primary"
          >
            + Create New JTRC
          </button>
        </div>

        {/* Filters */}
        <div className="jtrc-filters">
          <input
            type="text"
            className="admin-input jtrc-search-input"
            placeholder="Search by report number..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />

          <select
            className="admin-select jtrc-filter-select"
            value={collectionFilter}
            onChange={(e) => handleFilterChange('collection', e.target.value)}
          >
            <option value="">All Collections</option>
            {filterOptions.collections.map((col) => (
              <option key={col.id} value={col.id}>
                {col.name}
              </option>
            ))}
          </select>

          <select
            className="admin-select jtrc-filter-select"
            value={seasonFilter}
            onChange={(e) => handleFilterChange('season', e.target.value)}
          >
            <option value="">All Seasons</option>
            {filterOptions.seasons.map((season) => (
              <option key={season} value={season}>
                {season}
              </option>
            ))}
          </select>

          <select
            className="admin-select jtrc-filter-select"
            value={categoryFilter}
            onChange={(e) => handleFilterChange('category', e.target.value)}
          >
            <option value="">All Categories</option>
            {(dropdownOptions.prefixes || []).map((cat) => (
              <option key={cat.code} value={cat.code}>
                {cat.name}
              </option>
            ))}
          </select>

          <select
            className="admin-select jtrc-filter-select"
            value={statusFilter}
            onChange={(e) => handleFilterChange('status', e.target.value)}
          >
            {statusOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Stats Cards */}
      {loading ? (
        <div className="admin-card admin-p-lg admin-mb-lg">
          <SkeletonStatsGrid count={4} />
        </div>
      ) : (
        <div className="admin-stats-grid admin-mb-lg">
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Total Records</p>
            <p className="admin-stat-value">{stats.total}</p>
          </div>
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Drafts</p>
            <p className="admin-stat-value" style={{ color: '#854d0e' }}>
              {stats.draft}
            </p>
          </div>
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Pending Approval</p>
            <p className="admin-stat-value" style={{ color: '#1e40af' }}>
              {stats.pending}
            </p>
          </div>
          <div className="admin-card admin-p-lg">
            <p className="admin-stat-label">Approved</p>
            <p className="admin-stat-value" style={{ color: '#166534' }}>
              {stats.approved}
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
          <h2>JTRC Records ({totalCount})</h2>
        </div>

        <AdminTable
          columns={columns}
          data={jtrcList}
          loading={loading}
          emptyMessage="No JTRC records found"
          emptySubtext="Create your first Jewelry Technical Report Card to define specifications for production."
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

export default JTRCListPage;
