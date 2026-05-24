import { apiFetch } from './api.js'

export function getAccounts(userId) {
  const query = userId ? `?userId=${userId}` : ''
  return apiFetch(`/accounts${query}`)
}

export function getAccount(accountId) {
  return apiFetch(`/accounts/${accountId}`)
}

export function updateAccount(accountId, data) {
  return apiFetch(`/accounts/${accountId}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}
