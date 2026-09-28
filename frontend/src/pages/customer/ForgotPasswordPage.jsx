import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import LoadingButton from '../../components/common/LoadingButton';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [resetToken, setResetToken] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim()) return;

    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const res = await apiClient.post('/auth/forgot-password', { email });
      setMessage(res.message || 'Password reset instructions generated.');
      if (res.resetToken) {
        setResetToken(res.resetToken);
      }
    } catch (err) {
      setError(err.message || 'Unable to process reset request. Please check the email.');
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
                <i className="bi bi-key fs-4" />
              </span>
              <h3 className="fw-bold text-dark mt-2 mb-1">Reset Password</h3>
              <p className="text-muted small">Enter your email and we'll help you reset your password</p>
            </div>

            {error && (
              <div className="alert alert-danger py-2 px-3 small d-flex align-items-center gap-2 mb-4">
                <i className="bi bi-exclamation-circle-fill" />
                <div>{error}</div>
              </div>
            )}

            {message && (
              <div className="alert alert-success py-2 px-3 small mb-4">
                <div>{message}</div>
                {resetToken && (
                  <div className="mt-2 pt-2 border-top">
                    <span className="small d-block text-muted">Dev / Test Reset Token:</span>
                    <code className="user-select-all fw-bold text-dark">{resetToken}</code>
                    <div className="mt-2">
                      <Link to={`/reset-password?token=${resetToken}`} className="btn btn-dark btn-sm py-1">
                        Proceed to set new password &rarr;
                      </Link>
                    </div>
                  </div>
                )}
              </div>
            )}

            {!message && (
              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label className="form-label small fw-medium text-dark" htmlFor="email">
                    Account Email
                  </label>
                  <input
                    type="email"
                    id="email"
                    className="form-control"
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>

                <div className="d-grid mt-4">
                  <LoadingButton type="submit" loading={loading} className="btn-primary py-2 fw-semibold">
                    Send Reset Link
                  </LoadingButton>
                </div>
              </form>
            )}

            <div className="text-center mt-4 pt-3 border-top small">
              <Link to="/login" className="text-muted text-decoration-none">
                &larr; Back to Sign In
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
