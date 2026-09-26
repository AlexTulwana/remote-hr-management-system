import LogoIcon from './LogoIcon';
import NavItem from './NavItem';

export default function Sidebar({ items }) {
  return (
    <aside className="w-[200px] shrink-0 bg-surface-1 border-r border-border px-4 py-5">
      <div className="flex items-center gap-2 mb-7">
        <LogoIcon size={26} />
        <span className="text-[14px] font-medium">WorkFlowTech</span>
      </div>
      <nav>
        {items.map((item) => (
          <NavItem key={item.to} to={item.to} label={item.label} icon={item.icon} />
        ))}
      </nav>
    </aside>
  );
}
