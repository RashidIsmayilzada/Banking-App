import { apiFetch } from './api.js'

/**
 * List transactions with optional filters.
 * @param {Object} params - TransactionFilterParams
 * @param {number} [params.page]
 * @param {number} [params.size]
 * @param {string} [params.sort]
 * @param {string} [params.startDateTime] - ISO 8601 datetime string
 * @param {string} [params.endDateTime]   - ISO 8601 datetime string
 * @param {number} [params.amountMin]
 * @param {number} [params.amountMax]
 * @param {number} [params.amountEquals]
 * @param {string} [params.iban]
 * @param {number} [params.userId]
 * @param {number} [params.accountId]
 * @param {string} [params.channel]       - 'ATM' | 'ONLINE' | 'INTERNAL'
 * @returns {Promise<{items: Array, page: Object}>}
 */
export function listTransactions(params = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, value)
    }
  }
  const qs = query.toString()
  return apiFetch(`/transactions${qs ? `?${qs}` : ''}`)
}

/**
 * Create a new transaction.
 * @param {Object} data - TransactionRequest
 * @param {string} data.type            - 'TRANSFER' | 'DEPOSIT' | 'WITHDRAWAL'
 * @param {number} [data.fromAccountId]
 * @param {number} [data.toAccountId]
 * @param {string} [data.toIban]        - must match NL pattern for transfers to external IBANs
 * @param {number} [data.accountId]     - used for deposits/withdrawals
 * @param {number} data.amount
 * @param {string} data.description
 * @returns {Promise<{transaction: Object, sourceBalance: Object, destinationBalance: Object}>}
 */
export function createTransaction(data) {
  return apiFetch('/transactions', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
