import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createPayment } from '../api/paymentsApi';

const CURRENCIES = ['USD', 'EUR', 'GBP', 'INR', 'CAD', 'AUD'];
const METHODS = [
  { value: 'CREDIT_CARD', label: '💳 Credit Card' },
  { value: 'DEBIT_CARD', label: '🏦 Debit Card' },
  { value: 'PAYPAL', label: '🅿 PayPal' },
  { value: 'BANK_TRANSFER', label: '🏛 Bank Transfer' },
];

export default function PaymentForm({ onSuccess, onCancel }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    amount: '',
    currency: 'USD',
    paymentMethod: 'CREDIT_CARD',
    idempotencyKey: '',
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const e = {};
    if (!form.amount || isNaN(Number(form.amount))) e.amount = 'Enter a valid amount';
    else if (Number(form.amount) <= 0) e.amount = 'Amount must be positive';
    if (!form.currency) e.currency = 'Currency is required';
    if (!form.paymentMethod) e.paymentMethod = 'Payment method is required';
    return e;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length) {
      setErrors(validationErrors);
      return;
    }

    setLoading(true);
    setErrors({});

    try {
      const payload = {
        amount: parseFloat(form.amount),
        currency: form.currency,
        paymentMethod: form.paymentMethod,
      };

      const res = await createPayment(payload, form.idempotencyKey || null);
      toast.success(`✓ Payment created! ID: ${res.data.id.slice(0, 8)}…`);

      if (onSuccess) {
        onSuccess(res.data);
      } else {
        navigate(`/payments/${res.data.id}`);
      }
    } catch (err) {
      const msg = err.response?.data?.message ?? err.message ?? 'Failed to create payment';
      toast.error(`✕ ${msg}`);
      setErrors({ submit: msg });
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    setErrors((prev) => ({ ...prev, [field]: undefined }));
  };

  return (
    <form onSubmit={handleSubmit} className="animate-fadeInUp" id="payment-form" noValidate>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>

        {/* Amount + Currency row */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 140px', gap: 'var(--space-3)' }}>
          <div className="form-group">
            <label className="form-label" htmlFor="amount">Amount *</label>
            <input
              id="amount"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              className="form-input"
              value={form.amount}
              onChange={handleChange('amount')}
              autoFocus
            />
            {errors.amount && <span className="form-error">{errors.amount}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="currency">Currency *</label>
            <select
              id="currency"
              className="form-select"
              value={form.currency}
              onChange={handleChange('currency')}
            >
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
            {errors.currency && <span className="form-error">{errors.currency}</span>}
          </div>
        </div>

        {/* Payment Method */}
        <div className="form-group">
          <label className="form-label" htmlFor="paymentMethod">Payment Method *</label>
          <select
            id="paymentMethod"
            className="form-select"
            value={form.paymentMethod}
            onChange={handleChange('paymentMethod')}
          >
            {METHODS.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
          {errors.paymentMethod && <span className="form-error">{errors.paymentMethod}</span>}
        </div>

        {/* Idempotency Key (optional) */}
        <div className="form-group">
          <label className="form-label" htmlFor="idempotencyKey">
            Idempotency Key <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(optional)</span>
          </label>
          <input
            id="idempotencyKey"
            type="text"
            placeholder="e.g. order-12345-retry-1"
            className="form-input"
            value={form.idempotencyKey}
            onChange={handleChange('idempotencyKey')}
          />
          <span style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)' }}>
            Prevents duplicate payments if the request is retried.
          </span>
        </div>

        {errors.submit && (
          <div className="alert alert-error">{errors.submit}</div>
        )}

        {/* Actions */}
        <div style={{ display: 'flex', gap: 'var(--space-3)', justifyContent: 'flex-end', marginTop: 'var(--space-2)' }}>
          {onCancel && (
            <button type="button" className="btn btn-secondary" onClick={onCancel} id="btn-cancel-payment">
              Cancel
            </button>
          )}
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            id="btn-submit-payment"
          >
            {loading ? (
              <>
                <span className="spinner spinner-sm" style={{ borderTopColor: 'white' }} />
                Processing…
              </>
            ) : (
              '💳 Create Payment'
            )}
          </button>
        </div>
      </div>
    </form>
  );
}
