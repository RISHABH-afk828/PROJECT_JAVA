import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import StatusBadge from '../../components/common/StatusBadge';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';

export default function VendorDashboardPage() {
  const [store, setStore] = useState(null);
  const [lowStock, setLowStock] = useState([]);
  const [loading, setLoading] = useState(true);
  const [togglingStatus, setTogglingStatus] = useState(false);

  const [setupForm, setSetupForm] = useState({
    storeName: '',
    description: '',
    phone: '',
    address: '',
    latitude: '',
    longitude: '',
    deliveryRadiusKm: 5.0,
    imageUrl: '',
  });
  const [setupError, setSetupError] = useState(null);
  const [submittingSetup, setSubmittingSetup] = useState(false);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const storeData = await apiClient.get('/vendor/store');
      setStore(storeData);

      try {
        const stockData = await apiClient.get('/vendors/me/inventory/low-stock');
        setLowStock(stockData || []);
      } catch (stockErr) {
        console.warn('Could not load low-stock items:', stockErr);
        setLowStock([]);
      }
    } catch (err) {
      if (err.code === 'STORE_NOT_FOUND' || err.status === 404) {
        // Store does not exist yet for this account
        setStore(null);
      } else {
        // Genuine network error, 401/403, or 500 error
        setLoadError(err.message || 'Unable to load store information.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSetupChange = (e) => {
    const { name, value } = e.target;
    setSetupForm((prev) => ({ ...prev, [name]: value }));
    if (setupError) setSetupError(null);
  };

  const handleUseCurrentLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setSetupForm((prev) => ({
            ...prev,
            latitude: Number(pos.coords.latitude.toFixed(6)),
            longitude: Number(pos.coords.longitude.toFixed(6)),
          }));
        },
        (err) => {
          setSetupError('Could not get current location: ' + err.message);
        }
      );
    } else {
      setSetupError('Geolocation is not supported by your browser');
    }
  };

  const handleRegisterStore = async (e) => {
    e.preventDefault();
    const lat = parseFloat(setupForm.latitude);
    const lng = parseFloat(setupForm.longitude);
    const radius = parseFloat(setupForm.deliveryRadiusKm);

    if (isNaN(lat) || lat < -90 || lat > 90) {
      setSetupError('Latitude must be a valid number between -90 and 90');
      return;
    }
    if (isNaN(lng) || lng < -180 || lng > 180) {
      setSetupError('Longitude must be a valid number between -180 and 180');
      return;
    }
    if (isNaN(radius) || radius <= 0) {
      setSetupError('Delivery radius must be greater than 0 km');
      return;
    }

    setSubmittingSetup(true);
    setSetupError(null);
    try {
      const createdStore = await apiClient.post('/vendor/register', {
        storeName: setupForm.storeName.trim(),
        description: setupForm.description.trim() || undefined,
        phone: setupForm.phone.trim(),
        address: setupForm.address.trim(),
        latitude: lat,
        longitude: lng,
        deliveryRadiusKm: radius,
        imageUrl: setupForm.imageUrl.trim() || undefined,
      });
      setStore(createdStore);
      fetchDashboardData();
    } catch (err) {
      if (err.details && typeof err.details === 'object') {
        const detailsStr = Object.values(err.details).join('. ');
        setSetupError(`${err.message}: ${detailsStr}`);
      } else {
        setSetupError(err.message || 'Failed to register store. Please try again.');
      }
    } finally {
      setSubmittingSetup(false);
    }
  };

  const handleToggleStoreStatus = async () => {
    if (!store) return;
    setTogglingStatus(true);
    try {
      const nextOverride = store.isOpen ? false : true;
      const updated = await apiClient.patch('/vendor/store/status', {
        manualOpenOverride: nextOverride,
      });
      setStore(updated);
    } catch (err) {
      alert(err.message || 'Failed to update store status');
    } finally {
      setTogglingStatus(false);
    }
  };

  if (loading) {
    return (
      <div className="container-xl py-4">
        <Skeleton height="80px" className="mb-4" />
        <div className="row g-3 mb-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="col-6 col-md-3">
              <Skeleton height="100px" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="container-xl py-5">
        <div className="row justify-content-center">
          <div className="col-12 col-md-8 col-lg-6">
            <div className="card border shadow-sm p-4 text-center" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="text-danger mb-3">
                <i className="bi bi-exclamation-triangle-fill fs-1" />
              </div>
              <h4 className="fw-bold text-dark mb-2">Unable to Load Dashboard</h4>
              <p className="text-muted small mb-4">{loadError}</p>
              <div>
                <button className="btn btn-primary px-4 rounded-pill" onClick={fetchDashboardData}>
                  <i className="bi bi-arrow-clockwise me-2" /> Retry
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!store) {
    return (
      <div className="container-xl py-5">
        <div className="row justify-content-center">
          <div className="col-12 col-md-8 col-lg-6">
            <div className="card border shadow-sm p-4 p-md-5" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="text-center mb-4">
                <span
                  className="d-inline-flex align-items-center justify-content-center text-white rounded mb-2"
                  style={{ width: '48px', height: '48px', backgroundColor: 'var(--color-primary)' }}
                >
                  <i className="bi bi-shop fs-3" />
                </span>
                <h3 className="fw-bold text-dark mt-2 mb-1">Set Up Your Storefront</h3>
                <p className="text-muted small">
                  Complete your store profile to start listing products and fulfilling local customer orders.
                </p>
              </div>

              {setupError && (
                <div className="alert alert-danger py-2 px-3 small d-flex align-items-center gap-2 mb-3" role="alert">
                  <i className="bi bi-exclamation-circle-fill text-danger flex-shrink-0" />
                  <div>{setupError}</div>
                </div>
              )}

              <form onSubmit={handleRegisterStore}>
                <div className="mb-3">
                  <label className="form-label small fw-semibold text-dark">Store Name *</label>
                  <input
                    type="text"
                    name="storeName"
                    className="form-control"
                    placeholder="e.g. Green Valley Fresh Market"
                    value={setupForm.storeName}
                    onChange={handleSetupChange}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label small fw-semibold text-dark">Store Description</label>
                  <textarea
                    name="description"
                    className="form-control"
                    rows={2}
                    placeholder="Brief description of your products and neighborhood store"
                    value={setupForm.description}
                    onChange={handleSetupChange}
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label small fw-semibold text-dark">Contact Phone *</label>
                  <input
                    type="tel"
                    name="phone"
                    className="form-control"
                    placeholder="+919876543210"
                    value={setupForm.phone}
                    onChange={handleSetupChange}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label small fw-semibold text-dark">Store Address *</label>
                  <input
                    type="text"
                    name="address"
                    className="form-control"
                    placeholder="123 Market Road, Koramangala, Bengaluru"
                    value={setupForm.address}
                    onChange={handleSetupChange}
                    required
                  />
                </div>

                <div className="row g-2 mb-3">
                  <div className="col-6">
                    <label className="form-label small fw-semibold text-dark">Latitude * (-90 to 90)</label>
                    <input
                      type="number"
                      step="any"
                      name="latitude"
                      className="form-control"
                      placeholder="12.9352"
                      value={setupForm.latitude}
                      onChange={handleSetupChange}
                      required
                    />
                  </div>
                  <div className="col-6">
                    <label className="form-label small fw-semibold text-dark">Longitude * (-180 to 180)</label>
                    <input
                      type="number"
                      step="any"
                      name="longitude"
                      className="form-control"
                      placeholder="77.6245"
                      value={setupForm.longitude}
                      onChange={handleSetupChange}
                      required
                    />
                  </div>
                  <div className="col-12 mt-1">
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm rounded-pill w-100"
                      onClick={handleUseCurrentLocation}
                    >
                      <i className="bi bi-geo-alt me-1" /> Use My Current GPS Location
                    </button>
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label small fw-semibold text-dark">Delivery Radius (km) *</label>
                  <input
                    type="number"
                    step="0.5"
                    min="0.5"
                    max="50"
                    name="deliveryRadiusKm"
                    className="form-control"
                    value={setupForm.deliveryRadiusKm}
                    onChange={handleSetupChange}
                    required
                  />
                  <div className="form-text small">Maximum distance you can fulfill deliveries.</div>
                </div>

                <div className="mb-4">
                  <label className="form-label small fw-semibold text-dark">Store Banner / Image URL</label>
                  <input
                    type="url"
                    name="imageUrl"
                    className="form-control"
                    placeholder="https://example.com/store-image.jpg"
                    value={setupForm.imageUrl}
                    onChange={handleSetupChange}
                  />
                </div>

                <button
                  type="submit"
                  className="btn btn-primary w-100 py-2 fw-semibold rounded-pill"
                  disabled={submittingSetup}
                >
                  {submittingSetup ? 'Creating Store...' : 'Launch Storefront'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container-xl py-4">
      {/* Top Banner & Store Status Toggle */}
      <div className="card border p-4 shadow-sm mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
        <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
          <div>
            <div className="d-flex align-items-center gap-2 mb-1">
              <h3 className="fw-bolder mb-0 text-dark">{store?.storeName || 'Vendor Operating Console'}</h3>
              <StatusBadge status={store?.isOpen ? 'OPEN' : 'CLOSED'} />
            </div>
            <p className="text-muted small mb-0">{store?.address}</p>
          </div>

          <div className="d-flex align-items-center gap-3">
            <div className="text-end d-none d-sm-block">
              <span className="small text-muted d-block">Store Availability</span>
              <span className="fw-semibold text-dark small">
                {store?.isOpen ? 'Accepting Orders' : 'Store Closed'}
              </span>
            </div>
            <button
              className={`btn btn-sm px-3 rounded-pill fw-semibold ${
                store?.isOpen ? 'btn-outline-danger' : 'btn-success text-white'
              }`}
              disabled={togglingStatus}
              onClick={handleToggleStoreStatus}
            >
              {togglingStatus
                ? 'Updating...'
                : store?.isOpen
                ? 'Close Store Now'
                : 'Open Store for Orders'}
            </button>
          </div>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small fw-medium">Today's Orders</span>
            <h3 className="fw-bolder text-dark mb-0 mt-1">12</h3>
            <span className="text-success small mt-1 d-block"><i className="bi bi-arrow-up" /> +20% vs yesterday</span>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small fw-medium">Pending Orders</span>
            <h3 className="fw-bolder text-warning mb-0 mt-1">2</h3>
            <Link to="/vendor/orders" className="small text-decoration-none mt-1 d-block">View order queue &rarr;</Link>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small fw-medium">Sales Total Today</span>
            <h3 className="fw-bolder text-dark mb-0 mt-1">₹4,850</h3>
            <span className="text-muted small mt-1 d-block">10 completed orders</span>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small fw-medium">Low Stock Alerts</span>
            <h3 className={`fw-bolder mb-0 mt-1 ${lowStock.length > 0 ? 'text-danger' : 'text-success'}`}>
              {lowStock.length}
            </h3>
            <Link to="/vendor/inventory" className="small text-decoration-none mt-1 d-block">Manage stock &rarr;</Link>
          </div>
        </div>
      </div>

      {/* Low Stock Alerts & Quick Actions */}
      <div className="row g-4">
        <div className="col-12 col-lg-8">
          <div className="card border shadow-sm p-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h5 className="fw-bold mb-0 text-dark">
                <i className="bi bi-exclamation-triangle-fill text-warning me-2" />
                Low Stock Items
              </h5>
              <Link to="/vendor/inventory" className="btn btn-outline-dark btn-sm rounded-pill px-3">
                View Full Inventory
              </Link>
            </div>

            {lowStock.length === 0 ? (
              <div className="text-muted small py-3 text-center">
                All catalog products have sufficient stock levels!
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table align-middle table-sm mb-0">
                  <thead className="table-light">
                    <tr>
                      <th>Product</th>
                      <th>Unit</th>
                      <th>Available</th>
                      <th>Threshold</th>
                      <th className="text-end">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lowStock.map((item) => (
                      <tr key={item.id}>
                        <td className="fw-semibold text-dark">{item.productName}</td>
                        <td className="text-muted small">{item.productUnit}</td>
                        <td>
                          <span className={`badge ${item.isOutOfStock ? 'bg-danger' : 'bg-warning text-dark'}`}>
                            {item.availableQuantity} left
                          </span>
                        </td>
                        <td className="text-muted small">{item.lowStockThreshold}</td>
                        <td className="text-end">
                          <Link to="/vendor/inventory" className="btn btn-primary btn-sm py-0 px-2">
                            Restock
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Quick Operations Actions */}
        <div className="col-12 col-lg-4">
          <div className="card border shadow-sm p-4 h-100">
            <h5 className="fw-bold mb-3 text-dark">Store Operations</h5>
            <div className="d-flex flex-column gap-2">
              <Link to="/vendor/products" className="btn btn-outline-dark text-start p-3 d-flex align-items-center justify-content-between">
                <div>
                  <div className="fw-bold small">Product Catalog</div>
                  <div className="text-muted" style={{ fontSize: '0.75rem' }}>Add, edit, or archive products</div>
                </div>
                <i className="bi bi-chevron-right" />
              </Link>

              <Link to="/vendor/inventory" className="btn btn-outline-dark text-start p-3 d-flex align-items-center justify-content-between">
                <div>
                  <div className="fw-bold small">Inventory Adjustments</div>
                  <div className="text-muted" style={{ fontSize: '0.75rem' }}>Update stock counts and view movements</div>
                </div>
                <i className="bi bi-chevron-right" />
              </Link>

              <Link to="/vendor/orders" className="btn btn-outline-dark text-start p-3 d-flex align-items-center justify-content-between">
                <div>
                  <div className="fw-bold small">Incoming Orders Queue</div>
                  <div className="text-muted" style={{ fontSize: '0.75rem' }}>Accept, prepare, and dispatch orders</div>
                </div>
                <i className="bi bi-chevron-right" />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
