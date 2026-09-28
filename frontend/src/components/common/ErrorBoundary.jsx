import React from 'react';

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light p-3">
          <div className="card shadow-sm border p-4 p-md-5 text-center" style={{ maxWidth: '540px' }}>
            <div className="mb-3">
              <span className="badge bg-danger-subtle text-danger p-3 rounded-circle">
                <i className="bi bi-exclamation-octagon fs-2" />
              </span>
            </div>
            <h4 className="fw-bold mb-2">Something went wrong</h4>
            <p className="text-muted small mb-4">
              An unexpected error occurred while rendering this view. Your session data and cart items are safe.
            </p>
            {this.state.error?.message && (
              <div className="alert alert-light border small text-start text-danger font-monospace mb-4 p-2 text-truncate">
                {this.state.error.message}
              </div>
            )}
            <div className="d-flex justify-content-center gap-2">
              <button onClick={this.handleReload} className="btn btn-outline-dark btn-sm rounded-pill px-4">
                <i className="bi bi-arrow-clockwise me-1" />
                Reload Page
              </button>
              <button onClick={this.handleGoHome} className="btn btn-primary btn-sm rounded-pill px-4">
                Return Home
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
