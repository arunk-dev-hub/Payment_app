import { useState } from 'react';
import { toast } from 'react-toastify';
import { createRefund } from '../api/refundsApi';

export default function RefundForm({ payment, onSuccess, onCancel }) {
  const [form, setForm] = useState({ amount: '', reason: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const maxRefund = parseFloat(payment?.amount ?? 0);

  const validate = () => {
    const e = {};
    const amt = parseFloat(form.amount);
    if (!form.amount || isNaN(amt)) e.amount = 'Enter a valid refund amount';
    else if (amt <= 0) e.amount = 'Amount must be positive';
    else if (amt > maxRefund) e.amount = `Cannot exceed original payment (${maxRefund})`;
    if (form.reason && form.reason.length > 255) e.reason = 'Reason must be 255 characters or fewer';
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
    try {
      const res = await createRefund(payment.id, {
        amount: parseFloat(form.amount),
        reason: form.reason || undefined,
      });
      toast.success(`✓ Refund of ${form.amount} ${payment.currency} created!`);
      onSuccess?.(res.data);
      setForm({ amount: '', reason: '' });
    } catch (err) {
      const msg = err.response?.data?.message ?? err.message ?? 'Refund failed';
      toast.error(`✕ ${msg}`);
      setErrors({ submit: msg });
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field) => (e) => {
    setForm((p) => ({ ...p, [field]: e.target.value }));
    setErrors((p) => ({ ...p, [field]: undefined }));
  };

  return (
    <form onSubmit={handleSubmit} id="refund-form" noValidate>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 'var(--space-3)', alignItems: 'start' }}>
          <div className="form-group">
            <label className="form-label" htmlFor="refund-amount">
              Refund Amount *
              <span style={{ marginLeft: 'var(--space-2)', color: 'var(--text-muted)', fontWeight: 400 }}>
                (max: {maxRefund} {payment?.currency})
              </span>
            </label>
            <input
              id="refund-amount"
              type="number"
              step="0.01"
              min="0.01"
              max={maxRefund}
              placeholder="0.00"
              className="form-input"
              value={form.amount}
              onChange={handleChange('amount')}
              autoFocus
            />
            {errors.amount && <span className="form-error">{errors.amount}</span>}
          </div>
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="refund-reason">
            Reason <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(optional)</span>
          </label>
          <textarea
            id="refund-reason"
            className="form-input form-textarea"
            placeholder="e.g. Customer requested cancellation"
            value={form.reason}
            onChange={handleChange('reason')}
            rows={2}
            maxLength={255}
            style={{ resize: 'vertical', minHeight: '72px' }}
          />
          <span style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)', textAlign: 'right' }}>
            {form.reason.length}/255
          </span>
          {errors.reason && <span className="form-error">{errors.reason}</span>}
        </div>

        {errors.submit && (
          <div className="alert alert-error">{errors.submit}</div>
        )}

        <div style={{ display: 'flex', gap: 'var(--space-3)', justifyContent: 'flex-end' }}>
          {onCancel && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={onCancel} id="btn-cancel-refund">
              Cancel
            </button>
          )}
          <button
            type="submit"
            className="btn btn-danger btn-sm"
            disabled={loading}
            id="btn-submit-refund"
          >
            {loading ? (
              <>
                <span className="spinner spinner-sm" />
                Processing…
              </>
            ) : (
              '↩ Submit Refund'
            )}
          </button>
        </div>
      </div>
    </form>
  );
}
