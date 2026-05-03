<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <div class="t-body-sm" style="margin-bottom:4px">{{ todayLabel }}</div>
        <!-- TODO: Fetch employee name from GET /api/user/profile -->
        <h1 class="t-h1" style="margin:0">Hi Sven.</h1>
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
          <span v-if="kpi.sub" style="font-size:12px;font-weight:500"
                :style="{ color: kpi.deltaTone === 'pos' ? 'var(--teal)' : kpi.deltaTone === 'warn' ? 'var(--warning)' : kpi.dark ? 'rgba(255,255,255,0.7)' : 'var(--ink-soft)' }">
            {{ kpi.sub }}
          </span>
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
            <tr v-for="(a, i) in activity" :key="i">
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
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'

const todayLabel = new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }) + ' · ' + new Date().toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })

// TODO: Fetch stats from GET /employees/customers and GET /employees/transactions
const kpis = [
  { label: 'Total customers',     value: '247',    sub: '+8 this month', deltaTone: 'pos',  icon: 'users', tone: '',     dark: true  },
  { label: 'Pending approvals',   value: '12',     sub: '3 over 24h',   deltaTone: 'warn', icon: 'clock', tone: 'warn', dark: false },
  { label: 'Transactions today',  value: '1 084',  sub: '+12% vs avg',  deltaTone: 'pos',  icon: 'list',  tone: 'blue', dark: false },
  { label: 'Volume today',        value: '€412k',  sub: '↑ €38k',       deltaTone: 'pos',  icon: 'euro',  tone: 'teal', dark: false },
]

// TODO: Fetch from GET /employees/transactions?size=5
const activity = [
  { icon: 'user',     tone: 'pink',  title: 'New registration',              sub: 'Tom Bakker · BSN 987-654-321',      time: '14:02', status: 'Pending'  },
  { icon: 'arrowSwap',tone: 'blue',  title: 'Transfer · Janssen → El-Amin',  sub: '€1 200,00 · audited',               time: '13:55', status: 'Settled'  },
  { icon: 'shield',   tone: 'warn',  title: 'Limit increase · Doe',          sub: '€2 500 → €3 000',                   time: '13:40', status: 'By you'   },
  { icon: 'user',     tone: 'teal',  title: 'Account approved · Anna Visser',sub: 'Checking + Savings created',         time: '12:18', status: 'Settled'  },
  { icon: 'receipt',  tone: 'brown', title: 'Statement export',              sub: '2026-04 · 47 customers',            time: '11:02', status: 'Done'     },
]

const quickActions = [
  { icon: 'users',    label: 'All customer accounts',       sub: '247 active',       to: '/employee/customers'   },
  { icon: 'clock',    label: 'Pending approvals',           sub: '12 awaiting',      to: '/employee/approvals', tone: 'warn' },
  { icon: 'list',     label: 'System transactions',         sub: '1 084 today',      to: '/employee/transactions'},
  { icon: 'transfer', label: 'Transfer between customers',  sub: 'Audited move',     to: '/employee/transfer'   },
]
</script>
