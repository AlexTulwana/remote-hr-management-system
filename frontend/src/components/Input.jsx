export default function Input({ label, className = '', ...props }) {
  return (
    <label className="flex flex-col gap-1.5">
      {label ? <span className="text-[13px] text-text-secondary">{label}</span> : null}
      <input
        className={[
          'h-9 px-3 rounded-lg text-[13px] font-sans bg-surface-2 border border-border-strong',
          'focus:outline-none focus:border-text-primary transition-colors duration-200',
          className,
        ].join(' ')}
        {...props}
      />
    </label>
  );
}
