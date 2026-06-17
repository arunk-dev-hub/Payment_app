import PaymentStatusBadge from './PaymentStatusBadge';

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
    month: 'short', day: 'numeric', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export default function RefundList({ refunds, currency, loading }) {
  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)', padding: 'var(--space-4)' }}>
        <div className="spinner spinner-sm" />
        <span style={{ color: 'var(--text-muted)', fontSize: 'var(--text-sm)' }}>Loading refunds…</span>
      </div>
    );
  }

  if (!refunds?.length) {
    return (
      <div className="empty-state" style={{ padding: 'var(--space-8) 0' }}>
        <div className="empty-state-icon">↩</div>
        <h3>No refunds yet</h3>
        <p>Refunds for this payment will appear here.</p>
      </div>
    );
  }

  return (
    <div className="refund-list stagger">
      {refunds.map((refund) => (
        <div key={refund.id} className="refund-item animate-fadeInUp">
          <div className="refund-item-left">
            <div className="refund-item-amount">
              {formatAmount(refund.amount, currency)}
            </div>
            {refund.reason && (
              <div className="refund-item-reason">"{refund.reason}"</div>
            )}
            <div className="refund-item-id">ID: {refund.id?.slice(0, 16)}…</div>
          </div>
          <div className="refund-item-right">
            <PaymentStatusBadge status={refund.status} />
            <span className="refund-item-date">{formatDate(refund.createdAt)}</span>
          </div>
        </div>
      ))}
    </div>
  );
}
