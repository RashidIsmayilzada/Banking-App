<template>
  <AdminShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">System Audit Logs</h1>
      <span class="spacer" />
      <button class="btn btn--ghost" @click="fetchLogs" :disabled="loading">
        <AppIcon name="refresh" :size="16" /> Refresh
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
          <th style="padding-left:24px">Timestamp</th>
          <th>Admin User</th>
          <th>Action</th>
          <th>Target</th>
          <th style="padding-right:24px">Details</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="loading">
          <td colspan="5" class="muted" style="padding:24px;text-align:center">Loading audit logs...</td>
        </tr>
        <tr v-else-if="logs.length === 0">
          <td colspan="5" class="muted" style="padding:24px;text-align:center">No audit logs found.</td>
        </tr>
        <tr v-for="log in logs" :key="log.id">
          <td class="num muted" style="padding-left:24px">{{ formatDateTime(log.createdAt) }}</td>
          <td style="font-weight: 500;">@{{ log.actorUsername }}</td>
          <td>
              <span class="badge" style="background: var(--ink-faint); color: var(--ink)">
                {{ formatAction(log.action) }}
              </span>
          </td>
          <td>
              <span style="font-size: 13px; color: var(--ink-muted);">
                {{ log.targetType }} #{{ log.targetId }}
              </span>
          </td>
          <td style="padding-right:24px; font-size: 13px;">{{ log.details }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </AdminShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminShell from '@/components/layout/AdminShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import * as adminService from '@/services/admin'

const logs = ref([])
const loading = ref(false)
const error = ref(null)

async function fetchLogs() {
  loading.value = true
  error.value = null
  try {
    // Sort logs so the newest appear at the top
    const data = await adminService.getAuditLogs()
    logs.value = data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  } catch (err) {
    error.value = err.message || 'Failed to load audit logs.'
  } finally {
    loading.value = false
  }
}

function formatDateTime(dateString) {
  if (!dateString) return '—'
  const date = new Date(dateString)
  return date.toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

function formatAction(action) {
  if (!action) return 'UNKNOWN'
  return action.replace(/_/g, ' ')
}

onMounted(() => {
  fetchLogs()
})
</script>