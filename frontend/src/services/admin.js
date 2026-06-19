import { apiFetch } from './api.js'

// ==========================================
// EMPLOYEE MANAGEMENT
// ==========================================

export function getAllEmployees() {
    return apiFetch('/admin/employees')
}

export function getEmployee(id) {
    return apiFetch(`/admin/employees/${id}`)
}

export function createEmployee(data) {
    return apiFetch('/admin/employees', {
        method: 'POST',
        body: JSON.stringify(data)
    })
}

export function updateEmployee(id, data) {
    return apiFetch(`/admin/employees/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
    })
}

export function setEmployeeStatus(id, active) {
    return apiFetch(`/admin/employees/${id}/status?active=${active}`, {
        method: 'PATCH'
    })
}

export function deleteEmployee(id) {
    return apiFetch(`/admin/employees/${id}`, {
        method: 'DELETE'
    })
}

// ==========================================
// ACCOUNT MANAGEMENT
// ==========================================

export function getAllAccounts() {
    return apiFetch('/admin/accounts')
}

export function freezeAccount(id) {
    return apiFetch(`/admin/accounts/${id}/freeze`, { method: 'PATCH' })
}

export function unfreezeAccount(id) {
    return apiFetch(`/admin/accounts/${id}/unfreeze`, { method: 'PATCH' })
}

export function closeAccount(id) {
    return apiFetch(`/admin/accounts/${id}/close`, { method: 'PATCH' })
}

// ==========================================
// TRANSACTIONS & AUDIT LOGS
// ==========================================

export function reverseTransaction(id) {
    return apiFetch(`/admin/transactions/${id}/reverse`, {
        method: 'POST'
    })
}

export function getAuditLogs() {
    return apiFetch('/admin/audit-logs')
}