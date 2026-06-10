<template>
  <EmployeeShell>
    <h1 class="t-h1" style="margin:0 0 6px">Fund operations</h1>
    <p class="t-body muted" style="margin:0 0 24px">Transfer between customers or deposit funds into an account.</p>

    <div class="tabs" style="margin-bottom:24px">
      <div v-for="tab in ['Transfer', 'Deposit']" :key="tab" :class="['tab', activeTab === tab ? 'tab--active' : '']" @click="activeTab = tab; resetForms()">{{ tab }}</div>
    </div>

    <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:20px;align-items:flex-start">
      <!-- ── TRANSFER TAB ── -->
      <template v-if="activeTab === 'Transfer'">
        <div class="card" style="padding:28px">
          <div class="field" style="margin-bottom:16px">
            <label class="field__label">From customer (checking)</label>
            <div class="row" style="gap:8px">
              <input class="input" style="flex:1" v-model="fromSearch" placeholder="Search by name…" @keydown.enter="searchFrom" />
              <button class="btn btn--secondary btn--sm" @click="searchFrom" :disabled="fromSearching">
                <AppIcon name="search" :size="14" />
              </button>
            </div>
            <div v-if="fromResults.length" style="margin-top:6px;display:flex;flex-direction:column;gap:6px">
              <div v-for="r in fromResults" :key="r.iban" class="card card--flat" style="padding:10px 14px;cursor:pointer" @click="fromCustomer = r; fromResults = []">
                <div class="row"><AppAvatar :name="r.name" /><div><div style="font-weight:500;font-size:14px">{{ r.name }}</div><div class="iban">{{ r.iban }}</div></div></div>
              </div>
            </div>
            <div v-if="fromCustomer" class="card card--soft" style="padding:12px;margin-top:8px">
              <div class="row"><AppAvatar :name="fromCustomer.name" /><div><div style="font-weight:500;font-size:14px">{{ fromCustomer.name }}</div><div class="iban">{{ fromCustomer.iban }}</div></div><span class="spacer" /></div>
            </div>
          </div>

          <div style="display:flex;justify-content:center;margin:4px 0">
            <div style="width:36px;height:36px;border-radius:50%;border:1.5px solid var(--line);display:grid;place-items:center"><AppIcon name="arrowDown" :size="14" /></div>
          </div>

          <div class="field" style="margin-bottom:20px">
            <label class="field__label">To customer (checking)</label>
            <div class="row" style="gap:8px">
              <input class="input" style="flex:1" v-model="toSearch" placeholder="Search by name…" @keydown.enter="searchTo" />
              <button class="btn btn--secondary btn--sm" @click="searchTo" :disabled="toSearching">
                <AppIcon name="search" :size="14" />
              </button>
            </div>
            <div v-if="toResults.length" style="margin-top:6px;display:flex;flex-direction:column;gap:6px">
              <div v-for="r in toResults" :key="r.iban" class="card card--flat" style="padding:10px 14px;cursor:pointer" @click="toCustomer = r; toResults = []">
                <div class="row"><AppAvatar :name="r.name" /><div><div style="font-weight:500;font-size:14px">{{ r.name }}</div><div class="iban">{{ r.iban }}</div></div></div>
              </div>
            </div>
            <div v-if="toCustomer" class="card card--soft" style="padding:12px;margin-top:8px">
              <div class="row"><AppAvatar :name="toCustomer.name" /><div><div style="font-weight:500;font-size:14px">{{ toCustomer.name }}</div><div class="iban">{{ toCustomer.iban }}</div></div><span class="spacer" /></div>
            </div>
          </div>

          <div class="field" style="margin-bottom:14px">
            <label class="field__label">Amount (€)</label>
            <input class="input input--lg" v-model="transferForm.amount" placeholder="0.00" type="number" min="0.01" step="0.01" />
          </div>
          <AppField label="Reason / note (audit log)" v-model="transferForm.reason" placeholder="Required for employee transfers" />

          <div v-if="transferError" class="banner banner--danger" style="margin-top:20px"><AppIcon name="alert" :size="16" class="banner__icon" /><div>{{ transferError }}</div></div>
          <div v-if="transferSuccess" class="banner banner--success" style="margin-top:20px"><AppIcon name="check" :size="16" class="banner__icon" /><div>Transfer submitted successfully.</div></div>

          <div class="row" style="margin-top:24px">
            <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
            <span class="spacer" />
            <button class="btn btn--primary btn--lg" :disabled="transferLoading" @click="submitTransfer">
              <span v-if="transferLoading">Submitting…</span>
              <template v-else>Submit transfer <AppIcon name="arrowRight" :size="16" /></template>
            </button>
          </div>
        </div>
      </template>

      <!-- ── DEPOSIT TAB ── -->
      <template v-else>
        <div class="card" style="padding:28px">
          <p class="t-body-sm muted" style="margin:0 0 20px">Search for a customer and deposit funds directly into their checking account.</p>

          <div class="field" style="margin-bottom:16px">
            <label class="field__label">Customer account</label>
            <div class="row" style="gap:8px">
              <input class="input" style="flex:1" v-model="depositSearch" placeholder="Search by name…" @keydown.enter="searchDeposit" />
              <button class="btn btn--secondary btn--sm" @click="searchDeposit" :disabled="depositSearching">
                <AppIcon name="search" :size="14" />
              </button>
            </div>
            <div v-if="depositResults.length" style="margin-top:6px;display:flex;flex-direction:column;gap:6px">
              <div v-for="r in depositResults" :key="r.iban" class="card card--flat" style="padding:10px 14px;cursor:pointer" @click="depositTarget = r; depositResults = []">
                <div class="row"><AppAvatar :name="r.name" /><div><div style="font-weight:500;font-size:14px">{{ r.name }}</div><div class="iban">{{ r.iban }}</div></div></div>
              </div>
            </div>
            <div v-if="depositTarget" class="card card--soft" style="padding:12px;margin-top:8px">
              <div class="row"><AppAvatar :name="depositTarget.name" /><div><div style="font-weight:500;font-size:14px">{{ depositTarget.name }}</div><div class="iban">{{ depositTarget.iban }}</div></div><span class="spacer" /></div>
            </div>
          </div>

          <div class="field" style="margin-bottom:14px">
            <label class="field__label">Deposit amount (€)</label>
            <input class="input input--lg" v-model="depositForm.amount" placeholder="0.00" type="number" min="0.01" step="0.01" />
          </div>
          <AppField label="Reason (audit log)" v-model="depositForm.reason" placeholder="e.g. Branch cash deposit" />

          <div v-if="depositError" class="banner banner--danger" style="margin-top:20px"><AppIcon name="alert" :size="16" class="banner__icon" /><div>{{ depositError }}</div></div>
          <div v-if="depositSuccess" class="banner banner--success" style="margin-top:20px"><AppIcon name="check" :size="16" class="banner__icon" /><div>Deposit completed successfully.</div></div>

          <div class="row" style="margin-top:24px">
            <span class="spacer" />
            <button class="btn btn--primary btn--lg" :disabled="depositLoading" @click="submitDeposit">
              <span v-if="depositLoading">Processing…</span>
              <template v-else>Deposit funds <AppIcon name="arrowRight" :size="16" /></template>
            </button>
          </div>
        </div>
      </template>

      <!-- Recent transactions sidebar -->
      <div class="card" style="padding:0">
        <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
          <h3 class="t-h4" style="margin:0">Recent employee transactions</h3>
        </div>
        <div v-if="recentLoading" style="padding:20px;text-align:center" class="t-body-sm muted">Loading…</div>
        <div v-else-if="recentTransfers.length === 0" style="padding:20px;text-align:center" class="t-body-sm muted">No recent transactions.</div>
        <div v-else v-for="t in recentTransfers" :key="t.id" style="padding:14px 20px;border-bottom:1px solid var(--line)">
          <div class="row"><span class="num muted">{{ t.date }}</span><span class="spacer" /><span style="font-weight:500">{{ t.amount }}</span></div>
          <div style="margin-top:4px;font-size:13px"><span class="badge badge--dark" style="font-size:11px;margin-right:6px">{{ t.type }}</span>{{ t.from }}{{ t.to ? ` → ${t.to}` : '' }}</div>
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
import { apiFetch } from '@/services/api.js'

