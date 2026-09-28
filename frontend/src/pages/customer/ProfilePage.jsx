import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../store/AuthContext';
import LoadingButton from '../../components/common/LoadingButton';
import StatusBadge from '../../components/common/StatusBadge';
import AddressSection from '../../features/location/AddressSection';

export default function ProfilePage() {
  const { user, updateProfile, logout } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState(user?.name || '');
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  const handleUpdate = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    setLoading(true);
    setSuccessMsg(null);
    setErrorMsg(null);
    try {
      await updateProfile({ name: name.trim() });
      setSuccessMsg('Profile updated successfully!');
    } catch (err) {
      setErrorMsg(err.message || 'Failed to update profile.');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!user) {
    return (
      <div className="container py-5 text-center">
        <p>Please sign in to view your profile.</p>
        <Link to="/login" className="btn btn-primary btn-sm">Sign In</Link>
      </div>
    );
  }

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-md-8 col-lg-6">
          <div className="card border p-4 shadow-sm mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
            <div className="d-flex align-items-center gap-3 mb-4 pb-3 border-bottom">
              <div
                className="rounded-circle bg-dark text-white d-flex align-items-center justify-content-center fw-bold fs-3"
                style={{ width: '64px', height: '64px' }}
              >
                {user.name?.charAt(0).toUpperCase()}
              </div>
              <div>
                <h4 className="fw-bold mb-0 text-dark">{user.name}</h4>
                <div className="text-muted small">{user.email}</div>
                <div className="mt-1 d-flex gap-2 align-items-center">
                  <span className="badge bg-dark" style={{ fontSize: '0.7rem' }}>{user.role}</span>
                  <StatusBadge status={user.status || 'ACTIVE'} />
                </div>
              </div>
            </div>

            {successMsg && (
              <div className="alert alert-success py-2 px-3 small mb-3">
                <i className="bi bi-check-circle-fill me-2" />
                {successMsg}
              </div>
            )}

            {errorMsg && (
              <div className="alert alert-danger py-2 px-3 small mb-3">
                <i className="bi bi-exclamation-circle-fill me-2" />
                {errorMsg}
              </div>
            )}

            <form onSubmit={handleUpdate}>
              <div className="mb-3">
                <label className="form-label small fw-medium text-dark" htmlFor="profile-name">
                  Full Name
                </label>
                <input
                  type="text"
                  id="profile-name"
                  className="form-control"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-dark">Email Address</label>
                <input
                  type="email"
                  className="form-control bg-light"
                  value={user.email}
                  disabled
                  readOnly
                />
                <span className="text-muted" style={{ fontSize: '0.75rem' }}>Email cannot be changed directly in MVP.</span>
              </div>

              <div className="mb-4">
                <label className="form-label small fw-medium text-dark">Phone Number</label>
                <input
                  type="text"
                  className="form-control bg-light"
                  value={user.phone}
                  disabled
                  readOnly
                />
              </div>

              <div className="d-flex justify-content-between align-items-center pt-2 border-top">
                <button
                  type="button"
                  className="btn btn-outline-danger btn-sm px-3"
                  onClick={handleLogout}
                >
                  <i className="bi bi-box-arrow-right me-1" />
                  Sign Out
                </button>

                <LoadingButton type="submit" loading={loading} className="btn-primary btn-sm px-4">
                  Save Changes
                </LoadingButton>
              </div>
            </form>
          </div>

          {/* Delivery Addresses Management Section */}
          <AddressSection />

          {/* Quick role-specific navigation cards */}
          {user.role === 'VENDOR' && (
            <div className="card border p-3 shadow-sm bg-light">
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <h6 className="fw-bold mb-1 text-dark">Vendor Console</h6>
                  <p className="small text-muted mb-0">Manage your store catalog, incoming orders, and inventory.</p>
                </div>
                <Link to="/vendor/dashboard" className="btn btn-dark btn-sm">
                  Open Console &rarr;
                </Link>
              </div>
            </div>
          )}

          {user.role === 'DELIVERY_PARTNER' && (
            <div className="card border p-3 shadow-sm bg-light">
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <h6 className="fw-bold mb-1 text-dark">Delivery Console</h6>
                  <p className="small text-muted mb-0">Manage availability and view assigned order deliveries.</p>
                </div>
                <Link to="/delivery/dashboard" className="btn btn-dark btn-sm">
                  Open Console &rarr;
                </Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
