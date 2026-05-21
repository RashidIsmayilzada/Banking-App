<template>
  <div class="app" style="height:100vh">
    <AppTopBar :user="fullName" role="Pending" :initials="initials" avatar-tone="avatar--blue" />
    <div style="flex:1;display:flex;align-items:center;justify-content:center;padding:32px">
      <div style="max-width:520px;text-align:center">
        <div style="width:72px;height:72px;border-radius:50%;background:var(--warning-tint);color:var(--warning);display:grid;place-items:center;margin:0 auto 24px">
          <AppIcon name="clock" :size="32" :stroke="1.6" />
        </div>
        <h1 class="t-h1" style="margin:0 0 12px">
          Welcome, {{ firstName }}.
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
            <span class="muted">Submitted</span>
            <span style="font-weight:500">{{ submittedAt }}</span>
          </div>
        </div>
        <div class="t-body-sm" style="margin-bottom:24px">
          We'll email <strong style="color:var(--ink)">{{ email }}</strong> the moment your accounts are ready.
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
import { computed } from 'vue'
import AppTopBar from '@/components/shared/AppTopBar.vue'
import AppIcon from '@/components/shared/AppIcon.vue'

const user = JSON.parse(sessionStorage.getItem('pending_user') || 'null')

const firstName  = computed(() => user?.firstName || user?.username || 'there')
const fullName   = computed(() => [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.username || 'Pending User')
const initials   = computed(() => {
  if (user?.firstName) return [user.firstName[0], user?.lastName?.[0]].filter(Boolean).join('').toUpperCase()
  return (user?.username?.[0] ?? '?').toUpperCase()
})
const email      = computed(() => user?.email || '')
const submittedAt = computed(() => {
  const raw = user?.registeredAt ?? user?.lastLoginAt
  if (!raw) return '—'
  const date = Array.isArray(raw)
    ? new Date(raw[0], raw[1] - 1, raw[2], raw[3] ?? 0, raw[4] ?? 0)
    : new Date(raw)
  return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
    + ' · '
    + date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
})

function refreshStatus() {
  // future: poll GET /api/users/{id} and redirect when status changes to APPROVED
}
</script>
