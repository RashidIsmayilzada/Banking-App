const TOKEN_KEY = 'banking.auth.token'
const USER_KEY = 'banking.auth.user'

let currentToken = sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY) || ''
let currentUser = readStoredUser()

function readStoredUser() {
  const rawUser = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY)
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

function storageFor(keepSignedIn) {
  return keepSignedIn ? localStorage : sessionStorage
}

function clearStorage() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

function persistAuth(token, user, keepSignedIn) {
  clearStorage()
  const storage = storageFor(keepSignedIn)
  storage.setItem(TOKEN_KEY, token)
  storage.setItem(USER_KEY, JSON.stringify(user))
  currentToken = token
  currentUser = user
}

export function getToken() {
  return currentToken
}

export function getCurrentUser() {
  return currentUser
}

export function isAuthenticated() {
  return Boolean(currentToken)
}

export async function apiFetch(path, options = {}) {
  const headers = new Headers(options.headers || {})

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (currentToken) {
    headers.set('Authorization', `Bearer ${currentToken}`)
  }

  const response = await fetch(path, { ...options, headers })

  if (response.status === 401) {
    logout()
  }

  return response
}

export async function login(credentials, keepSignedIn) {
  const loginResponse = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  })

  if (!loginResponse.ok) {
    throw new Error(await loginResponse.text() || 'Invalid email or password')
  }

  currentToken = await loginResponse.text()
  const user = await fetchCurrentUser()
  persistAuth(currentToken, user, keepSignedIn)
  return user
}

export async function fetchCurrentUser() {
  const response = await apiFetch('/api/auth/me')

  if (!response.ok) {
    throw new Error(await response.text() || 'Unable to load current user')
  }

  currentUser = await response.json()
  return currentUser
}

export async function restoreAuth() {
  if (!currentToken) {
    return null
  }

  try {
    const user = await fetchCurrentUser()
    const keepSignedIn = Boolean(localStorage.getItem(TOKEN_KEY))
    persistAuth(currentToken, user, keepSignedIn)
    return user
  } catch {
    logout()
    return null
  }
}

export function logout() {
  currentToken = ''
  currentUser = null
  clearStorage()
}

export function homePathFor(user = currentUser) {
  if (user?.role === 'EMPLOYEE') {
    return '/employee/overview'
  }

  if (user?.role === 'CUSTOMER') {
    return user.customerStatus === 'APPROVED' ? '/customer/dashboard' : '/pending'
  }

  return '/login'
}