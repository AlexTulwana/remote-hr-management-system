import Card from './Card';

export default function StatCard({ label, value, variant = 'neutral' }) {
  const valueColor = variant === 'urgent' ? 'text-danger-text' : 'text-text-primary';

  return (
    <Card>
      <div className="text-[12px] text-text-secondary mb-2.5">{label}</div>
      <div className={`text-[22px] font-semibold ${valueColor}`}>{value}</div>
    </Card>
  );
}
