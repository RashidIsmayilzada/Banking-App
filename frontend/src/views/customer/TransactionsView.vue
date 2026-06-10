<template>
  <CustomerShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">Transactions</h1>
      <span class="spacer" />
      <button class="btn btn--secondary btn--sm"><AppIcon name="download" :size="14" /> Export CSV</button>
    </div>

    <!-- Filters -->
    <div class="card" style="margin-bottom:20px;padding:20px">
      <div class="row" style="margin-bottom:14px">
        <AppIcon name="filter" :size="16" />
        <h3 class="t-h4" style="margin:0">Search &amp; filter</h3>
        <span class="spacer" />
        <a style="font-size:13px;color:var(--ink-soft);cursor:pointer" @click="clearFilters">Clear all</a>
      </div>
      <div style="display:grid;grid-template-columns:repeat(4,1fr) auto;gap:12px;align-items:end">
        <AppField label="Start date" v-model="filters.startDate" placeholder="2026-04-01" />
        <AppField label="End date" v-model="filters.endDate" placeholder="2026-04-28" />
        <div class="field">
          <label class="field__label">Amount</label>
          <div class="row" style="gap:6px">
            <select class="select" style="width:70px;padding:12px 8px" v-model="filters.amountOp">
              <option>=</option><option>></option><option>&lt;</option>
            </select>
            <input class="input" placeholder="€ 100,00" v-model="filters.amount" />
          </div>
        </div>
        <AppField label="Counterparty IBAN" v-model="filters.iban" placeholder="NL.. INHO .." />
        <button class="btn btn--primary" @click="applyFilters">Apply</button>
      </div>
      <div v-if="activeFilters.length" class="row" style="margin-top:14px;gap:6px;flex-wrap:wrap">
        <span class="t-body-sm" style="color:var(--ink-faint)">Active:</span>
        <span v-for="f in activeFilters" :key="f" class="badge badge--dark">
          {{ f }} <AppIcon name="x" :size="10" />
        </span>
      </div>
    </div>

    <!-- Table -->
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:20px">Date</th>
            <th>Description</th>
            <th>From</th>
            <th>To</th>
            <th style="text-align:right;padding-right:20px">Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="5" class="muted" style="padding:24px;text-align:center">Loading transactions...</td>
          </tr>
          <tr v-else-if="transactions.length === 0">
            <td colspan="5" class="muted" style="padding:24px;text-align:center">No transactions found.</td>
          </tr>
          <tr v-for="tx in transactions" :key="tx.transactionId">
            <td class="num" style="padding-left:20px">{{ tx.date }}</td>
            <td style="font-weight:500">{{ tx.description }}</td>
            <td class="iban">{{ tx.from }}</td>
            <td class="iban">{{ tx.to }}</td>
            <td
              class="num"
              style="text-align:right;padding-right:20px;font-weight:500;font-size:14px"
              :style="{ color: tx.amount.startsWith('+') ? 'var(--teal)' : 'var(--ink)' }"
            >{{ tx.amount }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="pageMeta.totalPages > 0" style="padding:0 16px 12px">
        <AppPager :current-page="pageMeta.page + 1" :total="pageMeta.totalPages" :count="pageCount" @change="handlePageChange" />
      </div>
    </div>
  </CustomerShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppPager from '@/components/shared/AppPager.vue'
import * as userService from '@/services/user'
import { listTransactions } from '@/services/transaction.js'

const filters = ref({ startDate: '', endDate: '', amountOp: '=', amount: '', iban: '' })
const activeFilters = ref([])
const { user: currentUser } = useAuth()
const transactions = ref([])
const loading = ref(false)
const pageMeta = ref({ page: 0, size: 8, totalElements: 0, totalPages: 0 })
const pageCount = computed(() => {
  if (!pageMeta.value.totalElements) return '0 of 0'
  const start = pageMeta.value.page * pageMeta.value.size + 1
  const end = Math.min((pageMeta.value.page + 1) * pageMeta.value.size, pageMeta.value.totalElements)
  return `${start}–${end} of ${pageMeta.value.totalElements}`
})

function clearFilters() {
  filters.value = { startDate: '', endDate: '', amountOp: '=', amount: '', iban: '' }
  activeFilters.value = []
  pageMeta.value = { ...pageMeta.value, page: 0 }
  fetchTransactions()
}

function applyFilters() {
  activeFilters.value = Object.entries(filters.value).filter(([k, v]) => v && k !== 'amountOp').map(([, v]) => v)
  pageMeta.value = { ...pageMeta.value, page: 0 }
  fetchTransactions()
}

function parseMoney(value) {
  return value ? value.replace(/[€\s]/g, '').replace(',', '.') : ''
}

function formatMoney(value) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(value || 0))
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'
}

