const BASE = '/api'

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem('auth_token')

  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE}${path}`, { ...options, headers })

  if (!res.ok) {
    const body = await res.json().catch(() => null)
    const err = new Error(body?.message || res.statusText)
    err.status = res.status
    throw err
  }

  if (res.status === 204) return null
  return res.json()
}
