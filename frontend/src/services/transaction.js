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

export class listTransactions {
}

export class createTransaction {
}