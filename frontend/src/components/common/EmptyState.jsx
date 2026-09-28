import React from 'react';

export default function EmptyState({
  icon = 'bi-inbox',
  title = 'Nothing here yet',
  description = 'No items found matching your criteria.',
  actionLabel,
  onAction,
  className = '',
}) {
  return (
    <div className={`text-center py-5 px-3 ${className}`}>
      <div
        className="d-inline-flex align-items-center justify-content-center mb-3 rounded-circle"
        style={{
          width: '64px',
          height: '64px',
          backgroundColor: 'var(--color-surface-strong)',
          color: 'var(--color-text-secondary)',
          fontSize: '1.75rem',
        }}
      >
        <i className={`bi ${icon}`} />
      </div>
      <h5 className="fw-semibold text-dark mb-1">{title}</h5>
      <p className="text-muted small mb-3 mx-auto" style={{ maxWidth: '380px' }}>
        {description}
      </p>
      {actionLabel && onAction && (
        <button className="btn btn-outline-primary btn-sm px-3" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
