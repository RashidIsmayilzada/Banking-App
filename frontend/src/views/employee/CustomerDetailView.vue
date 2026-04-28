<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:20px">
      <RouterLink to="/employee/customers" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> All customers
      </RouterLink>
      <span class="spacer" />
      <RouterLink :to="`/employee/limits`" class="btn btn--secondary btn--sm">
        <AppIcon name="shield" :size="14" /> Set transfer limits
      </RouterLink>
      <button class="btn btn--ghost-danger btn--sm">Close customer</button>
    </div>

    <!-- TODO: Fetch customer from GET /employees/customers/{customerUserId} -->
    <div class="row" style="margin-bottom:24px;gap:20px">
      <AppAvatar name="Jane Doe" size="lg" />
      <div>
        <h1 class="t-h1" style="margin:0">Jane Doe</h1>
        <div class="row" style="gap:12px;margin-top:6px">
          <span class="t-body-sm">Customer #00247</span>
          <span class="t-body-sm">·</span>
          <span class="t-body-sm">Joined 12 Jan 2026</span>
          <AppStatus kind="active" />
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

      <!-- TODO: Fetch accounts from GET /employees/customers/{customerUserId}/accounts -->
      <div class="card" style="padding:0">
        <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
          <h3 class="t-h4" style="margin:0">Accounts</h3>
        </div>
        <div style="padding:16px;display:flex;flex-direction:column;gap:10px">
          <div class="card card--flat" style="padding:14px">
            <div class="row">
              <span class="badge badge--dark">Checking</span>
              <span class="iban">NL42 INHO …89</span>
              <span class="spacer" />
              <span style="font-weight:500">€6 218,40</span>
            </div>
          </div>
          <div class="card card--flat" style="padding:14px;background:var(--teal-tint)">
            <div class="row">
              <span class="badge" style="background:var(--teal);color:#fff">Savings</span>
              <span class="iban">NL42 INHO …21</span>
              <span class="spacer" />
              <span style="font-weight:500">€2 203,15</span>
            </div>
          </div>
          <hr class="divider" style="margin:6px 0" />
          <div class="row" style="font-size:13px">
            <span class="muted">Daily transfer limit</span>
            <span class="spacer" />
            <span style="font-weight:500">€2 500,00</span>
          </div>
          <div class="row" style="font-size:13px">
            <span class="muted">Absolute limit</span>
            <span class="spacer" />
            <span style="font-weight:500">−€500,00</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Transaction history -->
    <!-- TODO: Fetch from GET /employees/customers/{customerUserId}/transactions -->
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
      <div style="padding:0 16px 12px">
        <AppPager :current-page="1" count="1–5 of 138" />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import AppPager from '@/components/shared/AppPager.vue'

// TODO: Fetch from GET /employees/customers/{customerUserId} using route param
const personalInfo = [
  ['Email',         'jane.doe@example.com',          false],
  ['Phone',         '+31 6 1234 5678',               false],
  ['BSN',           '123-456-789',                   true ],
  ['Date of birth', '14 May 1992',                   false],
  ['Address',       'Keizersgracht 218, Amsterdam',  false],
]

// TODO: Fetch from GET /employees/customers/{customerUserId}/transactions
const transactions = [
  { id: 1, date: '28 Apr · 14:22', from: 'NL42 INHO …89', to: 'NL91 ABNA …42', initiator: 'Jane Doe',    amount: '−€42,18'   },
  { id: 2, date: '28 Apr · 09:01', from: 'NL42 INHO …89', to: 'NL11 INHO …07', initiator: 'Jane Doe',    amount: '−€250,00'  },
  { id: 3, date: '27 Apr · 18:30', from: 'NL91 RABO …01', to: 'NL42 INHO …89', initiator: '— external',  amount: '+€2 400,00'},
  { id: 4, date: '25 Apr · 20:45', from: 'ATM #14',        to: 'NL42 INHO …89', initiator: 'Jane (ATM)', amount: '−€100,00'  },
  { id: 5, date: '22 Apr · 08:15', from: 'NL42 INHO …21', to: 'NL42 INHO …89', initiator: 'Jane Doe',    amount: '+€150,00'  },
]
</script>
