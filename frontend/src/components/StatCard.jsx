export default function StatCard({ label, value, icon, colorClass = 'indigo', footer }) {
  return (
    <div className={`stat-card ${colorClass} animate-fadeInUp`}>
      <div className="stat-card-header">
        <div>
          <div className="stat-card-label">{label}</div>
          <div className="stat-card-value">{value}</div>
        </div>
        <div className="stat-card-icon">{icon}</div>
      </div>
      {footer && <div className="stat-card-footer">{footer}</div>}
    </div>
  );
}
