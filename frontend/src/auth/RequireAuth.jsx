import { Navigate, useLocation } from 'react-router-dom'
import { homeFor, useAuth } from './context.js'

export default function RequireAuth({ roles, children }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <main className="p-5 text-sm text-ink/42">Checking your session…</main>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to={homeFor(user)} replace />
  }

  return children
}
