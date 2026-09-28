import React, { useState, useEffect } from 'react';
import apiClient from '../../services/api/apiClient';
import { useAuth } from '../../store/AuthContext';
import LoadingButton from './LoadingButton';

export default function LocationModal({ show, onClose, onSelectLocation, currentLocation }) {
  const { isAuthenticated } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [savedAddresses, setSavedAddresses] = useState([]);
  const [geoLoading, setGeoLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    if (show && isAuthenticated) {
      fetchSavedAddresses();
    }
  }, [show, isAuthenticated]);

  const fetchSavedAddresses = async () => {
    try {
      const res = await apiClient.get('/users/me/addresses');
      setSavedAddresses(res || []);
    } catch {
      // Ignore if user has no addresses yet
    }
  };

  const handleSearch = async (query) => {
    setSearchQuery(query);
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    try {
      const res = await apiClient.get(`/locations/search?q=${encodeURIComponent(query.trim())}`);
      setSearchResults(res || []);
    } catch {
      setSearchResults([]);
    }
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setErrorMsg('Geolocation is not supported by your browser.');
      return;
    }

    setGeoLoading(true);
    setErrorMsg(null);

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          const res = await apiClient.get(`/locations/reverse-geocode?lat=${lat}&lng=${lng}`);
          const loc = {
            label: res.formattedAddress || `${res.locality || 'Detected Area'}, ${res.city || 'Bengaluru'}`,
            lat,
            lng,
            locality: res.locality,
            city: res.city,
          };
          onSelectLocation(loc);
          onClose();
        } catch {
          // Fallback if reverse geocode fails
          onSelectLocation({
            label: 'Current Location',
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          });
          onClose();
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

  const handlePickLocation = (loc) => {
    onSelectLocation(loc);
    onClose();
  };

  if (!show) return null;

  return (
    <div
      className="modal show d-block"
      tabIndex="-1"
      style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}
    >
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
          <div className="modal-header border-bottom pb-3">
            <h5 className="modal-title fw-bold text-dark d-flex align-items-center gap-2">
              <i className="bi bi-geo-alt-fill text-danger" />
              Choose Delivery Location
            </h5>
            <button type="button" className="btn-close" onClick={onClose} aria-label="Close" />
          </div>

          <div className="modal-body p-4">
            {errorMsg && (
              <div className="alert alert-warning py-2 px-3 small d-flex align-items-center gap-2 mb-3">
                <i className="bi bi-exclamation-triangle-fill flex-shrink-0" />
                <div>{errorMsg}</div>
              </div>
            )}

            {/* GPS Location Button */}
            <div className="mb-4">
              <LoadingButton
                type="button"
                loading={geoLoading}
                className="btn-outline-primary w-100 py-2 d-flex align-items-center justify-content-center gap-2"
                onClick={handleUseCurrentLocation}
              >
                <i className="bi bi-crosshair fs-5" />
                <span>Use My Current Location</span>
              </LoadingButton>
            </div>

            {/* Search Input */}
            <div className="mb-3">
              <label className="form-label small fw-medium text-dark">Or search your locality/area</label>
              <div className="input-group">
                <span className="input-group-text bg-white border-end-0">
                  <i className="bi bi-search text-muted" />
                </span>
                <input
                  type="text"
                  className="form-control border-start-0"
                  placeholder="e.g. Koramangala, Indiranagar, HSR..."
                  value={searchQuery}
                  onChange={(e) => handleSearch(e.target.value)}
                  autoFocus
                />
              </div>
            </div>

            {/* Autocomplete Results */}
            {searchResults.length > 0 && (
              <div className="list-group mb-4 shadow-sm" style={{ maxHeight: '180px', overflowY: 'auto' }}>
                {searchResults.map((res, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className="list-group-item list-group-item-action text-start py-2 d-flex align-items-center gap-2"
                    onClick={() => handlePickLocation({ label: res.label, lat: res.lat, lng: res.lng })}
                  >
                    <i className="bi bi-geo-alt text-muted" />
                    <span className="small text-truncate">{res.label}</span>
                  </button>
                ))}
              </div>
            )}

            {/* Saved Addresses (if customer has any) */}
            {isAuthenticated && savedAddresses.length > 0 && (
              <div className="mt-4 pt-3 border-top">
                <h6 className="fw-bold small text-dark mb-2">Saved Addresses</h6>
                <div className="d-flex flex-column gap-2" style={{ maxHeight: '200px', overflowY: 'auto' }}>
                  {savedAddresses.map((addr) => (
                    <div
                      key={addr.id}
                      className="border rounded p-2 d-flex justify-content-between align-items-center cursor-pointer hover-surface"
                      style={{ cursor: 'pointer' }}
                      onClick={() =>
                        handlePickLocation({
                          label: `${addr.label}: ${addr.house}, ${addr.locality}`,
                          lat: addr.latitude,
                          lng: addr.longitude,
                          addressId: addr.id,
                        })
                      }
                    >
                      <div className="d-flex align-items-center gap-2 text-truncate">
                        <span className="badge bg-light text-dark border small">{addr.label}</span>
                        <div className="small text-truncate" style={{ maxWidth: '280px' }}>
                          <span className="fw-medium text-dark">{addr.house}, {addr.street}</span>
                          <span className="text-muted d-block" style={{ fontSize: '0.75rem' }}>{addr.locality}, {addr.city}</span>
                        </div>
                      </div>
                      <i className="bi bi-chevron-right text-muted small" />
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
