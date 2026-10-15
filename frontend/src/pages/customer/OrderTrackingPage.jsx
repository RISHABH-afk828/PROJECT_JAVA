import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import Skeleton from '../../components/common/Skeleton';
import StatusBadge from '../../components/common/StatusBadge';

export default function OrderTrackingPage() {
  const { id } = useParams();
  const location = useLocation();
  const justPlaced = location.state?.justPlaced;

  const [tracking, setTracking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);
  const [retryingPayment, setRetryingPayment] = useState(false);
  const [retryError, setRetryError] = useState(null);

  useEffect(() => {
    fetchTracking();
    const interval = setInterval(fetchTracking, 10000); // 10s polling for live tracking
    return () => clearInterval(interval);
  }, [id]);

  const fetchTracking = async () => {
    try {
      const data = await apiClient.get(`/orders/${id}/tracking`);
      setTracking(data);
    } catch (err) {
      setErrorMsg(err.message || 'Could not load order tracking details.');
    } finally {
      setLoading(false);
    }
  };

  const loadRazorpayScript = () => {
    return new Promise((resolve) => {
      if (window.Razorpay) {
        resolve(true);
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  };

  const handleRetryPayment = async () => {
    if (!order) return;
    setRetryingPayment(true);
    setRetryError(null);
    try {
      const loaded = await loadRazorpayScript();
      if (!loaded) {
        throw new Error('Could not load Razorpay payment gateway. Please check internet connection.');
      }

      const pOrder = await apiClient.post('/payments/razorpay/create-order', { orderId: order.id });

      const options = {
        key: pOrder.keyId,
        amount: pOrder.amountInPaise,
        currency: pOrder.currency || 'INR',
        name: 'Hyperlocal Marketplace',
        description: `Order #${pOrder.orderNumber || order.orderNumber}`,
        order_id: pOrder.providerOrderId,
        handler: async (response) => {
          try {
            await apiClient.post('/payments/razorpay/verify', {
              orderId: order.id,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            fetchTracking();
          } catch (verifyErr) {
            setRetryError(verifyErr.message || 'Payment signature verification failed.');
          } finally {
            setRetryingPayment(false);
          }
        },
        modal: {
          ondismiss: () => {
            setRetryingPayment(false);
          },
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', (failResp) => {
        setRetryingPayment(false);
        const reason = failResp.error?.description || 'Payment failed at gateway.';
        setRetryError(`Payment failed: ${reason}`);
      });
      rzp.open();
    } catch (err) {
      setRetryError(err.message || 'Failed to initiate payment.');
      setRetryingPayment(false);
    }
  };

  if (loading && !tracking) {
    return (
      <div className="container-xl py-4">
        <Skeleton width="200px" height="32px" className="mb-4" />
        <Skeleton height="200px" className="mb-4" />
        <Skeleton height="300px" />
      </div>
    );
  }

  if (errorMsg && !tracking) {
    return (
      <div className="container-xl py-5 text-center">
        <div className="alert alert-danger mx-auto" style={{ maxWidth: '500px' }}>
          {errorMsg}
        </div>
        <Link to="/orders" className="btn btn-outline-dark btn-sm rounded-pill mt-3">
          Back to Orders
        </Link>
      </div>
    );
  }

  const order = tracking?.order;
  const history = tracking?.history || [];

  // Order progression stages
  const stages = [
    { key: 'PAYMENT_PENDING', label: 'Order Placed', icon: 'bi-receipt' },
    { key: 'ACCEPTED', label: 'Store Accepted', icon: 'bi-shop' },
    { key: 'PREPARING', label: 'Preparing', icon: 'bi-box-seam' },
    { key: 'READY', label: 'Ready for Pickup', icon: 'bi-bag-check' },
    { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: 'bi-bicycle' },
    { key: 'DELIVERED', label: 'Delivered', icon: 'bi-check-circle-fill' },
  ];

  const getStageIndex = (status) => {
    switch (status) {
      case 'PAYMENT_PENDING':
      case 'CREATED':
        return 0;
      case 'PAID':
      case 'VENDOR_PENDING':
      case 'ACCEPTED':
        return 1;
      case 'PREPARING':
        return 2;
      case 'READY':
      case 'DELIVERY_ASSIGNED':
      case 'PICKED_UP':
        return 3;
      case 'OUT_FOR_DELIVERY':
        return 4;
      case 'DELIVERED':
        return 5;
      default:
        return 0;
    }
  };

  const currentStageIdx = getStageIndex(order?.orderStatus);
  const isCancelled = ['REJECTED', 'CANCELLED', 'PAYMENT_FAILED', 'DELIVERY_FAILED'].includes(order?.orderStatus);

  return (
    <div className="container-xl py-4">
      {justPlaced && (
        <div className="alert alert-success d-flex align-items-center gap-2 mb-4" role="alert">
          <i className="bi bi-check-circle-fill fs-5" />
          <div>
            <strong>Order successfully placed!</strong> Store is reviewing your order items.
          </div>
        </div>
      )}

      {/* Navigation */}
      <Link to="/orders" className="text-decoration-none text-muted small d-inline-flex align-items-center gap-1 mb-3">
        <i className="bi bi-arrow-left" />
        Back to all orders
      </Link>

      {/* Order Banner */}
      <div className="card border shadow-sm p-4 mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div>
            <span className="badge bg-light text-muted border mb-1">
              Live Order Tracking
            </span>
            <div className="d-flex align-items-center gap-2 mb-1">
              <h3 className="fw-bolder text-dark mb-0" style={{ letterSpacing: '-0.02em' }}>
                Order #{order?.orderNumber}
              </h3>
              {order?.paymentMethod === 'COD' ? (
                <span className="badge bg-secondary bg-opacity-10 text-secondary border border-secondary small">
                  <i className="bi bi-cash me-1" />Cash on Delivery
                </span>
              ) : (
                <span className="badge bg-info bg-opacity-10 text-info-emphasis border border-info small">
                  <i className="bi bi-credit-card me-1" />Online (Razorpay)
                </span>
              )}
            </div>
            <p className="text-muted small mb-0">
              Fulfilled by <strong>{order?.vendorName}</strong> &bull; Placed at{' '}
              {order?.createdAt && new Date(order.createdAt).toLocaleString()}
            </p>
          </div>
          <div className="text-end">
            <StatusBadge status={order?.orderStatus} />
            <div className="fw-bolder text-dark fs-4 mt-1">₹{Number(order?.total || 0).toFixed(2)}</div>
          </div>
        </div>
      </div>

      {/* Progress Timeline Stepper */}
      {!isCancelled && (
        <div className="card border shadow-sm p-4 mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
          <h5 className="fw-bold text-dark mb-4">Fulfillment Status</h5>
          <div className="row g-2 text-center position-relative">
            {stages.map((stage, idx) => {
              const isCompleted = idx < currentStageIdx;
              const isCurrent = idx === currentStageIdx;

              return (
                <div key={stage.key} className="col-4 col-md-2">
                  <div className="d-flex flex-column align-items-center">
                    <div
                      className={`rounded-circle d-flex align-items-center justify-content-center mb-2 shadow-sm ${
                        isCurrent
                          ? 'bg-primary text-white border-primary'
                          : isCompleted
                          ? 'bg-success text-white'
                          : 'bg-light text-muted border'
                      }`}
                      style={{ width: '44px', height: '44px' }}
                    >
                      <i className={`bi ${stage.icon} fs-5`} />
                    </div>
                    <span
                      className={`small ${
                        isCurrent ? 'fw-bold text-primary' : isCompleted ? 'fw-semibold text-dark' : 'text-muted'
                      }`}
                      style={{ fontSize: '0.75rem' }}
                    >
                      {stage.label}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Details Row */}
      <div className="row g-4">
        {/* Left Column: Items & History */}
        <div className="col-lg-7">
          {/* Order Items */}
          <div className="card border shadow-sm p-4 mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
            <h5 className="fw-bold text-dark mb-3">Items in this Order</h5>
            <div className="list-group list-group-flush border rounded">
              {order?.items?.map((item) => (
                <div key={item.id} className="list-group-item d-flex justify-content-between align-items-center py-3">
                  <div>
                    <h6 className="fw-semibold text-dark mb-0">{item.productName}</h6>
                    <span className="text-muted small">
                      {item.unit} &bull; ₹{Number(item.unitPrice).toFixed(2)} &times; {item.quantity}
                    </span>
                  </div>
                  <span className="fw-bold text-dark">
                    ₹{Number(item.lineTotal).toFixed(2)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Status Timeline History */}
          <div className="card border shadow-sm p-4" style={{ borderRadius: 'var(--radius-md)' }}>
            <h5 className="fw-bold text-dark mb-3">Order History Log</h5>
            {history.length === 0 ? (
              <p className="text-muted small mb-0">No status changes recorded yet.</p>
            ) : (
              <div className="vstack gap-3 position-relative ps-3 border-start">
                {history.map((h, i) => (
                  <div key={h.id || i} className="position-relative">
                    <div
                      className="position-absolute rounded-circle bg-primary"
                      style={{
                        width: '10px',
                        height: '10px',
                        left: '-21px',
                        top: '4px',
                      }}
                    />
                    <div className="d-flex align-items-center justify-content-between">
                      <span className="fw-semibold text-dark small">{h.toStatus}</span>
                      <span className="text-muted" style={{ fontSize: '0.75rem' }}>
                        {new Date(h.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    {h.reason && <p className="text-muted small mb-0 mt-1">{h.reason}</p>}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Address & Bill Summary */}
        <div className="col-lg-5">
          {/* Delivery Address */}
          <div className="card border shadow-sm p-4 mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
            <h5 className="fw-bold text-dark mb-2">Delivery Address</h5>
            <div className="d-flex align-items-start gap-2 text-muted small">
              <i className="bi bi-geo-alt-fill text-danger mt-1" />
              <div>{order?.addressSnapshot}</div>
            </div>
          </div>

          {/* Payment & Bill Summary */}
          <div className="card border shadow-sm p-4" style={{ borderRadius: 'var(--radius-md)' }}>
            <h5 className="fw-bold text-dark mb-3">Receipt Summary</h5>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted small">Subtotal</span>
              <span className="fw-semibold text-dark small">₹{Number(order?.subtotal || 0).toFixed(2)}</span>
            </div>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted small">Delivery Fee</span>
              <span className="fw-semibold text-dark small">₹{Number(order?.deliveryFee || 0).toFixed(2)}</span>
            </div>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted small">Taxes (GST 5%)</span>
              <span className="fw-semibold text-dark small">₹{Number(order?.tax || 0).toFixed(2)}</span>
            </div>

            {order?.discount > 0 && (
              <div className="d-flex justify-content-between mb-2 text-success small">
                <span>Discount</span>
                <span className="fw-semibold">-₹{Number(order.discount).toFixed(2)}</span>
              </div>
            )}

            <hr className="my-2" />

            <div className="d-flex justify-content-between align-items-center mb-3">
              <span className="fw-bold text-dark">Total Paid / Due</span>
              <span className="fw-bolder text-dark fs-5">
                ₹{Number(order?.total || 0).toFixed(2)}
              </span>
            </div>

            <div className="d-flex justify-content-between align-items-center bg-light p-2 rounded small mb-2">
              <span className="text-muted">Payment Method:</span>
              <span className="fw-semibold text-dark">
                {order?.paymentMethod === 'COD' ? (
                  <span><i className="bi bi-cash me-1" />Cash on Delivery</span>
                ) : (
                  <span><i className="bi bi-credit-card me-1" />Online (Razorpay)</span>
                )}
              </span>
            </div>

            <div className="d-flex justify-content-between align-items-center bg-light p-2 rounded small">
              <span className="text-muted">Payment Status:</span>
              <StatusBadge status={order?.paymentStatus || 'CREATED'} />
            </div>

            {order?.paymentMethod === 'COD' ? (
              <div className="mt-3 pt-3 border-top">
                <div className="alert alert-secondary py-2 px-3 small mb-0 d-flex align-items-center gap-2">
                  <i className="bi bi-cash-coin fs-5 text-success flex-shrink-0" />
                  <div>
                    <strong>Cash on Delivery:</strong> Pay ₹{Number(order?.total || 0).toFixed(2)} with cash or UPI QR code to the delivery partner upon doorstep delivery.
                  </div>
                </div>
              </div>
            ) : order?.paymentStatus !== 'CAPTURED' && !isCancelled && (
              <div className="mt-3 pt-3 border-top">
                {retryError && (
                  <div className="alert alert-danger py-2 px-3 small mb-2">
                    {retryError}
                  </div>
                )}
                <button
                  className="btn btn-success w-100 py-2 fw-semibold rounded-pill d-flex align-items-center justify-content-center gap-2"
                  disabled={retryingPayment}
                  onClick={handleRetryPayment}
                >
                  <i className="bi bi-credit-card" />
                  {retryingPayment ? 'Processing...' : 'Complete Payment Now'}
                </button>
                <div className="text-muted text-center mt-1" style={{ fontSize: '0.75rem' }}>
                  Secure payment via Razorpay Test Mode
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
