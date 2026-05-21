<template>
  <AtmShell>
    <!-- TODO: Fetch account balance from GET /api/accounts (using X-ATM-Session-Token) -->
    <div class="card card--dark" style="padding:20px;margin-bottom:20px;text-align:center">
      <div class="t-label" style="margin-bottom:8px">Available · Checking</div>
      <div class="t-display" style="font-size:44px;color:#fff;margin:0">€6 218,40</div>
      <div class="iban" style="color:rgba(255,255,255,0.6);margin-top:8px">NL42 INHO 0123 4567 89</div>
    </div>

    <div class="col" style="gap:12px;flex:1">
      <RouterLink
        to="/atm/withdraw"
        class="btn btn--secondary"
        style="height:84px;font-size:22px;justify-content:flex-start;padding:0 24px;text-decoration:none"
      >
        <div class="icon-box" style="background:var(--teal-tint);color:var(--teal)">
          <AppIcon name="deposit" :size="22" />
        </div>
        <div style="text-align:left">
          <div>Deposit</div>
          <div style="font-size:12px;color:var(--ink-faint);font-weight:400;font-family:var(--font-body)">Add cash to your account</div>
        </div>
      </RouterLink>
      <RouterLink
        to="/atm/withdraw"
        class="btn btn--primary"
        style="height:84px;font-size:22px;justify-content:flex-start;padding:0 24px;text-decoration:none"
      >
        <div class="icon-box" style="background:rgba(255,255,255,0.15);color:#fff">
          <AppIcon name="withdraw" :size="22" />
        </div>
        <div style="text-align:left">
          <div>Withdraw</div>
          <div style="font-size:12px;color:rgba(255,255,255,0.7);font-weight:400;font-family:var(--font-body)">Take cash out</div>
        </div>
      </RouterLink>
    </div>

    <div class="row" style="margin-top:20px">
      <!-- TODO: DELETE /atm/sessions/{sessionId} then redirect to /atm/login -->
      <RouterLink to="/atm/login" class="btn btn--ghost btn--sm">
        <AppIcon name="logout" :size="14" /> Sign out
      </RouterLink>
      <span class="spacer" />
      <!-- TODO: Display actual user name from session -->
      <span class="t-body-sm">Hi, Jane</span>
    </div>
  </AtmShell>
</template>

<script setup>
import AtmShell from '@/components/layout/AtmShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
</script>
