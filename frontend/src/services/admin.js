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
// --Efe(Admin) updated paths to match refactored RESTful endpoints
// ==========================================

export function getAllAccounts() {
    return apiFetch('/accounts')
}

export function freezeAccount(id) {
    return apiFetch(`/accounts/${id}/freeze`, { method: 'PATCH' })
}

export function unfreezeAccount(id) {
    return apiFetch(`/accounts/${id}/unfreeze`, { method: 'PATCH' })
}

export function closeAccount(id) {
    return apiFetch(`/accounts/${id}/close`, { method: 'PATCH' })
}

// ==========================================
// TRANSACTIONS & AUDIT LOGS
// --Efe(Admin) updated paths to match refactored RESTful endpoints
// ==========================================

export function reverseTransaction(id) {
    return apiFetch(`/transactions/${id}/reverse`, {
        method: 'POST'
    })
}

export function getAuditLogs() {
    return apiFetch('/audit-logs')
}