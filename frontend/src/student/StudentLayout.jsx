import { Outlet } from 'react-router-dom'
import { SidebarShell } from '../shared/ui/SidebarNav.jsx'
import { IconGrid, IconDoc, IconCode, IconBook, IconChart, IconShield } from '../shared/ui/Icon.jsx'
import { student } from './mock/data.js'

const groups = [
  {
    label: 'Exam',
    items: [
      { to: '/student', end: true, icon: <IconGrid />, label: 'Dashboard' },
      { to: '/student/briefing', icon: <IconShield />, label: 'Briefing' },
      { to: '/student/exam', icon: <IconDoc />, label: 'Objective exam' },
      { to: '/student/coding', icon: <IconCode />, label: 'Coding exam' },
    ],
  },
  {
    label: 'Study',
    items: [
      { to: '/student/practice', icon: <IconBook />, label: 'Practice' },
      { to: '/student/results', icon: <IconChart />, label: 'Post-exam review' },
    ],
  },
]

export default function StudentLayout() {
  return (
    <div className="flex items-start min-h-screen bg-paper text-ink">
      <SidebarShell
        title="Cohort"
        subtitle="Student portal"
        groups={groups}
        user={{ initials: student.initials, name: student.name, role: student.id }}
      />
      <div className="flex-1 min-w-0">
        <Outlet />
      </div>
    </div>
  )
}