function buildParams() {
  const params = { page: pageMeta.value.page, size: pageMeta.value.size }
  if (filters.value.startDate) params.startDateTime = `${filters.value.startDate}T00:00:00`
  if (filters.value.endDate) params.endDateTime = `${filters.value.endDate}T23:59:59`
  if (filters.value.iban) params.iban = filters.value.iban
  const amount = parseMoney(filters.value.amount)
  if (amount) {
    if (filters.value.amountOp === '>') params.amountMin = amount
    else if (filters.value.amountOp === '<') params.amountMax = amount
    else params.amountEquals = amount
  }
  return params
}

function mapTransaction(tx) {
  // Outgoing when the source account belongs to the current user (deposits have no source).
  const outgoing = tx.fromAccount?.userId === currentUser.value?.userId
  const sign = outgoing ? '-' : '+'
  return {
    transactionId: tx.transactionId,
    date: formatDateTime(tx.createdAt),
    description: tx.description || '—',
    from: tx.fromAccount?.iban || '—',
    to: tx.toAccount?.iban || '—',
    amount: `${sign}${formatMoney(tx.amount.amount)}`,
  }
}

async function fetchTransactions() {
  loading.value = true
  transactions.value = []
  try {
    const params = {
      page: pageMeta.value.page,
      size: pageMeta.value.size,
    }

    if (filters.value.startDate) params.startDateTime = filters.value.startDate + 'T00:00:00'
    if (filters.value.endDate) params.endDateTime = filters.value.endDate + 'T23:59:59'

    const amount = parseMoney(filters.value.amount)
    if (amount) {
      if (filters.value.amountOp === '=') params.amountEquals = amount
      else if (filters.value.amountOp === '>') params.amountMin = amount
      else if (filters.value.amountOp === '<') params.amountMax = amount
    }

    if (filters.value.iban) params.iban = filters.value.iban.replace(/\s/g, '')

    const data = await listTransactions(params)
    console.log('[DEBUG] Received transactions data:', data)
    pageMeta.value = data.page

    const userId = currentUser.value?.id
    console.log('[DEBUG] Current userId:', userId)
    transactions.value = (data.items || []).map(tx => {
      const isIncoming = tx.transactionType === 'DEPOSIT' ||
        (tx.transactionType === 'TRANSFER' && tx.toAccount?.userId === userId)
      return {
        id: tx.transactionId,
        date: formatDateTime(tx.createdAt),
        description: tx.description,
        from: tx.fromAccount?.iban || '—',
        to: tx.toAccount?.iban || '—',
        amount: (isIncoming ? '+' : '-') + formatMoney(tx.amount?.amount),
      }
    })
  } catch (err) {
    console.error('[DEBUG] Failed to fetch transactions:', err)
    transactions.value = []
  } finally {
    loading.value = false
  }
}

function handlePageChange(newPage) {
  pageMeta.value = { ...pageMeta.value, page: newPage - 1 }
  fetchTransactions()
}

onMounted(async () => {
  await fetchTransactions()
})
</script>
