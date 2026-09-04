import DashboardShell from '../components/DashboardShell'

export default function StudentDashboard() {
  return (
    <DashboardShell
      eyebrow="Student"
      title="Your exams"
      intro="You are signed in. Scheduled exams and practice sessions will appear here."
    />
  )
}
