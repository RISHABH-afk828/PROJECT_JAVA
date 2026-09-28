import React, { useState, useEffect } from 'react';
import apiClient from '../../services/api/apiClient';
import LoadingButton from '../../components/common/LoadingButton';
import StatusBadge from '../../components/common/StatusBadge';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';

export default function VendorProductsPage() {
  const [store, setStore] = useState(null);
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  const [formData, setFormData] = useState({
    name: '',
    categoryId: '',
    price: '',
    unit: '',
    description: '',
    initialStock: 20,
    lowStockThreshold: 5,
    imageUrl: '',
    active: true,
  });

  useEffect(() => {
    init();
  }, []);

  const init = async () => {
    setLoading(true);
    try {
      const storeData = await apiClient.get('/vendor/store');
      setStore(storeData);

      const [catData, prodData] = await Promise.all([
        apiClient.get('/categories'),
        apiClient.get(`/vendors/${storeData.id}/products?availableOnly=false&page=0&size=100`),
      ]);

      setCategories(catData || []);
      setProducts(prodData?.content || prodData || []);
    } catch {
      //
    } finally {
      setLoading(false);
    }
  };

  const handleOpenAdd = () => {
    setEditingProduct(null);
    setFormData({
      name: '',
      categoryId: categories[0]?.id || '',
      price: '',
      unit: '',
      description: '',
      initialStock: 20,
      lowStockThreshold: 5,
      imageUrl: '',
      active: true,
    });
    setErrorMsg(null);
    setShowModal(true);
  };

  const handleOpenEdit = (product) => {
    setEditingProduct(product);
    setFormData({
      name: product.name,
      categoryId: product.categoryId,
      price: product.price,
      unit: product.unit,
      description: product.description || '',
      initialStock: product.availableQuantity,
      lowStockThreshold: product.lowStockThreshold || 5,
      imageUrl: product.imageUrl || '',
      active: product.active,
    });
    setErrorMsg(null);
    setShowModal(true);
  };

  const handleImageUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const data = new FormData();
    data.append('file', file);

    setUploadingImage(true);
    try {
      const res = await apiClient.post('/files', data, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setFormData((prev) => ({ ...prev, imageUrl: res.url }));
    } catch (err) {
      alert(err.message || 'Image upload failed. Allowed types: JPEG, PNG, WebP.');
    } finally {
      setUploadingImage(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!store) return;

    setSaving(true);
    setErrorMsg(null);
    try {
      if (editingProduct) {
        await apiClient.patch(`/vendors/${store.id}/products/${editingProduct.id}`, {
          name: formData.name,
          categoryId: Number(formData.categoryId),
          price: Number(formData.price),
          unit: formData.unit,
          description: formData.description,
          lowStockThreshold: Number(formData.lowStockThreshold),
          imageUrl: formData.imageUrl,
          active: formData.active,
        });
      } else {
        await apiClient.post(`/vendors/${store.id}/products`, {
          name: formData.name,
          categoryId: Number(formData.categoryId),
          price: Number(formData.price),
          unit: formData.unit,
          description: formData.description,
          initialStock: Number(formData.initialStock),
          lowStockThreshold: Number(formData.lowStockThreshold),
          imageUrl: formData.imageUrl,
          active: formData.active,
        });
      }
      setShowModal(false);
      init();
    } catch (err) {
      setErrorMsg(err.message || 'Could not save product.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (productId) => {
    if (!window.confirm('Are you sure you want to archive this product?')) return;
    try {
      await apiClient.delete(`/vendors/${store.id}/products/${productId}`);
      init();
    } catch (err) {
      alert(err.message || 'Could not archive product.');
    }
  };

  const handleToggleActive = async (product) => {
    const endpoint = product.active ? 'deactivate' : 'activate';
    try {
      await apiClient.post(`/vendors/${store.id}/products/${product.id}/${endpoint}`);
      init();
    } catch (err) {
      alert(err.message || 'Could not toggle product status.');
    }
  };

  return (
    <div className="container-xl py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h3 className="fw-bolder text-dark mb-0">Products & Catalog</h3>
          <p className="text-muted small mb-0">Manage items displayed to customers ordering from your store.</p>
        </div>
        <button
          className="btn btn-primary btn-sm rounded-pill px-3 fw-semibold"
          onClick={handleOpenAdd}
        >
          <i className="bi bi-plus-lg me-1" />
          Add Product
        </button>
      </div>

      {loading ? (
        <Skeleton height="300px" />
      ) : products.length === 0 ? (
        <EmptyState
          icon="bi-box-seam"
          title="No products yet"
          description="Your store currently has no products listed. Add your first item to begin serving customers."
          actionLabel="Add Product Now"
          onAction={handleOpenAdd}
        />
      ) : (
        <div className="card border shadow-sm">
          <div className="table-responsive">
            <table className="table align-middle table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Unit</th>
                  <th>Current Stock</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <div className="d-flex align-items-center gap-2">
                        <div
                          className="rounded bg-light border d-flex align-items-center justify-content-center overflow-hidden"
                          style={{ width: '40px', height: '40px' }}
                        >
                          {p.imageUrl ? (
                            <img src={p.imageUrl} alt={p.name} className="w-100 h-100 object-fit-cover" />
                          ) : (
                            <i className="bi bi-box-seam text-muted" />
                          )}
                        </div>
                        <div>
                          <div className="fw-semibold text-dark small">{p.name}</div>
                          {p.sku && <span className="text-muted" style={{ fontSize: '0.7rem' }}>SKU: {p.sku}</span>}
                        </div>
                      </div>
                    </td>
                    <td className="small text-muted">{p.categoryName}</td>
                    <td className="fw-bold text-dark small">₹{Number(p.price).toFixed(2)}</td>
                    <td className="small text-muted">{p.unit}</td>
                    <td>
                      <span className={`badge ${p.isOutOfStock ? 'bg-danger' : p.isLowStock ? 'bg-warning text-dark' : 'bg-light text-dark border'}`}>
                        {p.availableQuantity} in stock
                      </span>
                    </td>
                    <td>
                      <StatusBadge status={p.active ? 'ACTIVE' : 'INACTIVE'} />
                    </td>
                    <td className="text-end">
                      <div className="btn-group btn-group-sm">
                        <button
                          className="btn btn-outline-secondary"
                          onClick={() => handleToggleActive(p)}
                          title={p.active ? 'Deactivate' : 'Activate'}
                        >
                          <i className={`bi ${p.active ? 'bi-toggle-on text-success' : 'bi-toggle-off text-muted'}`} />
                        </button>
                        <button
                          className="btn btn-outline-secondary"
                          onClick={() => handleOpenEdit(p)}
                          title="Edit"
                        >
                          <i className="bi bi-pencil" />
                        </button>
                        <button
                          className="btn btn-outline-danger"
                          onClick={() => handleDelete(p.id)}
                          title="Archive"
                        >
                          <i className="bi bi-trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add / Edit Product Modal */}
      {showModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  {editingProduct ? 'Edit Product' : 'Add New Product'}
                </h5>
                <button type="button" className="btn-close" onClick={() => setShowModal(false)} />
              </div>

              <form onSubmit={handleSubmit}>
                <div className="modal-body p-4">
                  {errorMsg && (
                    <div className="alert alert-danger py-2 px-3 small mb-3">
                      <i className="bi bi-exclamation-circle-fill me-2" />
                      {errorMsg}
                    </div>
                  )}

                  <div className="row g-3">
                    <div className="col-12 col-md-8">
                      <label className="form-label small fw-medium text-dark">Product Name *</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="e.g. Farm Fresh Whole Milk"
                        value={formData.name}
                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-12 col-md-4">
                      <label className="form-label small fw-medium text-dark">Category *</label>
                      <select
                        className="form-select"
                        value={formData.categoryId}
                        onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                        required
                      >
                        {categories.map((c) => (
                          <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                      </select>
                    </div>

                    <div className="col-6 col-md-4">
                      <label className="form-label small fw-medium text-dark">Price (₹) *</label>
                      <input
                        type="number"
                        step="0.01"
                        min="0.01"
                        className="form-control"
                        placeholder="35.00"
                        value={formData.price}
                        onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-6 col-md-4">
                      <label className="form-label small fw-medium text-dark">Unit (e.g. 500 ml, 1 kg) *</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="500 ml"
                        value={formData.unit}
                        onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                        required
                      />
                    </div>

                    {!editingProduct && (
                      <div className="col-6 col-md-4">
                        <label className="form-label small fw-medium text-dark">Initial Stock Count *</label>
                        <input
                          type="number"
                          min="0"
                          className="form-control"
                          value={formData.initialStock}
                          onChange={(e) => setFormData({ ...formData, initialStock: e.target.value })}
                          required
                        />
                      </div>
                    )}

                    <div className="col-6 col-md-4">
                      <label className="form-label small fw-medium text-dark">Low Stock Alert Threshold</label>
                      <input
                        type="number"
                        min="1"
                        className="form-control"
                        value={formData.lowStockThreshold}
                        onChange={(e) => setFormData({ ...formData, lowStockThreshold: e.target.value })}
                      />
                    </div>

                    <div className="col-12">
                      <label className="form-label small fw-medium text-dark">Description</label>
                      <textarea
                        className="form-control"
                        rows="2"
                        placeholder="Short description of the product..."
                        value={formData.description}
                        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                      />
                    </div>

                    {/* Image Upload per PRD §3.11 & §7.14 */}
                    <div className="col-12">
                      <label className="form-label small fw-medium text-dark">Product Image</label>
                      <div className="d-flex align-items-center gap-3">
                        {formData.imageUrl && (
                          <img
                            src={formData.imageUrl}
                            alt="Preview"
                            className="rounded border object-fit-cover"
                            style={{ width: '60px', height: '60px' }}
                          />
                        )}
                        <input
                          type="file"
                          accept="image/jpeg,image/png,image/webp"
                          className="form-control form-control-sm"
                          onChange={handleImageUpload}
                          disabled={uploadingImage}
                        />
                      </div>
                      <span className="text-muted" style={{ fontSize: '0.75rem' }}>
                        {uploadingImage ? 'Uploading image...' : 'Accepted: JPG, PNG, WebP (max 5MB)'}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="modal-footer border-top">
                  <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setShowModal(false)}>
                    Cancel
                  </button>
                  <LoadingButton type="submit" loading={saving} className="btn-primary btn-sm px-4">
                    {editingProduct ? 'Save Changes' : 'Create Product'}
                  </LoadingButton>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
