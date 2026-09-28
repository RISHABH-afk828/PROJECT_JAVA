import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Request interceptor to attach JWT
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle data unwrapping and token refresh/expiration
apiClient.interceptors.response.use(
  (response) => {
    // If backend returns standard ApiResponse envelope
    if (response.data && typeof response.data.success === 'boolean') {
      if (response.data.success) {
        return response.data.data;
      }
    }
    return response.data;
  },
  async (error) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken && !originalRequest.url.includes('/auth/')) {
        originalRequest._retry = true;
        try {
          const res = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
          const newAuthData = res.data?.data || res.data;
          if (newAuthData?.accessToken) {
            localStorage.setItem('token', newAuthData.accessToken);
            apiClient.defaults.headers.common['Authorization'] = `Bearer ${newAuthData.accessToken}`;
            originalRequest.headers['Authorization'] = `Bearer ${newAuthData.accessToken}`;
            return apiClient(originalRequest);
          }
        } catch (refreshErr) {
          localStorage.removeItem('token');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          window.location.href = '/login?expired=true';
          return Promise.reject(refreshErr);
        }
      } else if (!originalRequest.url.includes('/auth/')) {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }

    // Normalize error payload from ApiResponse
    const errData = error.response?.data?.error || {
      code: error.code || 'NETWORK_ERROR',
      message: error.response?.data?.message || error.message || 'Something went wrong. Please check your connection.',
      details: error.response?.data?.details || null,
    };

    return Promise.reject(errData);
  }
);

export default apiClient;
