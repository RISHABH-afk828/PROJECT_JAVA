import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useOutletContext } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import { useAuth } from '../../store/AuthContext';
import EmptyState from '../../components/common/EmptyState';
import Skeleton from '../../components/common/Skeleton';

export default function CartPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const { setCartCount } = useOutletContext() || {};

  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updatingItemId, setUpdatingItemId] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    fetchCart();
  }, [isAuthenticated]);

  const fetchCart = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      if (isAuthenticated) {
        // Sync local cart items if any were stored offline
        const localCartRaw = localStorage.getItem('cart');
        if (localCartRaw) {
          try {
            const localCart = JSON.parse(localCartRaw);
            if (localCart?.items?.length > 0 && localCart.vendorId) {
              for (const item of localCart.items) {
                try {
                  await apiClient.post('/cart/items', {
                    vendorId: localCart.vendorId,
                    productId: item.productId,
                    quantity: item.quantity,
                  });
                } catch {
                  // ignore conflicts during bulk sync
                }
              }
              localStorage.removeItem('cart');
            }
          } catch {
            // ignore JSON parse error
          }
        }

        const data = await apiClient.get('/cart');
        setCart(data);
        if (setCartCount) {
          setCartCount(data?.totalItems || 0);
        }
      } else {
        // Unauthenticated local storage cart fallback
        const localCartRaw = localStorage.getItem('cart');
        if (localCartRaw) {
          const parsed = JSON.parse(localCartRaw);
          setCart(parsed);
          if (setCartCount) {
            const count = parsed.items?.reduce((acc, i) => acc + i.quantity, 0) || 0;
            setCartCount(count);
          }
        } else {
          setCart({ items: [], subtotal: 0, totalItems: 0 });
        }
      }
    } catch (err) {
      setErrorMsg(err.message || 'Failed to load cart');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateQuantity = async (itemId, newQuantity) => {
    setUpdatingItemId(itemId);
    setErrorMsg(null);
    try {
      if (isAuthenticated) {
        const updated = await apiClient.patch(`/cart/items/${itemId}`, { quantity: newQuantity });
        setCart(updated);
        if (setCartCount) setCartCount(updated.totalItems || 0);
      } else {
        // Update local cart
        const updatedItems = (cart.items || [])
          .map((i) => (i.id === itemId || i.productId === itemId ? { ...i, quantity: newQuantity } : i))
          .filter((i) => i.quantity > 0);

        const subtotal = updatedItems.reduce((acc, i) => acc + i.price * i.quantity, 0);
        const newCart = {
          ...cart,
          vendorId: updatedItems.length > 0 ? cart.vendorId : null,
          vendorName: updatedItems.length > 0 ? cart.vendorName : null,
          items: updatedItems,
          subtotal,
          totalItems: updatedItems.reduce((acc, i) => acc + i.quantity, 0),
        };
        setCart(newCart);
        localStorage.setItem('cart', JSON.stringify(newCart));
        if (setCartCount) setCartCount(newCart.totalItems);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Could not update item quantity');
    } finally {
      setUpdatingItemId(null);
    }
  };

  const handleRemoveItem = async (itemId) => {
    setUpdatingItemId(itemId);
    setErrorMsg(null);
    try {
      if (isAuthenticated) {
        const updated = await apiClient.delete(`/cart/items/${itemId}`);
        setCart(updated);
        if (setCartCount) setCartCount(updated.totalItems || 0);
      } else {
        const updatedItems = (cart.items || []).filter((i) => i.id !== itemId && i.productId !== itemId);
        const subtotal = updatedItems.reduce((acc, i) => acc + i.price * i.quantity, 0);
        const newCart = {
          ...cart,
          vendorId: updatedItems.length > 0 ? cart.vendorId : null,
          vendorName: updatedItems.length > 0 ? cart.vendorName : null,
          items: updatedItems,
          subtotal,
          totalItems: updatedItems.reduce((acc, i) => acc + i.quantity, 0),
        };
        setCart(newCart);
        localStorage.setItem('cart', JSON.stringify(newCart));
        if (setCartCount) setCartCount(newCart.totalItems);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Could not remove item');
    } finally {
      setUpdatingItemId(null);
    }
  };

  const handleClearCart = async () => {
    if (!window.confirm('Are you sure you want to clear your cart?')) return;
    try {
      if (isAuthenticated) {
        const updated = await apiClient.delete('/cart');
        setCart(updated);
        if (setCartCount) setCartCount(0);
      } else {
        const emptyCart = { vendorId: null, vendorName: null, items: [], subtotal: 0, totalItems: 0 };
        setCart(emptyCart);
        localStorage.removeItem('cart');
        if (setCartCount) setCartCount(0);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Could not clear cart');
    }
  };

  const handleProceedToCheckout = () => {
    if (!isAuthenticated) {
      navigate('/login?redirect=/checkout');
    } else {
      navigate('/checkout');
    }
  };

  const items = cart?.items || [];
  const hasOutOfStock = items.some((i) => i.isOutOfStock);

  if (loading) {
    return (
      <div className="container-xl py-5">
        <Skeleton width="200px" height="32px" className="mb-4" />
        <div className="row g-4">
          <div className="col-lg-8">
            <Skeleton height="100px" className="mb-3" />
            <Skeleton height="100px" className="mb-3" />
            <Skeleton height="100px" />
          </div>
          <div className="col-lg-4">
            <Skeleton height="260px" />
          </div>
        </div>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="container-xl py-5 text-center">
        <EmptyState
          icon="bi-bag"
          title="Your Cart is Empty"
          description="Looks like you haven't added any items to your cart yet. Explore fresh groceries and local products from neighborhood stores."
          actionLabel="Browse Stores"
          onAction={() => navigate('/')}
        />
      </div>
    );
  }

  return (
    <div className="container-xl py-4">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="fw-bolder mb-1 text-dark" style={{ letterSpacing: '-0.02em' }}>
            Shopping Cart
          </h2>
          <p className="text-muted small mb-0">
            Ordering from <strong className="text-dark">{cart?.vendorName || 'Neighborhood Store'}</strong>
          </p>
        </div>
        <button
          className="btn btn-outline-danger btn-sm d-flex align-items-center gap-1"
          onClick={handleClearCart}
        >
          <i className="bi bi-trash" />
          Clear Cart
        </button>
      </div>

      {errorMsg && (
        <div className="alert alert-danger d-flex align-items-center gap-2 mb-4" role="alert">
          <i className="bi bi-exclamation-triangle-fill" />
          <div>{errorMsg}</div>
        </div>
      )}

      {hasOutOfStock && (
        <div className="alert alert-warning d-flex align-items-center gap-2 mb-4" role="alert">
          <i className="bi bi-exclamation-circle-fill" />
          <div>Some items in your cart exceed available stock. Please reduce quantity to proceed to checkout.</div>
        </div>
      )}

      <div className="row g-4">
        {/* Cart Items List */}
        <div className="col-lg-8">
          <div className="card border shadow-sm" style={{ borderRadius: 'var(--radius-md)' }}>
            <div className="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
              <span className="fw-bold text-dark">
                {items.length} {items.length === 1 ? 'Item' : 'Items'}
              </span>
              {cart?.vendorId && (
                <Link
                  to={`/vendors/${cart.vendorId}`}
                  className="text-primary text-decoration-none small fw-semibold d-flex align-items-center gap-1"
                >
                  <i className="bi bi-plus" />
                  Add more items from this store
                </Link>
              )}
            </div>

            <div className="list-group list-group-flush">
              {items.map((item) => {
                const itemKey = item.id || item.productId;
                const isUpdating = updatingItemId === itemKey;

                return (
                  <div key={itemKey} className="list-group-item p-3">
                    <div className="row align-items-center g-3">
                      {/* Product Thumbnail */}
                      <div className="col-auto">
                        <div
                          className="bg-light rounded border d-flex align-items-center justify-content-center overflow-hidden"
                          style={{ width: '64px', height: '64px' }}
                        >
                          {item.imageUrl ? (
                            <img
                              src={item.imageUrl}
                              alt={item.productName || item.name}
                              className="w-100 h-100 object-fit-cover"
                            />
                          ) : (
                            <i className="bi bi-box-seam text-muted fs-4" />
                          )}
                        </div>
                      </div>

                      {/* Product Info */}
                      <div className="col">
                        <h6 className="fw-bold text-dark mb-1">
                          {item.productName || item.name}
                        </h6>
                        <span className="text-muted small d-block mb-1">
                          {item.unit} &bull; ₹{Number(item.unitPrice || item.price).toFixed(2)} each
                        </span>
                        {item.isOutOfStock && (
                          <span className="badge bg-danger small">
                            Only {item.availableQuantity} available
                          </span>
                        )}
                      </div>

                      {/* Quantity Controls */}
                      <div className="col-auto">
                        <div className="btn-group btn-group-sm border rounded">
                          <button
                            className="btn btn-light px-2"
                            disabled={isUpdating}
                            onClick={() => handleUpdateQuantity(itemKey, item.quantity - 1)}
                            title="Decrease quantity"
                          >
                            <i className="bi bi-dash" />
                          </button>
                          <span className="btn btn-white disabled fw-bold px-3 text-dark">
                            {isUpdating ? '...' : item.quantity}
                          </span>
                          <button
                            className="btn btn-light px-2"
                            disabled={isUpdating || (item.availableQuantity && item.quantity >= item.availableQuantity)}
                            onClick={() => handleUpdateQuantity(itemKey, item.quantity + 1)}
                            title="Increase quantity"
                          >
                            <i className="bi bi-plus" />
                          </button>
                        </div>
                      </div>

                      {/* Line Total & Remove */}
                      <div className="col-auto text-end" style={{ minWidth: '90px' }}>
                        <div className="fw-bold text-dark mb-1">
                          ₹{Number(item.lineTotal || (item.price * item.quantity)).toFixed(2)}
                        </div>
                        <button
                          className="btn btn-link text-muted p-0 text-decoration-none small"
                          disabled={isUpdating}
                          onClick={() => handleRemoveItem(itemKey)}
                          title="Remove item"
                        >
                          <i className="bi bi-trash text-danger me-1" />
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Bill Summary & Checkout */}
        <div className="col-lg-4">
          <div className="card border shadow-sm p-4 sticky-top" style={{ top: '80px', borderRadius: 'var(--radius-md)' }}>
            <h5 className="fw-bold text-dark mb-3">Order Summary</h5>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted">Item Subtotal</span>
              <span className="fw-semibold text-dark">₹{Number(cart?.subtotal || 0).toFixed(2)}</span>
            </div>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted">Estimated Delivery</span>
              <span className="text-muted small">Calculated at checkout</span>
            </div>

            <div className="d-flex justify-content-between mb-2">
              <span className="text-muted">Taxes (GST 5%)</span>
              <span className="text-muted small">Calculated at checkout</span>
            </div>

            <hr className="my-3" />

            <div className="d-flex justify-content-between align-items-center mb-4">
              <span className="fw-bold text-dark fs-6">Estimated Total</span>
              <span className="fw-bolder text-dark fs-5">
                ₹{Number(cart?.subtotal || 0).toFixed(2)}
              </span>
            </div>

            <button
              className="btn btn-primary w-100 py-2 fw-semibold d-flex align-items-center justify-content-center gap-2"
              disabled={hasOutOfStock}
              onClick={handleProceedToCheckout}
            >
              <span>Proceed to Checkout</span>
              <i className="bi bi-arrow-right" />
            </button>

            <div className="d-flex align-items-center gap-2 mt-3 text-muted small justify-content-center">
              <i className="bi bi-shield-check text-success" />
              <span>Safe & Secure Hyperlocal Checkout</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
