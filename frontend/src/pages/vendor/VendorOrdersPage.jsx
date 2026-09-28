import React, { useState, useEffect } from 'react';
import apiClient from '../../services/api/apiClient';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';

export default function VendorOrdersPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('VENDOR_PENDING'); // VENDOR_PENDING, ACCEPTED, PREPARING, READY, ALL
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [rejectModalOrder, setRejectModalOrder] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    fetchOrders();
    const interval = setInterval(fetchOrders, 10000); // 10s live polling
    return () => clearInterval(interval);
  }, [activeTab]);

  const fetchOrders = async () => {
    try {
      let url = '/vendor/orders?size=50';
      if (activeTab !== 'ALL') {
        url += `&status=${activeTab}`;
      }
      const res = await apiClient.get(url);
      setOrders(res?.content || []);
    } catch (err) {
      setErrorMsg(err.message || 'Failed to fetch vendor orders');
    } finally {
      setLoading(false);
    }
  };

  const handleAcceptOrder = async (orderId) => {
    setActionLoadingId(orderId);
    setErrorMsg(null);
    try {
      await apiClient.post(`/vendor/orders/${orderId}/accept`);
      fetchOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to accept order');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleStartPreparing = async (orderId) => {
    setActionLoadingId(orderId);
    setErrorMsg(null);
    try {
      await apiClient.post(`/vendor/orders/${orderId}/preparing`);
      fetchOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to update order to preparing');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleMarkReady = async (orderId) => {
    setActionLoadingId(orderId);
    setErrorMsg(null);
    try {
      await apiClient.post(`/vendor/orders/${orderId}/ready`);
      fetchOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to mark order ready');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleConfirmReject = async () => {
    if (!rejectModalOrder) return;
    setActionLoadingId(rejectModalOrder.id);
    setErrorMsg(null);
    try {
      await apiClient.post(`/vendor/orders/${rejectModalOrder.id}/reject`, {
        reason: rejectReason || 'Store at capacity / Unable to fulfill order',
      });
      setRejectModalOrder(null);
      setRejectReason('');
      fetchOrders();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to reject order');
    } finally {
      setActionLoadingId(null);
    }
  };

  const tabs = [
    { key: 'VENDOR_PENDING', label: 'Incoming Orders', icon: 'bi-bell-fill' },
    { key: 'ACCEPTED', label: 'Accepted', icon: 'bi-check2' },
    { key: 'PREPARING', label: 'Preparing', icon: 'bi-box-seam' },
    { key: 'READY', label: 'Ready for Pickup', icon: 'bi-bag-check' },
    { key: 'ALL', label: 'All Orders', icon: 'bi-list-ul' },
  ];

  return (
    <div className="container-xl py-4">
      {/* Header */}
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bolder text-dark mb-1" style={{ letterSpacing: '-0.02em' }}>
            Store Order Fulfillment
          </h2>
          <p className="text-muted small mb-0">
            Accept incoming orders, pack items, and stage parcels for delivery partner pickup.
          </p>
        </div>
        <button className="btn btn-outline-dark btn-sm rounded-pill d-flex align-items-center gap-1" onClick={fetchOrders}>
          <i className="bi bi-arrow-clockwise" />
          Refresh
        </button>
      </div>

      {errorMsg && (
        <div className="alert alert-danger d-flex align-items-center gap-2 mb-4" role="alert">
          <i className="bi bi-exclamation-triangle-fill" />
          <div>{errorMsg}</div>
        </div>
      )}

      {/* Tabs */}
      <div className="d-flex gap-2 overflow-auto pb-2 mb-4" style={{ scrollbarWidth: 'thin' }}>
        {tabs.map((tab) => (
          <button
            key={tab.key}
            className={`btn btn-sm rounded-pill px-3 text-nowrap d-flex align-items-center gap-2 ${
              activeTab === tab.key ? 'btn-dark' : 'btn-light border'
            }`}
            onClick={() => setActiveTab(tab.key)}
          >
            <i className={`bi ${tab.icon}`} />
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Order List */}
      {loading ? (
        <div className="vstack gap-3">
          <Skeleton height="140px" />
          <Skeleton height="140px" />
          <Skeleton height="140px" />
        </div>
      ) : orders.length === 0 ? (
        <EmptyState
          icon="bi-inbox"
          title={`No ${activeTab === 'ALL' ? '' : activeTab.replace('_', ' ')} Orders`}
          description="There are currently no orders in this fulfillment queue."
        />
      ) : (
        <div className="vstack gap-3">
          {orders.map((order) => {
            const isActing = actionLoadingId === order.id;

            return (
              <div
                key={order.id}
                className="card border shadow-sm p-4"
                style={{ borderRadius: 'var(--radius-md)' }}
              >
                <div className="d-flex flex-wrap align-items-center justify-content-between border-bottom pb-3 mb-3 gap-2">
                  <div>
                    <span className="text-muted small d-block">Order #{order.orderNumber}</span>
                    <span className="text-muted small">
                      Received at {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <div className="d-flex align-items-center gap-3">
                    <StatusBadge status={order.orderStatus} />
                    <span className="fw-bolder text-dark fs-5">₹{Number(order.total).toFixed(2)}</span>
                  </div>
                </div>

                <div className="row g-4 mb-3">
                  {/* Delivery Address Snapshot */}
                  <div className="col-md-5">
                    <div className="small text-muted mb-1">
                      <i className="bi bi-geo-alt-fill text-danger me-1" />
                      Delivery Destination:
                    </div>
                    <p className="text-dark small mb-0">{order.addressSnapshot}</p>
                  </div>

                  {/* Items Ordered */}
                  <div className="col-md-7">
                    <div className="small text-muted mb-1">
                      <i className="bi bi-basket me-1" />
                      Items to Pack ({order.items?.length || 0}):
                    </div>
                    <div className="bg-light p-2 rounded small border">
                      {order.items?.map((item, idx) => (
                        <div key={item.id || idx} className="d-flex justify-content-between py-1 border-bottom last-border-none">
                          <span className="text-dark fw-medium">
                            {item.quantity}&times; {item.productName} ({item.unit})
                          </span>
                          <span className="text-muted">₹{Number(item.lineTotal).toFixed(2)}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Fulfillment Actions */}
                <div className="d-flex flex-wrap align-items-center justify-content-end gap-2 pt-3 border-top">
                  {order.orderStatus === 'VENDOR_PENDING' && (
                    <>
                      <button
                        className="btn btn-outline-danger btn-sm px-3"
                        disabled={isActing}
                        onClick={() => setRejectModalOrder(order)}
                      >
                        Reject
                      </button>
                      <button
                        className="btn btn-primary btn-sm px-4 fw-semibold d-flex align-items-center gap-1"
                        disabled={isActing}
                        onClick={() => handleAcceptOrder(order.id)}
                      >
                        {isActing ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check-lg" />}
                        Accept Order
                      </button>
                    </>
                  )}

                  {order.orderStatus === 'ACCEPTED' && (
                    <>
                      <button
                        className="btn btn-outline-danger btn-sm px-3"
                        disabled={isActing}
                        onClick={() => setRejectModalOrder(order)}
                      >
                        Reject
                      </button>
                      <button
                        className="btn btn-warning text-dark btn-sm px-4 fw-semibold d-flex align-items-center gap-1"
                        disabled={isActing}
                        onClick={() => handleStartPreparing(order.id)}
                      >
                        {isActing ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-box-seam" />}
                        Start Preparing
                      </button>
                    </>
                  )}

                  {order.orderStatus === 'PREPARING' && (
                    <button
                      className="btn btn-success btn-sm px-4 fw-semibold d-flex align-items-center gap-1"
                      disabled={isActing}
                      onClick={() => handleMarkReady(order.id)}
                    >
                      {isActing ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-bag-check-fill" />}
                      Mark Ready for Pickup
                    </button>
                  )}

                  {order.orderStatus === 'READY' && (
                    <div className="badge bg-info bg-opacity-10 text-info border border-info px-3 py-2">
                      <i className="bi bi-bicycle me-1" />
                      Ready & Waiting for Delivery Partner Pickup
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Reject Order Modal */}
      {rejectModalOrder && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1070 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold text-dark">Reject Order #{rejectModalOrder.orderNumber}</h5>
                <button type="button" className="btn-close" onClick={() => setRejectModalOrder(null)} />
              </div>
              <div className="modal-body p-4">
                <p className="text-muted small mb-3">
                  Rejecting this order will automatically restore reserved product inventory back to your active stock
                  and notify the customer.
                </p>
                <label className="form-label small fw-semibold">Reason for Rejection</label>
                <select
                  className="form-select form-select-sm mb-3"
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                >
                  <option value="Store kitchen / fulfillment at capacity">Store kitchen / fulfillment at capacity</option>
                  <option value="Out of stock ingredient / item damaged">Out of stock ingredient / item damaged</option>
                  <option value="Store closing early for emergency">Store closing early for emergency</option>
                  <option value="Delivery address out of serviceable reach">Delivery address out of serviceable reach</option>
                </select>
              </div>
              <div className="modal-footer border-top">
                <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setRejectModalOrder(null)}>
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn btn-danger btn-sm px-4"
                  onClick={handleConfirmReject}
                >
                  Confirm Rejection & Restore Stock
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
