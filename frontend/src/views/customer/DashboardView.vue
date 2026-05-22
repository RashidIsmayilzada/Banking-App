<template>
  <CustomerShell>
    <div class="row" style="margin-bottom:28px">
      <div>
        <div class="t-body-sm" style="margin-bottom:4px">{{ todayLabel }}</div>
        <h1 class="t-h1" style="margin:0">Good afternoon, {{ firstName }}.</h1>
      </div>
      <span class="spacer" />
      <button class="btn btn--secondary"><AppIcon name="download" :size="16" /> Statements</button>
      <RouterLink to="/customer/transfer" class="btn btn--primary">
        <AppIcon name="plus" :size="16" /> New transfer
      </RouterLink>
    </div>

    <div class="card card--dark" style="padding:32px;margin-bottom:20px">
      <div class="row">
        <div>
          <div class="t-label" style="margin-bottom:8px">Total balance · all accounts</div>
          <div class="t-display" style="margin:0;font-size:64px;color:#fff">
            {{ totalBalanceParts.whole }}<span style="opacity:0.55;font-size:40px">{{ totalBalanceParts.cents }}</span>
          </div>
          <div class="row" style="margin-top:16px;gap:24px;font-size:14px">
            <div class="row" style="gap:6px;color:rgba(255,255,255,0.85)">
              <AppIcon name="arrowDown" :size="14" /> {{ monthlyIn }} in this month
            </div>
            <div class="row" style="gap:6px;color:rgba(255,255,255,0.6)">
              <AppIcon name="arrowUp" :size="14" /> {{ monthlyOut }} out
            </div>
          </div>
        </div>
        <span class="spacer" />
        <SparkChart
          :bars="[35,50,30,65,45,70,55,80,60,75,90,85]"
          :width="200"
          :accent-last="true"
        />
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:28px">
      <div v-if="loading" class="card muted" style="padding:24px;text-align:center;grid-column:1 / -1">Loading accounts...</div>
      <div v-else-if="accounts.length === 0" class="card muted" style="padding:24px;text-align:center;grid-column:1 / -1">No accounts available yet.</div>
      <div v-for="account in accounts" :key="account.id" :class="['acct-card', account.accountType === 'SAVINGS' ? 'acct-card--teal' : '']">
        <div class="row">
          <span class="badge" :style="account.accountType === 'SAVINGS' ? 'background:rgba(255,255,255,0.15);color:#fff' : undefined">{{ formatAccountType(account.accountType) }}</span>
          <span class="spacer" />
          <AppIcon name="moreH" :size="16" />
        </div>
        <div class="acct-card__balance">{{ formatMoney(account.balance) }}</div>
        <div class="acct-card__iban">{{ formatIban(account.iban) }}</div>
        <div class="row" style="margin-top:16px;gap:8px">
          <RouterLink to="/customer/transfer" class="btn btn--xs" :style="account.accountType === 'SAVINGS' ? 'background:rgba(255,255,255,0.15);color:#fff' : 'background:var(--surface-soft)'">
            <AppIcon name="arrowUpRight" :size="12" /> Send
          </RouterLink>
          <button class="btn btn--xs" :style="account.accountType === 'SAVINGS' ? 'background:rgba(255,255,255,0.15);color:#fff' : 'background:var(--surface-soft)'">
            <AppIcon name="arrowDownLeft" :size="12" /> Add money
          </button>
        </div>
      </div>
    </div>

    <!-- Quick actions -->
    <div class="row" style="margin-bottom:16px">
      <h2 class="t-h3" style="margin:0">Quick actions</h2>
    </div>
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:32px">
      <div
        v-for="[icon, label, tone] in quickActions"
        :key="label"
        class="card card--flat"
        style="cursor:pointer;display:flex;align-items:center;gap:12px;padding:14px"
      >
        <div :class="['icon-box', `icon-box--${tone}`]"><AppIcon :name="icon" :size="18" /></div>
        <span style="font-weight:500;font-size:14px">{{ label }}</span>
      </div>
    </div>

    <!-- Recent activity -->
    <div class="row" style="margin-bottom:12px">
      <h2 class="t-h3" style="margin:0">Recent activity</h2>
      <span class="spacer" />
      <RouterLink to="/customer/transactions" style="font-size:13px;color:var(--ink);font-weight:500;text-decoration:none;border-bottom:1px solid currentColor">
        See all →
      </RouterLink>
    </div>
    <div class="card" style="padding:0">
      <table class="table">
        <tbody>
          <tr v-if="transactionsLoading">
            <td colspan="4" class="muted" style="padding:24px;text-align:center">Loading activity...</td>
          </tr>
          <tr v-else-if="recentTransactions.length === 0">
            <td colspan="4" class="muted" style="padding:24px;text-align:center">No recent activity.</td>
          </tr>
          <tr v-for="tx in recentTransactions" :key="tx.id">
            <td style="width:56px">
              <div :class="['icon-box', `icon-box--${tx.iconTone}`]" style="width:36px;height:36px">
                <AppIcon :name="tx.icon" :size="16" />
              </div>
            </td>
            <td>
              <div style="font-weight:500;font-size:14px">{{ tx.title }}</div>
              <div class="iban">{{ tx.sub }}</div>
            </td>
            <td class="num muted">{{ tx.when }}</td>
            <td class="num" style="text-align:right;font-size:15px;font-weight:500" :style="{ color: tx.positive ? 'var(--teal)' : 'var(--ink)' }">
              {{ tx.amount }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </CustomerShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import SparkChart from '@/components/shared/SparkChart.vue'
import * as userService from '@/services/user'

const todayLabel = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })
const user = ref(null)
const recentTransactions = ref([])
const loading = ref(false)
const transactionsLoading = ref(false)

const accounts = computed(() => user.value?.accounts || [])
const firstName = computed(() => user.value?.firstName || user.value?.username || 'there')
const totalBalance = computed(() => accounts.value.reduce((sum, account) => sum + Number(account.balance || 0), 0))
const totalBalanceParts = computed(() => splitMoney(totalBalance.value))
const monthlyIn = computed(() => formatMoney(0))
const monthlyOut = computed(() => formatMoney(0))

const quickActions = [
  ['arrowUpRight', 'Send',      'blue'],
  ['arrowSwap',    'Move',      'teal'],
  ['receipt',      'Pay bill',  'pink'],
  ['copy',         'Copy IBAN', 'brown'],
]

function formatMoney(value) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(value || 0))
}

function splitMoney(value) {
  const formatted = formatMoney(value)
  const [whole, cents = '00'] = formatted.split(',')
  return { whole, cents: `,${cents}` }
}

function formatAccountType(type) {
  return type ? type.charAt(0) + type.slice(1).toLowerCase() : 'Account'
}

function formatIban(iban) {
  return iban ? iban.replace(/(.{4})/g, '$1 ').trim() : '—'
}

async function fetchDashboard() {
  loading.value = true
  try {
    user.value = await userService.getCurrentUser()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchDashboard()
})
</script>
