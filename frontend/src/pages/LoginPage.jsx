import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.username || !form.password) {
      setError('Please enter both username and password');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await login(form.username, form.password);
      toast.success(`Welcome back, ${form.username}! 🎉`);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message ?? 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const fillCredentials = (username, password) => {
    setForm({ username, password });
    setError('');
  };

  return (
    <div className="login-page">
      <div className="login-card">
        {/* Logo */}
        <div className="login-logo">
          <div className="login-logo-icon">💳</div>
          <div className="login-logo-text">
            <span className="login-logo-name">PayFlow</span>
            <span className="login-logo-tagline">Payment Service Dashboard</span>
          </div>
        </div>

        <h1 className="login-heading">Sign In</h1>
        <p className="login-subheading">Enter your credentials to continue</p>

        <form className="login-form" onSubmit={handleSubmit} id="login-form" noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              className="form-input"
              placeholder="Enter username"
              value={form.username}
              onChange={(e) => setForm((p) => ({ ...p, username: e.target.value }))}
              autoFocus
              autoComplete="username"
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className="form-input"
              placeholder="Enter password"
              value={form.password}
              onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
              autoComplete="current-password"
            />
          </div>

          {error && (
            <div className="alert alert-error" role="alert">
              ⚠ {error}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-lg w-full"
            disabled={loading}
            id="btn-login"
            style={{ marginTop: 'var(--space-2)' }}
          >
            {loading ? (
              <>
                <span className="spinner spinner-sm" style={{ borderTopColor: 'white' }} />
                Signing in…
              </>
            ) : (
              'Sign In →'
            )}
          </button>
        </form>

        {/* Demo credentials */}
        <div className="login-hint">
          <div style={{ marginBottom: 'var(--space-2)', fontWeight: 600, color: 'var(--text-secondary)' }}>
            Demo Credentials
          </div>
          <div className="login-hint-row">
            <button
              type="button"
              onClick={() => fillCredentials('admin', 'admin123')}
              style={{ background: 'none', border: 'none', cursor: 'pointer', textAlign: 'left', width: '100%' }}
              id="btn-fill-admin"
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0' }}>
                <span style={{ color: 'var(--text-secondary)' }}>
                  <strong>admin</strong> / admin123
                </span>
                <span className="login-hint-role admin">ADMIN</span>
              </div>
            </button>
          </div>
          <div className="login-hint-row">
            <button
              type="button"
              onClick={() => fillCredentials('user', 'password')}
              style={{ background: 'none', border: 'none', cursor: 'pointer', textAlign: 'left', width: '100%' }}
              id="btn-fill-user"
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0' }}>
                <span style={{ color: 'var(--text-secondary)' }}>
                  <strong>user</strong> / password
                </span>
                <span className="login-hint-role user">USER</span>
              </div>
            </button>
          </div>
          <div style={{ marginTop: 'var(--space-2)', color: 'var(--text-muted)', fontSize: '0.7rem' }}>
            Click a row to auto-fill credentials
          </div>
        </div>
      </div>
    </div>
  );
}
