<template>
  <div class="app" style="height:100vh">
    <AppTopBar user="Jane Doe" role="Pending" initials="JD" avatar-tone="avatar--blue" />
    <div style="flex:1;display:flex;align-items:center;justify-content:center;padding:32px">
      <div style="max-width:520px;text-align:center">
        <div style="width:72px;height:72px;border-radius:50%;background:var(--warning-tint);color:var(--warning);display:grid;place-items:center;margin:0 auto 24px">
          <AppIcon name="clock" :size="32" :stroke="1.6" />
        </div>
        <h1 class="t-h1" style="margin:0 0 12px">
          <!-- TODO: Fetch user name from GET /api/user/profile -->
          Welcome, Jane.
        </h1>
        <p class="t-body muted" style="margin:0 0 24px;font-size:16px">
          Your registration is in. An InHolland employee is reviewing your documents and will set up your checking and savings accounts shortly.
        </p>
        <div class="card card--soft" style="padding:20px;margin-bottom:20px">
          <div class="row" style="justify-content:space-between">
            <div class="row">
              <span class="status-dot status-dot--warn" />
              <span style="font-weight:500">Status</span>
            </div>
            <span class="badge badge--warn">Pending approval</span>
          </div>
          <hr class="divider" style="margin:14px 0" />
          <div class="row" style="justify-content:space-between;font-size:14px">
            <span class="muted">Expected review</span>
            <span style="font-weight:500">Within 24 hours</span>
          </div>
          <div class="row" style="justify-content:space-between;font-size:14px;margin-top:8px">
            <!-- TODO: Display actual submission timestamp from GET /api/user/profile -->
            <span class="muted">Submitted</span>
            <span style="font-weight:500">28 Apr 2026 · 14:02</span>
          </div>
        </div>
        <div class="t-body-sm" style="margin-bottom:24px">
          <!-- TODO: Display actual email from GET /api/user/profile -->
          We'll email <strong style="color:var(--ink)">jane.doe@example.com</strong> the moment your accounts are ready.
        </div>
        <div class="row" style="justify-content:center;gap:8px">
          <button class="btn btn--secondary" @click="refreshStatus">
            <AppIcon name="refresh" :size="16" /> Refresh status
          </button>
          <RouterLink to="/login" class="btn btn--ghost">Sign out</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppTopBar from '@/components/shared/AppTopBar.vue'
import AppIcon from '@/components/shared/AppIcon.vue'

// TODO: Poll GET /api/user/profile to detect when status changes to APPROVED
// and then redirect to /customer/dashboard
function refreshStatus() {
  // TODO: Fetch updated approval status from GET /api/user/profile
}
</script>
