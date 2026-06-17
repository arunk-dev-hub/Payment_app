import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAllPayments } from '../api/paymentsApi';
import PaymentTable from '../components/PaymentTable';

const STATUS_OPTIONS = ['', 'PENDING', 'COMPLETED', 'FAILED'];
const CURRENCY_OPTIONS = ['', 'USD', 'EUR', 'GBP', 'INR', 'CAD', 'AUD'];
const METHOD_OPTIONS = ['', 'CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER'];

export default function PaymentsPage() {
  const navigate = useNavigate();
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [filters, setFilters] = useState({
    status: '',
    currency: '',
    paymentMethod: '',
  });

  const fetchPayments = useCallback(async (currentPage, currentFilters) => {
    setLoading(true);
    setError('');
    try {
      const params = { page: currentPage, size: 10, sort: 'createdAt,desc' };
      if (currentFilters.status) params.status = currentFilters.status;
      if (currentFilters.currency) params.currency = currentFilters.currency;
      if (currentFilters.paymentMethod) params.paymentMethod = currentFilters.paymentMethod;

      const res = await getAllPayments(params);
      setPayments(res.data.content ?? []);
      setTotalPages(res.data.totalPages ?? 0);
      setTotalElements(res.data.totalElements ?? 0);
    } catch (err) {
      setError(err.response?.data?.message ?? 'Failed to load payments');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPayments(page, filters);
  }, [page, filters, fetchPayments]);

  const handleFilterChange = (field) => (e) => {
    const newFilters = { ...filters, [field]: e.target.value };
    setFilters(newFilters);
    setPage(0);
  };

  const clearFilters = () => {
    setFilters({ status: '', currency: '', paymentMethod: '' });
    setPage(0);
  };

  const hasFilters = filters.status || filters.currency || filters.paymentMethod;

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header animate-fadeInUp">
        <div>
          <h1 className="page-title">All Payments</h1>
          <p className="page-subtitle">
            {loading ? 'Loading…' : `${totalElements} total payment${totalElements !== 1 ? 's' : ''}`}
          </p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => navigate('/payments/new')}
          id="btn-new-payment"
        >
          ✦ New Payment
        </button>
      </div>

      {/* Filter Bar */}
      <div className="filter-bar animate-fadeInUp">
        <span className="filter-bar-label">Filter by:</span>

        <select
          className="form-select"
          value={filters.status}
          onChange={handleFilterChange('status')}
          id="filter-status"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>{s || 'All Statuses'}</option>
          ))}
        </select>

        <select
          className="form-select"
          value={filters.currency}
          onChange={handleFilterChange('currency')}
          id="filter-currency"
        >
          {CURRENCY_OPTIONS.map((c) => (
            <option key={c} value={c}>{c || 'All Currencies'}</option>
          ))}
        </select>

        <select
          className="form-select"
          value={filters.paymentMethod}
          onChange={handleFilterChange('paymentMethod')}
          id="filter-method"
        >
          {METHOD_OPTIONS.map((m) => (
            <option key={m} value={m}>{m ? m.replace('_', ' ') : 'All Methods'}</option>
          ))}
        </select>

        {hasFilters && (
          <button className="filter-clear-btn" onClick={clearFilters} id="btn-clear-filters">
            ✕ Clear
          </button>
        )}
      </div>

      {/* Error State */}
      {error && (
        <div className="alert alert-error animate-fadeIn" style={{ marginBottom: 'var(--space-5)' }}>
          ⚠ {error}
          <button
            onClick={() => fetchPayments(page, filters)}
            style={{ marginLeft: 'auto', background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', textDecoration: 'underline' }}
          >
            Retry
          </button>
        </div>
      )}

      {/* Table */}
      <div className="card animate-fadeInUp">
        <PaymentTable
          payments={payments}
          loading={loading}
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}
