<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:16px">
      <RouterLink to="/employee/customers" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> Jane Doe
      </RouterLink>
    </div>
    <h1 class="t-h1" style="margin:0 0 6px">Set transfer limits</h1>
    <!-- TODO: Display actual customer name and account from route param -->
    <p class="t-body muted" style="margin:0 0 24px">
      Customer: <strong style="color:var(--ink)">Jane Doe</strong> · Account:
      <span class="iban" style="color:var(--ink)">NL42 INHO …89</span>
    </p>

    <div style="display:grid;grid-template-columns:1.4fr 1fr;gap:20px;align-items:flex-start">
      <div class="card" style="padding:28px">
        <div class="col" style="gap:20px">
          <!-- Absolute limit -->
          <div>
            <AppField
              label="Absolute transfer limit (€)"
              v-model="form.absoluteLimit"
              hint="Account balance can never fall below this."
            />
            <div class="row" style="margin-top:8px;font-size:13px">
              <span class="muted">Current</span>
              <span style="font-weight:500;margin-left:6px">−€500,00</span>
              <span class="muted" style="margin:0 8px">→</span>
              <span style="font-weight:500">{{ form.absoluteLimit || '—' }}</span>
              <span class="badge" style="margin-left:8px">No change</span>
            </div>
          </div>

          <!-- Daily limit -->
          <div>
            <AppField
              label="Daily transfer limit (€)"
              v-model="form.dailyLimit"
              hint="Max amount sent per 24 hours."
            />
            <div class="row" style="margin-top:8px;font-size:13px">
              <span class="muted">Current</span>
              <span style="font-weight:500;margin-left:6px">€2 500,00</span>
              <span class="muted" style="margin:0 8px">→</span>
              <span style="color:var(--teal);font-weight:500">{{ form.dailyLimit || '—' }}</span>
              <span v-if="form.dailyLimit" class="badge badge--success" style="margin-left:8px">
                <AppIcon name="arrowUp" :size="10" /> Updated
              </span>
            </div>
          </div>

          <AppField
            label="Reason for change (audit log)"
            v-model="form.reason"
            placeholder="e.g. Customer requested increase"
          />

          <div class="banner banner--info">
            <AppIcon name="info" :size="16" class="banner__icon" />
            <div>Changes apply immediately. Customer will see new limits on next transfer attempt.</div>
          </div>

          <div class="row">
            <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
            <span class="spacer" />
            <!-- TODO: PATCH /employees/accounts/{accountId}/limits with absoluteTransferLimit, dailyTransferLimit -->
            <button class="btn btn--primary" @click="saveLimits">Save limits</button>
          </div>
        </div>
      </div>

      <!-- Limit history -->
      <!-- TODO: Fetch limit change history from account audit log -->
      <div class="card" style="padding:0">
        <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
          <h3 class="t-h4" style="margin:0">Limit history</h3>
        </div>
        <div
          v-for="h in history"
          :key="h.id"
          style="padding:14px 20px;border-bottom:1px solid var(--line)"
        >
          <div class="row">
            <span class="num muted" style="font-size:12px">{{ h.date }}</span>
            <span class="spacer" />
            <span :class="['badge', h.status === 'pending' ? 'badge--warn' : 'badge--success']">
              {{ h.status === 'pending' ? 'Pending' : 'Applied' }}
            </span>
          </div>
          <div style="margin-top:4px;font-weight:500;font-size:14px">{{ h.title }}</div>
          <div class="t-body-sm" style="margin-top:2px">{{ h.detail }}</div>
          <div class="t-body-sm" style="margin-top:2px">by {{ h.by }}</div>
        </div>
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'

const router = useRouter()

// TODO: Pre-populate from GET /employees/accounts/{accountId}
const form = ref({ absoluteLimit: '−500,00', dailyLimit: '3 000,00', reason: '' })

// TODO: PATCH /employees/accounts/{accountId}/limits
function saveLimits() { router.push('/employee/customers') }

// TODO: Fetch from account audit log
const history = [
  { id: 1, date: '28 Apr 2026', title: 'Pending change',   detail: 'Daily €2 500 → €3 000',       by: 'S. van Berg', status: 'pending' },
  { id: 2, date: '03 Mar 2026', title: 'Daily limit ↑',    detail: '€2 000 → €2 500',              by: 'L. Hartog',   status: 'applied' },
  { id: 3, date: '12 Jan 2026', title: 'Account opened',   detail: 'Daily €2 500 · Abs −€500',     by: 'S. van Berg', status: 'applied' },
]
</script>
