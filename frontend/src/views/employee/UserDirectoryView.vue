<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <h1 class="t-h1" style="margin:0">{{ title }}</h1>
        <p class="t-body muted" style="margin:6px 0 0">{{ totalElements }} {{ totalElements === 1 ? 'user' : 'users' }}</p>
      </div>
      <span class="spacer" />
      <div class="row" style="gap:8px;position:relative">
        <AppIcon name="search" :size="14" style="position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--ink-faint)" />
        <input class="input" placeholder="Search name, email, IBAN..." style="width:320px;padding-left:38px" v-model="search" @input="applyFilters" />
        <select v-if="!fixedRole" class="select" style="width:150px" v-model="roleFilter" @change="applyFilters">
          <option value="">All roles</option>
          <option value="CUSTOMER">Customers</option>
          <option value="EMPLOYEE">Employees</option>
        </select>
        <select v-if="props.fixedRole !== 'EMPLOYEE'" class="select" style="width:160px" v-model="statusFilter" @change="applyFilters">
          <option value="">All statuses</option>
          <option value="APPROVED">Active</option>
          <option value="PENDING_APPROVAL">Pending</option>
          <option value="REJECTED">Rejected</option>
          <option value="CLOSED">Closed</option>
        </select>
      </div>
    </div>

    <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">{{ error }}</div>
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:24px">User</th>
            <th>Role</th>
            <th>Email</th>
            <th>IBAN(s)</th>
            <th>Status</th>
            <th>Joined</th>
            <th style="padding-right:24px;text-align:right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="muted" style="padding:24px;text-align:center">Loading users...</td>
          </tr>
          <tr v-else-if="users.length === 0">
            <td colspan="7" class="muted" style="padding:24px;text-align:center">No users found.</td>
          </tr>
          <tr v-for="user in users" :key="user.id">
            <td style="padding-left:24px">
              <div class="row">
                <AppAvatar :name="user.name" />
                <div>
                  <div style="font-weight:500;font-size:14px">{{ user.name }}</div>
                  <div class="t-body-sm">#{{ user.id }}</div>
                </div>
              </div>
            </td>
            <td>{{ roleLabel(user.role) }}</td>
            <td class="muted" style="font-size:13px">{{ user.email }}</td>
            <td class="iban">{{ user.ibans }}</td>
            <td>
              <AppStatus v-if="user.status" :kind="user.status" />
              <span v-else class="muted">—</span>
            </td>
            <td class="num muted">{{ user.joined }}</td>
            <td style="padding-right:24px;text-align:right">
              <RouterLink
                v-if="user.role === 'CUSTOMER' && user.status === 'pending'"
                :to="`/employee/approvals/${user.id}`"
                class="btn btn--primary btn--xs"
              >Approve -></RouterLink>
              <RouterLink
                v-else-if="user.role === 'CUSTOMER'"
                :to="`/employee/customers/${user.id}`"
                class="btn btn--ghost btn--xs"
              >View</RouterLink>
              <div v-else-if="user.role === 'EMPLOYEE'" style="display:flex;gap:6px;justify-content:flex-end">
                <button
                  v-if="user.active !== false"
                  class="btn btn--ghost-danger btn--xs"
                  :disabled="actionLoading === user.id"
                  @click="handleCloseUser(user.id)"
                >Close</button>
                <button
                  v-else
                  class="btn btn--ghost btn--xs"
                  :disabled="actionLoading === user.id"
                  @click="handleReopenUser(user.id)"
                >Reopen</button>
              </div>
              <span v-else class="muted">-</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div style="padding:0 16px 12px" v-if="totalPages > 0">
        <AppPager
          :current-page="page + 1"
          :total="totalPages"
          :count="`${page * pageSize + 1}-${Math.min((page + 1) * pageSize, totalElements)} of ${totalElements}`"
          @change="handlePageChange"
        />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import AppPager from '@/components/shared/AppPager.vue'
import * as userService from '@/services/user'

const props = defineProps({
  title: { type: String, required: true },
  fixedRole: { type: String, default: '' },
})

const pageSize = 10
const search = ref('')
const roleFilter = ref('')
const statusFilter = ref('')
const users = ref([])
const loading = ref(false)
const error = ref(null)
const actionLoading = ref(null)
const page = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)

function displayName(user) {
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || user.email
}

function statusKind(status) {
  if (!status) return null
  if (status === 'APPROVED') return 'active'
  if (status === 'REJECTED') return 'rejected'
  if (status === 'CLOSED') return 'closed'
  return 'pending'
}

function roleLabel(role) {
  if (role === 'EMPLOYEE') return 'Employee'
  if (role === 'CUSTOMER') return 'Customer'
  return role || '-'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'
}

function accountSummary(user) {
  return user.accounts && user.accounts.length > 0 ? user.accounts.map(account => account.iban).join(', ') : '-'
}

async function fetchUsers() {
  loading.value = true
  error.value = null
  try {
    const params = {
      page: page.value,
      size: pageSize,
      search: search.value,
    }

    const role = props.fixedRole || roleFilter.value
    if (role) params.role = role
    if (statusFilter.value) params.status = statusFilter.value

    const response = await userService.getAllUsers(params)
    users.value = (response.content || []).map(user => ({
      id: user.id,
      name: displayName(user),
      role: user.role,
      email: user.email,
      ibans: accountSummary(user),
      active: user.active,
      status: user.role === 'EMPLOYEE'
        ? (user.active === false ? 'closed' : null)
        : statusKind(user.status),
      joined: formatDate(user.registeredAt),
    }))
    totalElements.value = response.totalElements ?? users.value.length
    totalPages.value = response.totalPages ?? 0
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function handlePageChange(newPage) {
  page.value = newPage - 1
  fetchUsers()
}

function applyFilters() {
  page.value = 0
  fetchUsers()
}

async function handleCloseUser(id) {
  if (!confirm('Close this employee? They will no longer be able to log in.')) return
  actionLoading.value = id
  error.value = null
  try {
    await userService.closeUser(id)
    await fetchUsers()
  } catch (err) {
    error.value = err.message || 'Failed to close user'
  } finally {
    actionLoading.value = null
  }
}

async function handleReopenUser(id) {
  if (!confirm('Reopen this employee? They will be able to log in again.')) return
  actionLoading.value = id
  error.value = null
  try {
    await userService.reopenUser(id)
    await fetchUsers()
  } catch (err) {
    error.value = err.message || 'Failed to reopen user'
  } finally {
    actionLoading.value = null
  }
}

watch(() => props.fixedRole, () => {
  search.value = ''
  roleFilter.value = ''
  statusFilter.value = ''
  applyFilters()
})

onMounted(() => {
  fetchUsers()
})
</script>
