import { NavLink } from 'react-router-dom'

export function SidebarLink({ to, end, icon, label }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `flex items-center gap-2.5 rounded-[7px] px-2.5 py-2 text-[13px]
         font-medium transition-colors ${
           isActive ? 'bg-navy/10 text-navy' : 'text-ink/62 hover:bg-navy/7'
         }`
      }
    >
      {icon}
      <span>{label}</span>
    </NavLink>
  )
}

export function SidebarShell({ logoLetter = 'C', title, subtitle, groups, user, children }) {
  return (
    <aside
      className="w-[216px] shrink-0 flex flex-col gap-[22px] p-3 px-3
        border-r border-navy/14 bg-surface sticky top-0 self-start h-screen overflow-y-auto"
    >
      <div className="px-2 pt-1">
        <div className="flex items-center gap-2.5">
          <span className="w-[26px] h-[26px] rounded-[7px] bg-navy flex items-center
            justify-center text-paper text-[13px] font-bold shrink-0">
            {logoLetter}
          </span>
          <span className="flex flex-col leading-tight">
            <span className="text-sm font-semibold tracking-[-0.015em]">{title}</span>
            <span className="text-[11px] text-ink/42">{subtitle}</span>
          </span>
        </div>
      </div>

      <nav className="flex flex-col gap-5">
        {groups.map((group) => (
          <div key={group.label} className="flex flex-col gap-0.5">
            <span className="text-[10px] font-semibold tracking-[0.1em] uppercase
              text-ink/38 px-2.5 pb-1.5">
              {group.label}
            </span>
            {group.items.map((item) => (
              <SidebarLink key={item.to} {...item} />
            ))}
          </div>
        ))}
      </nav>

      {children}

      {user && (
        <div className="mt-auto flex items-center gap-2.5 pt-3 px-2.5 border-t border-navy/10">
          <span className="w-7 h-7 rounded-full bg-navy/10 border border-navy/18
            flex items-center justify-center text-[11px] font-semibold text-navy shrink-0">
            {user.initials}
          </span>
          <span className="flex flex-col leading-tight">
            <span className="text-xs font-medium">{user.name}</span>
            <span className="text-[11px] text-ink/42">{user.role}</span>
          </span>
        </div>
      )}
    </aside>
  )
}
