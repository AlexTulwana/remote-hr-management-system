export default function LogoIcon({
  size = 26,
  primaryColor = 'var(--color-text-primary)',
  mutedColor = 'var(--color-text-muted)',
  surfaceColor = 'var(--color-surface-1)',
}) {
  return (
    <svg width={size} height={size} viewBox="0 0 800 620" xmlns="http://www.w3.org/2000/svg">
      <rect x="250" y="180" width="90" height="380" rx="20" fill={primaryColor} />
      <rect x="460" y="180" width="90" height="380" rx="20" fill={primaryColor} />
      <path d="M 185,435 A 230,90 -15 0,1 615,300" fill="none" stroke={mutedColor} strokeWidth="14" strokeLinecap="round" />
      <circle cx="340" cy="292" r="32" fill={surfaceColor} stroke={primaryColor} strokeWidth="6" />
      <path d="M 288,420 Q 288,330 340,330 Q 392,330 392,420 Z" fill={surfaceColor} stroke={primaryColor} strokeWidth="6" />
      <circle cx="460" cy="292" r="32" fill={surfaceColor} stroke={primaryColor} strokeWidth="6" />
      <path d="M 408,420 Q 408,330 460,330 Q 512,330 512,420 Z" fill={surfaceColor} stroke={primaryColor} strokeWidth="6" />
      <circle cx="400" cy="268" r="44" fill={primaryColor} />
      <path d="M 328,435 Q 328,318 400,318 Q 472,318 472,435 Z" fill={primaryColor} />
    </svg>
  );
}
