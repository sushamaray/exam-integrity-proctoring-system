import { useNavigate } from 'react-router-dom'
import Button from '../shared/ui/Button.jsx'
import { useAuth } from './context.js'

export default function LogoutButton() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const signOut = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <Button variant="ghost" size="sm" onClick={signOut} title="Sign out" aria-label="Sign out"
      className="px-2">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <polyline points="16 17 21 12 16 7" />
        <line x1="21" y1="12" x2="9" y2="12" />
      </svg>
    </Button>
  )
}
