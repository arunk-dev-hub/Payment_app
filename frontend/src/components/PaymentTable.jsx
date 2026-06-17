import { useNavigate } from 'react-router-dom';
import PaymentStatusBadge from './PaymentStatusBadge';

function formatAmount(amount, currency) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function methodIcon(method) {
  const icons = {
    CREDIT_CARD: '💳',
    DEBIT_CARD: '🏦',
    PAYPAL: '🅿',
    BANK_TRANSFER: '🏛',
  };
  return icons[method] ?? '💰';
}

export default function PaymentTable({ payments, loading, onPageChange, page, totalPages }) {
  const navigate = useNavigate();

  if (loading) {
    return (
      <div className="loading-center">
        <div className="spinner" />
        <span style={{ color: 'var(--text-muted)', fontSize: 'var(--text-sm)' }}>Loading payments…</span>
      </div>
    );
  }

  if (!payments?.length) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">📭</div>
        <h3>No payments found</h3>
        <p>Try adjusting your filters or create a new payment.</p>
      </div>
    );
  }

  return (
    <>
      <div className="table-wrapper">
        <table className="table" id="payments-table">
          <thead>
            <tr>
              <th>Method</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Currency</th>
              <th>Created</th>
              <th>ID</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((p) => (
              <tr
                key={p.id}
                className="clickable animate-fadeIn"
                onClick={() => navigate(`/payments/${p.id}`)}
                tabIndex={0}
                onKeyDown={(e) => e.key === 'Enter' && navigate(`/payments/${p.id}`)}
                title={`View payment ${p.id}`}
              >
                <td>
                  <span style={{ fontSize: '1.2rem', marginRight: '0.5rem' }}>
                    {methodIcon(p.paymentMethod)}
                  </span>
                  <span style={{ fontSize: 'var(--text-xs)', color: 'var(--text-secondary)' }}>
                    {p.paymentMethod?.replace('_', ' ')}
                  </span>
                </td>
                <td>
                  <strong style={{ color: 'var(--text-primary)' }}>
                    {formatAmount(p.amount, p.currency)}
                  </strong>
                </td>
                <td>
                  <PaymentStatusBadge status={p.status} />
                </td>
                <td>
                  <span style={{
                    padding: '2px 8px',
                    background: 'var(--bg-glass)',
                    borderRadius: 'var(--radius-full)',
                    fontSize: 'var(--text-xs)',
                    fontWeight: 600,
                    border: '1px solid var(--border-subtle)',
                  }}>
                    {p.currency}
                  </span>
                </td>
                <td style={{ color: 'var(--text-secondary)', fontSize: 'var(--text-sm)' }}>
                  {formatDate(p.createdAt)}
                </td>
                <td>
                  <span className="id-chip">{p.id?.slice(0, 8)}…</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button
            className="pagination-btn"
            disabled={page === 0}
            onClick={() => onPageChange(page - 1)}
            id="btn-prev-page"
          >
            ← Prev
          </button>

          {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
            const pageNum = i;
            return (
              <button
                key={pageNum}
                className={`pagination-btn${page === pageNum ? ' active' : ''}`}
                onClick={() => onPageChange(pageNum)}
                id={`btn-page-${pageNum}`}
              >
                {pageNum + 1}
              </button>
            );
          })}

          <button
            className="pagination-btn"
            disabled={page >= totalPages - 1}
            onClick={() => onPageChange(page + 1)}
            id="btn-next-page"
          >
            Next →
          </button>
        </div>
      )}
    </>
  );
}
