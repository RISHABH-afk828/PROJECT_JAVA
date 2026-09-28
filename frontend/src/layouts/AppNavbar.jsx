import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';

export default function AppNavbar({ selectedLocation, onOpenLocationModal, cartItemCount = 0 }) {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-light bg-white border-bottom sticky-top py-2" style={{ borderColor: 'var(--color-border)' }}>
      <div className="container-xl">
        {/* Brand */}
        <Link to="/" className="navbar-brand d-flex align-items-center gap-2 fw-bold text-dark me-4">
          <span
            className="rounded d-flex align-items-center justify-content-center text-white"
            style={{ width: '32px', height: '32px', backgroundColor: 'var(--color-primary)' }}
          >
            <i className="bi bi-shop" />
          </span>
          <span style={{ letterSpacing: '-0.02em', fontSize: '1.25rem' }}>Hyperlocal</span>
        </Link>

        {/* Location selector button */}
        <button
          className="btn btn-light btn-sm d-flex align-items-center gap-2 border px-3 py-1 text-truncate"
          style={{ maxWidth: '280px', borderRadius: 'var(--radius-pill)', backgroundColor: 'var(--color-surface)' }}
          onClick={onOpenLocationModal}
          type="button"
        >
          <i className="bi bi-geo-alt-fill text-danger" />
          <div className="text-start text-truncate" style={{ fontSize: '0.8rem' }}>
            <span className="text-muted d-block" style={{ fontSize: '0.7rem' }}>Deliver to</span>
            <span className="fw-semibold text-dark text-truncate">
              {selectedLocation?.label || 'Select Location'}
            </span>
          </div>
          <i className="bi bi-chevron-down text-muted ms-1" style={{ fontSize: '0.7rem' }} />
        </button>

        {/* Right side controls */}
        <div className="d-flex align-items-center ms-auto gap-2">
          {/* Cart Icon button */}
          <Link
            to="/cart"
            className="btn btn-outline-dark position-relative d-flex align-items-center justify-content-center p-2 rounded-circle"
            style={{ width: '40px', height: '40px' }}
            title="Cart"
          >
            <i className="bi bi-bag" />
            {cartItemCount > 0 && (
              <span
                className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
                style={{ fontSize: '0.65rem' }}
              >
                {cartItemCount}
              </span>
            )}
          </Link>

          {/* User profile / auth */}
          {isAuthenticated ? (
            <div className="dropdown">
              <button
                className="btn btn-light border d-flex align-items-center gap-2 rounded-pill px-3 py-1"
                type="button"
                id="userMenuButton"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                <div
                  className="rounded-circle bg-dark text-white d-flex align-items-center justify-content-center fw-bold"
                  style={{ width: '28px', height: '28px', fontSize: '0.8rem' }}
                >
                  {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <span className="d-none d-md-inline small fw-medium text-dark">{user?.name}</span>
                <i className="bi bi-chevron-down text-muted" style={{ fontSize: '0.7rem' }} />
              </button>
              <ul className="dropdown-menu dropdown-menu-end shadow-sm border mt-2" aria-labelledby="userMenuButton">
                <li className="px-3 py-2 border-bottom">
                  <div className="small fw-bold text-dark">{user?.name}</div>
                  <div className="text-muted" style={{ fontSize: '0.75rem' }}>{user?.email}</div>
                  <span className="badge bg-secondary mt-1" style={{ fontSize: '0.65rem' }}>{user?.role}</span>
                </li>

                {user?.role === 'CUSTOMER' && (
                  <>
                    <li><Link className="dropdown-item py-2" to="/orders"><i className="bi bi-receipt me-2" />My Orders</Link></li>
                    <li><Link className="dropdown-item py-2" to="/profile"><i className="bi bi-person me-2" />Account Settings</Link></li>
                  </>
                )}

                {user?.role === 'VENDOR' && (
                  <>
                    <li><Link className="dropdown-item py-2 fw-semibold text-primary" to="/vendor/dashboard"><i className="bi bi-speedometer2 me-2" />Vendor Console</Link></li>
                    <li><Link className="dropdown-item py-2" to="/vendor/orders"><i className="bi bi-box-seam me-2" />Orders Queue</Link></li>
                    <li><Link className="dropdown-item py-2" to="/vendor/products"><i className="bi bi-tags me-2" />Products & Catalog</Link></li>
                  </>
                )}

                {user?.role === 'DELIVERY_PARTNER' && (
                  <>
                    <li><Link className="dropdown-item py-2 fw-semibold text-primary" to="/delivery/dashboard"><i className="bi bi-bicycle me-2" />Delivery Console</Link></li>
                    <li><Link className="dropdown-item py-2" to="/delivery/history"><i className="bi bi-clock-history me-2" />Delivery History</Link></li>
                  </>
                )}

                {user?.role === 'ADMIN' && (
                  <>
                    <li><Link className="dropdown-item py-2 fw-semibold text-danger" to="/admin/dashboard"><i className="bi bi-shield-check me-2" />Admin Console</Link></li>
                    <li><Link className="dropdown-item py-2" to="/admin/vendors"><i className="bi bi-shop-window me-2" />Vendor Approvals</Link></li>
                  </>
                )}

                <li><hr className="dropdown-divider my-1" /></li>
                <li>
                  <button className="dropdown-item py-2 text-danger" onClick={handleLogout}>
                    <i className="bi bi-box-arrow-right me-2" />Sign Out
                  </button>
                </li>
              </ul>
            </div>
          ) : (
            <div className="d-flex align-items-center gap-2">
              <Link to="/login" className="btn btn-outline-dark btn-sm px-3 rounded-pill">
                Sign In
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm px-3 rounded-pill d-none d-sm-inline-block">
                Sign Up
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
