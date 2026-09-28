import React, { useState, useEffect } from 'react';
import apiClient from '../../services/api/apiClient';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';

export default function DeliveryDashboardPage() {
  const [profile, setProfile] = useState(null);
  const [activeOrder, setActiveOrder] = useState(null);
  const [availableOrders, setAvailableOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    fetchProfileAndOrders();
    const interval = setInterval(fetchProfileAndOrders, 8000); // 8s live polling
    return () => clearInterval(interval);
  }, []);

  const fetchProfileAndOrders = async () => {
    try {
      const p = await apiClient.get('/delivery/profile');
      setProfile(p);

      // Fetch my active orders
      const myOrdersRes = await apiClient.get('/delivery/my-orders?size=10');
      const active = (myOrdersRes?.content || []).find((o) =>
        ['DELIVERY_ASSIGNED', 'PICKED_UP', 'OUT_FOR_DELIVERY'].includes(o.orderStatus)
      );
      setActiveOrder(active || null);

      // If online and no active order, fetch available tasks
      if (p.isOnline && !active) {
        try {
          const avail = await apiClient.get('/delivery/available-orders');
          setAvailableOrders(avail || []);
        } catch {
          setAvailableOrders([]);
        }
      } else {
        setAvailableOrders([]);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Failed to sync delivery dashboard');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleOnline = async () => {
    if (!profile) return;
    try {
      const updated = await apiClient.post('/delivery/status', { isOnline: !profile.isOnline });
      setProfile(updated);
      fetchProfileAndOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to update online status');
    }
  };

  const handleAcceptOrder = async (orderId) => {
    setActionLoading(true);
    setErrorMsg(null);
    try {
      await apiClient.post(`/delivery/orders/${orderId}/accept`);
      fetchProfileAndOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to claim delivery');
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmPickup = async (orderId) => {
    setActionLoading(true);
    setErrorMsg(null);
    try {
      await apiClient.post(`/delivery/orders/${orderId}/pickup`);
      fetchProfileAndOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to confirm pickup');
    } finally {
      setActionLoading(false);
    }
  };

  const handleStartOutForDelivery = async (orderId) => {
    setActionLoading(true);
    setErrorMsg(null);
    try {
      await apiClient.post(`/delivery/orders/${orderId}/out-for-delivery`);
      fetchProfileAndOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to start delivery');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCompleteDelivery = async (orderId) => {
    setActionLoading(true);
    setErrorMsg(null);
    try {
      await apiClient.post(`/delivery/orders/${orderId}/deliver`);
      fetchProfileAndOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to complete delivery');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading && !profile) {
    return (
      <div className="container-xl py-4">
        <Skeleton width="220px" height="36px" className="mb-4" />
        <Skeleton height="160px" className="mb-4" />
        <Skeleton height="300px" />
      </div>
    );
  }

  return (
    <div className="container-xl py-4">
      {/* Header */}
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
        <div>
          <h2 className="fw-bolder text-dark mb-1" style={{ letterSpacing: '-0.02em' }}>
            Delivery Partner Console
          </h2>
          <p className="text-muted small mb-0">
            Accept nearby ready parcels, navigate routes, and deliver quickly to customers.
          </p>
        </div>

        {/* Online/Offline Toggle */}
        <div className="d-flex align-items-center gap-3 bg-white p-2 px-3 rounded-pill border shadow-sm">
          <div className="form-check form-switch mb-0 d-flex align-items-center gap-2">
            <input
              className="form-check-input"
              type="checkbox"
              role="switch"
              id="onlineSwitch"
              checked={profile?.isOnline || false}
              onChange={handleToggleOnline}
              style={{ cursor: 'pointer', width: '2.5em', height: '1.3em' }}
            />
            <label className="form-check-label fw-bold text-dark small" htmlFor="onlineSwitch" style={{ cursor: 'pointer' }}>
              {profile?.isOnline ? (
                <span className="text-success d-flex align-items-center gap-1">
                  <i className="bi bi-broadcast" /> Online & Available
                </span>
              ) : (
                <span className="text-muted d-flex align-items-center gap-1">
                  <i className="bi bi-moon-fill" /> Offline
                </span>
              )}
            </label>
          </div>
        </div>
      </div>

      {errorMsg && (
        <div className="alert alert-danger d-flex align-items-center gap-2 mb-4" role="alert">
          <i className="bi bi-exclamation-triangle-fill" />
          <div>{errorMsg}</div>
        </div>
      )}

      {/* Partner Metrics Bar */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small">Vehicle</span>
            <div className="fw-bold text-dark fs-6 mt-1">
              <i className="bi bi-bicycle me-1 text-primary" />
              {profile?.vehicleType || 'Motorcycle'}
            </div>
            <span className="text-muted small" style={{ fontSize: '0.75rem' }}>{profile?.vehicleNumber}</span>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small">Completed Deliveries</span>
            <div className="fw-bold text-dark fs-5 mt-1">{profile?.totalDeliveries || 0}</div>
            <span className="text-success small" style={{ fontSize: '0.75rem' }}>100% on-time fulfillment</span>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small">Location Beacon</span>
            <div className="fw-bold text-dark fs-6 mt-1">
              <i className="bi bi-geo-alt-fill text-danger me-1" />
              {profile?.currentLatitude?.toFixed(4)}, {profile?.currentLongitude?.toFixed(4)}
            </div>
            <span className="text-muted small" style={{ fontSize: '0.75rem' }}>Active area</span>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card p-3 border shadow-sm">
            <span className="text-muted small">Status</span>
            <div className="mt-1">
              <span className={`badge ${profile?.isOnline ? 'bg-success' : 'bg-secondary'}`}>
                {profile?.isOnline ? 'AVAILABLE FOR ORDERS' : 'OFFLINE'}
              </span>
            </div>
            <span className="text-muted small" style={{ fontSize: '0.75rem' }}>
              {activeOrder ? '1 Task in progress' : 'Ready for assignment'}
            </span>
          </div>
        </div>
      </div>

      {/* Active Order Card */}
      {activeOrder && (
        <div className="card border-primary border-2 shadow-sm p-4 mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
          <div className="d-flex flex-wrap align-items-center justify-content-between border-bottom pb-3 mb-3 gap-2">
            <div>
              <span className="badge bg-primary mb-1">Active Delivery Task</span>
              <h4 className="fw-bold text-dark mb-0">Order #{activeOrder.orderNumber}</h4>
              <span className="text-muted small">Store: <strong>{activeOrder.vendorName}</strong></span>
            </div>
            <div className="text-end">
              <StatusBadge status={activeOrder.orderStatus} />
              <div className="fw-bolder text-dark fs-5 mt-1">₹{Number(activeOrder.total).toFixed(2)}</div>
            </div>
          </div>

          <div className="row g-4 mb-4">
            {/* Step 1: Pickup Point */}
            <div className="col-md-6">
              <div className="p-3 bg-light rounded border h-100">
                <div className="d-flex align-items-center gap-2 mb-2">
                  <span className="badge bg-dark rounded-circle px-2">1</span>
                  <span className="fw-bold text-dark small">PICKUP: {activeOrder.vendorName}</span>
                </div>
                <p className="text-muted small mb-0">Store counter parcel pickup</p>
                {activeOrder.orderStatus === 'DELIVERY_ASSIGNED' && (
                  <span className="badge bg-warning text-dark mt-2">Proceed to store</span>
                )}
                {activeOrder.orderStatus !== 'DELIVERY_ASSIGNED' && (
                  <span className="badge bg-success mt-2">Parcel Picked Up</span>
                )}
              </div>
            </div>

            {/* Step 2: Dropoff Point */}
            <div className="col-md-6">
              <div className="p-3 bg-light rounded border h-100">
                <div className="d-flex align-items-center gap-2 mb-2">
                  <span className="badge bg-dark rounded-circle px-2">2</span>
                  <span className="fw-bold text-dark small">DROPOFF: Customer Address</span>
                </div>
                <p className="text-dark small mb-0">{activeOrder.addressSnapshot}</p>
                {activeOrder.orderStatus === 'OUT_FOR_DELIVERY' && (
                  <span className="badge bg-info text-dark mt-2">In Transit to Customer</span>
                )}
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="d-flex flex-wrap align-items-center justify-content-end gap-2 pt-3 border-top">
            {activeOrder.orderStatus === 'DELIVERY_ASSIGNED' && (
              <button
                className="btn btn-warning text-dark px-4 fw-semibold d-flex align-items-center gap-2"
                disabled={actionLoading}
                onClick={() => handleConfirmPickup(activeOrder.id)}
              >
                {actionLoading ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-box-arrow-in-down" />}
                Confirm Pickup from Store
              </button>
            )}

            {activeOrder.orderStatus === 'PICKED_UP' && (
              <button
                className="btn btn-primary px-4 fw-semibold d-flex align-items-center gap-2"
                disabled={actionLoading}
                onClick={() => handleStartOutForDelivery(activeOrder.id)}
              >
                {actionLoading ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-bicycle" />}
                Start Out for Delivery
              </button>
            )}

            {activeOrder.orderStatus === 'OUT_FOR_DELIVERY' && (
              <button
                className="btn btn-success px-4 py-2 fw-semibold d-flex align-items-center gap-2"
                disabled={actionLoading}
                onClick={() => handleCompleteDelivery(activeOrder.id)}
              >
                {actionLoading ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check-circle-fill" />}
                Complete Delivery to Customer
              </button>
            )}
          </div>
        </div>
      )}

      {/* Available Tasks Pool */}
      {!activeOrder && (
        <div className="card border shadow-sm p-4" style={{ borderRadius: 'var(--radius-md)' }}>
          <div className="d-flex align-items-center justify-content-between mb-3">
            <h5 className="fw-bold text-dark mb-0">Ready Orders for Pickup</h5>
            <span className="badge bg-light text-dark border">
              {availableOrders.length} available
            </span>
          </div>

          {!profile?.isOnline ? (
            <div className="text-center py-5">
              <i className="bi bi-moon-stars fs-1 text-muted d-block mb-2" />
              <h5 className="fw-bold text-dark">You are currently offline</h5>
              <p className="text-muted small mb-3">
                Switch the toggle at the top right to start receiving available orders in your area.
              </p>
              <button className="btn btn-success btn-sm rounded-pill px-4" onClick={handleToggleOnline}>
                Go Online Now
              </button>
            </div>
          ) : availableOrders.length === 0 ? (
            <div className="text-center py-5">
              <i className="bi bi-clock-history fs-1 text-muted d-block mb-2" />
              <h5 className="fw-bold text-dark">Waiting for new orders...</h5>
              <p className="text-muted small mb-0">
                Stores are currently preparing parcels. Orders will appear here as soon as they are ready for pickup.
              </p>
            </div>
          ) : (
            <div className="vstack gap-3">
              {availableOrders.map((order) => (
                <div key={order.id} className="card p-3 border">
                  <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
                    <div>
                      <span className="fw-bold text-dark">Order #{order.orderNumber}</span>
                      <span className="text-muted small ms-2">&bull; Store: {order.vendorName}</span>
                    </div>
                    <span className="badge bg-success">Ready for pickup</span>
                  </div>

                  <p className="text-muted small mb-3">
                    <i className="bi bi-geo-alt-fill text-danger me-1" />
                    Dropoff: {order.addressSnapshot}
                  </p>

                  <div className="d-flex align-items-center justify-content-between pt-2 border-top">
                    <span className="small text-muted">
                      Items: {order.items?.length || 0} &bull; Order Value: ₹{Number(order.total).toFixed(2)}
                    </span>
                    <button
                      className="btn btn-primary btn-sm px-4 fw-semibold"
                      disabled={actionLoading}
                      onClick={() => handleAcceptOrder(order.id)}
                    >
                      {actionLoading ? <span className="spinner-border spinner-border-sm" /> : 'Claim Delivery'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
