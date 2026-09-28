import React, { useState } from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';
import AppNavbar from './AppNavbar';
import LocationModal from '../components/common/LocationModal';
import { useAuth } from '../store/AuthContext';

export default function AppShell() {
  const { user } = useAuth();
  const location = useLocation();

  // Selected delivery location state
  const [selectedLocation, setSelectedLocation] = useState(() => {
    try {
      const saved = localStorage.getItem('selectedLocation');
      return saved ? JSON.parse(saved) : { label: 'Koramangala, Bengaluru', lat: 12.9352, lng: 77.6245 };
    } catch {
      return { label: 'Koramangala, Bengaluru', lat: 12.9352, lng: 77.6245 };
    }
  });

  const [showLocationModal, setShowLocationModal] = useState(false);
  const [cartCount, setCartCount] = useState(0);

  // Sync cart count from localStorage
  React.useEffect(() => {
    const updateCartCount = () => {
      try {
        const cart = JSON.parse(localStorage.getItem('cart') || '{"items":[]}');
        const count = cart.items?.reduce((acc, item) => acc + item.quantity, 0) || 0;
        setCartCount(count);
      } catch {
        setCartCount(0);
      }
    };
    updateCartCount();
    window.addEventListener('storage', updateCartCount);
    window.addEventListener('cart-updated', updateCartCount);
    return () => {
      window.removeEventListener('storage', updateCartCount);
      window.removeEventListener('cart-updated', updateCartCount);
    };
  }, []);

  const isActive = (path) => location.pathname === path;

  return (
    <div className="d-flex flex-column min-vh-100">
      <AppNavbar
        selectedLocation={selectedLocation}
        onOpenLocationModal={() => setShowLocationModal(true)}
        cartItemCount={cartCount}
      />

      <main className="flex-grow-1 pb-5 pb-md-4">
        <Outlet context={{ selectedLocation, setSelectedLocation, setCartCount }} />
      </main>

      {/* Footer */}
      <footer className="border-top py-4 bg-white mt-auto d-none d-md-block" style={{ borderColor: 'var(--color-border)' }}>
        <div className="container-xl d-flex flex-column flex-md-row justify-content-between align-items-center gap-2">
          <div className="d-flex align-items-center gap-2 text-muted small">
            <span className="fw-semibold text-dark">Hyperlocal Marketplace</span>
            <span>&bull;</span>
            <span>Local stores delivered to your doorstep</span>
          </div>
          <div className="text-muted small">
            &copy; 2026 Hyperlocal Inc. All rights reserved.
          </div>
        </div>
      </footer>

      {/* Mobile Bottom Navigation (PRD §4.1) */}
      <div
        className="d-md-none fixed-bottom bg-white border-top d-flex justify-content-around py-2 px-1"
        style={{ borderColor: 'var(--color-border)', zIndex: 1020, boxShadow: '0 -2px 8px rgba(0,0,0,0.05)' }}
      >
        <Link
          to="/"
          className={`d-flex flex-column align-items-center text-decoration-none ${
            isActive('/') ? 'text-primary fw-bold' : 'text-muted'
          }`}
          style={{ fontSize: '0.75rem' }}
        >
          <i className="bi bi-shop fs-5 mb-1" />
          <span>Home</span>
        </Link>

        <Link
          to="/search"
          className={`d-flex flex-column align-items-center text-decoration-none ${
            isActive('/search') ? 'text-primary fw-bold' : 'text-muted'
          }`}
          style={{ fontSize: '0.75rem' }}
        >
          <i className="bi bi-search fs-5 mb-1" />
          <span>Search</span>
        </Link>

        <Link
          to="/cart"
          className={`d-flex flex-column align-items-center text-decoration-none position-relative ${
            isActive('/cart') ? 'text-primary fw-bold' : 'text-muted'
          }`}
          style={{ fontSize: '0.75rem' }}
        >
          <i className="bi bi-bag fs-5 mb-1" />
          <span>Cart</span>
          {cartCount > 0 && (
            <span
              className="position-absolute top-0 start-50 translate-middle-x badge rounded-pill bg-danger"
              style={{ fontSize: '0.6rem', marginTop: '-4px', marginLeft: '12px' }}
            >
              {cartCount}
            </span>
          )}
        </Link>

        <Link
          to="/orders"
          className={`d-flex flex-column align-items-center text-decoration-none ${
            isActive('/orders') ? 'text-primary fw-bold' : 'text-muted'
          }`}
          style={{ fontSize: '0.75rem' }}
        >
          <i className="bi bi-receipt fs-5 mb-1" />
          <span>Orders</span>
        </Link>

        <Link
          to={user ? '/profile' : '/login'}
          className={`d-flex flex-column align-items-center text-decoration-none ${
            isActive('/profile') || isActive('/login') ? 'text-primary fw-bold' : 'text-muted'
          }`}
          style={{ fontSize: '0.75rem' }}
        >
          <i className="bi bi-person fs-5 mb-1" />
          <span>{user ? 'Account' : 'Sign In'}</span>
        </Link>
      </div>

      <LocationModal
        show={showLocationModal}
        onClose={() => setShowLocationModal(false)}
        currentLocation={selectedLocation}
        onSelectLocation={(loc) => {
          setSelectedLocation(loc);
          localStorage.setItem('selectedLocation', JSON.stringify(loc));
        }}
      />
    </div>
  );
}
