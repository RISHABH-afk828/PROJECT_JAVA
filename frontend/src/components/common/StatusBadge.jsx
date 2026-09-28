import React from 'react';

const STATUS_CONFIG = {
  ACTIVE: { bg: '#dcfce7', color: '#15803d', label: 'Active' },
  ONLINE: { bg: '#dcfce7', color: '#15803d', label: 'Online' },
  OPEN: { bg: '#dcfce7', color: '#15803d', label: 'Open' },
  PAID: { bg: '#dcfce7', color: '#15803d', label: 'Paid' },
  DELIVERED: { bg: '#dcfce7', color: '#15803d', label: 'Delivered' },
  ACCEPTED: { bg: '#e0e7ff', color: '#4338ca', label: 'Accepted' },
  PREPARING: { bg: '#fef3c7', color: '#b45309', label: 'Preparing' },
  READY: { bg: '#e0f2fe', color: '#0369a1', label: 'Ready' },
  DELIVERY_ASSIGNED: { bg: '#ede9fe', color: '#6d28d9', label: 'Delivery Assigned' },
  OUT_FOR_DELIVERY: { bg: '#fef3c7', color: '#b45309', label: 'Out for Delivery' },
  PENDING: { bg: '#fef3c7', color: '#b45309', label: 'Pending' },
  PENDING_APPROVAL: { bg: '#fef3c7', color: '#b45309', label: 'Pending Approval' },
  VENDOR_PENDING: { bg: '#fef3c7', color: '#b45309', label: 'Awaiting Vendor' },
  OFFLINE: { bg: '#f3f4f6', color: '#4b5563', label: 'Offline' },
  CLOSED: { bg: '#fee2e2', color: '#b91c1c', label: 'Closed' },
  REJECTED: { bg: '#fee2e2', color: '#b91c1c', label: 'Rejected' },
  CANCELLED: { bg: '#fee2e2', color: '#b91c1c', label: 'Cancelled' },
  FAILED: { bg: '#fee2e2', color: '#b91c1c', label: 'Failed' },
  SUSPENDED: { bg: '#fee2e2', color: '#b91c1c', label: 'Suspended' },
  INACTIVE: { bg: '#f3f4f6', color: '#6b7280', label: 'Inactive' },
};

export default function StatusBadge({ status, className = '' }) {
  const config = STATUS_CONFIG[status] || {
    bg: '#f3f4f6',
    color: '#4b5563',
    label: status || 'Unknown',
  };

  return (
    <span
      className={`badge rounded-pill fw-medium d-inline-flex align-items-center gap-1 ${className}`}
      style={{
        backgroundColor: config.bg,
        color: config.color,
        fontSize: '0.75rem',
        padding: '0.35rem 0.65rem',
        letterSpacing: '0.02em',
      }}
    >
      <span
        style={{
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          backgroundColor: config.color,
          display: 'inline-block',
        }}
      />
      {config.label}
    </span>
  );
}
