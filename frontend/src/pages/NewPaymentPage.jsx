import { useNavigate } from 'react-router-dom';
import PaymentForm from '../components/PaymentForm';

export default function NewPaymentPage() {
  const navigate = useNavigate();

  return (
    <div className="page-container">
      <button className="back-btn" onClick={() => navigate(-1)} id="btn-back-from-new">
        ← Back
      </button>

      <div className="page-header animate-fadeInUp">
        <div>
          <h1 className="page-title">New Payment</h1>
          <p className="page-subtitle">Create a new payment transaction</p>
        </div>
      </div>

      <div style={{ maxWidth: '560px' }}>
        <div className="card animate-fadeInUp">
          <PaymentForm onCancel={() => navigate(-1)} />
        </div>
      </div>
    </div>
  );
}
