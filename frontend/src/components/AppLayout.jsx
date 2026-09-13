import Sidebar from './Sidebar';

export default function AppLayout({ navItems, children }) {
  return (
    <div className="min-h-screen flex bg-surface-0">
      <Sidebar items={navItems} />
      <main className="flex-1 px-6 py-5">{children}</main>
    </div>
  );
}
