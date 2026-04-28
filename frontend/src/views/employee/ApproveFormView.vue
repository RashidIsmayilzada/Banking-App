<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:16px">
      <RouterLink to="/employee/approvals" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> Pending approvals
      </RouterLink>
    </div>

    <!-- TODO: Fetch applicant details from GET /employees/customers/{customerUserId} -->
    <h1 class="t-h1" style="margin:0 0 6px">Approve · Tom Bakker</h1>
    <p class="t-body muted" style="margin:0 0 24px">Create a checking + savings account and set transfer limits.</p>

    <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:20px;align-items:flex-start">
      <div class="card" style="padding:28px">
        <h3 class="t-h3" style="margin:0 0 16px">Accounts to create</h3>

        <!-- Checking IBAN -->
        <div class="card card--flat" style="margin-bottom:12px;border:1.5px solid var(--ink)">
          <div class="row" style="margin-bottom:12px">
            <span class="badge badge--dark">Checking</span>
            <span class="spacer" />
            <span class="t-body-sm">Required</span>
          </div>
          <div class="row" style="gap:8px">
            <input class="input iban" v-model="form.checkingIban" style="flex:1;font-family:var(--font-mono)" />
            <button class="btn btn--secondary btn--sm" @click="regenerate('checking')">
              <AppIcon name="refresh" :size="14" /> Regenerate
            </button>
          </div>
          <div class="field__hint" style="margin-top:6px">Auto-generated · format NLxx INHO 0xxxxxxxxx</div>
        </div>

        <!-- Savings IBAN -->
        <div class="card card--flat" style="margin-bottom:24px;background:var(--teal-tint);border-color:transparent">
          <div class="row" style="margin-bottom:12px">
            <span class="badge" style="background:var(--teal);color:#fff">Savings</span>
            <span class="spacer" />
            <span class="t-body-sm">Required</span>
          </div>
          <div class="row" style="gap:8px">
            <input class="input iban" v-model="form.savingsIban" style="flex:1;font-family:var(--font-mono)" />
            <button class="btn btn--secondary btn--sm" @click="regenerate('savings')">
              <AppIcon name="refresh" :size="14" /> Regenerate
            </button>
          </div>
        </div>

        <h3 class="t-h3" style="margin:0 0 12px">Transfer limits (checking)</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px">
          <AppField
            label="Absolute limit (min balance)"
            v-model="form.absoluteLimit"
            hint="Account never goes below this"
          />
          <AppField
            label="Daily transfer limit"
            v-model="form.dailyLimit"
            hint="Max sent per 24h"
          />
        </div>

        <hr class="divider" style="margin:24px 0" />
        <div class="row">
          <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
          <span class="spacer" />
          <!-- TODO: POST /employees/customers/{customerUserId}/approval with decision=REJECTED -->
          <button class="btn btn--secondary" @click="reject">Reject application</button>
          <!-- TODO: POST /employees/customers/{customerUserId}/approval with decision=APPROVED + limits -->
          <button class="btn btn--primary" @click="approve">Approve &amp; create accounts</button>
        </div>
      </div>

      <!-- Applicant info panel -->
      <div class="col" style="gap:16px">
        <div class="card">
          <div class="row" style="margin-bottom:16px">
            <AppAvatar name="Tom Bakker" size="lg" />
            <div>
              <h3 class="t-h3" style="margin:0">Tom Bakker</h3>
              <div class="t-body-sm">Applicant · 27 Apr · 14:02</div>
            </div>
          </div>
          <hr class="divider" style="margin:0 0 14px" />
          <div style="display:grid;grid-template-columns:1fr 1fr;row-gap:12px;column-gap:14px">
            <div v-for="[k,v,m] in applicantInfo" :key="k">
              <div class="t-label" style="margin-bottom:4px">{{ k }}</div>
              <div style="font-size:13px;font-weight:500" :style="{ fontFamily: m ? 'var(--font-mono)' : undefined }">{{ v }}</div>
            </div>
          </div>
        </div>
        <div class="banner banner--success">
          <AppIcon name="check" :size="16" class="banner__icon" />
          <div><strong>KYC checks passed</strong> automatically. BSN verified, sanctions list clear.</div>
        </div>
        <div class="banner banner--info">
          <AppIcon name="info" :size="16" class="banner__icon" />
          <div>Welcome email &amp; login credentials will be sent on approval.</div>
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
import AppAvatar from '@/components/shared/AppAvatar.vue'

const router = useRouter()

// TODO: Fetch applicant from GET /employees/customers/{route.params.id}
const form = ref({
  checkingIban: 'NL42 INHO 0044 8821 03',
  savingsIban:  'NL42 INHO 0044 8821 04',
  absoluteLimit: '−500,00',
  dailyLimit:    '2 500,00',
})

const applicantInfo = [
  ['Email', 'tom.b@example.com',  false],
  ['Phone', '+31 6 5544 3322',    false],
  ['BSN',   '987-654-321',        true ],
  ['DOB',   '02 Jul 1995',        false],
]

function regenerate(type) {
  // TODO: Generate a new IBAN for the given account type
  const rnd = Math.floor(Math.random() * 9e9).toString().padStart(10, '0')
  if (type === 'checking') form.value.checkingIban = `NL42 INHO 0${rnd.slice(0,3)} ${rnd.slice(3,7)} ${rnd.slice(7)}`
  else form.value.savingsIban = `NL42 INHO 0${rnd.slice(0,3)} ${rnd.slice(3,7)} ${rnd.slice(7)}`
}

// TODO: POST /employees/customers/{id}/approval with decision=APPROVED + account limits
function approve() { router.push('/employee/approvals') }
// TODO: POST /employees/customers/{id}/approval with decision=REJECTED
function reject()  { router.push('/employee/approvals') }
</script>
