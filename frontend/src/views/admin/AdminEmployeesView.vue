<template>
  <AdminShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">Employees</h1>
      <span class="spacer" />
      <button class="btn btn--primary" @click="openCreate">
        <AppIcon name="plus" :size="16" /> Add employee
      </button>
    </div>

    <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">
      <AppIcon name="alert" :size="16" class="banner__icon" />
      <div>{{ error }}</div>
    </div>

    <div class="card" style="padding:0">
      <table class="table">
        <thead>
        <tr>
          <th style="padding-left:24px">Employee</th>
          <th>Email</th>
          <th>Emp Number</th>
          <th>Status</th>
          <th>Joined</th>
          <th style="padding-right:24px;text-align:right">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="loading">
          <td colspan="6" class="muted" style="padding:24px;text-align:center">Loading staff records...</td>
        </tr>
        <tr v-else-if="employees.length === 0">
          <td colspan="6" class="muted" style="padding:24px;text-align:center">No employees found.</td>
        </tr>
        <tr v-for="emp in employees" :key="emp.id">
          <td style="padding-left:24px">
            <div class="row">
              <AppAvatar :name="displayName(emp)" />
              <div>
                <div style="font-weight:500;font-size:14px">{{ displayName(emp) }}</div>
                <div class="t-body-sm">@{{ emp.username }}</div>
              </div>
            </div>
          </td>
          <td class="muted" style="font-size:13px">{{ emp.email }}</td>
          <td class="num muted">{{ emp.employeeNumber }}</td>
          <td>
            <AppStatus :kind="emp.active ? 'active' : 'closed'" />
          </td>
          <td class="num muted">{{ formatDate(emp.createdAt) }}</td>
          <td style="padding-right:24px;text-align:right">
            <div class="row" style="justify-content:flex-end;gap:6px">
              <button class="btn btn--ghost btn--xs" @click="openEdit(emp)">Edit</button>
              <button
                  :class="['btn', 'btn--xs', emp.active ? 'btn--ghost-danger' : 'btn--ghost']"
                  @click="toggleStatus(emp)"
              >
                {{ emp.active ? 'Disable' : 'Enable' }}
              </button>
            </div>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <EmployeeForm
        :show="showForm"
        :employee="selectedEmployee"
        @close="showForm = false"
        @saved="handleFormSaved"
    />
  </AdminShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminShell from '@/components/layout/AdminShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import EmployeeForm from '@/components/admin/EmployeeForm.vue'
import * as adminService from '@/services/admin'

const employees = ref([])
const loading = ref(false)
const error = ref(null)

// 4. Added state for tracking the form and the selected employee
const showForm = ref(false)
const selectedEmployee = ref(null)

async function fetchEmployees() {
  loading.value = true
  error.value = null
  try {
    employees.value = await adminService.getAllEmployees()
  } catch (err) {
    error.value = err.message || 'Failed to load employees.'
  } finally {
    loading.value = false
  }
}

// 5. Added openCreate function
function openCreate() {
  selectedEmployee.value = null
  showForm.value = true
}

// 6. Added openEdit function
function openEdit(emp) {
  // Clone the object so we don't mutate the table row directly until it is saved to the DB
  selectedEmployee.value = { ...emp }
  showForm.value = true
}

async function toggleStatus(emp) {
  if (!confirm(`Are you sure you want to ${emp.active ? 'disable' : 'enable'} ${displayName(emp)}?`)) return
  try {
    const newStatus = !emp.active
    await adminService.setEmployeeStatus(emp.id, newStatus)
    emp.active = newStatus
  } catch (err) {
    alert('Failed to change status: ' + err.message)
  }
}

function handleFormSaved() {
  showForm.value = false
  fetchEmployees()
}

function displayName(emp) {
  return [emp.firstName, emp.lastName].filter(Boolean).join(' ') || emp.username || emp.email
}

function formatDate(dateString) {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
}

onMounted(() => {
  fetchEmployees()
})
</script>