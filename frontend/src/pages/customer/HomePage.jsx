import React, { useState, useEffect } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';

export default function HomePage() {
  const { selectedLocation } = useOutletContext() || {};
  const [vendors, setVendors] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [sortBy, setSortBy] = useState('distance');

  useEffect(() => {
    fetchNearbyVendors();
    fetchCategories();
  }, [selectedLocation, sortBy]);

  const fetchCategories = async () => {
    try {
      const data = await apiClient.get('/categories');
      setCategories(data?.content || data || []);
    } catch {
      // Fallback categories for display
      setCategories([
        { id: 1, name: 'Groceries', slug: 'groceries' },
        { id: 2, name: 'Dairy & Eggs', slug: 'dairy' },
        { id: 3, name: 'Fruits & Veggies', slug: 'fruits-vegetables' },
        { id: 4, name: 'Bakery & Snacks', slug: 'bakery-snacks' },
        { id: 5, name: 'Beverages', slug: 'beverages' },
        { id: 6, name: 'Personal Care', slug: 'personal-care' },
      ]);
    }
  };

  const fetchNearbyVendors = async () => {
    setLoading(true);
    try {
      const lat = selectedLocation?.lat || 12.9352;
      const lng = selectedLocation?.lng || 77.6245;
      const res = await apiClient.get(`/vendors/nearby?lat=${lat}&lng=${lng}&sort=${sortBy}&page=0&size=20`);
      setVendors(res?.content || res || []);
    } catch (err) {
      // Mock vendors for initial preview until M2 backend vendors populated
      setVendors([
        {
          id: 1,
          storeName: 'Fresh Mart Supermarket',
          description: 'Groceries, fresh dairy, pulses and daily essentials',
          distanceKm: 1.2,
          deliveryRadiusKm: 5.0,
          status: 'ACTIVE',
          isOpen: true,
          address: '4th Block, Koramangala',
          category: 'Groceries',
        },
        {
          id: 2,
          storeName: 'Daily Needs Express',
          description: 'Snacks, beverages, dairy, and instant foods',
          distanceKm: 2.1,
          deliveryRadiusKm: 4.0,
          status: 'ACTIVE',
          isOpen: true,
          address: '80 Feet Road, Indiranagar',
          category: 'Snacks & Drinks',
        },
        {
          id: 3,
          storeName: 'Organic Greens & Fruits',
          description: 'Farm-fresh vegetables and seasonal fruits',
          distanceKm: 3.4,
          deliveryRadiusKm: 6.0,
          status: 'ACTIVE',
          isOpen: false,
          address: 'HSR Layout Sector 1',
          category: 'Fruits & Veggies',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  // Filtered vendors
  const filteredVendors = vendors.filter((v) => {
    const matchesSearch = searchQuery
      ? v.storeName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.address?.toLowerCase().includes(searchQuery.toLowerCase())
      : true;
    const matchesCategory = selectedCategory
      ? v.category?.toLowerCase() === selectedCategory.toLowerCase()
      : true;
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="container-xl py-4">
      {/* Hero Banner / Location Prompt */}
      <div
        className="p-4 p-md-5 mb-4 rounded-3 text-dark border"
        style={{
          backgroundColor: 'var(--color-surface)',
          borderColor: 'var(--color-border)',
          borderRadius: 'var(--radius-lg)',
        }}
      >
        <div className="row align-items-center">
          <div className="col-lg-7">
            <span className="badge bg-dark mb-2 px-3 py-1 rounded-pill">Hyperlocal Delivery in 30 Mins</span>
            <h1 className="fw-bolder display-6 mb-2" style={{ letterSpacing: '-0.02em' }}>
              Everyday essentials from your favorite neighborhood stores.
            </h1>
            <p className="text-muted mb-4 lead fs-6">
              Order groceries, fresh produce, and essentials directly from trusted local shops nearby.
            </p>

            {/* Search Input Bar */}
            <div className="input-group mb-2 shadow-sm" style={{ maxWidth: '520px' }}>
              <span className="input-group-text bg-white border-end-0">
                <i className="bi bi-search text-muted" />
              </span>
              <input
                type="text"
                className="form-control border-start-0 py-2"
                placeholder="Search products or stores near you..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              {searchQuery && (
                <button
                  className="btn btn-white border-top border-bottom border-start-0 text-muted"
                  onClick={() => setSearchQuery('')}
                >
                  <i className="bi bi-x-circle" />
                </button>
              )}
            </div>

            {/* Recent Searches chips */}
            <div className="d-flex align-items-center gap-2 mt-2 flex-wrap">
              <span className="small text-muted">Popular:</span>
              {['Milk', 'Bread', 'Eggs', 'Fresh Vegetables', 'Snacks'].map((tag) => (
                <button
                  key={tag}
                  className="btn btn-sm btn-light border rounded-pill py-0 px-2"
                  style={{ fontSize: '0.75rem' }}
                  onClick={() => setSearchQuery(tag)}
                >
                  {tag}
                </button>
              ))}
            </div>
          </div>

          <div className="col-lg-5 d-none d-lg-flex justify-content-center">
            <div
              className="p-4 rounded-circle bg-white border shadow-sm d-flex align-items-center justify-content-center"
              style={{ width: '220px', height: '220px' }}
            >
              <i className="bi bi-shop-window text-dark" style={{ fontSize: '5rem' }} />
            </div>
          </div>
        </div>
      </div>

      {/* Category Chips Bar */}
      <div className="mb-4">
        <div className="d-flex align-items-center justify-content-between mb-2">
          <h5 className="fw-bold mb-0 text-dark">Explore Categories</h5>
          {selectedCategory && (
            <button
              className="btn btn-link text-decoration-none btn-sm p-0 text-muted"
              onClick={() => setSelectedCategory(null)}
            >
              Clear filter
            </button>
          )}
        </div>
        <div className="d-flex gap-2 overflow-auto pb-2" style={{ scrollbarWidth: 'thin' }}>
          <button
            className={`btn btn-sm rounded-pill px-3 text-nowrap ${
              selectedCategory === null ? 'btn-dark' : 'btn-outline-secondary border'
            }`}
            onClick={() => setSelectedCategory(null)}
          >
            All Stores
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              className={`btn btn-sm rounded-pill px-3 text-nowrap ${
                selectedCategory === cat.name ? 'btn-dark' : 'btn-outline-secondary border'
              }`}
              onClick={() => setSelectedCategory(cat.name)}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* Vendors Section Header & Sorting */}
      <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-3">
        <div>
          <h4 className="fw-bold text-dark mb-0">Nearby Stores</h4>
          <p className="text-muted small mb-0">
            Delivering to <span className="fw-semibold text-dark">{selectedLocation?.label || 'your location'}</span>
          </p>
        </div>

        <div className="d-flex align-items-center gap-2">
          <span className="small text-muted">Sort by:</span>
          <select
            className="form-select form-select-sm"
            style={{ width: 'auto' }}
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
          >
            <option value="distance">Distance: Nearest first</option>
            <option value="open">Open Stores First</option>
          </select>
        </div>
      </div>

      {/* Vendor Cards Grid */}
      {loading ? (
        <div className="row g-3">
          {[1, 2, 3].map((n) => (
            <div key={n} className="col-12 col-md-6 col-lg-4">
              <div className="card p-3 border">
                <Skeleton height="120px" className="mb-2" />
                <Skeleton width="60%" height="20px" className="mb-2" />
                <Skeleton width="40%" height="15px" />
              </div>
            </div>
          ))}
        </div>
      ) : filteredVendors.length === 0 ? (
        <EmptyState
          icon="bi-shop"
          title="No stores available in this area"
          description="We couldn't find any participating local vendors matching your search or location."
          actionLabel="Clear Filters"
          onAction={() => {
            setSearchQuery('');
            setSelectedCategory(null);
          }}
        />
      ) : (
        <div className="row g-3">
          {filteredVendors.map((vendor) => (
            <div key={vendor.id} className="col-12 col-md-6 col-lg-4">
              <Link
                to={`/vendors/${vendor.id}`}
                className="text-decoration-none text-dark"
              >
                <div className="card h-100 p-3 border hover-lift">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <div className="d-flex align-items-center gap-2">
                      <div
                        className="rounded bg-light border d-flex align-items-center justify-content-center text-dark fw-bold"
                        style={{ width: '48px', height: '48px' }}
                      >
                        <i className="bi bi-shop fs-4" />
                      </div>
                      <div>
                        <h6 className="fw-bold mb-0 text-dark">{vendor.storeName}</h6>
                        <span className="text-muted small">{vendor.address}</span>
                      </div>
                    </div>
                    <StatusBadge status={vendor.isOpen ? 'OPEN' : 'CLOSED'} />
                  </div>

                  <p className="text-muted small mb-3 flex-grow-1" style={{ fontSize: '0.85rem' }}>
                    {vendor.description}
                  </p>

                  <div className="d-flex align-items-center justify-content-between pt-2 border-top small text-muted">
                    <div className="d-flex align-items-center gap-1">
                      <i className="bi bi-geo-alt" />
                      <span>{vendor.distanceKm ? `${vendor.distanceKm} km away` : 'Nearby'}</span>
                    </div>
                    <div className="d-flex align-items-center gap-1 text-dark fw-medium">
                      <i className="bi bi-clock" />
                      <span>20-30 min</span>
                    </div>
                  </div>
                </div>
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
