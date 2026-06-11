<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:20px">
      <RouterLink to="/employee/customers" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> All customers
      </RouterLink>
      <span class="spacer" />
      <RouterLink :to="`/employee/limits/${customer?.id || ''}`" class="btn btn--secondary btn--sm">
        <AppIcon name="shield" :size="14" /> Set transfer limits
      </RouterLink>
      <button
        v-if="customer?.status === 'CLOSED'"
        class="btn btn--ghost btn--sm"
        @click="reopenCustomer"
        :disabled="closing"
      >{{ closing ? 'Reopening...' : 'Reopen customer' }}</button>
      <button
        v-else
        class="btn btn--ghost-danger btn--sm"
        @click="closeCustomer"
        :disabled="closing"
      >{{ closing ? 'Closing...' : 'Close customer' }}</button>
    </div>

    <div v-if="loading" style="padding: 40px; text-align: center;">Loading customer details...</div>
    <div v-else-if="error" class="banner banner--danger" style="margin-bottom: 24px;">{{ error }}</div>
    <div v-else-if="customer">
      <div class="row" style="margin-bottom:24px;gap:20px">
        <AppAvatar :name="customerName" size="lg" />
        <div>
          <h1 class="t-h1" style="margin:0">{{ customerName }}</h1>
          <div class="row" style="gap:12px;margin-top:6px">
            <span class="t-body-sm">Customer #{{ customer.id }}</span>
            <span class="t-body-sm">·</span>
            <span class="t-body-sm">Joined {{ formatDate(customer.registeredAt) }}</span>
            <AppStatus :kind="statusKind(customer.status)" />
          </div>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:24px">
        <div class="card" style="padding:0">
          <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
            <h3 class="t-h4" style="margin:0">Personal information</h3>
          </div>
          <div
            v-for="([k, v, mono], i) in personalInfo"
            :key="k"
            class="row"
            :style="{ padding:'12px 20px', borderBottom: i < personalInfo.length - 1 ? '1px solid var(--line)' : 'none' }"
          >
            <span class="muted" style="font-size:13px">{{ k }}</span>
            <span class="spacer" />
            <span :class="mono ? 'iban' : ''" style="font-size:14px;font-weight:500;color:var(--ink)">{{ v }}</span>
          </div>
        </div>

        <div class="card" style="padding:0">
          <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
            <h3 class="t-h4" style="margin:0">Accounts</h3>
          </div>
          <div style="padding:16px;display:flex;flex-direction:column;gap:10px">
            <template v-if="customer.accounts && customer.accounts.length > 0">
              <div
                v-for="acc in customer.accounts"
                :key="acc.iban"
                class="card card--flat"
                :style="{ padding:'14px', background: acc.accountType === 'SAVINGS' ? 'var(--teal-tint)' : undefined }"
              >
                <div class="row">
                  <span class="badge" :class="acc.accountType === 'SAVINGS' ? '' : 'badge--dark'" :style="acc.accountType === 'SAVINGS' ? 'background:var(--teal);color:#fff' : ''">
                    {{ formatAccountType(acc.accountType) }}
                  </span>
                  <span class="iban">{{ acc.iban }}</span>
                  <span class="spacer" />
                  <span style="font-weight:500">{{ formatMoney(acc.balance?.amount) }}</span>
                </div>
                <div v-if="acc.status === 'CLOSED'" style="margin-top:8px;display:flex;align-items:center;gap:8px">
                  <span class="badge badge--danger">CLOSED</span>
                  <button class="btn btn--ghost btn--sm" style="flex:1" @click="openAccount(acc.iban)" :disabled="closing">
                    Open account
                  </button>
                </div>
                <button v-else class="btn btn--ghost-danger btn--sm" style="margin-top:8px;width:100%" @click="closeAccount(acc.iban)" :disabled="closing">
                  Close account
                </button>
              </div>
            </template>
            <div v-else class="muted" style="padding: 10px; text-align: center; font-size: 13px;">No accounts yet.</div>
            
            <hr class="divider" style="margin:6px 0" />
            <!-- Note: Limits are usually per account or per user, here we show them if available -->
            <div class="row" style="font-size:13px">
              <span class="muted">Daily transfer limit</span>
              <span class="spacer" />
              <span style="font-weight:500">{{ formatMoney(primaryAccount?.dailyTransferLimit?.amount) }}</span>
            </div>
            <div class="row" style="font-size:13px">
              <span class="muted">Absolute limit</span>
              <span class="spacer" />
              <span style="font-weight:500">{{ formatMoney(primaryAccount?.absoluteTransferLimit?.amount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Transaction history -->
    <h3 class="t-h3" style="margin:0 0 12px">Transaction history</h3>
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:24px">Date</th>
            <th>From</th>
            <th>To</th>
            <th>Initiated by</th>
            <th style="padding-right:24px;text-align:right">Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="transactionsLoading">
            <td colspan="5" class="muted" style="padding:24px;text-align:center">Loading transactions...</td>
          </tr>
          <tr v-else-if="transactions.length === 0">
            <td colspan="5" class="muted" style="padding:24px;text-align:center">No transactions found.</td>
          </tr>
          <tr v-for="tx in transactions" :key="tx.id">
            <td class="num" style="padding-left:24px">{{ tx.date }}</td>
            <td class="iban">{{ tx.from }}</td>
            <td class="iban">{{ tx.to }}</td>
            <td>
              <div class="row" style="gap:8px">
                <AppAvatar :name="tx.initiator" />
                <span style="font-size:13px">{{ tx.initiator }}</span>
              </div>
            </td>
            <td class="num" style="padding-right:24px;text-align:right;font-weight:500"
                :style="{ color: tx.amount.startsWith('+') ? 'var(--teal)' : 'var(--ink)' }">
              {{ tx.amount }}
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="transactionPage.totalPages > 0" style="padding:0 16px 12px">
        <AppPager :current-page="transactionPage.page + 1" :total="transactionPage.totalPages" :count="transactionCount" @change="handleTransactionPage" />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import AppPager from '@/components/shared/AppPager.vue'
import * as userService from '@/services/user'
import * as accountService from '@/services/accounts'

const route = useRoute()
const router = useRouter()
const customer = ref(null)
const loading = ref(false)
const error = ref(null)
const transactions = ref([])
const transactionsLoading = ref(false)
const transactionPage = ref({ page: 0, size: 5, totalElements: 0, totalPages: 0 })
const closing = ref(false)

const customerName = computed(() => customer.value ? displayName(customer.value) : '')
const primaryAccount = computed(() => customer.value?.accounts?.find(a => a.accountType === 'CHECKING') || customer.value?.accounts?.[0] || null)
const transactionCount = computed(() => {
  const meta = transactionPage.value
  if (!meta.totalElements) return '0 of 0'
  const start = meta.page * meta.size + 1
  const end = Math.min((meta.page + 1) * meta.size, meta.totalElements)
  return `${start}–${end} of ${meta.totalElements}`
})

const personalInfo = computed(() => {
  if (!customer.value) return []
  return [
    ['Email', customer.value.email || '—', false],
    ['Phone', customer.value.phoneNumber || '—', false],
    ['BSN', customer.value.bsn || '—', true],
    ['Status', customer.value.status || '—', false],
  ]
})

function displayName(user) {
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || user.email || 'Customer'
}

function statusKind(status) {
  if (status === 'APPROVED') return 'active'
  if (status === 'REJECTED') return 'rejected'
  if (status === 'CLOSED') return 'closed'
  return 'pending'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'
}

function formatMoney(value, signed = false) {
  const number = Number(value || 0)
  const amount = new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Math.abs(number))
  if (!signed) return number < 0 ? `−${amount}` : amount
  return number >= 0 ? `+${amount}` : `−${amount}`
}

function formatAccountType(type) {
  return type ? type.charAt(0) + type.slice(1).toLowerCase() : 'Account'
}

function mapTransaction(tx) {
  const amount = Number(tx.amount?.amount || 0)
  const involvesTo = tx.toAccount?.userId === Number(route.params.id)
  return {
    id: tx.transactionId,
    date: formatDateTime(tx.createdAt),
    from: tx.fromAccount?.iban || '—',
    to: tx.toAccount?.iban || '—',
    initiator: tx.initiatedByUserId === Number(route.params.id) ? customerName.value : `User #${tx.initiatedByUserId}`,
    amount: formatMoney(involvesTo ? amount : -amount, true),
  }
}

async function fetchCustomer() {
  loading.value = true
  try {
    customer.value = await userService.getUserById(route.params.id)
    await fetchTransactions()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function fetchTransactions() {
  transactions.value = []
  transactionPage.value = { page: 0, size: 5, totalElements: 0, totalPages: 0 }
}

function handleTransactionPage(newPage) {
  transactionPage.value = { ...transactionPage.value, page: newPage - 1 }
  fetchTransactions()
}

async function closeAccount(iban) {
  if (!confirm(`Are you sure you want to close account ${iban}? This action cannot be undone.`)) {
    return
  }

  closing.value = true
  error.value = null

  try {
    await accountService.updateAccount(iban, { status: 'CLOSED' })
    await fetchCustomer()
    alert('Account closed successfully')
  } catch (err) {
    error.value = err.message || 'Failed to close account'
  } finally {
    closing.value = false
  }
}

async function openAccount(iban) {
  if (!confirm(`Reactivate account ${iban}?`)) return

  closing.value = true
  error.value = null
  try {
    await accountService.updateAccount(iban, { status: 'ACTIVE' })
    await fetchCustomer()
  } catch (err) {
    error.value = err.message || 'Failed to reactivate account'
  } finally {
    closing.value = false
  }
}

async function closeCustomer() {
  if (!confirm(`Close this customer profile? Their status will be set to CLOSED and all accounts will be closed.`)) return
  closing.value = true
  error.value = null
  try {
    await userService.closeUser(customer.value.id)
    await fetchCustomer()
  } catch (err) {
    error.value = err.message || 'Failed to close customer'
  } finally {
    closing.value = false
  }
}

async function reopenCustomer() {
  if (!confirm(`Reopen this customer profile? Their status will be set to APPROVED and all accounts will be reopened.`)) return
  closing.value = true
  error.value = null
  try {
    await userService.reopenUser(customer.value.id)
    await fetchCustomer()
  } catch (err) {
    error.value = err.message || 'Failed to reopen customer'
  } finally {
    closing.value = false
  }
}

onMounted(() => {
  fetchCustomer()
})
</script>
