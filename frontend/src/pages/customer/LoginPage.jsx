import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../store/AuthContext';
import LoadingButton from '../../components/common/LoadingButton';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(searchParams.get('expired') ? 'Your session expired. Please sign in again.' : null);

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    if (error) setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.email.trim() || !formData.password) {
      setError('Please provide your email/phone and password');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const user = await login(formData);
      // Route based on role
      if (user.role === 'VENDOR') {
        navigate('/vendor/dashboard');
      } else if (user.role === 'DELIVERY_PARTNER') {
        navigate('/delivery/dashboard');
      } else if (user.role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else {
        const redirect = searchParams.get('redirect') || '/';
        navigate(redirect);
      }
    } catch (err) {
      setError(err.message || 'Invalid email/phone or password');
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
                <i className="bi bi-shop fs-4" />
              </span>
              <h3 className="fw-bold text-dark mt-2 mb-1">Welcome back</h3>
              <p className="text-muted small">Sign in to your Hyperlocal account</p>
            </div>

            {error && (
              <div className="alert alert-danger py-2 px-3 small d-flex align-items-center gap-2 mb-4" role="alert">
                <i className="bi bi-exclamation-circle-fill text-danger flex-shrink-0" />
                <div>{error}</div>
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              <div className="mb-3">
                <label className="form-label small fw-medium text-dark" htmlFor="email">
                  Email or Phone Number
                </label>
                <input
                  type="text"
                  id="email"
                  name="email"
                  className="form-control"
                  placeholder="name@example.com or +91XXXXXXXXXX"
                  value={formData.email}
                  onChange={handleChange}
                  required
                  autoFocus
                />
              </div>

              <div className="mb-3">
                <div className="d-flex justify-content-between align-items-center mb-1">
                  <label className="form-label small fw-medium text-dark mb-0" htmlFor="password">
                    Password
                  </label>
                  <Link to="/forgot-password" style={{ fontSize: '0.8rem' }} className="text-muted text-decoration-none">
                    Forgot password?
                  </Link>
                </div>
                <div className="input-group">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    id="password"
                    name="password"
                    className="form-control"
                    placeholder="Enter your password"
                    value={formData.password}
                    onChange={handleChange}
                    required
                  />
                  <button
                    type="button"
                    className="btn btn-outline-secondary border"
                    onClick={() => setShowPassword(!showPassword)}
                    tabIndex="-1"
                  >
                    <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`} />
                  </button>
                </div>
              </div>

              <div className="d-grid mt-4">
                <LoadingButton type="submit" loading={loading} className="btn-primary py-2 fw-semibold">
                  Sign In
                </LoadingButton>
              </div>
            </form>

            <div className="text-center mt-4 pt-3 border-top small text-muted">
              Don't have an account?{' '}
              <Link to="/register" className="fw-semibold text-dark text-decoration-none">
                Create one now
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
