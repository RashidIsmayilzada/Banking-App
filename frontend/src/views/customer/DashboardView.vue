<template>
  <CustomerShell>
    <div class="row" style="margin-bottom:28px">
      <div>
        <div class="t-body-sm" style="margin-bottom:4px">{{ todayLabel }}</div>
        <!-- TODO: Fetch user name from GET /api/user/profile -->
        <h1 class="t-h1" style="margin:0">Good afternoon, Jane.</h1>
      </div>
      <span class="spacer" />
      <button class="btn btn--secondary"><AppIcon name="download" :size="16" /> Statements</button>
      <RouterLink to="/customer/transfer" class="btn btn--primary">
        <AppIcon name="plus" :size="16" /> New transfer
      </RouterLink>
    </div>

    <!-- Total balance hero -->
    <!-- TODO: Fetch account summary from GET /api/accounts/summary -->
    <div class="card card--dark" style="padding:32px;margin-bottom:20px">
      <div class="row">
        <div>
          <div class="t-label" style="margin-bottom:8px">Total balance · all accounts</div>
          <div class="t-display" style="margin:0;font-size:64px;color:#fff">
            €8 421<span style="opacity:0.55;font-size:40px">,55</span>
          </div>
          <div class="row" style="margin-top:16px;gap:24px;font-size:14px">
            <div class="row" style="gap:6px;color:rgba(255,255,255,0.85)">
              <AppIcon name="arrowDown" :size="14" /> €1 200,00 in this month
            </div>
            <div class="row" style="gap:6px;color:rgba(255,255,255,0.6)">
              <AppIcon name="arrowUp" :size="14" /> €860,00 out
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

    <!-- Account tiles -->
    <!-- TODO: Fetch accounts from GET /api/accounts -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:28px">
      <div class="acct-card">
        <div class="row">
          <span class="badge">Checking</span>
          <span class="spacer" />
          <AppIcon name="moreH" :size="16" />
        </div>
        <div class="acct-card__balance">€6 218,40</div>
        <div class="acct-card__iban">NL42 INHO 0123 4567 89</div>
        <div class="row" style="margin-top:16px;gap:8px">
          <RouterLink to="/customer/transfer" class="btn btn--xs" style="background:var(--surface-soft)">
            <AppIcon name="arrowUpRight" :size="12" /> Send
          </RouterLink>
          <button class="btn btn--xs" style="background:var(--surface-soft)">
            <AppIcon name="arrowDownLeft" :size="12" /> Add money
          </button>
        </div>
      </div>
      <div class="acct-card acct-card--teal">
        <div class="row">
          <span class="badge" style="background:rgba(255,255,255,0.15);color:#fff">Savings</span>
          <span class="spacer" />
          <AppIcon name="moreH" :size="16" />
        </div>
        <div class="acct-card__balance">€2 203,15</div>
        <div class="acct-card__iban">NL42 INHO 0987 6543 21</div>
        <div class="row" style="margin-top:16px;gap:8px">
          <RouterLink to="/customer/transfer" class="btn btn--xs" style="background:rgba(255,255,255,0.15);color:#fff">
            <AppIcon name="arrowUpRight" :size="12" /> Send
          </RouterLink>
          <button class="btn btn--xs" style="background:rgba(255,255,255,0.15);color:#fff">
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
    <!-- TODO: Fetch transactions from GET /api/transactions?page=0&size=5 -->
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
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import SparkChart from '@/components/shared/SparkChart.vue'

const todayLabel = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })

const quickActions = [
  ['arrowUpRight', 'Send',      'blue'],
  ['arrowSwap',    'Move',      'teal'],
  ['receipt',      'Pay bill',  'pink'],
  ['copy',         'Copy IBAN', 'brown'],
]

// TODO: Fetch transactions from GET /api/transactions?page=0&size=5
const recentTransactions = [
  { id: 1, icon: 'cash',       iconTone: 'warn',  title: 'Albert Heijn',        sub: 'NL91 ABNA …42', when: '28 Apr · 14:22', amount: '−€42,18',     positive: false },
  { id: 2, icon: 'building',   iconTone: 'teal',  title: 'Salary · ACME BV',    sub: 'NL91 RABO …01', when: '27 Apr · 18:30', amount: '+€2 400,00',   positive: true  },
  { id: 3, icon: 'arrowSwap',  iconTone: 'ink',   title: 'Transfer to savings',  sub: 'NL42 INHO …21', when: '26 Apr · 12:10', amount: '−€300,00',    positive: false },
  { id: 4, icon: 'withdraw',   iconTone: 'brown', title: 'ATM withdrawal',       sub: 'ATM #14 · Centrum', when: '25 Apr · 20:45', amount: '−€100,00', positive: false },
  { id: 5, icon: 'creditCard', iconTone: 'pink',  title: 'Spotify subscription', sub: 'NL11 INGB …55', when: '24 Apr · 11:02', amount: '−€9,99',     positive: false },
]
</script>
