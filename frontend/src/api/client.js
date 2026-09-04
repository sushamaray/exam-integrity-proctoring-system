/** An error carrying the HTTP status and the server's own message. */
export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/**
 * The single place requests are made. Every caller gets the same cookie
 * handling, the same JSON encoding, and the same error type.
 *
 * @param {string} path e.g. '/api/auth/me'
 * @param {{ method?: string, body?: unknown }} options
 * @returns {Promise<unknown|null>} parsed body, or null for a 204
 * @throws {ApiError} on any non-2xx response
 */
export async function apiFetch(path, { method = 'GET', body } = {}) {
  const response = await fetch(path, {
    method,
    // Same-origin in dev thanks to the Vite proxy, but explicit so this keeps
    // working if the frontend is ever served from a different host.
    credentials: 'include',
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 204) return null

  const text = await response.text()
  let payload = null
  if (text) {
    try {
      payload = JSON.parse(text)
    } catch {
      payload = { message: text }
    }
  }

  if (!response.ok) {
    throw new ApiError(
      response.status,
      payload?.message ?? `Request failed (${response.status})`,
      payload?.fieldErrors ?? {},
    )
  }

  return payload
}
