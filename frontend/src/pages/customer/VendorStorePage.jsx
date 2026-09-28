import React, { useState, useEffect } from 'react';
import { useParams, Link, useOutletContext } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';

export default function VendorStorePage() {
  const { vendorId } = useParams();
  const { selectedLocation, setCartCount } = useOutletContext() || {};

  const [vendor, setVendor] = useState(null);
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState(null);

  // Cart state
  const [cart, setCart] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('cart') || '{"vendorId":null,"vendorName":null,"items":[]}');
    } catch {
      return { vendorId: null, vendorName: null, items: [] };
    }
  });

  const [conflictModal, setConflictModal] = useState(null);

  useEffect(() => {
    fetchVendor();
    fetchCategories();
  }, [vendorId]);

  useEffect(() => {
    fetchProducts();
  }, [vendorId, selectedCategory, searchQuery]);

  const fetchVendor = async () => {
    try {
      const lat = selectedLocation?.lat || 12.9352;
      const lng = selectedLocation?.lng || 77.6245;
      const data = await apiClient.get(`/vendors/${vendorId}?lat=${lat}&lng=${lng}`);
      setVendor(data);
    } catch {
      setVendor(null);
    }
  };

  const fetchCategories = async () => {
    try {
      const data = await apiClient.get('/categories');
      setCategories(data || []);
    } catch {
      setCategories([]);
    }
  };

  const fetchProducts = async () => {
    setLoading(true);
    try {
      let url = `/vendors/${vendorId}/products?availableOnly=true&page=0&size=50`;
      if (selectedCategory) url += `&categoryId=${selectedCategory}`;
      if (searchQuery.trim()) url += `&q=${encodeURIComponent(searchQuery.trim())}`;
      const res = await apiClient.get(url);
      setProducts(res?.content || res || []);
    } catch {
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  // Cart operations (Single-vendor cart enforcement per PRD §3.5)
  const getItemQuantityInCart = (productId) => {
    const item = cart.items?.find((i) => i.productId === productId);
    return item ? item.quantity : 0;
  };

  const handleAddToCart = (product, quantity = 1) => {
    const currentVendorId = Number(vendorId);

    // Check if cart belongs to another vendor
    if (cart.vendorId && cart.vendorId !== currentVendorId && cart.items?.length > 0) {
      setConflictModal({
        newProduct: product,
        newQuantity: quantity,
      });
      return;
    }

    applyAddToCart(product, quantity, currentVendorId);
  };

  const applyAddToCart = (product, quantity, storeId) => {
    const existingItems = cart.items || [];
    const existingIndex = existingItems.findIndex((i) => i.productId === product.id);

    let updatedItems;
    if (existingIndex > -1) {
      updatedItems = [...existingItems];
      const newQty = updatedItems[existingIndex].quantity + quantity;
      if (newQty <= 0) {
        updatedItems.splice(existingIndex, 1);
      } else {
        updatedItems[existingIndex].quantity = newQty;
      }
    } else {
      updatedItems = [
        ...existingItems,
        {
          productId: product.id,
          name: product.name,
          price: product.price,
          unit: product.unit,
          imageUrl: product.imageUrl,
          quantity,
        },
      ];
    }

    const updatedCart = {
      vendorId: updatedItems.length > 0 ? storeId : null,
      vendorName: updatedItems.length > 0 ? vendor?.storeName : null,
      items: updatedItems,
    };

    setCart(updatedCart);
    localStorage.setItem('cart', JSON.stringify(updatedCart));
    window.dispatchEvent(new Event('cart-updated'));
    if (setCartCount) {
      const count = updatedItems.reduce((acc, i) => acc + i.quantity, 0);
      setCartCount(count);
    }
  };

  const handleReplaceCart = () => {
    if (!conflictModal) return;
    const { newProduct, newQuantity } = conflictModal;
    const currentVendorId = Number(vendorId);

    const newCart = {
      vendorId: currentVendorId,
      vendorName: vendor?.storeName,
      items: [
        {
          productId: newProduct.id,
          name: newProduct.name,
          price: newProduct.price,
          unit: newProduct.unit,
          imageUrl: newProduct.imageUrl,
          quantity: newQuantity,
        },
      ],
    };

    setCart(newCart);
    localStorage.setItem('cart', JSON.stringify(newCart));
    window.dispatchEvent(new Event('cart-updated'));
    if (setCartCount) setCartCount(newQuantity);
    setConflictModal(null);
  };

  if (!vendor && !loading) {
    return (
      <div className="container py-5 text-center">
        <EmptyState
          icon="bi-shop"
          title="Store Not Found"
          description="The store you are looking for does not exist or is currently inactive."
          actionLabel="Browse Nearby Stores"
          onAction={() => window.location.href = '/'}
        />
      </div>
    );
  }

  return (
    <div className="container-xl py-4">
      {/* Back Link */}
      <Link to="/" className="text-decoration-none text-muted small d-inline-flex align-items-center gap-1 mb-3">
        <i className="bi bi-arrow-left" />
        Back to all stores
      </Link>

      {/* Store Header Card */}
      <div className="card border p-4 shadow-sm mb-4" style={{ borderRadius: 'var(--radius-md)' }}>
        <div className="row align-items-center">
          <div className="col-auto">
            <div
              className="rounded bg-light border d-flex align-items-center justify-content-center text-dark"
              style={{ width: '80px', height: '80px' }}
            >
              <i className="bi bi-shop fs-1" />
            </div>
          </div>
          <div className="col">
            <div className="d-flex align-items-center gap-2 mb-1 flex-wrap">
              <h2 className="fw-bolder mb-0 text-dark" style={{ letterSpacing: '-0.02em' }}>
                {vendor?.storeName || 'Loading store...'}
              </h2>
              {vendor && <StatusBadge status={vendor.isOpen ? 'OPEN' : 'CLOSED'} />}
            </div>

            <p className="text-muted small mb-2">{vendor?.description}</p>

            <div className="d-flex align-items-center gap-3 small text-muted flex-wrap">
              <span className="d-flex align-items-center gap-1">
                <i className="bi bi-geo-alt text-danger" />
                {vendor?.address}
              </span>
              <span>&bull;</span>
              <span className="d-flex align-items-center gap-1">
                <i className="bi bi-pin-map text-primary" />
                {vendor?.distanceKm ? `${vendor.distanceKm} km away` : 'Nearby'}
              </span>
              <span>&bull;</span>
              <span className="d-flex align-items-center gap-1">
                <i className="bi bi-bicycle text-success" />
                Delivery radius: {vendor?.deliveryRadiusKm} km
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Search & Categories Bar */}
      <div className="row g-3 align-items-center mb-4">
        <div className="col-12 col-md-5">
          <div className="input-group shadow-sm">
            <span className="input-group-text bg-white border-end-0">
              <i className="bi bi-search text-muted" />
            </span>
            <input
              type="text"
              className="form-control border-start-0"
              placeholder={`Search products in ${vendor?.storeName || 'store'}...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {searchQuery && (
              <button
                className="btn btn-white border-top border-bottom border-start-0 text-muted"
                onClick={() => setSearchQuery('')}
              >
                <i className="bi bi-x" />
              </button>
            )}
          </div>
        </div>

        <div className="col-12 col-md-7">
          <div className="d-flex gap-2 overflow-auto pb-1" style={{ scrollbarWidth: 'thin' }}>
            <button
              className={`btn btn-sm rounded-pill px-3 text-nowrap ${
                selectedCategory === null ? 'btn-dark' : 'btn-outline-secondary border'
              }`}
              onClick={() => setSelectedCategory(null)}
            >
              All Items
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                className={`btn btn-sm rounded-pill px-3 text-nowrap ${
                  selectedCategory === cat.id ? 'btn-dark' : 'btn-outline-secondary border'
                }`}
                onClick={() => setSelectedCategory(cat.id)}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Product Catalog Grid */}
      {loading ? (
        <div className="row g-3">
          {[1, 2, 3, 4, 5, 6].map((n) => (
            <div key={n} className="col-6 col-md-4 col-lg-3">
              <div className="card p-3 border">
                <Skeleton height="140px" className="mb-2" />
                <Skeleton width="70%" height="20px" className="mb-2" />
                <Skeleton width="40%" height="15px" />
              </div>
            </div>
          ))}
        </div>
      ) : products.length === 0 ? (
        <EmptyState
          icon="bi-box-seam"
          title="No products available"
          description="There are currently no products matching your search in this store."
          actionLabel="Clear Filter"
          onAction={() => {
            setSelectedCategory(null);
            setSearchQuery('');
          }}
        />
      ) : (
        <div className="row g-3">
          {products.map((product) => {
            const qty = getItemQuantityInCart(product.id);
            return (
              <div key={product.id} className="col-6 col-md-4 col-lg-3">
                <div className="card h-100 p-3 border d-flex flex-column justify-content-between position-relative">
                  {/* Clickable Area for Detail Modal */}
                  <div
                    style={{ cursor: 'pointer' }}
                    onClick={() => setSelectedProduct(product)}
                  >
                    <div
                      className="bg-light rounded d-flex align-items-center justify-content-center mb-2 overflow-hidden position-relative"
                      style={{ height: '140px' }}
                    >
                      {product.imageUrl ? (
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="w-100 h-100 object-fit-cover"
                        />
                      ) : (
                        <i className="bi bi-box-seam text-muted fs-1" />
                      )}

                      {/* Stock badge */}
                      {product.isOutOfStock ? (
                        <span className="position-absolute top-0 start-0 m-2 badge bg-danger">
                          Out of stock
                        </span>
                      ) : product.isLowStock ? (
                        <span className="position-absolute top-0 start-0 m-2 badge bg-warning text-dark">
                          Only {product.availableQuantity} left
                        </span>
                      ) : null}
                    </div>

                    <span className="badge bg-light text-muted border small mb-1" style={{ fontSize: '0.7rem' }}>
                      {product.categoryName || 'General'}
                    </span>

                    <h6 className="fw-bold text-dark text-truncate mb-1" title={product.name}>
                      {product.name}
                    </h6>

                    <span className="text-muted small d-block mb-2" style={{ fontSize: '0.8rem' }}>
                      {product.unit}
                    </span>
                  </div>

                  {/* Price & Add to Cart Controls */}
                  <div className="d-flex align-items-center justify-content-between pt-2 border-top mt-auto">
                    <span className="fw-bold text-dark fs-6">
                      ₹{Number(product.price).toFixed(2)}
                    </span>

                    {product.isOutOfStock ? (
                      <button className="btn btn-sm btn-light border text-muted" disabled>
                        Unavailable
                      </button>
                    ) : qty > 0 ? (
                      <div className="btn-group btn-group-sm border rounded">
                        <button
                          type="button"
                          className="btn btn-light px-2"
                          onClick={() => handleAddToCart(product, -1)}
                        >
                          <i className="bi bi-dash" />
                        </button>
                        <span className="btn btn-white disabled text-dark fw-bold px-2">
                          {qty}
                        </span>
                        <button
                          type="button"
                          className="btn btn-light px-2"
                          onClick={() => handleAddToCart(product, 1)}
                          disabled={qty >= product.availableQuantity}
                        >
                          <i className="bi bi-plus" />
                        </button>
                      </div>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-outline-dark btn-sm rounded-pill px-3"
                        onClick={() => handleAddToCart(product, 1)}
                      >
                        + Add
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Cross-vendor Cart Conflict Dialog (PRD §3.5) */}
      {conflictModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1070 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold text-dark">Replace cart items?</h5>
                <button type="button" className="btn-close" onClick={() => setConflictModal(null)} />
              </div>
              <div className="modal-body p-4">
                <p className="text-dark mb-2">
                  Your cart currently contains items from <strong>{cart.vendorName || 'another store'}</strong>.
                </p>
                <p className="text-muted small mb-0">
                  Hyperlocal delivers from one neighborhood store per order to guarantee fast 30-min delivery.
                  Would you like to discard your previous cart and start a fresh order from <strong>{vendor?.storeName}</strong>?
                </p>
              </div>
              <div className="modal-footer border-top">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => setConflictModal(null)}
                >
                  Keep Existing Cart
                </button>
                <button
                  type="button"
                  className="btn btn-danger btn-sm px-3"
                  onClick={handleReplaceCart}
                >
                  Clear & Add New Item
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Product Detail Modal */}
      {selectedProduct && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold text-dark">{selectedProduct.name}</h5>
                <button type="button" className="btn-close" onClick={() => setSelectedProduct(null)} />
              </div>
              <div className="modal-body p-4">
                <div
                  className="bg-light rounded d-flex align-items-center justify-content-center mb-3"
                  style={{ height: '220px' }}
                >
                  {selectedProduct.imageUrl ? (
                    <img src={selectedProduct.imageUrl} alt={selectedProduct.name} className="h-100 object-fit-contain" />
                  ) : (
                    <i className="bi bi-box-seam fs-1 text-muted" />
                  )}
                </div>

                <div className="d-flex justify-content-between align-items-center mb-2">
                  <h4 className="fw-bold text-dark mb-0">₹{Number(selectedProduct.price).toFixed(2)}</h4>
                  <span className="badge bg-secondary">{selectedProduct.unit}</span>
                </div>

                <p className="text-muted small mb-3">
                  {selectedProduct.description || 'No additional description provided for this product.'}
                </p>

                <div className="d-flex justify-content-between align-items-center pt-3 border-top">
                  <span className="small text-muted">
                    Availability: {selectedProduct.availableQuantity > 0 ? (
                      <span className="text-success fw-semibold">In Stock ({selectedProduct.availableQuantity} available)</span>
                    ) : (
                      <span className="text-danger fw-semibold">Out of Stock</span>
                    )}
                  </span>

                  <button
                    className="btn btn-primary btn-sm px-4"
                    disabled={selectedProduct.availableQuantity <= 0}
                    onClick={() => {
                      handleAddToCart(selectedProduct, 1);
                      setSelectedProduct(null);
                    }}
                  >
                    Add to Cart
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
