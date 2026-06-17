import axios from 'axios';

// Base instance — credentials injected by interceptor from AuthContext
const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// The interceptor is set up externally (from AuthContext) after login
export default api;
