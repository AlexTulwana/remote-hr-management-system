import { useState } from 'react';
import LogoIcon from './LogoIcon';
import NavItem from './NavItem';

function getInitialCollapsed() {
  return localStorage.getItem('sidebarCollapsed') === 'true';
}

export default function Sidebar({ items }) {
  const [collapsed, setCollapsed] = useState(getInitialCollapsed);

  function toggle() {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('sidebarCollapsed', String(next));
      return next;
    });
  }

  return (
    <aside
      className={[
        'shrink-0 bg-surface-1 border-r border-border py-5 flex flex-col transition-[width] duration-200 ease-out',
        collapsed ? 'w-[64px] px-2' : 'w-[200px] px-4',
      ].join(' ')}
    >
      <div
        className={[
          'flex items-center mb-7',
          collapsed ? 'flex-col gap-2' : 'justify-between',
        ].join(' ')}
      >
        <div className="flex items-center gap-2">
          <LogoIcon size={26} />
          {!collapsed ? <span className="text-[14px] font-medium">WorkFlowTech</span> : null}
        </div>
        <button
          onClick={toggle}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          className="w-6 h-6 rounded-full border border-border-strong bg-surface-2 text-[12px] flex items-center justify-center shrink-0 transition-[background,transform] duration-200 ease-out hover:bg-surface-1 hover:-translate-y-px"
        >
          {collapsed ? '\u203a' : '\u2039'}
        </button>
      </div>

      <nav className="flex-1">
        {items.map((item) => (
          <NavItem
            key={item.to}
            to={item.to}
            label={item.label}
            icon={item.icon}
            collapsed={collapsed}
          />
        ))}
      </nav>
    </aside>
  );
}
