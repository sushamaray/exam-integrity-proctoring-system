import { http } from '../shared/lib/api.js'

export const register = (payload) =>
  http('/auth/register', { method: 'POST', body: JSON.stringify(payload) })

export const login = (email, password) =>
  http('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })

export const logout = () => http('/auth/logout', { method: 'POST' })

export const me = async () => {
  try {
    return await http('/auth/me')
  } catch (err) {
    if (err.status === 401) return null
    throw err
  }
}
