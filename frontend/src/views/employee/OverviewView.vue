<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <div class="t-body-sm" style="margin-bottom:4px">{{ todayLabel }}</div>
        <h1 class="t-h1" style="margin:0">Hi {{ authStore.user?.username || 'Employee' }}.</h1>
      </div>
      <span class="spacer" />
      <RouterLink to="/employee/customers" class="btn btn--secondary">
        <AppIcon name="search" :size="16" /> Find customer
      </RouterLink>
      <RouterLink to="/employee/transfer" class="btn btn--primary">
        <AppIcon name="plus" :size="16" /> New transfer
      </RouterLink>
    </div>

    <!-- KPI cards -->
    <!-- TODO: Fetch stats from GET /employees/customers and GET /employees/transactions -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:32px">
      <div
        v-for="kpi in kpis"
        :key="kpi.label"
        :class="['card', kpi.dark ? 'card--dark' : '']"
        style="padding:20px"
      >
        <div class="row">
          <div :class="['icon-box', kpi.dark ? '' : `icon-box--${kpi.tone}`]"
               :style="kpi.dark ? { background:'rgba(255,255,255,0.15)', color:'#fff' } : {}">
            <AppIcon :name="kpi.icon" :size="18" />
          </div>
          <span class="spacer" />
          <AppIcon name="moreH" :size="16" style="opacity:0.5" />
        </div>
        <div class="kpi__value" style="margin-top:24px" :style="{ color: kpi.dark ? '#fff' : 'var(--ink)' }">{{ kpi.value }}</div>
        <div class="row" style="margin-top:8px;gap:8px">
          <span class="kpi__label" :style="{ color: kpi.dark ? 'rgba(255,255,255,0.6)' : undefined }">{{ kpi.label }}</span>
          <span class="spacer" />
        </div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:2fr 1fr;gap:20px">
      <!-- Latest activity -->
      <!-- TODO: Fetch recent activity from GET /employees/transactions?size=5 -->
      <div class="card" style="padding:0">
        <div class="row" style="padding:20px 24px;border-bottom:1px solid var(--line)">
          <h3 class="t-h3" style="margin:0">Latest activity</h3>
          <span class="spacer" />
          <RouterLink to="/employee/transactions" style="font-size:13px;color:var(--ink);font-weight:500;text-decoration:none;border-bottom:1px solid currentColor">
            View all →
          </RouterLink>
        </div>
        <table class="table">
          <tbody>
            <tr v-if="activityLoading">
              <td colspan="4" class="muted" style="padding:24px;text-align:center">Loading activity...</td>
            </tr>
            <tr v-else-if="activity.length === 0">
              <td colspan="4" class="muted" style="padding:24px;text-align:center">No recent activity.</td>
            </tr>
            <tr v-for="(a, i) in activity" v-else :key="i">
              <td style="width:48px;padding-left:24px">
                <div :class="['icon-box', `icon-box--${a.tone}`]" style="width:32px;height:32px">
                  <AppIcon :name="a.icon" :size="14" />
                </div>
              </td>
              <td>
                <div style="font-weight:500;font-size:14px">{{ a.title }}</div>
                <div class="t-body-sm" style="margin-top:2px">{{ a.sub }}</div>
              </td>
              <td class="num muted">{{ a.time }}</td>
              <td style="padding-right:24px;text-align:right">
                <span :class="['badge', a.status === 'Pending' ? 'badge--warn' : a.status === 'By you' ? 'badge--info' : 'badge--success']">
                  {{ a.status }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Quick actions -->
      <div class="card">
        <h3 class="t-h3" style="margin:0 0 14px">Quick actions</h3>
        <div class="col" style="gap:8px">
          <RouterLink
            v-for="qa in quickActions"
            :key="qa.label"
            :to="qa.to"
            class="row"
            style="padding:12px;border-radius:12px;cursor:pointer;text-decoration:none;color:inherit;transition:background 0.12s ease"
            @mouseover="($el) => $el.style.background = 'var(--surface-soft)'"
          >
            <div :class="['icon-box', `icon-box--${qa.tone || 'ink'}`]" style="width:36px;height:36px">
              <AppIcon :name="qa.icon" :size="16" />
            </div>
            <div>
              <div style="font-weight:500;font-size:14px">{{ qa.label }}</div>
              <div class="t-body-sm">{{ qa.sub }}</div>
            </div>
            <span class="spacer" />
            <AppIcon name="chevronRight" :size="14" style="color:var(--ink-faint)" />
          </RouterLink>
        </div>
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import { useAuth } from '@/stores/auth'
import * as userService from '@/services/user'
import { listTransactions } from '@/services/transaction'

const authStore = useAuth()

const todayLabel = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }) + ' · ' + new Date().toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })

