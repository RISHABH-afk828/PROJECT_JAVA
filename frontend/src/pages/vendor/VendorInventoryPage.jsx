import React, { useState, useEffect } from 'react';
import apiClient from '../../services/api/apiClient';
import LoadingButton from '../../components/common/LoadingButton';
import Skeleton from '../../components/common/Skeleton';
import EmptyState from '../../components/common/EmptyState';

export default function VendorInventoryPage() {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedItem, setSelectedItem] = useState(null);
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [movements, setMovements] = useState([]);
  const [adjusting, setAdjusting] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  const [adjustForm, setAdjustForm] = useState({
    quantityChange: 10,
    reason: 'RESTOCK',
  });

  useEffect(() => {
    fetchInventory();
  }, []);

  const fetchInventory = async () => {
    setLoading(true);
    try {
      const data = await apiClient.get('/vendors/me/inventory');
      setInventory(data || []);
    } catch {
      setInventory([]);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenAdjust = (item) => {
    setSelectedItem(item);
    setAdjustForm({
      quantityChange: 10,
      reason: 'RESTOCK',
    });
    setErrorMsg(null);
    setShowAdjustModal(true);
  };

  const handleOpenHistory = async (item) => {
    setSelectedItem(item);
    setShowHistoryModal(true);
    try {
      const data = await apiClient.get(`/vendors/me/inventory/${item.productId}/movements`);
      setMovements(data?.content || data || []);
    } catch {
      setMovements([]);
    }
  };

  const handleAdjustSubmit = async (e) => {
    e.preventDefault();
    if (!selectedItem) return;

    const change = Number(adjustForm.quantityChange);
    if (selectedItem.availableQuantity + change < 0) {
      setErrorMsg('Stock cannot be reduced below zero.');
      return;
    }

    setAdjusting(true);
    setErrorMsg(null);
    try {
      await apiClient.patch(`/vendors/me/inventory/${selectedItem.productId}`, {
        quantityChange: change,
        reason: adjustForm.reason,
      });
      setShowAdjustModal(false);
      fetchInventory();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to adjust stock.');
    } finally {
      setAdjusting(false);
    }
  };

  const resultingStock = selectedItem
    ? selectedItem.availableQuantity + Number(adjustForm.quantityChange || 0)
    : 0;

  return (
    <div className="container-xl py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h3 className="fw-bolder text-dark mb-0">Inventory & Stock</h3>
          <p className="text-muted small mb-0">
            Real-time stock levels, reorder thresholds, and movement auditing.
          </p>
        </div>
      </div>

      {loading ? (
        <Skeleton height="300px" />
      ) : inventory.length === 0 ? (
        <EmptyState
          icon="bi-boxes"
          title="No inventory records"
          description="You have not added any products to your catalog yet."
          actionLabel="Go to Products"
          onAction={() => window.location.href = '/vendor/products'}
        />
      ) : (
        <div className="card border shadow-sm">
          <div className="table-responsive">
            <table className="table align-middle table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th>Product</th>
                  <th>Unit</th>
                  <th>Available Quantity</th>
                  <th>Low-Stock Alert Level</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {inventory.map((item) => (
                  <tr key={item.id}>
                    <td className="fw-semibold text-dark">{item.productName}</td>
                    <td className="text-muted small">{item.productUnit}</td>
                    <td>
                      <span className="fw-bold fs-6 text-dark">{item.availableQuantity}</span>
                    </td>
                    <td className="text-muted small">{item.lowStockThreshold} units</td>
                    <td>
                      {item.isOutOfStock ? (
                        <span className="badge bg-danger">Out of Stock</span>
                      ) : item.isLowStock ? (
                        <span className="badge bg-warning text-dark">Low Stock Alert</span>
                      ) : (
                        <span className="badge bg-success-subtle text-success border border-success-subtle">
                          In Stock
                        </span>
                      )}
                    </td>
                    <td className="text-end">
                      <div className="btn-group btn-group-sm">
                        <button
                          className="btn btn-outline-dark"
                          onClick={() => handleOpenAdjust(item)}
                        >
                          <i className="bi bi-plus-slash-minus me-1" />
                          Adjust Stock
                        </button>
                        <button
                          className="btn btn-outline-secondary"
                          onClick={() => handleOpenHistory(item)}
                          title="Movement History"
                        >
                          <i className="bi bi-clock-history" />
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

      {/* Stock Adjustment Modal */}
      {showAdjustModal && selectedItem && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  Adjust Stock — {selectedItem.productName}
                </h5>
                <button type="button" className="btn-close" onClick={() => setShowAdjustModal(false)} />
              </div>

              <form onSubmit={handleAdjustSubmit}>
                <div className="modal-body p-4">
                  {errorMsg && (
                    <div className="alert alert-danger py-2 px-3 small mb-3">
                      <i className="bi bi-exclamation-circle-fill me-2" />
                      {errorMsg}
                    </div>
                  )}

                  <div className="d-flex justify-content-between align-items-center p-3 rounded bg-light mb-3">
                    <div>
                      <span className="text-muted small d-block">Current Stock</span>
                      <h4 className="fw-bold mb-0 text-dark">{selectedItem.availableQuantity}</h4>
                    </div>
                    <i className="bi bi-arrow-right fs-4 text-muted" />
                    <div>
                      <span className="text-muted small d-block">Resulting Stock</span>
                      <h4 className={`fw-bold mb-0 ${resultingStock < 0 ? 'text-danger' : 'text-success'}`}>
                        {resultingStock}
                      </h4>
                    </div>
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-medium text-dark">
                      Quantity Adjustment (positive to add, negative to reduce)
                    </label>
                    <input
                      type="number"
                      className="form-control"
                      value={adjustForm.quantityChange}
                      onChange={(e) => setAdjustForm({ ...adjustForm, quantityChange: e.target.value })}
                      required
                      autoFocus
                    />
                    <div className="d-flex gap-2 mt-2">
                      {[+10, +25, +50, -5].map((preset) => (
                        <button
                          key={preset}
                          type="button"
                          className="btn btn-sm btn-light border"
                          onClick={() => setAdjustForm({ ...adjustForm, quantityChange: preset })}
                        >
                          {preset > 0 ? `+${preset}` : preset}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-medium text-dark">Reason for Adjustment</label>
                    <select
                      className="form-select"
                      value={adjustForm.reason}
                      onChange={(e) => setAdjustForm({ ...adjustForm, reason: e.target.value })}
                    >
                      <option value="RESTOCK">Restock (New shipment received)</option>
                      <option value="MANUAL_ADJUSTMENT">Manual Inventory Correction</option>
                      <option value="DAMAGED_EXPIRED">Damaged / Expired stock removal</option>
                    </select>
                  </div>
                </div>

                <div className="modal-footer border-top">
                  <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => setShowAdjustModal(false)}>
                    Cancel
                  </button>
                  <LoadingButton
                    type="submit"
                    loading={adjusting}
                    className="btn-primary btn-sm px-4"
                    disabled={resultingStock < 0}
                  >
                    Confirm Adjustment
                  </LoadingButton>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Movement History Modal */}
      {showHistoryModal && selectedItem && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content border-0 shadow" style={{ borderRadius: 'var(--radius-md)' }}>
              <div className="modal-header border-bottom">
                <h5 className="modal-title fw-bold text-dark">
                  Stock Movement Log — {selectedItem.productName}
                </h5>
                <button type="button" className="btn-close" onClick={() => setShowHistoryModal(false)} />
              </div>

              <div className="modal-body p-4">
                {movements.length === 0 ? (
                  <div className="text-center py-4 text-muted small">No movements recorded yet.</div>
                ) : (
                  <div className="table-responsive">
                    <table className="table table-sm align-middle mb-0">
                      <thead className="table-light">
                        <tr>
                          <th>Timestamp</th>
                          <th>Change</th>
                          <th>Resulting Quantity</th>
                          <th>Reason</th>
                        </tr>
                      </thead>
                      <tbody>
                        {movements.map((m) => (
                          <tr key={m.id}>
                            <td className="small text-muted">{new Date(m.createdAt).toLocaleString()}</td>
                            <td>
                              <span className={`fw-bold small ${m.quantityChange > 0 ? 'text-success' : 'text-danger'}`}>
                                {m.quantityChange > 0 ? `+${m.quantityChange}` : m.quantityChange}
                              </span>
                            </td>
                            <td className="small fw-semibold">{m.resultingQuantity}</td>
                            <td>
                              <span className="badge bg-light text-dark border small">{m.reason}</span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              <div className="modal-footer border-top">
                <button type="button" className="btn btn-outline-dark btn-sm" onClick={() => setShowHistoryModal(false)}>
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
