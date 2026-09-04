import { useCallback, useEffect, useMemo, useState } from 'react'

import { apiFetch } from '../api/client'
import { AuthContext } from './authContext'

/**
 * Owns the signed-in user for the whole app.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // Ask the server who we are on first paint. This is what makes a browser
  // refresh keep the user signed in: the session lives in the JSESSIONID
  // cookie, not in React state.
  useEffect(() => {
    let cancelled = false

    apiFetch('/api/auth/me')
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        // A 401 here is the normal "not signed in yet" case, not a failure.
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (email, password) => {
    const me = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    })
    setUser(me)
    return me
  }, [])

  const logout = useCallback(async () => {
    try {
      await apiFetch('/api/auth/logout', { method: 'POST' })
    } finally {
      // Clear locally even if the call failed, so the user is never stuck on a
      // screen they believe they have left.
      setUser(null)
    }
  }, [])

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
