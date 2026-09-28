import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../store/AuthContext';
import LoadingButton from '../../components/common/LoadingButton';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    role: 'CUSTOMER',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    if (error) setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.email.trim() || !formData.phone.trim() || !formData.password) {
      setError('Please fill in all required fields.');
      return;
    }
    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const user = await register(formData);
      if (user.role === 'VENDOR') {
        navigate('/vendor/dashboard');
      } else if (user.role === 'DELIVERY_PARTNER') {
        navigate('/delivery/dashboard');
      } else {
        navigate('/');
      }
    } catch (err) {
      setError(err.message || 'Registration failed. Please check your details and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-sm-10 col-md-8 col-lg-6">
          <div className="card border p-4 p-md-5 shadow-sm" style={{ borderRadius: 'var(--radius-md)' }}>
            <div className="text-center mb-4">
              <span
                className="d-inline-flex align-items-center justify-content-center text-white rounded mb-2"
                style={{ width: '44px', height: '44px', backgroundColor: 'var(--color-primary)' }}
              >
                <i className="bi bi-person-plus fs-4" />
              </span>
              <h3 className="fw-bold text-dark mt-2 mb-1">Create an account</h3>
              <p className="text-muted small">Join Hyperlocal as a Customer, Local Store, or Delivery Partner</p>
            </div>

            {error && (
              <div className="alert alert-danger py-2 px-3 small d-flex align-items-center gap-2 mb-4" role="alert">
                <i className="bi bi-exclamation-circle-fill text-danger flex-shrink-0" />
                <div>{error}</div>
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              {/* Account Type Selection */}
              <div className="mb-3">
                <label className="form-label small fw-medium text-dark">I want to join as:</label>
                <div className="row g-2">
                  <div className="col-4">
                    <input
                      type="radio"
                      className="btn-check"
                      name="role"
                      id="role-customer"
                      value="CUSTOMER"
                      checked={formData.role === 'CUSTOMER'}
                      onChange={handleChange}
                    />
                    <label className="btn btn-outline-dark w-100 py-2 text-center" htmlFor="role-customer" style={{ fontSize: '0.85rem' }}>
                      <i className="bi bi-bag d-block mb-1 fs-5" />
                      Customer
                    </label>
                  </div>
                  <div className="col-4">
                    <input
                      type="radio"
                      className="btn-check"
                      name="role"
                      id="role-vendor"
                      value="VENDOR"
                      checked={formData.role === 'VENDOR'}
                      onChange={handleChange}
                    />
                    <label className="btn btn-outline-dark w-100 py-2 text-center" htmlFor="role-vendor" style={{ fontSize: '0.85rem' }}>
                      <i className="bi bi-shop d-block mb-1 fs-5" />
                      Local Store
                    </label>
                  </div>
                  <div className="col-4">
                    <input
                      type="radio"
                      className="btn-check"
                      name="role"
                      id="role-delivery"
                      value="DELIVERY_PARTNER"
                      checked={formData.role === 'DELIVERY_PARTNER'}
                      onChange={handleChange}
                    />
                    <label className="btn btn-outline-dark w-100 py-2 text-center" htmlFor="role-delivery" style={{ fontSize: '0.85rem' }}>
                      <i className="bi bi-bicycle d-block mb-1 fs-5" />
                      Delivery
                    </label>
                  </div>
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-dark" htmlFor="name">
                  Full Name {formData.role === 'VENDOR' && '(or Store Owner Name)'}
                </label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  className="form-control"
                  placeholder="e.g. Rahul Sharma"
                  value={formData.name}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-dark" htmlFor="email">
                  Email Address
                </label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  className="form-control"
                  placeholder="name@example.com"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-dark" htmlFor="phone">
                  Phone Number
                </label>
                <input
                  type="tel"
                  id="phone"
                  name="phone"
                  className="form-control"
                  placeholder="+91XXXXXXXXXX"
                  value={formData.phone}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-medium text-dark" htmlFor="password">
                  Password
                </label>
                <div className="input-group">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    id="password"
                    name="password"
                    className="form-control"
                    placeholder="At least 6 characters"
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
                  Create Account
                </LoadingButton>
              </div>
            </form>

            <div className="text-center mt-4 pt-3 border-top small text-muted">
              Already have an account?{' '}
              <Link to="/login" className="fw-semibold text-dark text-decoration-none">
                Sign in
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
