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

export function listTransactions(params = {}) {
    const query = new URLSearchParams()
    if (params.page !== undefined) query.append('page', params.page)
    if (params.size !== undefined) query.append('size', params.size)
    if (params.sort) query.append('sort', params.sort)
    if (params.startDateTime) query.append('startDateTime', params.startDateTime)
    if (params.endDateTime) query.append('endDateTime', params.endDateTime)
    if (params.amountMin !== undefined) query.append('amountMin', params.amountMin)
    if (params.amountMax !== undefined) query.append('amountMax', params.amountMax)
    if (params.amountEquals !== undefined) query.append('amountEquals', params.amountEquals)
    if (params.iban) query.append('iban', params.iban)
    if (params.channel) query.append('channel', params.channel)
    if (params.userId !== undefined) query.append('userId', params.userId)

    const queryString = query.toString()
    return request(`/transactions${queryString ? '?' + queryString : ''}`)
}

export function createTransaction(payload) {
    return request('/transactions', {
        method: 'POST',
        body: JSON.stringify(payload),
    })
}