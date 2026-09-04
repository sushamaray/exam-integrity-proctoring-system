import { createContext, useContext } from 'react'

/**
 * The context object and its hook live apart from the provider component so
 * that AuthProvider.jsx exports only a component, which is what React Fast
 * Refresh needs to hot-reload it during development.
 */
export const AuthContext = createContext(null)

/** @returns {{ user: object|null, loading: boolean, login: Function, logout: Function }} */
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}
