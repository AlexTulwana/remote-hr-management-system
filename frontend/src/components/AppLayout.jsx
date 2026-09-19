import Sidebar from './Sidebar';
import TopBar from './TopBar';

export default function AppLayout({ navItems, children }) {
  return (
    <div className="min-h-screen flex bg-surface-0">
      <Sidebar items={navItems} />
      <div className="flex-1 flex flex-col min-w-0">
        <TopBar />
        <main className="flex-1 px-6 py-5 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
