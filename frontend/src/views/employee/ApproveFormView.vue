<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:16px">
      <RouterLink to="/employee/approvals" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> Pending approvals
      </RouterLink>
    </div>

    <h1 class="t-h1" style="margin:0 0 6px">
      Approve · {{ applicant ? applicantName : 'Loading...' }}
    </h1>
    <p class="t-body muted" style="margin:0 0 24px">Create a checking + savings account and set transfer limits.</p>

    <div v-if="loading" style="padding: 40px; text-align: center;">Loading applicant details...</div>
    <div v-else-if="error" class="banner banner--danger" style="margin-bottom: 24px;">{{ error }}</div>
    <div v-else-if="applicant" style="display:grid;grid-template-columns:1.5fr 1fr;gap:20px;align-items:flex-start">
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
            <AppAvatar :name="applicantName" size="lg" />
            <div>
              <h3 class="t-h3" style="margin:0">{{ applicantName }}</h3>
              <div class="t-body-sm">Applicant · {{ applicant.registeredAt ? new Date(applicant.registeredAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }) : '—' }}</div>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import * as userService from '@/services/user'

const router = useRouter()
const route = useRoute()

const applicant = ref(null)
const loading = ref(false)
const error = ref(null)
const applicantName = computed(() => applicant.value ? [applicant.value.firstName, applicant.value.lastName].filter(Boolean).join(' ') || applicant.value.username || applicant.value.email : '')

const form = ref({
  checkingIban: '',
  savingsIban:  '',
  absoluteLimit: '-500',
  dailyLimit:    '2500',
})

const applicantInfo = computed(() => {
  if (!applicant.value) return []
  return [
    ['Email', applicant.value.email || '—', false],
    ['Phone', applicant.value.phoneNumber || '—', false],
    ['BSN', applicant.value.bsn || '—', true],
    ['Status', applicant.value.status || '—', false],
  ]
})

function generateIban() {
  const rnd = Math.floor(Math.random() * 9e9).toString().padStart(10, '0')
  return `NL${Math.floor(Math.random()*90+10)} INHO 0${rnd.slice(0,3)} ${rnd.slice(3,7)} ${rnd.slice(7)}`
}

function regenerate(type) {
  if (type === 'checking') form.value.checkingIban = generateIban()
  else form.value.savingsIban = generateIban()
}

async function fetchApplicant() {
  loading.value = true
  try {
    applicant.value = await userService.getUserById(route.params.id)
    form.value.checkingIban = generateIban()
    form.value.savingsIban = generateIban()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function approve() {
  if (!confirm('Are you sure you want to approve this customer and create their accounts?')) return
  loading.value = true
  try {
    // Approve the customer (creating their accounts) and apply the checking
    // transfer limits in one call.
    await userService.approveUser(route.params.id, 'APPROVED', {
      checkingAbsoluteLimit: parseFloat(form.value.absoluteLimit),
      checkingDailyLimit: parseFloat(form.value.dailyLimit),
    })
    router.push('/employee/approvals')
  } catch (err) {
    alert('Failed to approve: ' + err.message)
  } finally {
    loading.value = false
  }
}

async function reject() {
  if (!confirm('Are you sure you want to reject this application?')) return
  loading.value = true
  try {
    await userService.approveUser(route.params.id, 'REJECTED')
    router.push('/employee/approvals')
  } catch (err) {
    alert('Failed to reject: ' + err.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchApplicant()
})
</script>
