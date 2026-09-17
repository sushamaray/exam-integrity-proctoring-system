const USE_MOCKS = true // flip to false when the backend is ready

const delay = (ms, value) =>
  new Promise((resolve) => setTimeout(() => resolve(value), ms))

async function http(path, options) {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export { USE_MOCKS, delay, http }
