export default function Button({
  children,
  variant = 'primary',
  className = '',
  ...props
}) {
  const base =
    'h-9 px-4 rounded-full text-[13px] font-medium font-sans transition-[background,transform,border] duration-200 ease-out cursor-pointer';

  const variants = {
    primary:
      'bg-text-primary text-surface-2 border border-transparent hover:bg-text-secondary',
    secondary:
      'bg-surface-2 text-text-primary border border-border-strong hover:bg-surface-1 hover:-translate-y-px',
    danger:
      'bg-surface-2 text-red-500 border border-red-500/40 hover:bg-red-500/10',
  };

  return (
    <button className={[base, variants[variant], className].filter(Boolean).join(' ')} {...props}>
      {children}
    </button>
  );
}
