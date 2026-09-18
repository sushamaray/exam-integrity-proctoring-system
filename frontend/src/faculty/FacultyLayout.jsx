import { Outlet } from 'react-router-dom'
import { SidebarShell } from '../shared/ui/SidebarNav.jsx'
import { IconCalendar, IconGrid, IconFlag, IconGraph, IconBars } from '../shared/ui/Icon.jsx'
import { useAuth } from '../auth/context.js'
import LogoutButton from '../auth/LogoutButton.jsx'
import { initialsOf } from '../auth/initials.js'

const groups = [
  {
    label: 'Invigilation',
    items: [
      { to: '/faculty', end: true, icon: <IconCalendar />, label: 'Exams' },
      { to: '/faculty/monitor', icon: <IconGrid />, label: 'Live monitor' },
      { to: '/faculty/review', icon: <IconFlag />, label: 'Review queue' },
    ],
  },
  {
    label: 'Evidence',
    items: [
      { to: '/faculty/collusion', icon: <IconGraph />, label: 'Collusion graph' },
      { to: '/faculty/analysis', icon: <IconBars />, label: 'Item analysis' },
    ],
  },
]

const roleLabel = (role = '') => role.charAt(0) + role.slice(1).toLowerCase()

export default function FacultyLayout() {
  const { user } = useAuth()

  return (
    <div className="flex items-start min-h-screen bg-paper text-ink">
      <SidebarShell
        title="Cohort"
        subtitle="Faculty console"
        groups={groups}
        user={{
          initials: initialsOf(user.fullName),
          name: user.fullName,
          role: roleLabel(user.role),
        }}
        action={<LogoutButton />}
      />
      <div className="flex-1 min-w-0">
        <Outlet />
      </div>
    </div>
  )
}
