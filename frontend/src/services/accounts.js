import { apiFetch } from './api.js'

export function getAccounts(userId) {
  const query = userId ? `?userId=${userId}` : ''
  return apiFetch(`/accounts${query}`)
}

export function getAccount(iban) {
  return apiFetch(`/accounts/${iban}`)
}

export function updateAccount(iban, data) {
  return apiFetch(`/accounts/${iban}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}
