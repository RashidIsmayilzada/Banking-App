const BASE = '/api'

async function request(path, options = {}) {
  const token = localStorage.getItem('auth_token')
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE}${path}`, { ...options, headers })

  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try {
      const body = await res.json()
      message = body.message || body.error || message
    } catch {
      // ignore parse errors
    }
    throw new Error(message)
  }

  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export function getAllUsers(params = {}) {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.append('page', params.page)
  if (params.size !== undefined) query.append('size', params.size)
  if (params.role) query.append('role', params.role)
  if (params.active !== undefined) query.append('active', params.active)
  if (params.hasAccount !== undefined) query.append('hasAccount', params.hasAccount)
  if (params.status) query.append('status', params.status)
  if (params.search) query.append('search', params.search)

  const queryString = query.toString()
  return request(`/users${queryString ? '?' + queryString : ''}`)
}

export function getUserById(id) {
  return request(`/users/${id}`)
}

export function searchCustomers(search, params = {}) {
  return getAllUsers({
    role: 'CUSTOMER',
    size: 10,
    page: 0,
    search,
    ...params,
  })
}

export async function getPendingApprovalCount() {
  const response = await getAllUsers({
    role: 'CUSTOMER',
    status: 'PENDING_APPROVAL',
    page: 0,
    size: 1,
  })

  return response.totalElements ?? 0
}

export function approveUser(id, status, limits = {}) {
  return request(`/users/${id}/approval`, {
    method: 'PATCH',
    body: JSON.stringify({ status, ...limits })
  })
}

export function closeUser(id) {
  return request(`/users/${id}/close`, {
    method: 'PATCH'
  })
}

export function reopenUser(id) {
  return request(`/users/${id}/reopen`, {
    method: 'PATCH'
  })
}