const router = useRouter()
const activeTab = ref('Transfer')

// Transfer form
const fromSearch = ref(''); const fromResults = ref([]); const fromSearching = ref(false); const fromCustomer = ref(null)
const toSearch = ref('');   const toResults = ref([]);   const toSearching = ref(false);   const toCustomer = ref(null)
const transferForm = ref({ amount: '', reason: '' })
const transferLoading = ref(false); const transferError = ref(''); const transferSuccess = ref(false)

// Deposit form
const depositSearch = ref(''); const depositResults = ref([]); const depositSearching = ref(false); const depositTarget = ref(null)
const depositForm = ref({ amount: '', reason: '' })
const depositLoading = ref(false); const depositError = ref(''); const depositSuccess = ref(false)

function resetForms() {
  fromSearch.value = ''; fromResults.value = []; fromCustomer.value = null
  toSearch.value = ''; toResults.value = []; toCustomer.value = null
  transferForm.value = { amount: '', reason: '' }
  transferError.value = ''; transferSuccess.value = false
  depositSearch.value = ''; depositResults.value = []; depositTarget.value = null
  depositForm.value = { amount: '', reason: '' }
  depositError.value = ''; depositSuccess.value = false
}

async function searchAccounts(name) {
  return apiFetch(`/accounts/search?name=${encodeURIComponent(name.trim())}`)
}

