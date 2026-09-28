import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';

export default function OrdersPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    fetchOrders(page);
  }, [page]);

  const fetchOrders = async (pageNum) => {
    setLoading(true);
    try {
      const res = await apiClient.get(`/orders?page=${pageNum}&size=10`);
      setOrders(res?.content || []);
      setTotalPages(res?.totalPages || 1);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading && orders.length === 0) {
    return (
      <div className="container-xl py-4">
        <Skeleton width="200px" height="32px" className="mb-4" />
        <div className="vstack gap-3">
          <Skeleton height="120px" />
          <Skeleton height="120px" />
          <Skeleton height="120px" />
        </div>
      </div>
    );
  }

  if (!loading && orders.length === 0) {
    return (
      <div className="container-xl py-5 text-center">
        <EmptyState
          icon="bi-receipt"
          title="No Orders Yet"
          description="You haven't placed any orders yet. Discover delicious food and essentials from neighborhood vendors."
          actionLabel="Start Shopping"
          onAction={() => window.location.href = '/'}
        />
      </div>
    );
  }

  return (
    <div className="container-xl py-4">
      <h2 className="fw-bolder text-dark mb-4" style={{ letterSpacing: '-0.02em' }}>
        My Orders
      </h2>

      <div className="vstack gap-3">
        {orders.map((order) => (
          <div
            key={order.id}
            className="card border shadow-sm p-4"
            style={{ borderRadius: 'var(--radius-md)' }}
          >
            <div className="d-flex flex-wrap align-items-center justify-content-between border-bottom pb-3 mb-3 gap-2">
              <div>
                <span className="text-muted small d-block">Order #{order.orderNumber}</span>
                <h5 className="fw-bold text-dark mb-0">{order.vendorName}</h5>
                <span className="text-muted small">
                  Placed on {new Date(order.createdAt).toLocaleDateString()} at{' '}
                  {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>
              <div className="text-end">
                <StatusBadge status={order.orderStatus} />
                <div className="fw-bolder text-dark fs-5 mt-1">₹{Number(order.total).toFixed(2)}</div>
              </div>
            </div>

            <div className="row align-items-center g-3">
              <div className="col">
                <div className="small text-muted mb-1">Items ({order.items?.length || 0}):</div>
                <div className="small text-dark text-truncate" style={{ maxWidth: '600px' }}>
                  {order.items?.map((i) => `${i.productName} (${i.quantity})`).join(', ')}
                </div>
              </div>
              <div className="col-auto">
                <Link
                  to={`/orders/${order.id}`}
                  className="btn btn-outline-dark btn-sm rounded-pill px-4 fw-semibold d-flex align-items-center gap-2"
                >
                  <span>Track Order</span>
                  <i className="bi bi-arrow-right" />
                </Link>
              </div>
            </div>
          </div>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="d-flex justify-content-center gap-2 mt-4">
          <button
            className="btn btn-outline-secondary btn-sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </button>
          <span className="align-self-center small text-muted">
            Page {page + 1} of {totalPages}
          </span>
          <button
            className="btn btn-outline-secondary btn-sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
