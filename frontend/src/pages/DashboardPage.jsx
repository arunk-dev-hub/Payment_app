import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getAllPayments } from '../api/paymentsApi';
import StatCard from '../components/StatCard';
import PaymentTable from '../components/PaymentTable';

export default function DashboardPage() {
  const { isAdmin, auth } = useAuth();
  const navigate = useNavigate();
  const [recentPayments, setRecentPayments] = useState([]);
  const [stats, setStats] = useState({
    total: 0,
    pending: 0,
    completed: 0,
    failed: 0,
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isAdmin) return; // Only admin can fetch all payments for stats
    const fetchData = async () => {
      setLoading(true);
      try {
        const [allRes, pendingRes, completedRes, failedRes] = await Promise.all([
          getAllPayments({ page: 0, size: 8, sort: 'createdAt,desc' }),
          getAllPayments({ page: 0, size: 1, status: 'PENDING' }),
          getAllPayments({ page: 0, size: 1, status: 'COMPLETED' }),
          getAllPayments({ page: 0, size: 1, status: 'FAILED' }),
        ]);

        setRecentPayments(allRes.data.content ?? []);
        setStats({
          total: allRes.data.totalElements ?? 0,
          pending: pendingRes.data.totalElements ?? 0,
          completed: completedRes.data.totalElements ?? 0,
          failed: failedRes.data.totalElements ?? 0,
        });
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [isAdmin]);

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header animate-fadeInUp">
        <div>
          <h1 className="page-title">
            {isAdmin ? '📊 Dashboard' : `👋 Hello, ${auth?.username}`}
          </h1>
          <p className="page-subtitle">
            {isAdmin
              ? 'Overview of your payment service activity'
              : 'Manage your payments and refunds'}
          </p>
        </div>
      </div>

      {/* Stats Grid — Admin only */}
      {isAdmin && (
        <div className="stat-grid stagger">
          <StatCard
            label="Total Payments"
            value={loading ? '…' : stats.total}
            icon="💳"
            colorClass="indigo"
            footer="All time"
          />
          <StatCard
            label="Pending"
            value={loading ? '…' : stats.pending}
            icon="⏳"
            colorClass="yellow"
            footer="Awaiting completion"
          />
          <StatCard
            label="Completed"
            value={loading ? '…' : stats.completed}
            icon="✅"
            colorClass="green"
            footer="Successfully processed"
          />
          <StatCard
            label="Failed"
            value={loading ? '…' : stats.failed}
            icon="❌"
            colorClass="red"
            footer="Need attention"
          />
        </div>
      )}

      {/* Quick Actions */}
      <div className="quick-actions animate-fadeInUp">
        <button
          className="quick-action-btn"
          onClick={() => navigate('/payments/new')}
          id="btn-new-payment-quick"
        >
          <div className="quick-action-icon">✦</div>
          New Payment
        </button>

        {isAdmin && (
          <button
            className="quick-action-btn"
            onClick={() => navigate('/payments')}
            id="btn-all-payments-quick"
          >
            <div className="quick-action-icon">◈</div>
            All Payments
          </button>
        )}
      </div>

      {/* Recent Payments Table — Admin only */}
      {isAdmin && (
        <>
          <h2 className="section-heading animate-fadeInUp">Recent Payments</h2>
          <div className="card animate-fadeInUp">
            <PaymentTable
              payments={recentPayments}
              loading={loading}
              page={0}
              totalPages={1}
            />
          </div>
        </>
      )}

      {/* USER view — helpful info */}
      {!isAdmin && (
        <div className="card animate-fadeInUp" style={{ marginTop: 'var(--space-4)' }}>
          <h2 style={{ fontSize: 'var(--text-xl)', fontWeight: 700, marginBottom: 'var(--space-4)' }}>
            Getting Started
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)', color: 'var(--text-secondary)', fontSize: 'var(--text-sm)' }}>
            <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-start' }}>
              <span style={{ fontSize: '1.2rem' }}>💳</span>
              <div>
                <strong style={{ color: 'var(--text-primary)' }}>Create a Payment</strong>
                <p style={{ marginTop: '2px' }}>Use the "New Payment" button to initiate a payment with Credit Card, Debit Card, PayPal, or Bank Transfer.</p>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-start' }}>
              <span style={{ fontSize: '1.2rem' }}>↩</span>
              <div>
                <strong style={{ color: 'var(--text-primary)' }}>Request a Refund</strong>
                <p style={{ marginTop: '2px' }}>Open any payment and use the refund form to request a partial or full refund.</p>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-start' }}>
              <span style={{ fontSize: '1.2rem' }}>🔍</span>
              <div>
                <strong style={{ color: 'var(--text-primary)' }}>Track Status</strong>
                <p style={{ marginTop: '2px' }}>Each payment shows a real-time status: Pending, Completed, or Failed.</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
