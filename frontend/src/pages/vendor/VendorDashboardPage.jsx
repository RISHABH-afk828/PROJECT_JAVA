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

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const storeData = await apiClient.get('/vendor/store');
      setStore(storeData);

      const stockData = await apiClient.get('/vendors/me/inventory/low-stock');
      setLowStock(stockData || []);
    } catch {
      // Fallback
    } finally {
      setLoading(false);
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
