import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';

export default function AdminDashboardPage() {
  const [metrics, setMetrics] = useState(null);
  const [loadingMetrics, setLoadingMetrics] = useState(true);
  const [metricsError, setMetricsError] = useState('');

  // User Management State
  const [users, setUsers] = useState([]);
  const [userPage, setUserPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalUsers, setTotalUsers] = useState(0);
  const [roleFilter, setRoleFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [updatingUserId, setUpdatingUserId] = useState(null);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  const fetchMetrics = async () => {
    try {
      setLoadingMetrics(true);
      setMetricsError('');
      const res = await apiClient.get('/admin/dashboard');
      if (res.data?.success) {
        setMetrics(res.data.data);
      }
    } catch (err) {
      setMetricsError(err.response?.data?.message || 'Failed to fetch dashboard metrics.');
    } finally {
      setLoadingMetrics(false);
    }
  };

  const fetchUsers = async () => {
    try {
      setLoadingUsers(true);
      setActionError('');
      const params = {
        page: userPage,
        size: 10,
      };
      if (roleFilter) params.role = roleFilter;
      if (searchQuery.trim()) params.q = searchQuery.trim();

      const res = await apiClient.get('/admin/users', { params });
      if (res.data?.success) {
        setUsers(res.data.data.content || []);
        setTotalPages(res.data.data.totalPages || 1);
        setTotalUsers(res.data.data.totalElements || 0);
      }
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to load user directory.');
    } finally {
      setLoadingUsers(false);
    }
  };

  useEffect(() => {
    fetchMetrics();
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [userPage, roleFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setUserPage(0);
    fetchUsers();
  };

  const handleToggleUserStatus = async (user) => {
    const nextStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const confirmMsg = `Are you sure you want to mark ${user.name} (${user.email}) as ${nextStatus}?`;
    if (!window.confirm(confirmMsg)) return;

    try {
      setUpdatingUserId(user.id);
      setActionError('');
      setActionSuccess('');

      const res = await apiClient.patch(`/admin/users/${user.id}/status`, { status: nextStatus });
      if (res.data?.success) {
        setActionSuccess(`User ${user.name} updated to ${nextStatus}.`);
        setUsers((prev) =>
          prev.map((u) => (u.id === user.id ? { ...u, status: nextStatus } : u))
        );
      }
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to update user status.');
    } finally {
      setUpdatingUserId(null);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(amount || 0);
  };

  const getRoleBadgeClass = (role) => {
    switch (role) {
      case 'ADMIN':
        return 'bg-dark';
      case 'VENDOR':
        return 'bg-primary';
      case 'DELIVERY_PARTNER':
        return 'bg-info text-dark';
      default:
        return 'bg-secondary';
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-success';
      case 'SUSPENDED':
        return 'bg-danger';
      case 'INACTIVE':
        return 'bg-warning text-dark';
      default:
        return 'bg-secondary';
    }
  };

  return (
    <div className="container-xl py-4">
      {/* Top Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <div>
          <h2 className="fw-bold mb-1">
            <i className="bi bi-shield-shaded text-danger me-2" />
            Admin Governance Console
          </h2>
          <p className="text-muted mb-0 small">
            Real-time platform metrics, store approvals, and user account management.
          </p>
        </div>
        <div className="d-flex gap-2">
          <Link to="/admin/vendors" className="btn btn-outline-dark btn-sm rounded-pill px-3">
            <i className="bi bi-shop me-1" />
            Manage Vendors
          </Link>
          <button
            onClick={() => {
              fetchMetrics();
              fetchUsers();
            }}
            className="btn btn-dark btn-sm rounded-pill px-3"
            disabled={loadingMetrics}
          >
            <i className={`bi bi-arrow-clockwise me-1 ${loadingMetrics ? 'spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {metricsError && (
        <div className="alert alert-danger py-2 small mb-4" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2" />
          {metricsError}
        </div>
      )}

      {/* KPI Cards Grid */}
      <div className="row g-3 mb-4">
        {/* Total GMV */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card border-0 shadow-sm rounded-3 p-3 h-100 bg-white">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="text-muted small fw-medium text-uppercase">Total GMV</span>
              <span className="badge bg-success-subtle text-success p-2 rounded-circle">
                <i className="bi bi-currency-rupee fs-6" />
              </span>
            </div>
            <h3 className="fw-bold mb-1">
              {loadingMetrics ? '...' : formatCurrency(metrics?.totalGmv)}
            </h3>
            <span className="text-muted small">Platform gross merchandise volume</span>
          </div>
        </div>

        {/* Orders Summary */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card border-0 shadow-sm rounded-3 p-3 h-100 bg-white">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="text-muted small fw-medium text-uppercase">Total Orders</span>
              <span className="badge bg-primary-subtle text-primary p-2 rounded-circle">
                <i className="bi bi-bag-check fs-6" />
              </span>
            </div>
            <h3 className="fw-bold mb-1">
              {loadingMetrics ? '...' : metrics?.totalOrders ?? 0}
            </h3>
            <div className="d-flex gap-2 small">
              <span className="text-success fw-medium">
                {metrics?.completedOrders ?? 0} completed
              </span>
              <span className="text-muted">•</span>
              <span className="text-primary fw-medium">
                {metrics?.activeOrders ?? 0} active
              </span>
            </div>
          </div>
        </div>

        {/* Vendors Summary */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card border-0 shadow-sm rounded-3 p-3 h-100 bg-white">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="text-muted small fw-medium text-uppercase">Vendors</span>
              <span className="badge bg-warning-subtle text-warning p-2 rounded-circle">
                <i className="bi bi-shop fs-6" />
              </span>
            </div>
            <h3 className="fw-bold mb-1">
              {loadingMetrics ? '...' : metrics?.totalVendors ?? 0}
            </h3>
            <div className="d-flex gap-2 small">
              <span className="text-success fw-medium">
                {metrics?.activeVendors ?? 0} active
              </span>
              {(metrics?.pendingVendors ?? 0) > 0 && (
                <>
                  <span className="text-muted">•</span>
                  <span className="text-danger fw-bold">
                    {metrics?.pendingVendors} pending
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Users Summary */}
        <div className="col-12 col-sm-6 col-lg-3">
          <div className="card border-0 shadow-sm rounded-3 p-3 h-100 bg-white">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="text-muted small fw-medium text-uppercase">Platform Users</span>
              <span className="badge bg-info-subtle text-info p-2 rounded-circle">
                <i className="bi bi-people fs-6" />
              </span>
            </div>
            <h3 className="fw-bold mb-1">
              {loadingMetrics
                ? '...'
                : (metrics?.totalCustomers ?? 0) + (metrics?.totalDeliveryPartners ?? 0)}
            </h3>
            <div className="d-flex gap-2 small text-muted">
              <span>{metrics?.totalCustomers ?? 0} customers</span>
              <span>•</span>
              <span>{metrics?.totalDeliveryPartners ?? 0} drivers</span>
            </div>
          </div>
        </div>
      </div>

      {/* Pending Vendor Applications Banner */}
      {(metrics?.pendingVendors ?? 0) > 0 && (
        <div className="alert alert-warning border-0 shadow-sm d-flex flex-wrap justify-content-between align-items-center p-3 mb-4 rounded-3">
          <div className="d-flex align-items-center gap-2">
            <i className="bi bi-exclamation-circle-fill text-warning fs-5" />
            <div>
              <strong className="d-block">
                {metrics.pendingVendors} Store Application{metrics.pendingVendors > 1 ? 's' : ''} Awaiting Approval
              </strong>
              <span className="small text-muted">
                Review business details, store hours, and service radius before onboarding to the customer catalog.
              </span>
            </div>
          </div>
          <Link
            to="/admin/vendors?status=PENDING_APPROVAL"
            className="btn btn-warning btn-sm fw-semibold rounded-pill px-3 mt-2 mt-sm-0"
          >
            Review Applications &rarr;
          </Link>
        </div>
      )}

      {/* User Directory Section */}
      <div className="card border shadow-sm rounded-3">
        <div className="card-header bg-white border-bottom py-3">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
            <div>
              <h5 className="fw-bold mb-0">Platform User Directory</h5>
              <small className="text-muted">Total {totalUsers} registered accounts</small>
            </div>
            <form onSubmit={handleSearchSubmit} className="d-flex gap-2 flex-wrap">
              <select
                className="form-select form-select-sm"
                style={{ width: '160px' }}
                value={roleFilter}
                onChange={(e) => {
                  setRoleFilter(e.target.value);
                  setUserPage(0);
                }}
              >
                <option value="">All Roles</option>
                <option value="CUSTOMER">Customer</option>
                <option value="VENDOR">Vendor</option>
                <option value="DELIVERY_PARTNER">Delivery Partner</option>
                <option value="ADMIN">Admin</option>
              </select>
              <div className="input-group input-group-sm" style={{ width: '220px' }}>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Search name/email/phone..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <button type="submit" className="btn btn-outline-secondary">
                  <i className="bi bi-search" />
                </button>
              </div>
            </form>
          </div>
        </div>

        {actionSuccess && (
          <div className="alert alert-success m-3 py-2 small mb-0" role="alert">
            <i className="bi bi-check-circle-fill me-2" />
            {actionSuccess}
          </div>
        )}
        {actionError && (
          <div className="alert alert-danger m-3 py-2 small mb-0" role="alert">
            <i className="bi bi-exclamation-triangle-fill me-2" />
            {actionError}
          </div>
        )}

        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>User</th>
                <th>Contact</th>
                <th>Role</th>
                <th>Status</th>
                <th>Registered</th>
                <th className="text-end">Action</th>
              </tr>
            </thead>
            <tbody>
              {loadingUsers ? (
                <tr>
                  <td colSpan="6" className="text-center py-4 text-muted">
                    <div className="spinner-border spinner-border-sm me-2 text-primary" role="status" />
                    Loading user accounts...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan="6" className="text-center py-4 text-muted">
                    No users matching criteria.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div className="fw-semibold text-dark">{u.name}</div>
                      <small className="text-muted">ID: #{u.id}</small>
                    </td>
                    <td>
                      <div>{u.email}</div>
                      <small className="text-muted">{u.phone || 'No phone'}</small>
                    </td>
                    <td>
                      <span className={`badge ${getRoleBadgeClass(u.role)}`}>
                        {u.role}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${getStatusBadgeClass(u.status)}`}>
                        {u.status}
                      </span>
                    </td>
                    <td>
                      <small className="text-muted">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                      </small>
                    </td>
                    <td className="text-end">
                      {u.role !== 'ADMIN' && (
                        <button
                          onClick={() => handleToggleUserStatus(u)}
                          disabled={updatingUserId === u.id}
                          className={`btn btn-sm rounded-pill px-3 ${
                            u.status === 'ACTIVE'
                              ? 'btn-outline-danger'
                              : 'btn-outline-success'
                          }`}
                        >
                          {updatingUserId === u.id ? (
                            <span className="spinner-border spinner-border-sm" role="status" />
                          ) : u.status === 'ACTIVE' ? (
                            'Suspend'
                          ) : (
                            'Activate'
                          )}
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="card-footer bg-white d-flex justify-content-between align-items-center py-2">
            <span className="small text-muted">
              Page {userPage + 1} of {totalPages}
            </span>
            <div className="d-flex gap-1">
              <button
                className="btn btn-outline-secondary btn-sm"
                disabled={userPage <= 0}
                onClick={() => setUserPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </button>
              <button
                className="btn btn-outline-secondary btn-sm"
                disabled={userPage >= totalPages - 1}
                onClick={() => setUserPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
