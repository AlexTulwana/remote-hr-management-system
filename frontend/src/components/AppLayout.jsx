import Sidebar from './Sidebar';
import TopBar from './TopBar';

const APP_BACKGROUND = '/backgrounds/app-bg.png';

export default function AppLayout({ navItems, children }) {
  return (
    <div className="min-h-screen flex bg-surface-0">
      <Sidebar items={navItems} />
      <div className="flex-1 flex flex-col min-w-0">
        <TopBar />
        <main className="flex-1 relative overflow-y-auto">
          <div
            className="absolute inset-0 z-0 bg-cover bg-center grayscale opacity-[0.08] dark:opacity-[0.12] pointer-events-none"
            style={{ backgroundImage: `url(${APP_BACKGROUND})` }}
          />
          <div className="relative z-10 px-6 py-5">{children}</div>
        </main>
      </div>
    </div>
  );
}
