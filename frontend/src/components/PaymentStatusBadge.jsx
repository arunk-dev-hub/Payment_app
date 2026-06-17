const STATUS_MAP = {
  PENDING: { label: 'Pending', cls: 'badge-pending' },
  COMPLETED: { label: 'Completed', cls: 'badge-completed' },
  FAILED: { label: 'Failed', cls: 'badge-failed' },
};

export default function PaymentStatusBadge({ status }) {
  const { label, cls } = STATUS_MAP[status?.toUpperCase()] ?? {
    label: status ?? 'Unknown',
    cls: 'badge-pending',
  };

  return <span className={`badge ${cls}`}>{label}</span>;
}
