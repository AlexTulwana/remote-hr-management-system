export default function Card({ children, hoverable = false, className = '' }) {
  return (
    <div
      className={[
        'bg-surface-2 border border-border rounded-lg p-3.5',
        hoverable &&
          'transition-[background,transform] duration-200 ease-out cursor-default hover:border-border-strong hover:-translate-y-px',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {children}
    </div>
  );
}
