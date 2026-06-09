<template>
  <AdminShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">Admin Dashboard</h1>
      <span class="spacer" />
      <button class="btn btn--ghost" @click="fetchDashboardData" :disabled="loading">
        <AppIcon name="refresh" :size="16" /> Refresh Data
      </button>
    </div>

    <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">
      <AppIcon name="alert" :size="16" class="banner__icon" />
      <div>{{ error }}</div>
    </div>

    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 24px;">

      <div class="card" style="display: flex; flex-direction: column; gap: 8px;">
        <div class="row" style="color: var(--ink-muted);">
          <AppIcon name="users" :size="16" />
          <span style="font-size: 13px; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px;">Active Staff</span>
        </div>
        <div v-if="loading" class="t-h2" style="color: var(--ink-faint)">--</div>
        <div v-else class="t-h1" style="margin: 0; color: var(--blue)">{{ activeEmployeesCount }}</div>
        <div class="t-body-sm muted">Out of {{ totalEmployees }} total records</div>
      </div>

      <div class="card" style="display: flex; flex-direction: column; gap: 8px;">
        <div class="row" style="color: var(--ink-muted);">
          <AppIcon name="wallet" :size="16" />
          <span style="font-size: 13px; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px;">System Accounts</span>
        </div>
        <div v-if="loading" class="t-h2" style="color: var(--ink-faint)">--</div>
        <div v-else class="t-h1" style="margin: 0; color: var(--teal)">{{ totalAccounts }}</div>
        <div class="t-body-sm muted">{{ frozenAccountsCount }} currently frozen</div>
      </div>

      <div class="card" style="display: flex; flex-direction: column; gap: 8px;">
        <div class="row" style="color: var(--ink-muted);">
          <AppIcon name="shield" :size="16" />
          <span style="font-size: 13px; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px;">Audit Events</span>
        </div>
        <div v-if="loading" class="t-h2" style="color: var(--ink-faint)">--</div>
        <div v-else class="t-h1" style="margin: 0; color: var(--purple)">{{ totalLogs }}</div>
        <div class="t-body-sm muted">Recorded security actions</div>
      </div>

    </div>

    <div class="card" style="padding:0">
      <div style="padding: 16px 24px; border-bottom: 1px solid var(--border);">
        <h3 style="margin: 0; font-size: 14px; font-weight: 600;">Recent Security Activity</h3>
      </div>
      <table class="table">
        <tbody>
        <tr v-if="loading">
          <td colspan="3" class="muted" style="padding:24px;text-align:center">Loading recent activity...</td>
        </tr>
        <tr v-else-if="recentLogs.length === 0">
          <td colspan="3" class="muted" style="padding:24px;text-align:center">No recent activity.</td>
        </tr>
        <tr v-for="log in recentLogs" :key="log.id">
          <td class="num muted" style="padding-left:24px; width: 160px;">{{ formatTime(log.createdAt) }}</td>
          <td>
              <span class="badge" style="background: var(--ink-faint); color: var(--ink); font-size: 11px;">
                {{ formatAction(log.action) }}
              </span>
          </td>
          <td style="padding-right:24px; font-size: 13px; color: var(--ink-muted);">
            <strong>@{{ log.actorUsername }}</strong> {{ log.details.toLowerCase() }}
          </td>
        </tr>
        </tbody>
      </table>
      <div style="padding: 12px 24px; background: var(--surface-hover); text-align: center; border-top: 1px solid var(--border);">
        <router-link to="/admin/audit-logs" style="font-size: 13px; font-weight: 500; text-decoration: none; color: var(--blue);">
          View Full Audit Trail &rarr;
        </router-link>
      </div>
    </div>
  </AdminShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AdminShell from '@/components/layout/AdminShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import * as adminService from '@/services/admin'

const loading = ref(false)
const error = ref(null)

const employees = ref([])
const accounts = ref([])
const logs = ref([])

// Computed Stats
const totalEmployees = computed(() => employees.value.length)
const activeEmployeesCount = computed(() => employees.value.filter(e => e.active).length)

const totalAccounts = computed(() => accounts.value.length)
const frozenAccountsCount = computed(() => accounts.value.filter(a => a.frozen).length)

const totalLogs = computed(() => logs.value.length)
const recentLogs = computed(() => {
  return [...logs.value]
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .slice(0, 5) // Just grab the 5 most recent
})

async function fetchDashboardData() {
  loading.value = true
  error.value = null
  try {
    // Fire all three API calls simultaneously for faster loading
    const [empData, accData, logData] = await Promise.all([
      adminService.getAllEmployees(),
      adminService.getAllAccounts(),
      adminService.getAuditLogs()
    ])

    employees.value = empData
    accounts.value = accData
    logs.value = logData
  } catch (err) {
    error.value = err.message || 'Failed to sync dashboard data.'
  } finally {
    loading.value = false
  }
}

function formatTime(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  // Show "Today, 14:30" or the short date if older
  if (date.toDateString() === new Date().toDateString()) {
    return `Today, ${date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`
  }
  return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }) + ', ' +
      date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
}

function formatAction(action) {
  if (!action) return 'SYSTEM'
  return action.replace(/_/g, ' ')
}

onMounted(() => {
  fetchDashboardData()
})
</script>