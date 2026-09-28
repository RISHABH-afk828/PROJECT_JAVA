import React, { useState, useEffect } from 'react';
import apiClient from '../../services/api/apiClient';
import LoadingButton from '../../components/common/LoadingButton';

export default function AddressSection({ onSelectAddress, selectedAddressId }) {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  const [formData, setFormData] = useState({
    label: 'Home',
    house: '',
    street: '',
    locality: '',
    city: 'Bengaluru',
    state: 'Karnataka',
    postalCode: '560034',
    latitude: 12.9352,
    longitude: 77.6245,
    deliveryInstructions: '',
    isDefault: false,
  });

  useEffect(() => {
    fetchAddresses();
  }, []);

  const fetchAddresses = async () => {
    setLoading(true);
    try {
      const data = await apiClient.get('/users/me/addresses');
      setAddresses(data || []);
    } catch {
      setAddresses([]);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData((prev) => ({ ...prev, [e.target.name]: value }));
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrorMsg(null);
    try {
      await apiClient.post('/users/me/addresses', formData);
      setShowForm(false);
      setFormData({
        label: 'Home',
        house: '',
        street: '',
        locality: '',
        city: 'Bengaluru',
        state: 'Karnataka',
        postalCode: '560034',
        latitude: 12.9352,
        longitude: 77.6245,
        deliveryInstructions: '',
        isDefault: false,
      });
      fetchAddresses();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to save address.');
    } finally {
      setSaving(false);
    }
  };

  const handleSetDefault = async (id) => {
    try {
      await apiClient.post(`/users/me/addresses/${id}/default`);
      fetchAddresses();
    } catch (err) {
      alert(err.message || 'Could not set default address.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this address?')) return;
    try {
      await apiClient.delete(`/users/me/addresses/${id}`);
      fetchAddresses();
    } catch (err) {
      alert(err.message || 'Could not delete address.');
    }
  };

  return (
    <div className="card border p-4 shadow-sm mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="fw-bold mb-0 text-dark">
          <i className="bi bi-geo-alt me-2 text-danger" />
          Saved Addresses
        </h5>
        {!showForm && (
          <button
            type="button"
            className="btn btn-outline-dark btn-sm rounded-pill px-3"
            onClick={() => setShowForm(true)}
          >
            <i className="bi bi-plus me-1" />
            Add New Address
          </button>
        )}
      </div>

      {errorMsg && (
        <div className="alert alert-danger py-2 px-3 small mb-3">
          <i className="bi bi-exclamation-circle-fill me-2" />
          {errorMsg}
        </div>
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="border rounded p-3 mb-4 bg-light">
          <h6 className="fw-bold small text-dark mb-3">Add New Delivery Address</h6>
          <div className="row g-2 mb-2">
            <div className="col-4">
              <label className="form-label small fw-medium text-dark">Label</label>
              <select name="label" className="form-select form-select-sm" value={formData.label} onChange={handleChange}>
                <option value="Home">Home</option>
                <option value="Work">Work</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div className="col-8">
              <label className="form-label small fw-medium text-dark">House / Flat / Block</label>
              <input
                type="text"
                name="house"
                className="form-control form-control-sm"
                placeholder="e.g. Flat 301, Tower B"
                value={formData.house}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="mb-2">
            <label className="form-label small fw-medium text-dark">Street / Road / Landmark</label>
            <input
              type="text"
              name="street"
              className="form-control form-control-sm"
              placeholder="e.g. 5th Main, Near Park"
              value={formData.street}
              onChange={handleChange}
              required
            />
          </div>

          <div className="row g-2 mb-2">
            <div className="col-6">
              <label className="form-label small fw-medium text-dark">Locality / Area</label>
              <input
                type="text"
                name="locality"
                className="form-control form-control-sm"
                placeholder="e.g. Koramangala"
                value={formData.locality}
                onChange={handleChange}
                required
              />
            </div>
            <div className="col-6">
              <label className="form-label small fw-medium text-dark">Postal Code</label>
              <input
                type="text"
                name="postalCode"
                className="form-control form-control-sm"
                placeholder="560034"
                value={formData.postalCode}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="mb-2">
            <label className="form-label small fw-medium text-dark">Delivery Instructions (optional)</label>
            <input
              type="text"
              name="deliveryInstructions"
              className="form-control form-control-sm"
              placeholder="e.g. Leave with security guard"
              value={formData.deliveryInstructions}
              onChange={handleChange}
            />
          </div>

          <div className="form-check mb-3">
            <input
              type="checkbox"
              id="isDefault"
              name="isDefault"
              className="form-check-input"
              checked={formData.isDefault}
              onChange={handleChange}
            />
            <label htmlFor="isDefault" className="form-check-label small text-dark">
              Make this my default delivery address
            </label>
          </div>

          <div className="d-flex justify-content-end gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => setShowForm(false)}
            >
              Cancel
            </button>
            <LoadingButton type="submit" loading={saving} className="btn-primary btn-sm px-3">
              Save Address
            </LoadingButton>
          </div>
        </form>
      )}

      {loading ? (
        <div className="text-center py-3 text-muted small">Loading saved addresses...</div>
      ) : addresses.length === 0 ? (
        <div className="text-muted small py-2">
          No addresses saved yet. Add your address to speed up checkout.
        </div>
      ) : (
        <div className="d-flex flex-column gap-3">
          {addresses.map((addr) => (
            <div
              key={addr.id}
              className={`border rounded p-3 position-relative ${
                selectedAddressId === addr.id ? 'border-primary bg-light' : ''
              }`}
              style={{ cursor: onSelectAddress ? 'pointer' : 'default' }}
              onClick={() => onSelectAddress && onSelectAddress(addr)}
            >
              <div className="d-flex justify-content-between align-items-start mb-1">
                <div className="d-flex align-items-center gap-2">
                  <span className="badge bg-dark" style={{ fontSize: '0.75rem' }}>{addr.label}</span>
                  {addr.isDefault && (
                    <span className="badge bg-success-subtle text-success border border-success-subtle" style={{ fontSize: '0.7rem' }}>
                      Default Address
                    </span>
                  )}
                </div>

                <div className="dropdown" onClick={(e) => e.stopPropagation()}>
                  <button className="btn btn-sm btn-light border py-0 px-2" data-bs-toggle="dropdown">
                    <i className="bi bi-three-dots" />
                  </button>
                  <ul className="dropdown-menu dropdown-menu-end shadow-sm small">
                    {!addr.isDefault && (
                      <li>
                        <button className="dropdown-item py-1" onClick={() => handleSetDefault(addr.id)}>
                          <i className="bi bi-star me-2" />Set as Default
                        </button>
                      </li>
                    )}
                    <li>
                      <button className="dropdown-item py-1 text-danger" onClick={() => handleDelete(addr.id)}>
                        <i className="bi bi-trash me-2" />Delete Address
                      </button>
                    </li>
                  </ul>
                </div>
              </div>

              <div className="fw-semibold text-dark small">{addr.house}, {addr.street}</div>
              <div className="text-muted small">{addr.locality}, {addr.city}, {addr.state} - {addr.postalCode}</div>
              {addr.deliveryInstructions && (
                <div className="small text-muted mt-1 fst-italic">
                  <i className="bi bi-info-circle me-1" />
                  Note: {addr.deliveryInstructions}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
