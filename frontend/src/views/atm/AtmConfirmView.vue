<template>
  <AtmShell>
    <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center">
      <div style="width:80px;height:80px;border-radius:50%;background:var(--teal);color:#fff;display:grid;place-items:center;margin-bottom:20px">
        <AppIcon name="check" :size="40" :stroke="2.5" />
      </div>
      <!-- TODO: Display actual withdrawn amount from transaction response -->
      <h2 class="t-h1" style="margin:0 0 8px">€200 withdrawn</h2>
      <p class="t-body muted" style="margin:0 0 24px">Please take your cash from the slot below.</p>

      <div class="card card--soft" style="width:100%;padding:18px;margin-bottom:20px">
        <div class="summary-row">
          <span class="muted">From</span>
          <!-- TODO: Show actual account IBAN -->
          <span class="iban" style="color:var(--ink)">NL42 INHO …89</span>
        </div>
        <div class="summary-row">
          <span class="muted">Amount</span>
          <span style="font-weight:500">€200,00</span>
        </div>
        <div class="summary-row">
          <span class="muted">New balance</span>
          <span style="font-weight:500;font-size:18px">€6 018,40</span>
        </div>
        <div class="summary-row">
          <span class="muted">Reference</span>
          <!-- TODO: Display actual transaction reference from API response -->
          <span class="iban" style="color:var(--ink)">TX-9F2A-04281445</span>
        </div>
      </div>

      <div class="col" style="width:100%;gap:8px">
        <RouterLink to="/atm/home" class="btn btn--secondary btn--block">Another transaction</RouterLink>
        <!-- TODO: DELETE /atm/sessions/{sessionId} then navigate to /atm/login -->
        <RouterLink to="/atm/login" class="btn btn--ghost btn--block">End session</RouterLink>
      </div>
    </div>
  </AtmShell>
</template>

<script setup>
import AtmShell from '@/components/layout/AtmShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
</script>
