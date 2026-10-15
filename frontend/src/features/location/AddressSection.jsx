import React, { useState, useEffect } from 'react';
import apiClient from '../../services/api/apiClient';
import LoadingButton from '../../components/common/LoadingButton';

const emptyAddress = {
  label: 'Home',
  house: '',
  street: '',
  locality: '',
  city: '',
  state: '',
  postalCode: '',
  latitude: null,
  longitude: null,
  deliveryInstructions: '',
  isDefault: false,
};

export default function AddressSection({ onSelectAddress, selectedAddressId }) {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  const [formData, setFormData] = useState(emptyAddress);
  const [geoLoading, setGeoLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);

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

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setErrorMsg('Geolocation is not supported by your browser.');
      return;
    }
    setGeoLoading(true);
    setErrorMsg(null);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const lat = pos.coords.latitude;
          const lng = pos.coords.longitude;
          const res = await apiClient.get(`/locations/reverse-geocode?lat=${lat}&lng=${lng}`);
          setFormData((prev) => ({
            ...prev,
            latitude: Number(lat.toFixed(6)),
            longitude: Number(lng.toFixed(6)),
            city: res.city || prev.city,
            state: res.state || prev.state,
            postalCode: res.postalCode || prev.postalCode,
            locality: res.locality || prev.locality,
            street: res.street || prev.street,
            house: res.house || prev.house,
          }));
        } catch {
          // If reverse geocoding fails, at least save coordinates
          setFormData((prev) => ({
            ...prev,
            latitude: Number(pos.coords.latitude.toFixed(6)),
            longitude: Number(pos.coords.longitude.toFixed(6)),
          }));
        } finally {
          setGeoLoading(false);
        }
      },
      (err) => {
        setGeoLoading(false);
        if (err.code === 1) {
          setErrorMsg('Location permission denied. Please search your locality or address manually.');
        } else {
          setErrorMsg('Could not detect your current location. Please search manually.');
        }
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  };

  const handleSearch = async (query) => {
    setSearchQuery(query);
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    setSearchLoading(true);
    try {
      const res = await apiClient.get(`/locations/search?q=${encodeURIComponent(query.trim())}`);
      setSearchResults(res || []);
    } catch {
      setSearchResults([]);
    } finally {
      setSearchLoading(false);
    }
  };

  const handleSelectSearchResult = (res) => {
    setFormData((prev) => ({
      ...prev,
      latitude: res.lat != null ? Number(Number(res.lat).toFixed(6)) : prev.latitude,
      longitude: res.lng != null ? Number(Number(res.lng).toFixed(6)) : prev.longitude,
      city: res.city || prev.city,
      state: res.state || prev.state,
      postalCode: res.postalCode || prev.postalCode,
      locality: res.locality || prev.locality,
      street: res.street || prev.street,
      house: res.house || prev.house,
    }));
    setSearchQuery('');
    setSearchResults([]);
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (formData.latitude == null || formData.longitude == null || isNaN(formData.latitude) || isNaN(formData.longitude)) {
      setErrorMsg('Please use GPS or Search to set delivery coordinates for this address.');
      return;
    }
    if (!formData.city.trim() || !formData.state.trim() || !formData.postalCode.trim() || !formData.house.trim() || !formData.street.trim() || !formData.locality.trim()) {
      setErrorMsg('Please fill in all required address fields.');
      return;
    }
    setSaving(true);
    setErrorMsg(null);
    try {
      await apiClient.post('/users/me/addresses', {
        ...formData,
        house: formData.house.trim(),
        street: formData.street.trim(),
        locality: formData.locality.trim(),
        city: formData.city.trim(),
        state: formData.state.trim(),
        postalCode: formData.postalCode.trim(),
        deliveryInstructions: formData.deliveryInstructions ? formData.deliveryInstructions.trim() : '',
      });
      setShowForm(false);
      setFormData(emptyAddress);
      setSearchQuery('');
      setSearchResults([]);
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
            onClick={() => {
              setFormData(emptyAddress);
              setErrorMsg(null);
              setShowForm(true);
            }}
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

          {/* Location Detection / Autocomplete */}
          <div className="mb-3">
            <div className="d-flex gap-2 mb-2">
              <LoadingButton
                type="button"
                loading={geoLoading}
                className="btn-outline-primary btn-sm flex-grow-1 d-flex align-items-center justify-content-center gap-1"
                onClick={handleUseCurrentLocation}
              >
                <i className="bi bi-crosshair" />
                <span>Use My Current GPS Location</span>
              </LoadingButton>
            </div>
            <div className="input-group input-group-sm">
              <span className="input-group-text bg-white">
                <i className="bi bi-search text-muted" />
              </span>
              <input
                type="text"
                className="form-control"
                placeholder="Search area, landmark or street to autofill..."
                value={searchQuery}
                onChange={(e) => handleSearch(e.target.value)}
              />
            </div>
            {searchResults.length > 0 && (
              <div className="list-group shadow-sm mt-1 position-relative" style={{ maxHeight: '160px', overflowY: 'auto', zIndex: 10 }}>
                {searchResults.map((res, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className="list-group-item list-group-item-action text-start py-2 small d-flex align-items-center gap-2"
                    onClick={() => handleSelectSearchResult(res)}
                  >
                    <i className="bi bi-geo-alt text-muted" />
                    <span className="text-truncate">{res.label}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Coordinate Status */}
          <div className="mb-3">
            {formData.latitude != null && formData.longitude != null ? (
              <span className="badge bg-success-subtle text-success border border-success small">
                <i className="bi bi-geo-alt-fill me-1" />
                Coordinates: {formData.latitude.toFixed(4)}, {formData.longitude.toFixed(4)}
              </span>
            ) : (
              <span className="badge bg-warning-subtle text-warning-emphasis border border-warning small">
                <i className="bi bi-exclamation-circle me-1" />
                Coordinates not set — click GPS button or select from search
              </span>
            )}
          </div>

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
              <label className="form-label small fw-medium text-dark">House / Flat / Block *</label>
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
            <label className="form-label small fw-medium text-dark">Street / Road / Landmark *</label>
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
              <label className="form-label small fw-medium text-dark">Locality / Area *</label>
              <input
                type="text"
                name="locality"
                className="form-control form-control-sm"
                placeholder="e.g. Bandra West, Virar East"
                value={formData.locality}
                onChange={handleChange}
                required
              />
            </div>
            <div className="col-6">
              <label className="form-label small fw-medium text-dark">Postal Code *</label>
              <input
                type="text"
                name="postalCode"
                className="form-control form-control-sm"
                placeholder="e.g. 401305"
                value={formData.postalCode}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="row g-2 mb-2">
            <div className="col-6">
              <label className="form-label small fw-medium text-dark">City *</label>
              <input
                type="text"
                name="city"
                className="form-control form-control-sm"
                placeholder="e.g. Mumbai, Palghar"
                value={formData.city}
                onChange={handleChange}
                required
              />
            </div>
            <div className="col-6">
              <label className="form-label small fw-medium text-dark">State *</label>
              <input
                type="text"
                name="state"
                className="form-control form-control-sm"
                placeholder="e.g. Maharashtra"
                value={formData.state}
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
