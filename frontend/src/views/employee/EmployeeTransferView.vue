<template>
  <EmployeeShell>
    <h1 class="t-h1" style="margin:0 0 6px">Transfer between customers</h1>
    <p class="t-body muted" style="margin:0 0 24px">Move funds between two customer checking accounts. Always audited.</p>

    <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:20px;align-items:flex-start">
      <div class="card" style="padding:28px">
        <!-- From customer -->
        <!-- TODO: GET /employees/customers?q=… to search customers -->
        <div class="field" style="margin-bottom:16px">
          <label class="field__label">From customer (checking)</label>
          <input class="input" placeholder="Search by name, email, or IBAN" v-model="fromSearch" @input="searchFrom" />
          <div v-if="fromCustomer" class="card card--soft" style="padding:12px;margin-top:8px">
            <div class="row">
              <AppAvatar :name="fromCustomer.name" />
              <div>
                <div style="font-weight:500;font-size:14px">{{ fromCustomer.name }}</div>
                <div class="iban">{{ fromCustomer.iban }}</div>
              </div>
              <span class="spacer" />
              <div style="text-align:right">
                <div style="font-weight:500">{{ fromCustomer.balance }}</div>
                <div class="t-body-sm">Available</div>
              </div>
            </div>
          </div>
        </div>

        <div style="display:flex;justify-content:center;margin:4px 0">
          <div style="width:36px;height:36px;border-radius:50%;border:1.5px solid var(--line);display:grid;place-items:center">
            <AppIcon name="arrowDown" :size="14" />
          </div>
        </div>

        <!-- To customer -->
        <div class="field" style="margin-bottom:20px">
          <label class="field__label">To customer (checking)</label>
          <input class="input" placeholder="Search by name, email, or IBAN" v-model="toSearch" @input="searchTo" />
          <div v-if="toCustomer" class="card card--soft" style="padding:12px;margin-top:8px">
            <div class="row">
              <AppAvatar :name="toCustomer.name" />
              <div>
                <div style="font-weight:500;font-size:14px">{{ toCustomer.name }}</div>
                <div class="iban">{{ toCustomer.iban }}</div>
              </div>
              <span class="spacer" />
              <div style="text-align:right">
                <div style="font-weight:500">{{ toCustomer.balance }}</div>
                <div class="t-body-sm">Available</div>
              </div>
            </div>
          </div>
        </div>

        <div class="field" style="margin-bottom:14px">
          <label class="field__label">Amount</label>
          <input class="input input--lg" v-model="form.amount" placeholder="€ 0,00" />
        </div>

        <AppField
          label="Reason / note (audit log)"
          v-model="form.reason"
          placeholder="Required for employee transfers"
        />

        <div v-if="form.amount && form.reason && !submitError" class="banner banner--success" style="margin-top:20px">
          <AppIcon name="check" :size="16" class="banner__icon" />
          <div>Ready to submit. Confirm the details above before proceeding.</div>
        </div>

        <div v-if="submitError" class="banner banner--danger" style="margin-top:20px">
          <AppIcon name="alert" :size="16" class="banner__icon" />
          <div>{{ submitError }}</div>
        </div>
        <div v-if="submitSuccess" class="banner banner--success" style="margin-top:20px">
          <AppIcon name="check" :size="16" class="banner__icon" />
          <div>Transfer submitted successfully.</div>
        </div>

        <div class="row" style="margin-top:24px">
          <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
          <span class="spacer" />
          <button class="btn btn--primary btn--lg" :disabled="submitLoading" @click="submitTransfer">
            <span v-if="submitLoading">Submitting…</span>
            <template v-else>Submit transfer <AppIcon name="arrowRight" :size="16" /></template>
          </button>
        </div>
      </div>

      <!-- Recent employee transfers — GET /transactions?channel=EMPLOYEE -->
      <div class="card" style="padding:0">
        <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
          <h3 class="t-h4" style="margin:0">Recent employee transfers</h3>
        </div>
        <div v-if="recentLoading" style="padding:20px;text-align:center" class="t-body-sm muted">Loading…</div>
        <div v-else-if="recentError" style="padding:16px 20px;color:var(--danger)" class="t-body-sm">{{ recentError }}</div>
        <div v-else-if="recentTransfers.length === 0" style="padding:20px;text-align:center" class="t-body-sm muted">No recent employee transfers.</div>
        <div
          v-else
          v-for="t in recentTransfers"
          :key="t.id"
          style="padding:14px 20px;border-bottom:1px solid var(--line)"
        >
          <div class="row">
            <span class="num muted">{{ t.date }}</span>
            <span class="spacer" />
            <span style="font-weight:500">{{ t.amount }}</span>
          </div>
          <div class="row" style="margin-top:4px;font-size:13px">
            <span>{{ t.from }} → {{ t.to }}</span>
          </div>
        </div>
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import { createTransaction, listTransactions } from '@/services/transaction'

const router = useRouter()

const fromSearch = ref('')
const toSearch   = ref('')
const form = ref({ amount: '', reason: '' })

// Customer search has no backend endpoint yet — populated via hardcoded placeholders
const fromCustomer = ref(null)
const toCustomer   = ref(null)

function searchFrom() {}
function searchTo()   {}

const submitLoading = ref(false)
const submitError   = ref(null)
const submitSuccess = ref(false)

async function submitTransfer() {
  submitError.value   = null
  submitSuccess.value = false

  const amount = parseFloat(String(form.value.amount).replace(/[^0-9.]/g, ''))
  if (!amount || amount <= 0)       { submitError.value = 'Enter a valid amount.'; return }
  if (!form.value.reason.trim())    { submitError.value = 'Reason / note is required.'; return }
  if (!fromCustomer.value || !toCustomer.value) {
    submitError.value = 'Select both a source and a destination customer.'
    return
  }

  if (!fromCustomer.value.iban) {
    submitError.value = 'Source account IBAN unavailable.'
    return
  }

  submitLoading.value = true
  try {
    await createTransaction({
      type: 'TRANSFER',
      fromIban: fromCustomer.value.iban.replace(/\s/g, ''),
      toIban: toCustomer.value.iban.replace(/\s/g, ''),
      amount,
      description: form.value.reason,
    })
    submitSuccess.value = true
    form.value = { amount: '', reason: '' }
    fromCustomer.value = null
    toCustomer.value   = null
    loadRecentTransfers()
  } catch (err) {
    submitError.value = err.message
  } finally {
    submitLoading.value = false
  }
}

// Recent employee transfers — GET /transactions?channel=EMPLOYEE
const recentTransfers = ref([])
const recentLoading   = ref(false)
const recentError     = ref(null)

function formatDate(isoString) {
  if (!isoString) return '—'
  const d = new Date(isoString)
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })
}

function formatAmount(moneyDto) {
  if (!moneyDto) return '—'
  return `€${Number(moneyDto.amount).toLocaleString('nl-NL', { minimumFractionDigits: 2 })}`
}

async function loadRecentTransfers() {
  recentLoading.value = true
  recentError.value   = null
  try {
    const page = await listTransactions({ channel: 'EMPLOYEE', page: 0, size: 5 })
    recentTransfers.value = (page.items || []).map(tx => ({
      id:     tx.transactionId,
      date:   formatDate(tx.createdAt),
      from:   tx.fromAccount?.name || tx.fromAccount?.iban || '—',
      to:     tx.toAccount?.name   || tx.toAccount?.iban   || '—',
      amount: formatAmount(tx.amount),
    }))
  } catch (err) {
    recentError.value = err.message
  } finally {
    recentLoading.value = false
  }
}

onMounted(loadRecentTransfers)
</script>
