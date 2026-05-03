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

    <!-- TODO: Fetch account detail from GET /api/accounts/{accountId} -->
    <div class="row" style="margin-bottom:28px;gap:16px">
      <span class="badge badge--dark">Checking</span>
      <AppStatus kind="active" />
    </div>
    <div class="t-display" style="margin:0;font-size:64px">
      €6 218<span style="opacity:0.4">,40</span>
    </div>
    <div class="iban" style="margin-top:8px;font-size:14px">NL42 INHO 0123 4567 89 · BIC INHONL2A</div>

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
        <!-- TODO: Fetch monthly summary from GET /api/accounts/{accountId}/summary -->
        <div class="card">
          <h3 class="t-h3" style="margin:0 0 16px">This month</h3>
          <div class="row" style="justify-content:space-between">
            <span class="muted" style="font-size:14px">In</span>
            <span style="color:var(--teal);font-weight:500">+€2 400,00</span>
          </div>
          <hr class="divider" style="margin:10px 0" />
          <div class="row" style="justify-content:space-between">
            <span class="muted" style="font-size:14px">Out</span>
            <span style="font-weight:500">−€860,00</span>
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
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import SparkChart from '@/components/shared/SparkChart.vue'

// TODO: Fetch account detail from GET /api/accounts/{accountId}
const accountDetails = [
  ['Account holder',              'Jane Doe',     false],
  ['Account type',                'Checking',     false],
  ['Daily transfer limit',        '€2 500,00',    false],
  ['Absolute limit (min balance)','−€500,00',     false],
  ['Opened on',                   '12 January 2026', false],
  ['BIC',                         'INHONL2A',     true ],
]
</script>
