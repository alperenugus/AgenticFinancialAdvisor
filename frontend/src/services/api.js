import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance with interceptor for token
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 95000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests if available
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle 401 errors (unauthorized)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  getCurrentUser: (token) => {
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    return api.get('/auth/me', { headers });
  },
  validateToken: (token) => {
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    return api.post('/auth/validate', {}, { headers });
  },
};

// User Profile API
export const userProfileAPI = {
  get: () => api.get('/profile'),
  create: (profile) => api.post('/profile', profile),
  update: (profile) => api.put('/profile', profile),
};

// Portfolio API
export const portfolioAPI = {
  get: () => api.get('/portfolio'),
  addHolding: (holding) => api.post('/portfolio/holdings', holding),
  removeHolding: (holdingId) => api.delete(`/portfolio/holdings/${holdingId}`),
  refresh: () => api.post('/portfolio/refresh'),
};

// Advisor API
export const advisorAPI = {
  analyze: (query, sessionId) => 
    api.post('/advisor/analyze', { query, sessionId }),
  getStatus: () => api.get('/advisor/status'),
};

export default api;

