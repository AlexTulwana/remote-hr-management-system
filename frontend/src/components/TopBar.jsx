import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../auth/ThemeContext';
import Button from './Button';
import SegmentClock from './SegmentClock';

export default function TopBar() {
  const { logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  return (
    <header className="h-14 shrink-0 flex items-center justify-end gap-4 px-6 border-b border-border bg-surface-0">
      <SegmentClock
        scale={0.55}
        onColor="var(--color-text-primary)"
        offColor="var(--color-surface-1)"
        dotColor="var(--color-text-muted)"
      />
      <button
        onClick={toggleTheme}
        aria-label="Toggle dark mode"
        className="w-9 h-9 rounded-full border border-border-strong bg-surface-2 text-[15px] flex items-center justify-center transition-[background,transform] duration-200 ease-out hover:bg-surface-1 hover:-translate-y-px"
      >
        {theme === 'dark' ? '☀' : '☽'}
      </button>
      <Button variant="secondary" onClick={logout}>
        Log out
      </Button>
    </header>
  );
}
