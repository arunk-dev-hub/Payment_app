import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import api from '../api/axiosInstance';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    // Restore from sessionStorage on reload
    try {
      const stored = sessionStorage.getItem('payflow_auth');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  // Inject Basic Auth header into every Axios request
  useEffect(() => {
    const interceptorId = api.interceptors.request.use((config) => {
      if (auth?.credentials) {
        config.headers['Authorization'] = `Basic ${auth.credentials}`;
      }
      return config;
    });

    return () => {
      api.interceptors.request.eject(interceptorId);
    };
  }, [auth]);

  const login = useCallback(async (username, password) => {
    const credentials = btoa(`${username}:${password}`);
    // Verify credentials by hitting a protected endpoint
    const response = await api.get('/payments', {
      params: { page: 0, size: 1 },
      headers: { Authorization: `Basic ${credentials}` },
    });

    // If we get here without throwing, credentials are valid
    // Determine role: admin can list all payments, user can too but let's verify
    let role = 'USER';
    if (username === 'admin') role = 'ADMIN';

    const authData = { username, credentials, role };
    setAuth(authData);
    sessionStorage.setItem('payflow_auth', JSON.stringify(authData));
    return authData;
  }, []);

  // For USER role, the /payments GET returns 403 — try a different check
  const loginWithFallback = useCallback(async (username, password) => {
    const credentials = btoa(`${username}:${password}`);

    // Determine role by username (matches the in-memory config)
    const role = username.toLowerCase() === 'admin' ? 'ADMIN' : 'USER';

    // For admin: verify by listing payments; for user: verify health (public) then trust
    // Actually let's try the health endpoint to verify connectivity and credentials
    // by trying payments first and falling back gracefully
    try {
      await api.get('/payments', {
        params: { page: 0, size: 1 },
        headers: { Authorization: `Basic ${credentials}` },
      });
    } catch (err) {
      if (err.response?.status === 403 && role === 'USER') {
        // USER gets 403 on GET /payments — that's expected. Check a USER-accessible endpoint
        try {
          // Users can't list all payments but the 403 means they ARE authenticated
          // 403 = Forbidden (auth OK, but no permission) — treat as valid user auth
        } catch {
          throw new Error('Invalid credentials');
        }
      } else if (err.response?.status === 401) {
        throw new Error('Invalid username or password');
      } else if (!err.response) {
        throw new Error('Cannot connect to server. Is the backend running?');
      } else if (err.response?.status !== 403) {
        throw err;
      }
    }

    const authData = { username, credentials, role };
    setAuth(authData);
    sessionStorage.setItem('payflow_auth', JSON.stringify(authData));
    return authData;
  }, []);

  const logout = useCallback(() => {
    setAuth(null);
    sessionStorage.removeItem('payflow_auth');
  }, []);

  const isAdmin = auth?.role === 'ADMIN';
  const isAuthenticated = !!auth;

  return (
    <AuthContext.Provider value={{ auth, login: loginWithFallback, logout, isAdmin, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
