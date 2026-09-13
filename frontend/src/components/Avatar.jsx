export default function Avatar({ initials, size = 'md' }) {
  const sizes = {
    sm: 'w-[26px] h-[26px] text-[11px]',
    md: 'w-8 h-8 text-[12px]',
  };

  return (
    <div
      className={[
        'rounded-full bg-surface-1 border border-border flex items-center justify-center font-medium shrink-0',
        sizes[size],
      ].join(' ')}
    >
      {initials}
    </div>
  );
}
