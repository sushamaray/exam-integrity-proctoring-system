import { ApiError } from './client'

/**
 * Stands in for the Spring backend while VITE_MOCK_API is on, so the frontend
 * runs on its own. Mirrors DemoDataSeeder and the auth endpoints' responses.
 */
const DEMO_PASSWORD = 'password123'
const USERS = [
  { id: 1, fullName: 'Rahim Ahmed', email: 'student@uiu.ac.bd', role: 'STUDENT' },
  { id: 2, fullName: 'Dr. Farhana Islam', email: 'faculty@uiu.ac.bd', role: 'FACULTY' },
  { id: 3, fullName: 'Nasir Uddin', email: 'invigilator@uiu.ac.bd', role: 'INVIGILATOR' },
]
const STORAGE_KEY = 'mock-session-user'

// Storage can be unavailable (private windows, blocked site data); the mock
// then behaves like a session that ends on refresh.
function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem(STORAGE_KEY))
  } catch {
    return null
  }
}

function writeSession(user) {
  try {
    if (user) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // Nothing to do; see readSession.
  }
}

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export async function mockFetch(path, { method = 'GET', body } = {}) {
  // Enough latency that loading states are still visible.
  await delay(300)

  if (method === 'GET' && path === '/api/auth/me') {
    const user = readSession()
    if (!user) throw new ApiError(401, 'Not signed in')
    return user
  }

  if (method === 'POST' && path === '/api/auth/login') {
    const email = body?.email?.trim() ?? ''
    const password = body?.password ?? ''

    const fieldErrors = {}
    if (!email) fieldErrors.email = 'Email is required'
    if (!password) fieldErrors.password = 'Password is required'
    if (Object.keys(fieldErrors).length > 0) {
      throw new ApiError(400, 'Please check the highlighted fields', fieldErrors)
    }

    const user = USERS.find((candidate) => candidate.email === email.toLowerCase())
    if (!user || password !== DEMO_PASSWORD) {
      throw new ApiError(401, 'Invalid email or password')
    }

    writeSession(user)
    return user
  }

  if (method === 'POST' && path === '/api/auth/logout') {
    writeSession(null)
    return null
  }

  throw new ApiError(503, 'Backend is disabled in frontend-only mode')
}
