import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import axios from 'axios';

const PAGE_TITLES = {
  '/': 'Dashboard',
  '/payments': 'All Payments',
  '/payments/new': 'New Payment',
};

function getTitle(pathname) {
  if (pathname.startsWith('/payments/') && pathname !== '/payments/new') {
    return 'Payment Detail';
  }
  return PAGE_TITLES[pathname] ?? 'PayFlow';
}

export default function Navbar() {
  const location = useLocation();
  const [health, setHealth] = useState('checking'); // 'up' | 'down' | 'checking'
  const title = getTitle(location.pathname);

  useEffect(() => {
    let cancelled = false;

    const checkHealth = async () => {
      try {
        const res = await axios.get('/actuator/health');
        if (!cancelled) {
          setHealth(res.data?.status === 'UP' ? 'up' : 'down');
        }
      } catch {
        if (!cancelled) setHealth('down');
      }
    };

    checkHealth();
    const interval = setInterval(checkHealth, 30000);

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  const healthLabel = health === 'up' ? 'API Online' : health === 'down' ? 'API Offline' : 'Checking…';

  return (
    <header className="navbar" role="banner">
      <div className="navbar-left">
        <span className="navbar-title">{title}</span>
        <span className="navbar-breadcrumb">PayFlow / {title}</span>
      </div>
      <div className="navbar-right">
        <div className="health-indicator" title={`Backend status: ${healthLabel}`} id="health-indicator">
          <span className={`health-dot ${health}`} />
          <span>{healthLabel}</span>
        </div>
      </div>
    </header>
  );
}
