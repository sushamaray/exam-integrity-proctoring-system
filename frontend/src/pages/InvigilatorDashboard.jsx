import DashboardShell from '../components/DashboardShell'

export default function InvigilatorDashboard() {
  return (
    <DashboardShell
      eyebrow="Invigilator"
      title="Review queue"
      intro="You are signed in. Flagged exam sessions awaiting a human decision will appear here."
    />
  )
}
