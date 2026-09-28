import React from 'react';

export default function LoadingButton({
  loading = false,
  children,
  disabled = false,
  className = 'btn-primary',
  type = 'button',
  onClick,
  ...props
}) {
  return (
    <button
      type={type}
      className={`btn ${className} d-inline-flex align-items-center justify-content-center gap-2`}
      disabled={disabled || loading}
      onClick={onClick}
      {...props}
    >
      {loading && (
        <span
          className="spinner-border spinner-border-sm"
          role="status"
          aria-hidden="true"
        />
      )}
      {children}
    </button>
  );
}
