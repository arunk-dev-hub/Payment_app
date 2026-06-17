import api from './axiosInstance';

/**
 * Create a new payment
 * @param {Object} data - { amount, currency, paymentMethod }
 * @param {string} [idempotencyKey] - Optional idempotency key
 */
export const createPayment = (data, idempotencyKey = null) => {
  const headers = {};
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;
  return api.post('/payments', data, { headers });
};

/**
 * Get a payment by ID
 * @param {string} id - Payment UUID
 */
export const getPaymentById = (id) => api.get(`/payments/${id}`);

/**
 * Get all payments (paginated + filtered) — ADMIN only
 * @param {Object} params - { status, currency, paymentMethod, page, size }
 */
export const getAllPayments = (params = {}) => api.get('/payments', { params });

/**
 * Update payment status — ADMIN only
 * @param {string} id - Payment UUID
 * @param {string} status - New status: PENDING | COMPLETED | FAILED
 */
export const updatePaymentStatus = (id, status) =>
  api.patch(`/payments/${id}/status`, { status });

/**
 * Fetch health status from actuator
 */
export const getHealth = () => axios_raw_get('/actuator/health');

// Raw axios for health (no auth needed)
async function axios_raw_get(url) {
  const { default: axios } = await import('axios');
  return axios.get(url);
}
