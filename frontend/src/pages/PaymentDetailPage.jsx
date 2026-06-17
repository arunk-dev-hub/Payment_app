import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getPaymentById, updatePaymentStatus } from '../api/paymentsApi';
import { getRefundsByPaymentId } from '../api/refundsApi';
import { useAuth } from '../context/AuthContext';
import PaymentStatusBadge from '../components/PaymentStatusBadge';
import RefundForm from '../components/RefundForm';
import RefundList from '../components/RefundList';

function formatAmount(amount, currency) {
  try {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount);
  } catch {
    return `${amount} ${currency}`;
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString('en-US', {
    weekday: 'short', month: 'short', day: 'numeric',
    year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function methodLabel(method) {
  const labels = {
    CREDIT_CARD: '💳 Credit Card',
    DEBIT_CARD: '🏦 Debit Card',
    PAYPAL: '🅿 PayPal',
    BANK_TRANSFER: '🏛 Bank Transfer',
  };
  return labels[method] ?? method ?? '—';
}

export default function PaymentDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAdmin } = useAuth();

  const [payment, setPayment] = useState(null);
  const [refunds, setRefunds] = useState([]);
  const [paymentLoading, setPaymentLoading] = useState(true);
  const [refundsLoading, setRefundsLoading] = useState(true);
  const [error, setError] = useState('');

  const [showRefundForm, setShowRefundForm] = useState(false);
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [confirmingStatus, setConfirmingStatus] = useState(null);

  // Auto-reset confirmation status after 3 seconds
  useEffect(() => {
    if (!confirmingStatus) return;
    const timeoutId = setTimeout(() => setConfirmingStatus(null), 3000);
    return () => clearTimeout(timeoutId);
  }, [confirmingStatus]);

  // Fetch payment
  useEffect(() => {
    const fetchPayment = async () => {
      setPaymentLoading(true);
      try {
        const res = await getPaymentById(id);
        setPayment(res.data);
      } catch (err) {
        if (err.response?.status === 404) setError('Payment not found.');
        else setError('Failed to load payment details.');
      } finally {
        setPaymentLoading(false);
      }
    };
    fetchPayment();
  }, [id]);

  // Fetch refunds
  const fetchRefunds = async () => {
    setRefundsLoading(true);
    try {
      const res = await getRefundsByPaymentId(id);
      setRefunds(res.data ?? []);
    } catch {
      setRefunds([]);
    } finally {
      setRefundsLoading(false);
    }
  };

  useEffect(() => {
    fetchRefunds();
  }, [id]);

  const handleStatusUpdate = async (newStatus) => {
    if (confirmingStatus !== newStatus) {
      setConfirmingStatus(newStatus);
      return;
    }
    setConfirmingStatus(null);
    setStatusUpdating(true);
    try {
      const res = await updatePaymentStatus(id, newStatus);
      setPayment(res.data);
      toast.success(`✓ Status updated to ${newStatus}`);
    } catch (err) {
      const msg = err.response?.data?.message ?? 'Failed to update status';
      toast.error(`✕ ${msg}`);
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleRefundSuccess = (refundData) => {
    setShowRefundForm(false);
    fetchRefunds();
  };

  const copyId = () => {
    navigator.clipboard.writeText(payment?.id ?? '');
    toast.info('Payment ID copied!');
  };

  if (paymentLoading) {
    return (
      <div className="page-container">
        <div className="loading-center">
          <div className="spinner" />
          <span style={{ color: 'var(--text-muted)' }}>Loading payment…</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-container">
        <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>
        <div className="alert alert-error" style={{ maxWidth: 480 }}>{error}</div>
      </div>
    );
  }

  const canRefund = payment?.status === 'COMPLETED';
  const canUpdateStatus = isAdmin && payment?.status === 'PENDING';

  return (
    <div className="page-container">
      <button className="back-btn" onClick={() => navigate(-1)} id="btn-back-from-detail">
        ← Back
      </button>

      {/* Header */}
      <div className="page-header animate-fadeInUp">
        <div>
          <h1 className="page-title" style={{ fontSize: 'var(--text-2xl)' }}>
            Payment Detail
          </h1>
          <button className="id-chip" onClick={copyId} title="Click to copy" id="btn-copy-id" style={{ marginTop: 'var(--space-2)', cursor: 'pointer' }}>
            📋 {payment?.id}
          </button>
        </div>
        <PaymentStatusBadge status={payment?.status} />
      </div>

      {/* Amount Hero */}
      <div className="card animate-fadeInUp" style={{ marginBottom: 'var(--space-5)' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 'var(--space-4)' }}>
          <div>
            <div style={{ fontSize: 'var(--text-xs)', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 'var(--space-2)' }}>
              Amount
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 'var(--space-2)' }}>
              <span className="amount-display">
                {formatAmount(payment?.amount, payment?.currency)}
              </span>
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)', marginBottom: 'var(--space-1)' }}>Method</div>
            <div style={{ fontSize: 'var(--text-lg)', fontWeight: 600 }}>
              {methodLabel(payment?.paymentMethod)}
            </div>
          </div>
        </div>

        <div className="divider" />

        {/* Detail Grid */}
        <div className="detail-grid">
          <div className="detail-field">
            <span className="detail-field-label">Status</span>
            <PaymentStatusBadge status={payment?.status} />
          </div>
          <div className="detail-field">
            <span className="detail-field-label">Currency</span>
            <span className="detail-field-value">{payment?.currency}</span>
          </div>
          <div className="detail-field">
            <span className="detail-field-label">Created At</span>
            <span className="detail-field-value" style={{ fontSize: 'var(--text-sm)', color: 'var(--text-secondary)' }}>
              {formatDate(payment?.createdAt)}
            </span>
          </div>
          <div className="detail-field">
            <span className="detail-field-label">Updated At</span>
            <span className="detail-field-value" style={{ fontSize: 'var(--text-sm)', color: 'var(--text-secondary)' }}>
              {formatDate(payment?.updatedAt)}
            </span>
          </div>
          {payment?.idempotencyKey && (
            <div className="detail-field" style={{ gridColumn: '1 / -1' }}>
              <span className="detail-field-label">Idempotency Key</span>
              <span className="detail-field-value monospace">{payment?.idempotencyKey}</span>
            </div>
          )}
        </div>

        {/* Admin: Status Update */}
        {canUpdateStatus && (
          <>
            <div className="divider" />
            <div>
              <div style={{ fontSize: 'var(--text-sm)', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 'var(--space-3)' }}>
                🔧 Update Status (Admin)
              </div>
              <div className="status-update-section">
                <span className="status-update-label">Move to:</span>
                <button
                  className="btn btn-success btn-sm"
                  onClick={() => handleStatusUpdate('COMPLETED')}
                  disabled={statusUpdating}
                  id="btn-status-completed"
                  style={{ minWidth: '140px' }}
                >
                  {statusUpdating ? (
                    <span className="spinner spinner-sm" />
                  ) : confirmingStatus === 'COMPLETED' ? (
                    '⚠️ Confirm?'
                  ) : (
                    '✓ Mark Completed'
                  )}
                </button>
                <button
                  className="btn btn-danger btn-sm"
                  onClick={() => handleStatusUpdate('FAILED')}
                  disabled={statusUpdating}
                  id="btn-status-failed"
                  style={{ minWidth: '140px' }}
                >
                  {statusUpdating ? (
                    <span className="spinner spinner-sm" />
                  ) : confirmingStatus === 'FAILED' ? (
                    '⚠️ Confirm?'
                  ) : (
                    '✕ Mark Failed'
                  )}
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Refunds Section */}
      <div className="card animate-fadeInUp">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-5)', flexWrap: 'wrap', gap: 'var(--space-3)' }}>
          <h2 className="section-heading" style={{ margin: 0, flex: 1 }}>
            ↩ Refunds
            {refunds.length > 0 && (
              <span style={{
                marginLeft: 'var(--space-2)',
                padding: '2px 8px',
                background: 'var(--bg-glass)',
                borderRadius: 'var(--radius-full)',
                fontSize: 'var(--text-xs)',
                color: 'var(--text-muted)',
                border: '1px solid var(--border-subtle)',
                fontWeight: 600,
              }}>
                {refunds.length}
              </span>
            )}
          </h2>

          {canRefund && !showRefundForm && (
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => setShowRefundForm(true)}
              id="btn-show-refund-form"
            >
              + Create Refund
            </button>
          )}

          {payment?.status === 'PENDING' && (
            <span style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)' }}>
              Refunds are available after payment is completed
            </span>
          )}

          {payment?.status === 'FAILED' && (
            <span style={{ fontSize: 'var(--text-xs)', color: 'var(--color-danger)' }}>
              Cannot refund a failed payment
            </span>
          )}
        </div>

        {/* Refund Form (inline) */}
        {showRefundForm && (
          <div style={{
            padding: 'var(--space-5)',
            background: 'var(--bg-glass)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--color-danger-border)',
            marginBottom: 'var(--space-5)',
            animation: 'fadeInUp 0.3s ease',
          }}>
            <div style={{ fontSize: 'var(--text-base)', fontWeight: 700, marginBottom: 'var(--space-4)', color: 'var(--text-primary)' }}>
              New Refund
            </div>
            <RefundForm
              payment={payment}
              onSuccess={handleRefundSuccess}
              onCancel={() => setShowRefundForm(false)}
            />
          </div>
        )}

        {/* Refund List */}
        <RefundList
          refunds={refunds}
          currency={payment?.currency}
          loading={refundsLoading}
        />
      </div>
    </div>
  );
}
