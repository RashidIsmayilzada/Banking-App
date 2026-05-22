<template>
  <CustomerShell>
    <div class="row" style="margin-bottom:20px">
      <RouterLink to="/customer/dashboard" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> Accounts
      </RouterLink>
      <span class="spacer" />
      <button class="btn btn--secondary btn--sm"><AppIcon name="download" :size="14" /> Statement</button>
      <button class="btn btn--secondary btn--sm"><AppIcon name="settings" :size="14" /> Settings</button>
    </div>

    <div class="row" style="margin-bottom:28px;gap:16px">
      <span class="badge badge--dark">{{ formatAccountType(account?.accountType) }}</span>
      <AppStatus :kind="account?.active ? 'active' : 'closed'" />
    </div>
    <div class="t-display" style="margin:0;font-size:64px">
      {{ balanceParts.whole }}<span style="opacity:0.4">{{ balanceParts.cents }}</span>
    </div>
    <div class="iban" style="margin-top:8px;font-size:14px">{{ formatIban(account?.iban) }} · BIC INHONL2A</div>

    <div class="row" style="margin-top:24px;margin-bottom:32px;gap:8px">
      <RouterLink to="/customer/transfer" class="btn btn--primary">
        <AppIcon name="arrowUpRight" :size="16" /> Send
      </RouterLink>
      <button class="btn btn--secondary"><AppIcon name="arrowSwap" :size="16" /> Move to savings</button>
      <button class="btn btn--secondary"><AppIcon name="copy" :size="16" /> Copy IBAN</button>
    </div>

    <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:20px">
      <div>
        <h3 class="t-h3" style="margin:0 0 12px">Account details</h3>
        <div class="card" style="padding:0">
          <div
            v-for="([k, v, mono], i) in accountDetails"
            :key="k"
            class="row"
            :style="{ padding:'14px 20px', borderBottom: i < accountDetails.length - 1 ? '1px solid var(--line)' : 'none' }"
          >
            <span class="muted" style="font-size:14px">{{ k }}</span>
            <span class="spacer" />
            <span :class="mono ? 'iban' : ''" style="font-weight:500;font-size:14px">{{ v }}</span>
          </div>
        </div>
      </div>

      <div class="col" style="gap:16px">
        <div class="card">
          <h3 class="t-h3" style="margin:0 0 16px">This month</h3>
          <div class="row" style="justify-content:space-between">
            <span class="muted" style="font-size:14px">In</span>
            <span style="color:var(--teal);font-weight:500">+{{ formatMoney(monthlyIn) }}</span>
          </div>
          <hr class="divider" style="margin:10px 0" />
          <div class="row" style="justify-content:space-between">
            <span class="muted" style="font-size:14px">Out</span>
            <span style="font-weight:500">−{{ formatMoney(monthlyOut) }}</span>
          </div>
          <SparkChart :bars="[40,25,60,35,55,30,70,45,65,50,75,60,80]" :height="48" :dim-early="true" style="margin-top:16px" />
        </div>
        <div class="card card--soft">
          <div class="row">
            <AppIcon name="info" :size="16" />
            <strong style="font-size:14px">Tip</strong>
          </div>
          <p class="t-body-sm" style="margin:6px 0 0">
            Set up a recurring transfer to your savings to hit your goal faster.
          </p>
          <RouterLink to="/customer/transfer" style="color:var(--ink);font-weight:500;font-size:13px;margin-top:8px;display:inline-block;text-decoration:none;border-bottom:1px solid currentColor">
            Set up auto-save →
          </RouterLink>
        </div>
      </div>
    </div>
  </CustomerShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import SparkChart from '@/components/shared/SparkChart.vue'
import * as userService from '@/services/user'

const user = ref(null)
const account = computed(() => user.value?.accounts?.find(item => item.accountType === 'CHECKING') || user.value?.accounts?.[0] || null)
const accountHolder = computed(() => user.value ? [user.value.firstName, user.value.lastName].filter(Boolean).join(' ') || user.value.username : '—')
const balanceParts = computed(() => splitMoney(account.value?.balance || 0))
const monthlyIn = computed(() => 0)
const monthlyOut = computed(() => 0)

const accountDetails = computed(() => [
  ['Account holder', accountHolder.value, false],
  ['Account type', formatAccountType(account.value?.accountType), false],
  ['Daily transfer limit', formatMoney(account.value?.dailyTransferLimit), false],
  ['Absolute limit (min balance)', formatMoney(account.value?.absoluteTransferLimit), false],
  ['Opened on', formatDate(account.value?.createdAt), false],
  ['BIC', 'INHONL2A', true],
])

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

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('en-GB', { day: '2-digit', month: 'long', year: 'numeric' }) : '—'
}

onMounted(async () => {
  user.value = await userService.getCurrentUser()
})
</script>
