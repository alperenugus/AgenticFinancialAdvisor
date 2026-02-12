import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// User Profile API
export const userProfileAPI = {
  get: (userId) => api.get(`/profile/${userId}`),
  create: (profile) => api.post('/profile', profile),
  update: (userId, profile) => api.put(`/profile/${userId}`, profile),
};

// Portfolio API
export const portfolioAPI = {
  get: (userId) => api.get(`/portfolio/${userId}`),
  addHolding: (userId, holding) => api.post(`/portfolio/${userId}/holdings`, holding),
  removeHolding: (userId, holdingId) => api.delete(`/portfolio/${userId}/holdings/${holdingId}`),
  refresh: (userId) => api.post(`/portfolio/${userId}/refresh`),
};

// Advisor API
export const advisorAPI = {
  analyze: (userId, query, sessionId) => 
    api.post('/advisor/analyze', { userId, query, sessionId }),
  getRecommendations: (userId) => api.get(`/advisor/recommendations/${userId}`),
  getRecommendation: (userId, symbol) => 
    api.get(`/advisor/recommendations/${userId}/${symbol}`),
  getStatus: () => api.get('/advisor/status'),
};

export default api;