const totalCustomers = ref('...')
const pendingApprovals = ref('...')
const transactionCount = ref('...')
const transactionVolume = ref('...')
const activity = ref([])
const activityLoading = ref(false)

const kpis = computed(() => [
  { label: 'Total customers',   value: totalCustomers.value,    icon: 'users', tone: '',     dark: true  },
  { label: 'Pending approvals', value: pendingApprovals.value,  icon: 'clock', tone: 'warn', dark: false },
  { label: 'Transactions',      value: transactionCount.value,  icon: 'list',  tone: 'blue', dark: false },
  { label: 'Listed volume',     value: transactionVolume.value, icon: 'euro',  tone: 'teal', dark: false },
])

async function fetchStats() {
  try {
    const allUsers = await userService.getAllUsers({ role: 'CUSTOMER', size: 1 })
    totalCustomers.value = allUsers.totalElements.toString()
    
    const pendingCount = await userService.getPendingApprovalCount()
    pendingApprovals.value = pendingCount.toString()

    const transactions = await listTransactions({ page: 0, size: 5 })
    const items = transactions.items || []
    transactionCount.value = (transactions.page?.totalElements ?? items.length).toString()
    transactionVolume.value = formatEur(items.reduce((sum, tx) => sum + Number(tx.amount?.amount || 0), 0))
  } catch (err) {
    console.error('Failed to fetch stats:', err)
  }
}

function formatEur(amount) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amount)
}

function activityTone(channel) {
  if (channel === 'EMPLOYEE') return 'blue'
  if (channel === 'ATM') return 'pink'
  return 'teal'
}

function activityIcon(type) {
  if (type === 'WITHDRAWAL') return 'withdraw'
  if (type === 'DEPOSIT') return 'deposit'
  return 'arrowSwap'
}

function activityTime(isoString) {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
}

async function fetchActivity() {
  activityLoading.value = true
  try {
    const response = await listTransactions({ page: 0, size: 5 })
    activity.value = (response.items || []).map(tx => ({
      icon: activityIcon(tx.transactionType),
      tone: activityTone(tx.channel),
      title: tx.transactionType || 'Transaction',
      sub: `${tx.fromAccount?.iban || '—'} → ${tx.toAccount?.iban || '—'}`,
      time: activityTime(tx.createdAt),
      status: tx.channel || 'Settled',
    }))
  } catch (err) {
    console.error('Failed to fetch activity:', err)
    activity.value = []
  } finally {
    activityLoading.value = false
  }
}

const quickActions = [
  { icon: 'users',    label: 'All customer accounts',       sub: 'Active database',       to: '/employee/customers'   },
  { icon: 'clock',    label: 'Pending approvals',           sub: 'Awaiting review',      to: '/employee/approvals', tone: 'warn' },
  { icon: 'list',     label: 'System transactions',         sub: 'Live ledger',      to: '/employee/transactions'},
  { icon: 'transfer', label: 'Transfer between customers',  sub: 'Audited move',     to: '/employee/transfer'   },
]

onMounted(() => {
  fetchStats()
  fetchActivity()
})
</script>
