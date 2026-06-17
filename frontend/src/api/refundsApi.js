import api from './axiosInstance';

/**
 * Create a refund for a payment
 * @param {string} paymentId - Payment UUID
 * @param {Object} data - { amount, reason }
 */
export const createRefund = (paymentId, data) =>
  api.post(`/payments/${paymentId}/refunds`, data);

/**
 * Get all refunds for a payment
 * @param {string} paymentId - Payment UUID
 */
export const getRefundsByPaymentId = (paymentId) =>
  api.get(`/payments/${paymentId}/refunds`);
