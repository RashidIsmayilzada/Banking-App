const BASE = '/api'

async function apiRequest(path, options = {}) {
  const token = localStorage.getItem('auth_token')
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE}${path}`, { ...options, headers })

  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try {
      const body = await res.json()
      message = body.message || body.error || message
    } catch { /* ignore parse errors */ }
    throw new Error(message)
  }

  const text = await res.text()
  return text ? JSON.parse(text) : null
}

/**
 * POST /transactions
 * @param {{ type: 'TRANSFER'|'DEPOSIT'|'WITHDRAWAL', fromAccountId?: number, toAccountId?: number, toIban?: string, accountId?: number, amount: number, description: string }} body
 */
export function createTransaction(body) {
  return apiRequest('/transactions', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/**
 * GET /transactions — customers are automatically scoped to their own data by the backend.
 * @param {{ page?: number, size?: number, sort?: string, startDateTime?: string, endDateTime?: string, amountMin?: number, amountMax?: number, iban?: string, userId?: number, accountId?: number, channel?: string, amountEquals?: number }} params
 */
export function listTransactions(params = {}) {
  const qs = new URLSearchParams(
    Object.fromEntries(Object.entries(params).filter(([, v]) => v != null && v !== ''))
  ).toString()
  return apiRequest(`/transactions${qs ? `?${qs}` : ''}`)
}
