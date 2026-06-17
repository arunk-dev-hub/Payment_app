import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/', label: 'Dashboard', icon: '⬡', exact: true },
  { to: '/payments', label: 'All Payments', icon: '◈', adminOnly: true },
  { to: '/payments/new', label: 'New Payment', icon: '✦' },
];

export default function Sidebar() {
  const { auth, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = auth?.username?.slice(0, 2).toUpperCase() ?? 'U';

  return (
    <aside className="sidebar" role="navigation" aria-label="Main navigation">
      {/* Brand */}
      <div className="sidebar-brand">
        <div className="sidebar-logo">
          <div className="sidebar-logo-icon">💳</div>
          <div className="sidebar-logo-text">
            <span className="sidebar-logo-name">PayFlow</span>
            <span className="sidebar-logo-sub">Payment Service</span>
          </div>
        </div>
      </div>

      {/* Nav Links */}
      <nav className="sidebar-nav">
        <span className="sidebar-section-label">Navigation</span>

        {navItems.map((item) => {
          if (item.adminOnly && !isAdmin) return null;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.exact}
              className={({ isActive }) =>
                `sidebar-link${isActive ? ' active' : ''}`
              }
              id={`nav-${item.label.toLowerCase().replace(/\s+/g, '-')}`}
            >
              <span className="sidebar-link-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          );
        })}
      </nav>

      {/* User Footer */}
      <div className="sidebar-footer">
        <div className="sidebar-user-card">
          <div className="sidebar-avatar">{initials}</div>
          <div className="sidebar-user-info">
            <div className="sidebar-username">{auth?.username}</div>
            <div className="sidebar-role">{auth?.role}</div>
          </div>
          <button
            className="sidebar-logout-btn"
            onClick={handleLogout}
            title="Logout"
            id="btn-logout"
          >
            ⏻
          </button>
        </div>
      </div>
    </aside>
  );
}
