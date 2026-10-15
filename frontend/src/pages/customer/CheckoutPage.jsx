import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useOutletContext } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import { useAuth } from '../../store/AuthContext';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';

export default function CheckoutPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { setCartCount } = useOutletContext() || {};

  const [step, setStep] = useState(1); // 1: Address, 2: Review & Quote, 3: Payment
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [loadingAddresses, setLoadingAddresses] = useState(true);

  // Address creation form modal state
  const initialAddressState = {
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
  };
  const [showAddressModal, setShowAddressModal] = useState(false);
  const [newAddress, setNewAddress] = useState(initialAddressState);
  const [addressModalError, setAddressModalError] = useState(null);
  const [addressGeoLoading, setAddressGeoLoading] = useState(false);
  const [addressSearchQuery, setAddressSearchQuery] = useState('');
  const [addressSearchResults, setAddressSearchResults] = useState([]);

  // Cart & Quote state
  const [cart, setCart] = useState(null);
  const [quote, setQuote] = useState(null);
  const [loadingQuote, setLoadingQuote] = useState(false);

  // Placing order state
  const [placingOrder, setPlacingOrder] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('RAZORPAY');
  const [errorMessage, setErrorMessage] = useState(null);

  useEffect(() => {
    fetchAddresses();
    fetchCart();
  }, []);

  const fetchAddresses = async () => {
    setLoadingAddresses(true);
    try {
      const data = await apiClient.get('/users/me/addresses');
      setAddresses(data || []);
      // Auto-select default or first address
      const def = data.find((a) => a.isDefault) || data[0];
      if (def) setSelectedAddressId(def.id);
    } catch {
      setAddresses([]);
    } finally {
      setLoadingAddresses(false);
    }
  };

  const fetchCart = async () => {
    try {
      const data = await apiClient.get('/cart');
      setCart(data);
      if (!data || !data.items || data.items.length === 0) {
        // empty cart
      }
    } catch {
      setCart(null);
    }
  };

  const handleUseCurrentLocationInModal = () => {
    if (!navigator.geolocation) {
      setAddressModalError('Geolocation is not supported by your browser.');
      return;
    }
    setAddressGeoLoading(true);
    setAddressModalError(null);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const lat = pos.coords.latitude;
          const lng = pos.coords.longitude;
          const res = await apiClient.get(`/locations/reverse-geocode?lat=${lat}&lng=${lng}`);
          setNewAddress((prev) => ({
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
          setNewAddress((prev) => ({
            ...prev,
            latitude: Number(pos.coords.latitude.toFixed(6)),
            longitude: Number(pos.coords.longitude.toFixed(6)),
          }));
        } finally {
          setAddressGeoLoading(false);
        }
      },
      (err) => {
        setAddressGeoLoading(false);
        if (err.code === 1) {
          setAddressModalError('Location permission denied. Please search your locality or address manually.');
        } else {
          setAddressModalError('Could not detect current location. Please search manually.');
        }
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  };

  const handleSearchAddressInModal = async (query) => {
    setAddressSearchQuery(query);
    if (!query.trim()) {
      setAddressSearchResults([]);
      return;
    }
    try {
      const res = await apiClient.get(`/locations/search?q=${encodeURIComponent(query.trim())}`);
      setAddressSearchResults(res || []);
    } catch {
      setAddressSearchResults([]);
    }
  };

  const handleSelectSearchResultInModal = (res) => {
    setNewAddress((prev) => ({
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
    setAddressSearchQuery('');
    setAddressSearchResults([]);
  };

  const handleCreateAddress = async (e) => {
    e.preventDefault();
    if (newAddress.latitude == null || newAddress.longitude == null || isNaN(newAddress.latitude) || isNaN(newAddress.longitude)) {
      setAddressModalError('Please use GPS or Search to set delivery coordinates for this address.');
      return;
    }
    if (!newAddress.city.trim() || !newAddress.state.trim() || !newAddress.postalCode.trim() || !newAddress.house.trim() || !newAddress.street.trim() || !newAddress.locality.trim()) {
      setAddressModalError('Please fill in all required address fields.');
      return;
    }
    try {
      const created = await apiClient.post('/users/me/addresses', {
        ...newAddress,
        house: newAddress.house.trim(),
        street: newAddress.street.trim(),
        locality: newAddress.locality.trim(),
        city: newAddress.city.trim(),
        state: newAddress.state.trim(),
        postalCode: newAddress.postalCode.trim(),
        deliveryInstructions: newAddress.deliveryInstructions ? newAddress.deliveryInstructions.trim() : '',
      });
      setAddresses((prev) => [created, ...prev]);
      setSelectedAddressId(created.id);
      setShowAddressModal(false);
      setNewAddress(initialAddressState);
      setAddressSearchQuery('');
      setAddressSearchResults([]);
      setAddressModalError(null);
    } catch (err) {
      setAddressModalError(err.message || 'Failed to save address');
    }
  };

  const fetchQuoteForAddress = async (addressId) => {
    setLoadingQuote(true);
    setErrorMessage(null);
    try {
      const quoteData = await apiClient.post('/checkout/quote', { addressId });
      setQuote(quoteData);
      setStep(2);
    } catch (err) {
      setErrorMessage(err.message || 'Serviceability check failed for this address.');
    } finally {
      setLoadingQuote(false);
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

  const handlePlaceOrder = async () => {
    setPlacingOrder(true);
    setErrorMessage(null);
    try {
      if (paymentMethod === 'RAZORPAY') {
        const loaded = await loadRazorpayScript();
        if (!loaded) {
          throw new Error('Could not load Razorpay payment gateway. Please check your internet connection.');
        }

        // 1. Create Order
        const order = await apiClient.post('/checkout/create', {
          addressId: selectedAddressId,
          paymentMethod: 'RAZORPAY',
        });
        if (setCartCount) setCartCount(0);
        localStorage.removeItem('cart');

        // 2. Initiate Gateway Order
        const pOrder = await apiClient.post('/payments/razorpay/create-order', { orderId: order.id });

        // 3. Open Real Razorpay Checkout Modal
        const options = {
          key: pOrder.keyId,
          amount: pOrder.amountInPaise,
          currency: pOrder.currency || 'INR',
          name: 'Hyperlocal Marketplace',
          description: `Order #${pOrder.orderNumber || order.orderNumber}`,
          order_id: pOrder.providerOrderId,
          prefill: {
            name: user?.fullName || user?.name || '',
            email: user?.email || '',
            contact: user?.phoneNumber || user?.phone || '',
          },
          theme: {
            color: '#198754',
          },
          handler: async (response) => {
            try {
              setPlacingOrder(true);
              // Server-side strict HMAC-SHA256 signature verification
              await apiClient.post('/payments/razorpay/verify', {
                orderId: order.id,
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              });

              // Navigate to live tracking upon successful verification
              navigate(`/orders/${order.id}`, { state: { justPlaced: true, orderNumber: order.orderNumber } });
            } catch (verifyErr) {
              setErrorMessage(verifyErr.message || 'Payment signature verification failed. Please check order status.');
              setPlacingOrder(false);
            }
          },
          modal: {
            ondismiss: () => {
              setPlacingOrder(false);
              navigate(`/orders/${order.id}`, { state: { orderNumber: order.orderNumber } });
            },
          },
        };

        const rzp = new window.Razorpay(options);
        rzp.on('payment.failed', (failResp) => {
          setPlacingOrder(false);
          navigate(`/orders/${order.id}`, { state: { orderNumber: order.orderNumber } });
        });
        rzp.open();
      } else {
        // COD order
        const order = await apiClient.post('/checkout/create', {
          addressId: selectedAddressId,
          paymentMethod: 'COD',
        });
        if (setCartCount) setCartCount(0);
        localStorage.removeItem('cart');
        navigate(`/orders/${order.id}`, { state: { justPlaced: true, orderNumber: order.orderNumber } });
        setPlacingOrder(false);
      }
    } catch (err) {
      setErrorMessage(err.message || 'Failed to place order. Please try again.');
      setPlacingOrder(false);
    }
  };

  if (cart && (!cart.items || cart.items.length === 0)) {
    return (
      <div className="container py-5 text-center">
        <EmptyState
          icon="bi-bag-x"
          title="Your Cart is Empty"
          description="You don't have any items in your cart to checkout."
          actionLabel="Browse Stores"
          onAction={() => navigate('/')}
        />
      </div>
    );
  }

  const selectedAddr = addresses.find((a) => a.id === selectedAddressId);

  return (
    <div className="container-xl py-4">
      {/* Checkout Header & Stepper */}
      <div className="mb-4">
        <Link to="/cart" className="text-decoration-none text-muted small d-inline-flex align-items-center gap-1 mb-2">
          <i className="bi bi-arrow-left" />
          Back to cart
        </Link>
        <h2 className="fw-bolder text-dark mb-3" style={{ letterSpacing: '-0.02em' }}>
          Checkout
        </h2>

        {/* Stepper tabs */}
        <div className="d-flex align-items-center gap-2 border-bottom pb-3">
          <button
            className={`btn btn-sm rounded-pill px-3 fw-semibold ${step === 1 ? 'btn-dark' : 'btn-light border'}`}
            onClick={() => setStep(1)}
          >
            1. Delivery Address
          </button>
          <i className="bi bi-chevron-right text-muted small" />
          <button
            className={`btn btn-sm rounded-pill px-3 fw-semibold ${step === 2 ? 'btn-dark' : 'btn-light border'}`}
            disabled={!selectedAddressId}
            onClick={() => {
              if (selectedAddressId) fetchQuoteForAddress(selectedAddressId);
            }}
          >
            2. Review & Delivery Quote
          </button>
          <i className="bi bi-chevron-right text-muted small" />
          <button
            className={`btn btn-sm rounded-pill px-3 fw-semibold ${step === 3 ? 'btn-dark' : 'btn-light border'}`}
            disabled={!quote}
            onClick={() => setStep(3)}
          >
            3. Payment
          </button>
        </div>
      </div>

      {errorMessage && (
        <div className="alert alert-danger d-flex align-items-center gap-2 mb-4" role="alert">
          <i className="bi bi-exclamation-triangle-fill flex-shrink-0" />
          <div>{errorMessage}</div>
        </div>
      )}

      <div className="row g-4">
        {/* Step Content */}
        <div className="col-lg-8">
          {/* STEP 1: ADDRESS SELECTION */}
          {step === 1 && (
            <div className="card border shadow-sm p-4" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="d-flex align-items-center justify-content-between mb-3">
                <h5 className="fw-bold text-dark mb-0">Select Delivery Address</h5>
                <button
                  className="btn btn-outline-dark btn-sm d-flex align-items-center gap-1 rounded-pill"
                  onClick={() => setShowAddressModal(true)}
                >
                  <i className="bi bi-plus-lg" />
                  Add New Address
                </button>
              </div>

              {loadingAddresses ? (
                <div className="vstack gap-3">
                  <Skeleton height="80px" />
                  <Skeleton height="80px" />
                </div>
              ) : addresses.length === 0 ? (
                <div className="text-center py-4 border rounded bg-light">
                  <i className="bi bi-geo-alt fs-2 text-muted mb-2 d-block" />
                  <p className="text-muted mb-2">No saved addresses found.</p>
                  <button
                    className="btn btn-primary btn-sm rounded-pill px-4"
                    onClick={() => setShowAddressModal(true)}
                  >
                    Add Your First Address
                  </button>
                </div>
              ) : (
                <div className="vstack gap-3">
                  {addresses.map((addr) => {
                    const isSelected = addr.id === selectedAddressId;
                    return (
                      <div
                        key={addr.id}
                        className={`card p-3 border cursor-pointer transition ${
                          isSelected ? 'border-primary bg-primary bg-opacity-10' : 'hover-shadow'
                        }`}
                        style={{ cursor: 'pointer', borderRadius: 'var(--radius-md)' }}
                        onClick={() => setSelectedAddressId(addr.id)}
                      >
                        <div className="d-flex align-items-start justify-content-between">
                          <div className="d-flex align-items-start gap-2">
                            <input
                              type="radio"
                              name="selectedAddress"
                              className="form-check-input mt-1"
                              checked={isSelected}
                              onChange={() => setSelectedAddressId(addr.id)}
                            />
                            <div>
                              <div className="d-flex align-items-center gap-2 mb-1">
                                <span className="fw-bold text-dark">{addr.label || 'Address'}</span>
                                {addr.isDefault && (
                                  <span className="badge bg-secondary" style={{ fontSize: '0.65rem' }}>Default</span>
                                )}
                              </div>
                              <p className="text-muted small mb-0">
                                {addr.house}, {addr.street}, {addr.locality}, {addr.city}, {addr.state} - {addr.postalCode}
                              </p>
                              {addr.deliveryInstructions && (
                                <p className="text-dark small mt-1 mb-0 fst-italic">
                                  Instructions: "{addr.deliveryInstructions}"
                                </p>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              <div className="mt-4 pt-3 border-top d-flex justify-content-end">
                <button
                  className="btn btn-primary px-4 fw-semibold"
                  disabled={!selectedAddressId || loadingQuote}
                  onClick={() => fetchQuoteForAddress(selectedAddressId)}
                >
                  {loadingQuote ? 'Verifying Serviceability...' : 'Continue to Order Review'}
                </button>
              </div>
            </div>
          )}

          {/* STEP 2: REVIEW & AUTHORITATIVE QUOTE */}
          {step === 2 && (
            <div className="card border shadow-sm p-4" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="d-flex align-items-center justify-content-between mb-3">
                <h5 className="fw-bold text-dark mb-0">Review Delivery & Items</h5>
                <button
                  className="btn btn-link text-decoration-none btn-sm p-0"
                  onClick={() => setStep(1)}
                >
                  Change Address
                </button>
              </div>

              {/* Delivery Address Snapshot */}
              <div className="p-3 bg-light rounded border mb-4">
                <div className="small text-muted mb-1">Delivering to:</div>
                <div className="fw-semibold text-dark mb-1">
                  {selectedAddr?.house}, {selectedAddr?.street}, {selectedAddr?.locality}
                </div>
                <div className="small text-muted">
                  {selectedAddr?.city}, {selectedAddr?.state} - {selectedAddr?.postalCode}
                </div>
                {quote?.distanceKm !== undefined && (
                  <div className="badge bg-success bg-opacity-10 text-success border border-success mt-2">
                    <i className="bi bi-geo-alt-fill me-1" />
                    Distance from store: {quote.distanceKm} km (Serviceable)
                  </div>
                )}
              </div>

              {/* Items Summary */}
              <h6 className="fw-bold text-dark mb-2">Order Items from {cart?.vendorName}</h6>
              <div className="list-group list-group-flush border rounded mb-4">
                {cart?.items?.map((item) => (
                  <div key={item.id || item.productId} className="list-group-item d-flex justify-content-between align-items-center py-2 px-3">
                    <div>
                      <span className="fw-semibold text-dark">{item.productName || item.name}</span>
                      <span className="text-muted small ms-2">&times; {item.quantity} ({item.unit})</span>
                    </div>
                    <span className="fw-bold text-dark">
                      ₹{Number(item.lineTotal || (item.price * item.quantity)).toFixed(2)}
                    </span>
                  </div>
                ))}
              </div>

              <div className="d-flex justify-content-between pt-3 border-top">
                <button className="btn btn-outline-secondary btn-sm" onClick={() => setStep(1)}>
                  Back
                </button>
                <button className="btn btn-primary px-4 fw-semibold" onClick={() => setStep(3)}>
                  Proceed to Payment
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: PAYMENT METHOD */}
          {step === 3 && (
            <div className="card border shadow-sm p-4" style={{ borderRadius: 'var(--radius-md)' }}>
              <h5 className="fw-bold text-dark mb-3">Choose Payment Method</h5>

              <div className="vstack gap-3 mb-4">
                <label className="card p-3 border cursor-pointer border-primary bg-primary bg-opacity-10" style={{ cursor: 'pointer' }}>
                  <div className="d-flex align-items-center justify-content-between">
                    <div className="d-flex align-items-center gap-3">
                      <input
                        type="radio"
                        name="payment"
                        checked={paymentMethod === 'RAZORPAY'}
                        onChange={() => setPaymentMethod('RAZORPAY')}
                        className="form-check-input"
                      />
                      <div>
                        <div className="fw-bold text-dark">Razorpay Secure Online Payment</div>
                        <div className="text-muted small">UPI (Google Pay, PhonePe, Paytm), Credit/Debit Cards, NetBanking</div>
                      </div>
                    </div>
                    <span className="badge bg-primary">Recommended</span>
                  </div>
                </label>

                <label className="card p-3 border cursor-pointer opacity-75" style={{ cursor: 'pointer' }}>
                  <div className="d-flex align-items-center gap-3">
                    <input
                      type="radio"
                      name="payment"
                      checked={paymentMethod === 'COD'}
                      onChange={() => setPaymentMethod('COD')}
                      className="form-check-input"
                    />
                    <div>
                      <div className="fw-bold text-dark">Cash on Delivery (Pay at Doorstep)</div>
                      <div className="text-muted small">Pay via Cash or QR code on delivery</div>
                    </div>
                  </div>
                </label>
              </div>

              <div className="d-flex justify-content-between pt-3 border-top">
                <button className="btn btn-outline-secondary btn-sm" onClick={() => setStep(2)}>
                  Back
                </button>
                <button
                  className="btn btn-success px-4 py-2 fw-semibold d-flex align-items-center gap-2"
                  disabled={placingOrder}
                  onClick={handlePlaceOrder}
                >
                  {placingOrder ? (
                    <>
                      <span className="spinner-border spinner-border-sm" role="status" />
                      <span>Creating Order...</span>
                    </>
                  ) : (
                    <>
                      <i className="bi bi-lock-fill" />
                      <span>Place Order &bull; ₹{Number(quote?.total || cart?.subtotal || 0).toFixed(2)}</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Right Authoritative Bill Sidebar */}
        <div className="col-lg-4">
          <div className="card border shadow-sm p-4 sticky-top" style={{ top: '80px', borderRadius: 'var(--radius-md)' }}>
            <h5 className="fw-bold text-dark mb-3">Bill Breakdown</h5>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted">Item Subtotal</span>
              <span className="fw-semibold text-dark">
                ₹{Number(quote?.subtotal || cart?.subtotal || 0).toFixed(2)}
              </span>
            </div>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted">Delivery Fee</span>
              <span className="fw-semibold text-dark">
                {quote ? `₹${Number(quote.deliveryFee).toFixed(2)}` : 'Calculated at step 2'}
              </span>
            </div>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted">Taxes (GST 5%)</span>
              <span className="fw-semibold text-dark">
                {quote ? `₹${Number(quote.tax).toFixed(2)}` : 'Calculated at step 2'}
              </span>
            </div>

            {quote && quote.discount > 0 && (
              <div className="d-flex justify-content-between mb-2 text-success">
                <span>Discount</span>
                <span className="fw-semibold">-₹{Number(quote.discount).toFixed(2)}</span>
              </div>
            )}

            <hr className="my-3" />

            <div className="d-flex justify-content-between align-items-center mb-3">
              <span className="fw-bold text-dark fs-6">To Pay</span>
              <span className="fw-bolder text-dark fs-4">
                ₹{Number(quote?.total || cart?.subtotal || 0).toFixed(2)}
              </span>
            </div>

            <div className="bg-light p-3 rounded small text-muted">
              <div className="d-flex align-items-center gap-2 mb-1">
                <i className="bi bi-clock-history text-primary" />
                <span className="fw-semibold text-dark">Fast Hyperlocal Delivery</span>
              </div>
              <div>Directly fulfilled by {cart?.vendorName || 'local vendor'} to your doorstep.</div>
            </div>
          </div>
        </div>
      </div>

      {/* Add Address Modal */}
      {showAddressModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1070 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
              <form onSubmit={handleCreateAddress}>
                <div className="modal-header border-bottom">
                  <h5 className="modal-title fw-bold text-dark">Add New Address</h5>
                  <button type="button" className="btn-close" onClick={() => setShowAddressModal(false)} />
                </div>
                <div className="modal-body p-4 vstack gap-3">
                  {addressModalError && (
                    <div className="alert alert-danger py-2 px-3 small mb-0">
                      <i className="bi bi-exclamation-circle-fill me-2" />
                      {addressModalError}
                    </div>
                  )}

                  {/* GPS & Autocomplete */}
                  <div>
                    <button
                      type="button"
                      className="btn btn-outline-primary btn-sm w-100 d-flex align-items-center justify-content-center gap-2 py-2 mb-2"
                      disabled={addressGeoLoading}
                      onClick={handleUseCurrentLocationInModal}
                    >
                      <i className="bi bi-crosshair" />
                      <span>{addressGeoLoading ? 'Detecting Location...' : 'Use My Current GPS Location'}</span>
                    </button>

                    <div className="input-group input-group-sm">
                      <span className="input-group-text bg-white">
                        <i className="bi bi-search text-muted" />
                      </span>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Search area, landmark or street to autofill..."
                        value={addressSearchQuery}
                        onChange={(e) => handleSearchAddressInModal(e.target.value)}
                      />
                    </div>
                    {addressSearchResults.length > 0 && (
                      <div className="list-group shadow-sm mt-1 position-relative" style={{ maxHeight: '150px', overflowY: 'auto', zIndex: 10 }}>
                        {addressSearchResults.map((res, idx) => (
                          <button
                            key={idx}
                            type="button"
                            className="list-group-item list-group-item-action text-start py-2 small d-flex align-items-center gap-2"
                            onClick={() => handleSelectSearchResultInModal(res)}
                          >
                            <i className="bi bi-geo-alt text-muted" />
                            <span className="text-truncate">{res.label}</span>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Coordinates Status */}
                  <div>
                    {newAddress.latitude != null && newAddress.longitude != null ? (
                      <span className="badge bg-success-subtle text-success border border-success small">
                        <i className="bi bi-geo-alt-fill me-1" />
                        Coordinates: {newAddress.latitude.toFixed(4)}, {newAddress.longitude.toFixed(4)}
                      </span>
                    ) : (
                      <span className="badge bg-warning-subtle text-warning-emphasis border border-warning small">
                        <i className="bi bi-exclamation-circle me-1" />
                        Coordinates not set — click GPS button or select from search
                      </span>
                    )}
                  </div>

                  <div>
                    <label className="form-label small fw-semibold">Label</label>
                    <select
                      className="form-select form-select-sm"
                      value={newAddress.label}
                      onChange={(e) => setNewAddress({ ...newAddress, label: e.target.value })}
                    >
                      <option value="Home">Home</option>
                      <option value="Work">Work</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                  <div>
                    <label className="form-label small fw-semibold">Flat / House / Floor *</label>
                    <input
                      type="text"
                      className="form-control form-control-sm"
                      required
                      placeholder="e.g. Flat 302, Green Acres"
                      value={newAddress.house}
                      onChange={(e) => setNewAddress({ ...newAddress, house: e.target.value })}
                    />
                  </div>
                  <div>
                    <label className="form-label small fw-semibold">Street / Area *</label>
                    <input
                      type="text"
                      className="form-control form-control-sm"
                      required
                      placeholder="e.g. 80 Feet Road, 4th Block"
                      value={newAddress.street}
                      onChange={(e) => setNewAddress({ ...newAddress, street: e.target.value })}
                    />
                  </div>
                  <div className="row g-2">
                    <div className="col-6">
                      <label className="form-label small fw-semibold">Locality *</label>
                      <input
                        type="text"
                        className="form-control form-control-sm"
                        required
                        placeholder="e.g. Bandra West, Virar East"
                        value={newAddress.locality}
                        onChange={(e) => setNewAddress({ ...newAddress, locality: e.target.value })}
                      />
                    </div>
                    <div className="col-6">
                      <label className="form-label small fw-semibold">Postal Code *</label>
                      <input
                        type="text"
                        className="form-control form-control-sm"
                        required
                        placeholder="e.g. 401305"
                        value={newAddress.postalCode}
                        onChange={(e) => setNewAddress({ ...newAddress, postalCode: e.target.value })}
                      />
                    </div>
                  </div>
                  <div className="row g-2">
                    <div className="col-6">
                      <label className="form-label small fw-semibold">City *</label>
                      <input
                        type="text"
                        className="form-control form-control-sm"
                        required
                        placeholder="e.g. Mumbai, Palghar"
                        value={newAddress.city}
                        onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })}
                      />
                    </div>
                    <div className="col-6">
                      <label className="form-label small fw-semibold">State *</label>
                      <input
                        type="text"
                        className="form-control form-control-sm"
                        required
                        placeholder="e.g. Maharashtra"
                        value={newAddress.state}
                        onChange={(e) => setNewAddress({ ...newAddress, state: e.target.value })}
                      />
                    </div>
                  </div>
                  <div>
                    <label className="form-label small fw-semibold">Delivery Instructions (Optional)</label>
                    <input
                      type="text"
                      className="form-control form-control-sm"
                      placeholder="e.g. Ring bell, leave package at door"
                      value={newAddress.deliveryInstructions}
                      onChange={(e) => setNewAddress({ ...newAddress, deliveryInstructions: e.target.value })}
                    />
                  </div>
                </div>
                <div className="modal-footer border-top">
                  <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setShowAddressModal(false)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn-primary btn-sm px-4">
                    Save Address
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
