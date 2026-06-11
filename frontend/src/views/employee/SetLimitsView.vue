<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:16px">
      <RouterLink to="/employee/customers" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> {{ customerName }}
      </RouterLink>
    </div>
    <h1 class="t-h1" style="margin:0 0 6px">Set transfer limits</h1>
    <p class="t-body muted" style="margin:0 0 24px">
      Customer: <strong style="color:var(--ink)">{{ customerName }}</strong> · Account:
      <span class="iban" style="color:var(--ink)">{{ account?.iban || '—' }}</span>
    </p>

    <div v-if="error" class="banner banner--danger" style="margin-bottom:20px">{{ error }}</div>

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
              <span style="font-weight:500;margin-left:6px">{{ formatMoney(account?.absoluteTransferLimit?.amount) }}</span>
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
              <span style="font-weight:500;margin-left:6px">{{ formatMoney(account?.dailyTransferLimit) }}</span>
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

          <div v-if="saveError" class="banner banner--danger">
            <AppIcon name="info" :size="16" class="banner__icon" />
            <div>{{ saveError }}</div>
          </div>

          <div class="row">
            <button class="btn btn--ghost" @click="$router.back()" :disabled="saving">Cancel</button>
            <span class="spacer" />
            <button class="btn btn--primary" :disabled="saving" @click="saveLimits">
              {{ saving ? 'Saving…' : 'Save limits' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Limit history -->
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import * as userService from '@/services/user'
import { updateAccount } from '@/services/accounts'

const router = useRouter()
const route = useRoute()
const customer = ref(null)
const saving = ref(false)
const saveError = ref(null)
const customerName = computed(() => customer.value ? [customer.value.firstName, customer.value.lastName].filter(Boolean).join(' ') || customer.value.username || customer.value.email : 'Customer')
const account = computed(() => customer.value?.accounts?.find(item => item.accountType === 'CHECKING') || customer.value?.accounts?.[0] || null)

const form = ref({ absoluteLimit: '', dailyLimit: '', reason: '' })
const saving = ref(false)
const error = ref(null)

async function saveLimits() {
  if (!account.value?.iban) {
    error.value = 'No account found for this customer'
    return
  }

  saving.value = true
  error.value = null

  try {
    const payload = {}

async function saveLimits() {
  if (!account.value?.iban) return
  saving.value = true
  saveError.value = null
  try {
    await updateAccount(account.value.iban, {
      absoluteTransferLimit: parseFloat(form.value.absoluteLimit),
      dailyTransferLimit: parseFloat(form.value.dailyLimit),
    })
    router.push('/employee/customers')
  } catch (err) {
    saveError.value = err.message || 'Failed to save limits'
  } finally {
    saving.value = false
  }
}

const history = ref([])

function formatMoney(value) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(value || 0))
}

onMounted(async () => {
  if (!route.params.id) return
  customer.value = await userService.getUserById(route.params.id)
  form.value.absoluteLimit = String(account.value?.absoluteTransferLimit?.amount ?? '')
  form.value.dailyLimit = String(account.value?.dailyTransferLimit?.amount ?? '')
})
</script>
