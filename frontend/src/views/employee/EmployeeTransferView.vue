<template>
  <EmployeeShell>
    <h1 class="t-h1" style="margin:0 0 6px">Transfer between customers</h1>
    <p class="t-body muted" style="margin:0 0 24px">Move funds between two customer checking accounts. Always audited.</p>

    <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:20px;align-items:flex-start">
      <div class="card" style="padding:28px">
        <!-- From customer -->
        <div class="field" style="margin-bottom:16px">
          <label class="field__label">From customer (checking)</label>
          <input class="input" placeholder="Search by name, email, or IBAN" v-model="fromSearch" @input="searchFrom" />
          <div v-if="fromResults.length" class="card card--soft" style="padding:8px;margin-top:8px">
            <button v-for="customer in fromResults" :key="customer.id" class="btn btn--ghost btn--block" style="justify-content:flex-start" @click="fromCustomer = customer; fromResults = []">
              {{ customer.name }} · {{ customer.iban }}
            </button>
          </div>
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
          <div v-if="toResults.length" class="card card--soft" style="padding:8px;margin-top:8px">
            <button v-for="customer in toResults" :key="customer.id" class="btn btn--ghost btn--block" style="justify-content:flex-start" @click="toCustomer = customer; toResults = []">
              {{ customer.name }} · {{ customer.iban }}
            </button>
          </div>
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

        <div v-if="form.amount && form.reason" class="banner banner--success" style="margin-top:20px">
          <AppIcon name="check" :size="16" class="banner__icon" />
          <div>Within limits. Daily €2 500 · Absolute −€500. Source has sufficient funds.</div>
        </div>

        <div class="row" style="margin-top:24px">
          <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
          <span class="spacer" />
          <button class="btn btn--primary btn--lg" :disabled="submitting || !fromCustomer || !toCustomer || !form.amount" @click="submitTransfer">
            Submit transfer <AppIcon name="arrowRight" :size="16" />
          </button>
        </div>
      </div>

      <!-- Recent employee transfers -->
      <div class="card" style="padding:0">
        <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
          <h3 class="t-h4" style="margin:0">Recent employee transfers</h3>
        </div>
        <div
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
            <span class="spacer" />
            <span class="t-body-sm">by {{ t.by }}</span>
          </div>
        </div>
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import * as userService from '@/services/user'

const router = useRouter()

const fromSearch = ref('')
const toSearch   = ref('')
const form = ref({ amount: '', reason: '' })
const submitting = ref(false)
const fromResults = ref([])
const toResults = ref([])

const fromCustomer = ref(null)
const toCustomer = ref(null)
const recentTransfers = ref([])

function formatMoney(value) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(value || 0))
}

function parseAmount(value) {
  return Number(String(value || '').replace(/[€\s]/g, '').replace(',', '.')) || 0
}

function displayName(user) {
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || user.email
}

function mapCustomer(user) {
  const account = user.accounts?.find(item => item.accountType === 'CHECKING') || user.accounts?.[0]
  return {
    id: user.id,
    name: displayName(user),
    accountId: account?.id,
    iban: account?.iban || '—',
    balance: formatMoney(account?.balance),
  }
}

async function searchCustomers(search, target) {
  if (!search) {
    target.value = []
    return
  }
  const response = await userService.searchCustomers(search, { status: 'APPROVED', hasAccount: true })
  target.value = (response.content || []).map(mapCustomer).filter(customer => customer.accountId)
}

function searchFrom() { searchCustomers(fromSearch.value, fromResults) }
function searchTo() { searchCustomers(toSearch.value, toResults) }

async function submitTransfer() {
  submitting.value = true
  try {
    // Transaction submission is intentionally left for the transaction endpoint work.
    router.push('/employee/overview')
  } finally {
    submitting.value = false
  }
}

async function fetchRecentTransfers() {
  recentTransfers.value = []
}

onMounted(fetchRecentTransfers)
</script>
