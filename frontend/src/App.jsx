import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './store/AuthContext';
import ErrorBoundary from './components/common/ErrorBoundary';
import AppShell from './layouts/AppShell';
import HomePage from './pages/customer/HomePage';
import LoginPage from './pages/customer/LoginPage';
import RegisterPage from './pages/customer/RegisterPage';
import ForgotPasswordPage from './pages/customer/ForgotPasswordPage';
import ResetPasswordPage from './pages/customer/ResetPasswordPage';
import ProfilePage from './pages/customer/ProfilePage';
import './styles/globals.css';

import VendorStorePage from './pages/customer/VendorStorePage';
import CartPage from './pages/customer/CartPage';
import CheckoutPage from './pages/customer/CheckoutPage';
import OrdersPage from './pages/customer/OrdersPage';
import OrderTrackingPage from './pages/customer/OrderTrackingPage';
import VendorDashboardPage from './pages/vendor/VendorDashboardPage';
import VendorOrdersPage from './pages/vendor/VendorOrdersPage';
import VendorProductsPage from './pages/vendor/VendorProductsPage';
import VendorInventoryPage from './pages/vendor/VendorInventoryPage';
import DeliveryDashboardPage from './pages/delivery/DeliveryDashboardPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminVendorsPage from './pages/admin/AdminVendorsPage';

// Protected Route Wrapper
const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    return <Navigate to="/" replace />;
  }

  return children;
};

// Placeholder page for routes in upcoming milestones
const PlaceholderPage = ({ title, role }) => (
  <div className="container py-5 text-center">
    <div className="card p-5 border shadow-sm mx-auto" style={{ maxWidth: '600px', borderRadius: 'var(--radius-md)' }}>
      <h3 className="fw-bold mb-2">{title}</h3>
      <p className="text-muted">
        This view is part of the implementation plan and will be populated in subsequent milestones.
      </p>
      {role && <span className="badge bg-dark align-self-center py-2 px-3">{role} Module</span>}
    </div>
  </div>
);

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter>
        <Routes>
          <Route path="/" element={<AppShell />}>
            <Route index element={<HomePage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="forgot-password" element={<ForgotPasswordPage />} />
            <Route path="reset-password" element={<ResetPasswordPage />} />
            <Route
              path="profile"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />

            {/* Storefront & Catalog */}
            <Route path="vendors/:vendorId" element={<VendorStorePage />} />
            <Route path="cart" element={<CartPage />} />
            <Route
              path="checkout"
              element={
                <ProtectedRoute>
                  <CheckoutPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="orders"
              element={
                <ProtectedRoute>
                  <OrdersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="orders/:id"
              element={
                <ProtectedRoute>
                  <OrderTrackingPage />
                </ProtectedRoute>
              }
            />

            {/* Vendor Portal */}
            <Route
              path="vendor/dashboard"
              element={
                <ProtectedRoute allowedRoles={['VENDOR']}>
                  <VendorDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="vendor/orders"
              element={
                <ProtectedRoute allowedRoles={['VENDOR']}>
                  <VendorOrdersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="vendor/products"
              element={
                <ProtectedRoute allowedRoles={['VENDOR']}>
                  <VendorProductsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="vendor/inventory"
              element={
                <ProtectedRoute allowedRoles={['VENDOR']}>
                  <VendorInventoryPage />
                </ProtectedRoute>
              }
            />

            {/* Delivery Portal */}
            <Route
              path="delivery/dashboard"
              element={
                <ProtectedRoute allowedRoles={['DELIVERY_PARTNER']}>
                  <DeliveryDashboardPage />
                </ProtectedRoute>
              }
            />

            {/* Admin Portal */}
            <Route
              path="admin/dashboard"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <AdminDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="admin/vendors"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <AdminVendorsPage />
                </ProtectedRoute>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </ErrorBoundary>
  );
}
