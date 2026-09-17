import { Outlet } from 'react-router-dom'
import { SidebarShell } from '../shared/ui/SidebarNav.jsx'
import { IconCalendar, IconGrid, IconFlag, IconClock, IconGraph, IconSplit, IconBars } from '../shared/ui/Icon.jsx'
import { invigilator } from './mock/data.js'

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
      { to: '/faculty/session/8f21c', icon: <IconClock />, label: 'Session detail' },
      { to: '/faculty/collusion', icon: <IconGraph />, label: 'Collusion graph' },
      { to: '/faculty/diff/a/b', icon: <IconSplit />, label: 'Answer diff' },
      { to: '/faculty/analysis', icon: <IconBars />, label: 'Item analysis' },
    ],
  },
]

export default function FacultyLayout() {
  return (
    <div className="flex items-start min-h-screen bg-paper text-ink">
      <SidebarShell
        title="Cohort"
        subtitle="Faculty console"
        groups={groups}
        user={{ initials: invigilator.initials, name: invigilator.name, role: invigilator.role }}
      />
      <div className="flex-1 min-w-0">
        <Outlet />
      </div>
    </div>
  )
}