async function searchFrom() {
  if (!fromSearch.value.trim()) return
  fromSearching.value = true
  try { fromResults.value = await searchAccounts(fromSearch.value) } catch {}
  fromSearching.value = false
}

async function searchTo() {
  if (!toSearch.value.trim()) return
  toSearching.value = true
  try { toResults.value = await searchAccounts(toSearch.value) } catch {}
  toSearching.value = false
}

async function searchDeposit() {
  if (!depositSearch.value.trim()) return
  depositSearching.value = true
  try { depositResults.value = await searchAccounts(depositSearch.value) } catch {}
  depositSearching.value = false
}

async function submitTransfer() {
  transferError.value = ''; transferSuccess.value = false
  if (!fromCustomer.value || !toCustomer.value) { transferError.value = 'Select both source and destination.'; return }
  const amount = parseFloat(transferForm.value.amount)
  if (!amount || amount <= 0) { transferError.value = 'Enter a valid amount.'; return }
  if (!transferForm.value.reason.trim()) { transferError.value = 'Reason is required.'; return }
  transferLoading.value = true
  try {
    await createTransaction({ type: 'TRANSFER', fromIban: fromCustomer.value.iban, toIban: toCustomer.value.iban, amount, description: transferForm.value.reason })
    transferSuccess.value = true
    transferForm.value = { amount: '', reason: '' }; fromCustomer.value = null; toCustomer.value = null
    loadRecentTransfers()
  } catch (err) { transferError.value = err.message }
  finally { transferLoading.value = false }
}

async function submitDeposit() {
  depositError.value = ''; depositSuccess.value = false
  if (!depositTarget.value) { depositError.value = 'Select a customer account.'; return }
  const amount = parseFloat(depositForm.value.amount)
  if (!amount || amount <= 0) { depositError.value = 'Enter a valid amount.'; return }
  if (!depositForm.value.reason.trim()) { depositError.value = 'Reason is required.'; return }
  depositLoading.value = true
  try {
    await createTransaction({ type: 'DEPOSIT', iban: depositTarget.value.iban, amount, description: depositForm.value.reason })
    depositSuccess.value = true
    depositForm.value = { amount: '', reason: '' }; depositTarget.value = null
    loadRecentTransfers()
  } catch (err) { depositError.value = err.message }
  finally { depositLoading.value = false }
}

const recentTransfers = ref([])
const recentLoading = ref(false)

function formatDate(iso) { return iso ? new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—' }
function formatAmount(m) { return m ? `€${Number(m.amount).toLocaleString('nl-NL', { minimumFractionDigits: 2 })}` : '—' }

async function loadRecentTransfers() {
  recentLoading.value = true
  try {
    const page = await listTransactions({ page: 0, size: 8 })
    recentTransfers.value = (page.items || []).map(tx => ({
      id: tx.transactionId,
      date: formatDate(tx.createdAt),
      type: tx.transactionType,
      from: tx.fromAccount?.name || tx.fromAccount?.iban || '—',
      to: tx.toAccount?.name || tx.toAccount?.iban || null,
      amount: formatAmount(tx.amount),
    }))
  } catch {}
  recentLoading.value = false
}

onMounted(loadRecentTransfers)
</script>
