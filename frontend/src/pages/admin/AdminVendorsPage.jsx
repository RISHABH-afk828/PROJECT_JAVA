import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';

export default function AdminVendorsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialStatus = searchParams.get('status') || '';

  const [vendors, setVendors] = useState([]);
  const [statusFilter, setStatusFilter] = useState(initialStatus);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalVendors, setTotalVendors] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [feedback, setFeedback] = useState({ type: '', message: '' });

  const fetchVendors = async () => {
    try {
      setLoading(true);
      setFeedback({ type: '', message: '' });
      const params = {
        page,
        size: 10,
      };
      if (statusFilter) {
        params.status = statusFilter;
      }

      const res = await apiClient.get('/admin/vendors', { params });
      if (res.data?.success) {
        setVendors(res.data.data.content || []);
        setTotalPages(res.data.data.totalPages || 1);
        setTotalVendors(res.data.data.totalElements || 0);
      }
    } catch (err) {
      setFeedback({
        type: 'danger',
        message: err.response?.data?.message || 'Failed to fetch vendor list.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVendors();
  }, [page, statusFilter]);

  const handleTabChange = (newStatus) => {
    setStatusFilter(newStatus);
    setPage(0);
    if (newStatus) {
      setSearchParams({ status: newStatus });
    } else {
      setSearchParams({});
    }
  };

  const handleApprove = async (vendor) => {
    if (!window.confirm(`Approve store "${vendor.name}" to go live on the customer marketplace?`)) {
      return;
    }
    try {
      setActionLoadingId(vendor.id);
      const res = await apiClient.post(`/admin/vendors/${vendor.id}/approve`);
      if (res.data?.success) {
        setFeedback({
          type: 'success',
          message: `Store "${vendor.name}" has been approved and activated!`,
        });
        fetchVendors();
      }
    } catch (err) {
      setFeedback({
        type: 'danger',
        message: err.response?.data?.message || 'Failed to approve store.',
      });
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleSuspend = async (vendor) => {
    if (!window.confirm(`Are you sure you want to suspend "${vendor.name}"? Products will be hidden from customer search.`)) {
      return;
    }
    try {
      setActionLoadingId(vendor.id);
      const res = await apiClient.post(`/admin/vendors/${vendor.id}/suspend`);
      if (res.data?.success) {
        setFeedback({
          type: 'warning',
          message: `Store "${vendor.name}" has been suspended.`,
        });
        fetchVendors();
      }
    } catch (err) {
      setFeedback({
        type: 'danger',
        message: err.response?.data?.message || 'Failed to suspend store.',
      });
    } finally {
      setActionLoadingId(null);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="badge bg-success">Active / Live</span>;
      case 'PENDING_APPROVAL':
        return <span className="badge bg-warning text-dark">Pending Approval</span>;
      case 'SUSPENDED':
        return <span className="badge bg-danger">Suspended</span>;
      default:
        return <span className="badge bg-secondary">{status}</span>;
    }
  };

  return (
    <div className="container-xl py-4">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <div>
          <div className="d-flex align-items-center gap-2 mb-1">
            <Link to="/admin/dashboard" className="text-decoration-none text-muted small">
              &larr; Admin Console
            </Link>
          </div>
          <h2 className="fw-bold mb-0">
            <i className="bi bi-shop-window text-primary me-2" />
            Vendor Onboarding & Governance
          </h2>
          <small className="text-muted">
            Inspect store details, verify physical locations and approve new merchant applications.
          </small>
        </div>
        <button
          onClick={fetchVendors}
          className="btn btn-outline-dark btn-sm rounded-pill px-3"
          disabled={loading}
        >
          <i className={`bi bi-arrow-clockwise me-1 ${loading ? 'spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Alerts */}
      {feedback.message && (
        <div className={`alert alert-${feedback.type} alert-dismissible fade show small mb-4`} role="alert">
          <i className={`bi ${feedback.type === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'} me-2`} />
          {feedback.message}
          <button
            type="button"
            className="btn-close"
            onClick={() => setFeedback({ type: '', message: '' })}
            aria-label="Close"
          />
        </div>
      )}

      {/* Filter Tabs */}
      <ul className="nav nav-pills mb-3 border-bottom pb-2">
        <li className="nav-item">
          <button
            className={`nav-link rounded-pill py-1 px-3 ${statusFilter === '' ? 'active' : ''}`}
            onClick={() => handleTabChange('')}
          >
            All Stores
          </button>
        </li>
        <li className="nav-item ms-2">
          <button
            className={`nav-link rounded-pill py-1 px-3 ${
              statusFilter === 'PENDING_APPROVAL' ? 'active bg-warning text-dark' : ''
            }`}
            onClick={() => handleTabChange('PENDING_APPROVAL')}
          >
            <i className="bi bi-hourglass-split me-1" />
            Pending Approval
          </button>
        </li>
        <li className="nav-item ms-2">
          <button
            className={`nav-link rounded-pill py-1 px-3 ${statusFilter === 'ACTIVE' ? 'active' : ''}`}
            onClick={() => handleTabChange('ACTIVE')}
          >
            <i className="bi bi-check-circle me-1" />
            Active
          </button>
        </li>
        <li className="nav-item ms-2">
          <button
            className={`nav-link rounded-pill py-1 px-3 ${statusFilter === 'SUSPENDED' ? 'active' : ''}`}
            onClick={() => handleTabChange('SUSPENDED')}
          >
            <i className="bi bi-slash-circle me-1" />
            Suspended
          </button>
        </li>
      </ul>

      {/* Stores List */}
      <div className="card border shadow-sm rounded-3">
        <div className="card-header bg-white border-bottom py-3">
          <div className="d-flex justify-content-between align-items-center">
            <span className="fw-semibold text-dark">
              {statusFilter ? `${statusFilter.replace('_', ' ')} Stores` : 'All Registered Stores'}
            </span>
            <span className="badge bg-light text-dark border">{totalVendors} total</span>
          </div>
        </div>

        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr className="small text-muted">
                <th>Store & Description</th>
                <th>Address & Coordinates</th>
                <th>Service Radius</th>
                <th>Contact</th>
                <th>Status</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" className="text-center py-5 text-muted">
                    <div className="spinner-border spinner-border-sm me-2 text-primary" role="status" />
                    Loading stores...
                  </td>
                </tr>
              ) : vendors.length === 0 ? (
                <tr>
                  <td colSpan="6" className="text-center py-5 text-muted">
                    <i className="bi bi-inbox fs-3 d-block mb-2 text-secondary" />
                    No stores found matching the selected filter.
                  </td>
                </tr>
              ) : (
                vendors.map((vendor) => (
                  <tr key={vendor.id}>
                    <td>
                      <div className="fw-bold text-dark">{vendor.name}</div>
                      <small className="text-muted d-block" style={{ maxWidth: '280px' }}>
                        {vendor.description || 'No description provided'}
                      </small>
                    </td>
                    <td>
                      <div className="small text-dark" style={{ maxWidth: '240px' }}>
                        {vendor.address}
                      </div>
                      <small className="text-muted font-monospace" style={{ fontSize: '0.75rem' }}>
                        {vendor.latitude?.toFixed(4)}, {vendor.longitude?.toFixed(4)}
                      </small>
                    </td>
                    <td>
                      <span className="badge bg-light text-dark border">
                        <i className="bi bi-pin-map me-1 text-primary" />
                        {vendor.serviceRadiusKm} km
                      </span>
                    </td>
                    <td>
                      <div className="small">{vendor.phone}</div>
                    </td>
                    <td>{getStatusBadge(vendor.status)}</td>
                    <td className="text-end">
                      <div className="d-flex justify-content-end gap-2">
                        {vendor.status === 'PENDING_APPROVAL' && (
                          <>
                            <button
                              onClick={() => handleApprove(vendor)}
                              disabled={actionLoadingId === vendor.id}
                              className="btn btn-sm btn-success rounded-pill px-3"
                            >
                              {actionLoadingId === vendor.id ? (
                                <span className="spinner-border spinner-border-sm" role="status" />
                              ) : (
                                <>
                                  <i className="bi bi-check-lg me-1" />
                                  Approve
                                </>
                              )}
                            </button>
                            <button
                              onClick={() => handleSuspend(vendor)}
                              disabled={actionLoadingId === vendor.id}
                              className="btn btn-sm btn-outline-danger rounded-pill px-2"
                            >
                              Reject
                            </button>
                          </>
                        )}

                        {vendor.status === 'ACTIVE' && (
                          <button
                            onClick={() => handleSuspend(vendor)}
                            disabled={actionLoadingId === vendor.id}
                            className="btn btn-sm btn-outline-danger rounded-pill px-3"
                          >
                            {actionLoadingId === vendor.id ? (
                              <span className="spinner-border spinner-border-sm" role="status" />
                            ) : (
                              'Suspend'
                            )}
                          </button>
                        )}

                        {vendor.status === 'SUSPENDED' && (
                          <button
                            onClick={() => handleApprove(vendor)}
                            disabled={actionLoadingId === vendor.id}
                            className="btn btn-sm btn-outline-success rounded-pill px-3"
                          >
                            {actionLoadingId === vendor.id ? (
                              <span className="spinner-border spinner-border-sm" role="status" />
                            ) : (
                              'Reactivate'
                            )}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="card-footer bg-white d-flex justify-content-between align-items-center py-2">
            <span className="small text-muted">
              Page {page + 1} of {totalPages}
            </span>
            <div className="d-flex gap-1">
              <button
                className="btn btn-outline-secondary btn-sm"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </button>
              <button
                className="btn btn-outline-secondary btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
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
