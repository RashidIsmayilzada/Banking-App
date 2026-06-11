<template>
  <AdminShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">System Accounts</h1>
      <span class="spacer" />
      <div class="row" style="gap:8px;position:relative">
        <AppIcon name="search" :size="14" style="position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--ink-faint)" />
        <input class="input" placeholder="Search IBAN..." style="width:280px;padding-left:38px" v-model="searchQuery" />
      </div>
    </div>

    <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">
      <AppIcon name="alert" :size="16" class="banner__icon" />
      <div>{{ error }}</div>
    </div>

    <div class="card" style="padding:0">
      <table class="table">
        <thead>
        <tr>
          <th style="padding-left:24px">Account (IBAN)</th>
          <th>Type</th>
          <th>Balance</th>
          <th>Status</th>
          <th style="padding-right:24px;text-align:right">Admin Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="loading">
          <td colspan="5" class="muted" style="padding:24px;text-align:center">Loading system accounts...</td>
        </tr>
        <tr v-else-if="filteredAccounts.length === 0">
          <td colspan="5" class="muted" style="padding:24px;text-align:center">No accounts found.</td>
        </tr>
        <tr v-for="acc in filteredAccounts" :key="acc.id">
          <td style="padding-left:24px">
            <div class="iban" style="font-size: 14px; color: var(--ink); font-weight: 500;">
              {{ acc.iban }}
            </div>
            <div class="t-body-sm" style="margin-top: 2px;">ID: {{ acc.id }}</div>
          </td>
          <td>
              <span class="badge" :class="acc.accountType === 'SAVINGS' ? '' : 'badge--dark'" :style="acc.accountType === 'SAVINGS' ? 'background:var(--teal);color:#fff' : ''">
                {{ formatAccountType(acc.accountType) }}
              </span>
          </td>
          <td class="num" style="font-weight: 500;">
            {{ formatEur(acc.balance?.amount || 0) }}
          </td>
          <td>
            <AppStatus :kind="getStatusKind(acc)" />
          </td>
          <td style="padding-right:24px;text-align:right">
            <div class="row" style="justify-content:flex-end;gap:6px">
              <button
                  v-if="!acc.closed"
                  :class="['btn', 'btn--xs', acc.frozen ? 'btn--ghost' : 'btn--ghost-danger']"
                  @click="toggleFreeze(acc)"
              >
                {{ acc.frozen ? 'Unfreeze' : 'Freeze' }}
              </button>

              <button
                  v-if="!acc.closed"
                  class="btn btn--danger btn--xs"
                  @click="handleClose(acc)"
              >
                Force Close
              </button>
            </div>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </AdminShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AdminShell from '@/components/layout/AdminShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import * as adminService from '@/services/admin'

const accounts = ref([])
const loading = ref(false)
const error = ref(null)
const searchQuery = ref('')

// Filter accounts locally based on the search box
const filteredAccounts = computed(() => {
  if (!searchQuery.value) return accounts.value
  const lowerQuery = searchQuery.value.toLowerCase()
  return accounts.value.filter(acc =>
      acc.iban.toLowerCase().includes(lowerQuery) ||
      acc.id.toString().includes(lowerQuery)
  )
})

async function fetchAccounts() {
  loading.value = true
  error.value = null
  try {
    accounts.value = await adminService.getAllAccounts()
  } catch (err) {
    error.value = err.message || 'Failed to load accounts.'
  } finally {
    loading.value = false
  }
}

async function toggleFreeze(acc) {
  const action = acc.frozen ? 'unfreeze' : 'freeze'
  if (!confirm(`Are you sure you want to ${action} account ${acc.iban}?`)) return

  try {
    if (acc.frozen) {
      await adminService.unfreezeAccount(acc.id)
      acc.frozen = false
    } else {
      await adminService.freezeAccount(acc.id)
      acc.frozen = true
    }
  } catch (err) {
    alert(`Failed to ${action} account: ` + err.message)
  }
}

async function handleClose(acc) {
  if (!confirm(`CRITICAL WARNING: Are you absolutely sure you want to FORCE CLOSE account ${acc.iban}? This action cannot be undone.`)) return

  try {
    await adminService.closeAccount(acc.id)
    acc.closed = true
    acc.status = 'CLOSED'
  } catch (err) {
    alert('Failed to close account: ' + err.message)
  }
}

// Formatters
function formatEur(amount) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amount)
}

function formatAccountType(type) {
  if (!type) return 'Unknown'
  return type.charAt(0) + type.slice(1).toLowerCase()
}

function getStatusKind(acc) {
  if (acc.closed || acc.status === 'CLOSED') return 'closed'
  if (acc.frozen) return 'pending' // Uses your yellow warning badge
  return 'active'
}

onMounted(() => {
  fetchAccounts()
})
</script>