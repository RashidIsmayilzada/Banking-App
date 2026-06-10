<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <h1 class="t-h1" style="margin:0">System transactions</h1>
        <p class="t-body muted" style="margin:6px 0 0">{{ transactionSummary }}</p>
      </div>
      <span class="spacer" />
      <button class="btn btn--secondary btn--sm"><AppIcon name="download" :size="14" /> Export</button>
    </div>

    <!-- Filters -->
    <div class="card" style="margin-bottom:20px;padding:20px">
      <div style="display:grid;grid-template-columns:repeat(5,1fr) auto;gap:12px;align-items:end">
        <AppField label="Start date" v-model="filters.startDate" placeholder="2026-04-01" />
        <AppField label="End date"   v-model="filters.endDate"   placeholder="2026-04-28" />
        <AppField label="From IBAN"  v-model="filters.fromIban"  placeholder="NL.." />
        <AppField label="To IBAN"    v-model="filters.toIban"    placeholder="NL.." />
        <div class="field">
          <label class="field__label">Initiated by</label>
          <select class="select" v-model="filters.channel">
            <option value="">Anyone</option>
            <option value="CUSTOMER">Customer</option>
            <option value="EMPLOYEE">Employee</option>
            <option value="ATM">ATM</option>
          </select>
        </div>
        <button class="btn btn--primary" @click="applyFilters">Apply</button>
      </div>
    </div>

    <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">{{ error }}</div>
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:24px">Timestamp</th>
            <th>From</th>
            <th>To</th>
            <th>Initiated by</th>
            <th>Type</th>
            <th style="padding-right:24px;text-align:right">Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="muted" style="padding:24px;text-align:center">Loading transactions...</td>
          </tr>
          <tr v-else-if="transactions.length === 0">
            <td colspan="6" class="muted" style="padding:24px;text-align:center">No transactions found.</td>
          </tr>
          <tr v-for="tx in transactions" :key="tx.id">
            <td class="num" style="padding-left:24px">{{ tx.timestamp }}</td>
            <td class="iban">{{ tx.from }}</td>
            <td class="iban">{{ tx.to }}</td>
            <td>
              <div class="row" style="gap:8px">
                <AppAvatar :name="tx.initiator" />
                <span style="font-size:13px">{{ tx.initiator }}</span>
              </div>
            </td>
            <td>
              <span :class="['badge', tx.typeTone ? `badge--${tx.typeTone}` : '']">{{ tx.type }}</span>
            </td>
            <td class="num" style="padding-right:24px;text-align:right;font-weight:500">{{ tx.amount }}</td>
          </tr>
        </tbody>
      </table>
      <div style="padding:0 16px 12px" v-if="totalPages > 0">
        <AppPager :current-page="page + 1" :total="totalPages" :count="pageCount" @change="handlePageChange" />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppPager from '@/components/shared/AppPager.vue'
import { listTransactions } from '@/services/transaction'

const filters = ref({ startDate: '', endDate: '', fromIban: '', toIban: '', channel: '' })
const transactions = ref([])
const loading = ref(false)
const error = ref(null)
const page = ref(0)
const pageSize = 10
const totalElements = ref(0)
const totalPages = ref(0)

const transactionSummary = computed(() => `${totalElements.value} transactions`)
const pageCount = computed(() => {
  if (totalElements.value === 0) return '0 of 0'
  const start = page.value * pageSize + 1
  const end = Math.min((page.value + 1) * pageSize, totalElements.value)
  return `${start}–${end} of ${totalElements.value}`
})

function formatDate(isoString) {
  if (!isoString) return '—'
  const date = new Date(isoString)
  return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' }) + ' · ' + date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
}

function formatAmount(moneyDto) {
  if (!moneyDto) return '—'
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: moneyDto.currency || 'EUR' }).format(Number(moneyDto.amount))
}

function typeTone(channel) {
  if (channel === 'EMPLOYEE') return 'info'
  if (channel === 'ATM') return 'pink'
  return ''
}

function transactionParams() {
  return {
    page: page.value,
    size: pageSize,
    startDateTime: filters.value.startDate ? `${filters.value.startDate}T00:00:00` : undefined,
    endDateTime: filters.value.endDate ? `${filters.value.endDate}T23:59:59` : undefined,
    iban: filters.value.fromIban || filters.value.toIban || undefined,
    channel: filters.value.channel || undefined,
  }
}

async function fetchTransactions() {
  loading.value = true
  error.value = null
  try {
    const response = await listTransactions(transactionParams())
    transactions.value = (response.items || []).map(tx => ({
      id: tx.transactionId,
      timestamp: formatDate(tx.createdAt),
      from: tx.fromAccount?.iban || '—',
      to: tx.toAccount?.iban || '—',
      initiator: tx.initiatedByUserId ? `User #${tx.initiatedByUserId}` : tx.channel || 'System',
      type: tx.transactionType || tx.channel || 'Transaction',
      typeTone: typeTone(tx.channel),
      amount: formatAmount(tx.amount),
    }))
    totalElements.value = response.page?.totalElements ?? transactions.value.length
    totalPages.value = response.page?.totalPages ?? 1
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  fetchTransactions()
}

function handlePageChange(newPage) {
  page.value = newPage - 1
  fetchTransactions()
}

onMounted(fetchTransactions)
</script>
