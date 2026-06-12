const BASE = '/api'
const ATM_TOKEN_KEY = 'atm_token'
const ATM_USER_KEY  = 'atm_user'
const ATM_TX_KEY    = 'atm_last_tx'
const ATM_ACCT_KEY  = 'atm_account'

export function getAtmToken() { return sessionStorage.getItem(ATM_TOKEN_KEY) }
export function getAtmUser()  { return JSON.parse(sessionStorage.getItem(ATM_USER_KEY) || 'null') }
export function getAtmAccount() { return JSON.parse(sessionStorage.getItem(ATM_ACCT_KEY) || 'null') }
export function setAtmAccount(acct) { sessionStorage.setItem(ATM_ACCT_KEY, JSON.stringify(acct)) }
export function getAtmLastTx() { return JSON.parse(sessionStorage.getItem(ATM_TX_KEY) || 'null') }
export function setAtmLastTx(tx) { sessionStorage.setItem(ATM_TX_KEY, JSON.stringify(tx)) }

export function setAtmSession(accessToken, user) {
  sessionStorage.setItem(ATM_TOKEN_KEY, accessToken)
  sessionStorage.setItem(ATM_USER_KEY, JSON.stringify(user))
}

export function clearAtmSession() {
  sessionStorage.removeItem(ATM_TOKEN_KEY)
  sessionStorage.removeItem(ATM_USER_KEY)
  sessionStorage.removeItem(ATM_TX_KEY)
  sessionStorage.removeItem(ATM_ACCT_KEY)
}

async function atmFetch(path, options = {}) {
  const token = getAtmToken()
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, { ...options, headers })
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new Error(body?.message || res.statusText)
  }
  if (res.status === 204) return null
  return res.json()
}

export async function atmLogin(email, password) {
  const res = await fetch(`${BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new Error(body?.message || res.statusText)
  }
  return res.json()
}

export function atmGetAccounts() {
  return atmFetch('/accounts')
}

export function atmCreateTransaction(data) {
  return atmFetch('/transactions', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function atmLogout() {
  const token = getAtmToken()
  if (!token) return Promise.resolve()
  return fetch(`${BASE}/auth/logout`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  }).catch(() => {})
}
