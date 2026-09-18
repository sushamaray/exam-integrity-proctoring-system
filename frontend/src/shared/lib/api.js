const USE_MOCKS = true

const delay = (ms, value) =>
  new Promise((resolve) => setTimeout(() => resolve(value), ms))

function csrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : null
}

let minting = null

async function ensureCsrfToken() {
  if (csrfToken()) return

  minting ??= fetch('/api/ping', { credentials: 'include' }).finally(() => {
    minting = null
  })
  await minting
}

const SAFE_METHODS = ['GET', 'HEAD']

class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Request failed (${status})`)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = body?.fieldErrors ?? {}
  }
}

async function http(path, options = {}) {
  if (!SAFE_METHODS.includes((options.method ?? 'GET').toUpperCase())) {
    await ensureCsrfToken()
  }
  const token = csrfToken()

  const res = await fetch(`/api${path}`, {
    ...options,

    credentials: 'include',

    headers: {
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { 'X-XSRF-TOKEN': token } : {}),
      ...options.headers,
    },
  })

  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body)
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') return null
  return res.json()
}

export { USE_MOCKS, ApiError, delay, http }
