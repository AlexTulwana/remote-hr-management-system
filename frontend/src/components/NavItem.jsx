import { NavLink } from 'react-router-dom';

export default function NavItem({ to, label, icon: Icon }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        [
          'flex items-center gap-2.5 px-2.5 py-2 rounded-lg text-[13px] mb-0.5',
          'transition-[background,transform] duration-200 ease-out',
          isActive
            ? 'bg-surface-2 border border-border font-medium text-text-primary'
            : 'text-text-secondary border border-transparent hover:bg-surface-2 hover:border-border-strong hover:-translate-y-px',
        ].join(' ')
      }
    >
      {Icon ? <Icon size={16} /> : null}
      <span>{label}</span>
    </NavLink>
  );
}
