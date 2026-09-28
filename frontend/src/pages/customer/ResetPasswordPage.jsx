import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import LoadingButton from '../../components/common/LoadingButton';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState(searchParams.get('token') || '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!token.trim()) {
      setError('Reset token is required.');
      return;
    }
    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await apiClient.post('/auth/reset-password', {
        token: token.trim(),
        newPassword,
      });
      setSuccess(true);
    } catch (err) {
      setError(err.message || 'Failed to reset password. Token may be invalid or expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-sm-10 col-md-8 col-lg-5">
          <div className="card border p-4 p-md-5 shadow-sm" style={{ borderRadius: 'var(--radius-md)' }}>
            <div className="text-center mb-4">
              <span
                className="d-inline-flex align-items-center justify-content-center text-white rounded mb-2"
                style={{ width: '44px', height: '44px', backgroundColor: 'var(--color-primary)' }}
              >
                <i className="bi bi-shield-lock fs-4" />
              </span>
              <h3 className="fw-bold text-dark mt-2 mb-1">Set New Password</h3>
              <p className="text-muted small">Choose a strong password for your account</p>
            </div>

            {error && (
              <div className="alert alert-danger py-2 px-3 small d-flex align-items-center gap-2 mb-4">
                <i className="bi bi-exclamation-circle-fill text-danger flex-shrink-0" />
                <div>{error}</div>
              </div>
            )}

            {success ? (
              <div className="text-center py-3">
                <div className="text-success fs-1 mb-2">
                  <i className="bi bi-check-circle" />
                </div>
                <h5 className="fw-bold text-dark">Password Updated</h5>
                <p className="text-muted small mb-4">You can now sign in with your new password.</p>
                <Link to="/login" className="btn btn-primary w-100 py-2">
                  Sign In Now
                </Link>
              </div>
            ) : (
              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label className="form-label small fw-medium text-dark" htmlFor="token">
                    Reset Token
                  </label>
                  <input
                    type="text"
                    id="token"
                    className="form-control"
                    placeholder="Paste your reset token"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label small fw-medium text-dark" htmlFor="newPassword">
                    New Password
                  </label>
                  <input
                    type="password"
                    id="newPassword"
                    className="form-control"
                    placeholder="At least 6 characters"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label small fw-medium text-dark" htmlFor="confirmPassword">
                    Confirm New Password
                  </label>
                  <input
                    type="password"
                    id="confirmPassword"
                    className="form-control"
                    placeholder="Re-enter new password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                  />
                </div>

                <div className="d-grid mt-4">
                  <LoadingButton type="submit" loading={loading} className="btn-primary py-2 fw-semibold">
                    Update Password
                  </LoadingButton>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
