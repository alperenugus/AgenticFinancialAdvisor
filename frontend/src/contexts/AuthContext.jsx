import { createContext, useContext, useState, useEffect } from 'react';
import { authAPI } from '../services/api';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(() => {
    return localStorage.getItem('token');
  });

  useEffect(() => {
    // Check for token in URL (from OAuth callback).
    // Prefer fragment token to avoid token leakage via query strings.
    const urlParams = new URLSearchParams(window.location.search);
    const hashParams = new URLSearchParams(
      window.location.hash.startsWith('#')
        ? window.location.hash.substring(1)
        : window.location.hash
    );
    const tokenFromUrl = hashParams.get('token') || urlParams.get('token');
    
    if (tokenFromUrl) {
      setToken(tokenFromUrl);
      localStorage.setItem('token', tokenFromUrl);
      // Clean up URL and normalize callback path after token extraction.
      const normalizedPath = window.location.pathname === '/auth/callback'
        ? '/'
        : window.location.pathname;
      window.history.replaceState({}, document.title, normalizedPath);
    }

    // Load user if token exists
    if (token || tokenFromUrl) {
      loadUser(token || tokenFromUrl);
    } else {
      setLoading(false);
    }
  }, []);

  const loadUser = async (authToken) => {
    try {
      const response = await authAPI.getCurrentUser(authToken);
      setUser(response.data);
      setToken(authToken);
    } catch (error) {
      console.error('Error loading user:', error);
      logout();
    } finally {
      setLoading(false);
    }
  };

  const login = () => {
    // Redirect to backend OAuth2 endpoint
    const backendUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    window.location.href = `${backendUrl}/oauth2/authorization/google`;
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('token');
  };

  const value = {
    user,
    token,
    loading,
    login,
    logout,
    isAuthenticated: !!user && !!token,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

