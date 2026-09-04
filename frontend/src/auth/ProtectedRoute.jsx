import { Navigate, useLocation } from 'react-router-dom'

import { useAuth } from './authContext'
import { HOME_BY_ROLE } from './roles'

/**
 * Gates a route on being signed in, and optionally on holding one of the given
 * roles.
 *
 * <ProtectedRoute roles={['FACULTY']}><FacultyDashboard /></ProtectedRoute>
 *
 * This is a convenience for the person using the app, not a security boundary
 * -- the real check is the SecurityFilterChain on the server, which rejects the
 * API call no matter what the browser renders.
 */
export default function ProtectedRoute({ roles, children }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  // Render nothing until the session check finishes, otherwise refreshing a
  // protected page flashes the login screen before bouncing back.
  if (loading) {
    return <div className="grid h-full place-items-center text-ink-400">Loading...</div>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/login'} replace />
  }

  return children
}
