export default function Pill({ children, variant = 'neutral' }) {
  const variants = {
    success: 'bg-success-bg text-success-text',
    urgent: 'bg-danger-bg text-danger-text',
    info: 'bg-info-bg text-info-text',
    warning: 'bg-warning-bg text-warning-text',
    neutral: 'bg-surface-1 text-text-secondary',
  };

  return (
    <span
      className={[
        'inline-block text-[11px] font-medium px-2.5 py-0.5 rounded-full',
        variants[variant],
      ].join(' ')}
    >
      {children}
    </span>
  );
}
