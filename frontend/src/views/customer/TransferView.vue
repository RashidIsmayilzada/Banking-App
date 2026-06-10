<template>
  <CustomerShell>
    <h1 class="t-h1" style="margin:0 0 8px">Send money</h1>
    <p class="t-body muted" style="margin:0 0 24px">Move money between your accounts or send to another customer.</p>

    <div class="tabs" style="margin-bottom:24px">
      <div v-for="tab in tabs" :key="tab" :class="['tab', activeTab === tab ? 'tab--active' : '']" @click="switchTab(tab)">{{ tab }}</div>
    </div>

    <div style="display:grid;grid-template-columns:1.4fr 1fr;gap:20px;align-items:flex-start">

      <!-- ── Left: main form ── -->
      <div class="card" style="padding:28px">
        <h3 class="t-h3" style="margin:0 0 20px">Move funds</h3>

        <div v-if="accountsLoading" class="muted t-body-sm" style="padding:20px 0;text-align:center">Loading accounts…</div>
        <template v-else>

          <!-- From account picker -->
          <div class="field" style="margin-bottom:16px">
            <label class="field__label">From</label>
            <div style="position:relative">
              <div class="input row" style="justify-content:space-between;cursor:pointer" @click="togglePicker('from')">
                <div class="row" style="gap:10px">
                  <div class="icon-box icon-box--ink" style="width:32px;height:32px;border-radius:8px;flex-shrink:0">
                    <AppIcon name="wallet" :size="14" />
                  </div>
                  <div v-if="fromAccount">
                    <div style="font-weight:500;font-size:14px">{{ fromAccount.accountType === 'CHECKING' ? 'Checking' : 'Savings' }}</div>
                    <div class="iban" style="font-size:12px">{{ fromAccount.iban }}</div>
                  </div>
                  <div v-else class="muted" style="font-size:14px">Select account</div>
                </div>
                <div class="row" style="gap:8px;flex-shrink:0">
                  <span style="font-weight:500">{{ fromAccount ? formatEur(fromAccount.balance?.amount) : '—' }}</span>
                  <AppIcon name="chevronDown" :size="14" />
                </div>
              </div>
              <div v-if="openPicker === 'from'" class="card" style="position:absolute;top:100%;left:0;right:0;z-index:10;padding:6px;margin-top:4px;box-shadow:0 4px 16px rgba(0,0,0,0.12)">
                <div v-for="a in checkingAccounts" :key="a.iban"
                  class="row" style="padding:10px 12px;border-radius:8px;cursor:pointer;gap:10px"
                  :style="{ background: a.iban === fromAccount?.iban ? 'var(--surface-soft)' : '' }"
                  @click="fromAccount = a; openPicker = null">
                  <div class="icon-box icon-box--ink" style="width:28px;height:28px;border-radius:6px;flex-shrink:0"><AppIcon name="wallet" :size="12" /></div>
                  <div style="flex:1">
                    <div style="font-weight:500;font-size:13px">Checking · {{ a.iban }}</div>
                  </div>
                  <span style="font-weight:500;font-size:13px">{{ formatEur(a.balance?.amount) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div style="display:flex;justify-content:center;margin:-4px 0">
            <div style="width:36px;height:36px;border-radius:50%;background:var(--surface);border:1.5px solid var(--line);display:grid;place-items:center;z-index:1;position:relative">
              <AppIcon name="arrowDown" :size="14" />
            </div>
          </div>

          <!-- To account picker -->
          <div class="field" style="margin-bottom:24px">
            <label class="field__label">To</label>
            <div v-if="activeTab === 'Between my accounts'" style="position:relative">
              <div class="input row" style="justify-content:space-between;cursor:pointer" @click="togglePicker('to')">
                <div class="row" style="gap:10px">
                  <div class="icon-box icon-box--teal" style="width:32px;height:32px;border-radius:8px;flex-shrink:0">
                    <AppIcon name="pieChart" :size="14" />
                  </div>
                  <div v-if="toAccount">
                    <div style="font-weight:500;font-size:14px">{{ toAccount.accountType === 'CHECKING' ? 'Checking' : 'Savings' }}</div>
                    <div class="iban" style="font-size:12px">{{ toAccount.iban }}</div>
                  </div>
                  <div v-else class="muted" style="font-size:14px">Select account</div>
                </div>
                <div class="row" style="gap:8px;flex-shrink:0">
                  <span style="font-weight:500">{{ toAccount ? formatEur(toAccount.balance?.amount) : '—' }}</span>
                  <AppIcon name="chevronDown" :size="14" />
                </div>
              </div>
              <div v-if="openPicker === 'to'" class="card" style="position:absolute;top:100%;left:0;right:0;z-index:10;padding:6px;margin-top:4px;box-shadow:0 4px 16px rgba(0,0,0,0.12)">
                <div v-for="a in accounts.filter(a => a.iban !== fromAccount?.iban)" :key="a.iban"
                  class="row" style="padding:10px 12px;border-radius:8px;cursor:pointer;gap:10px"
                  :style="{ background: a.iban === toAccount?.iban ? 'var(--surface-soft)' : '' }"
                  @click="toAccount = a; openPicker = null">
                  <div class="icon-box icon-box--teal" style="width:28px;height:28px;border-radius:6px;flex-shrink:0"><AppIcon name="pieChart" :size="12" /></div>
                  <div style="flex:1">
                    <div style="font-weight:500;font-size:13px">{{ a.accountType === 'CHECKING' ? 'Checking' : 'Savings' }} · {{ a.iban }}</div>
                  </div>
                  <span style="font-weight:500;font-size:13px">{{ formatEur(a.balance?.amount) }}</span>
                </div>
              </div>
            </div>
            <!-- External IBAN input for "To another customer" tab -->
            <div v-else class="input row" style="gap:10px;padding:10px 14px">
              <div class="icon-box icon-box--blue" style="width:32px;height:32px;border-radius:8px;flex-shrink:0"><AppIcon name="user" :size="14" /></div>
              <input v-model="extIban" class="iban" placeholder="NL10INHO0000000011" style="border:none;outline:none;background:transparent;font-size:14px;flex:1;font-family:var(--font-mono)" />
            </div>
          </div>

          <!-- Amount -->
          <div class="field" style="margin-bottom:16px">
            <label class="field__label">Amount (€)</label>
            <input class="input input--xl" v-model="amount" placeholder="0.00" type="number" min="0.01" step="0.01" />
            <div class="row" style="gap:8px;margin-top:8px;flex-wrap:wrap">
              <button v-for="preset in [50, 100, 250, 500, 1000]" :key="preset"
                :class="['amount-chip', Number(amount) === preset ? 'amount-chip--active' : '']"
                style="font-size:13px;padding:8px 14px"
                @click="amount = String(preset)">
                {{ formatEur(preset) }}
              </button>
            </div>
          </div>

          <AppField label="Description" v-model="description" placeholder="e.g. Save for holiday" />

          <!-- Real daily limit banner -->
          <div v-if="fromAccount" class="banner banner--info" style="margin-top:20px">
            <AppIcon name="info" :size="16" class="banner__icon" />
            <div>Daily transfer limit: <strong>{{ formatEur(fromAccount.dailyTransferLimit?.amount) }}</strong></div>
          </div>

          <div v-if="submitError" class="banner banner--danger" style="margin-top:16px">
            <AppIcon name="alert" :size="16" class="banner__icon" /><div>{{ submitError }}</div>
          </div>
          <div v-if="submitSuccess" class="banner banner--success" style="margin-top:16px">
            <AppIcon name="check" :size="16" class="banner__icon" /><div>Transfer completed successfully.</div>
          </div>

          <div class="row" style="margin-top:24px">
            <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
            <span class="spacer" />
            <button class="btn btn--primary btn--lg" :disabled="submitLoading" @click="submit">
              <span v-if="submitLoading">Sending…</span>
              <template v-else>Send <AppIcon name="arrowRight" :size="16" /></template>
            </button>
          </div>
        </template>
      </div>

      <!-- ── Right column ── -->
      <div class="col" style="gap:16px">

        <!-- To another customer quick panel -->
        <div v-if="activeTab === 'To another customer'" class="card">
          <h3 class="t-h3" style="margin:0 0 6px">Recipient</h3>
          <p class="t-body-sm" style="margin:0 0 12px">Enter the IBAN of the person you want to send to.</p>
          <div class="banner banner--info" style="margin:0">
            <AppIcon name="info" :size="16" class="banner__icon" />
            <div>The recipient IBAN is entered in the form on the left.</div>
          </div>
        </div>

        <!-- Recent recipients -->
        <div class="card card--soft">
          <h4 class="t-h4" style="margin:0 0 10px">Recent recipients</h4>
          <div v-if="recipientsLoading" class="muted t-body-sm" style="padding:8px 0">Loading…</div>
          <div v-else-if="recentRecipients.length === 0" class="muted t-body-sm" style="padding:8px 0">No recent transfers yet.</div>
          <div v-else v-for="r in recentRecipients" :key="r.iban"
            class="row" style="padding:8px 0;border-bottom:1px dashed var(--line)">
            <AppAvatar :name="r.name" />
            <div>
              <div style="font-weight:500;font-size:13px">{{ r.name }}</div>
              <div class="iban">{{ r.iban }}</div>
            </div>
            <span class="spacer" />
            <button class="btn btn--ghost btn--xs" @click="useRecipient(r)">
              <AppIcon name="arrowRight" :size="12" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </CustomerShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import { getAccounts } from '@/services/accounts.js'
import { createTransaction, listTransactions } from '@/services/transaction.js'

const router = useRouter()

const tabs = ['Between my accounts', 'To another customer']
const activeTab = ref('Between my accounts')

const accounts = ref([])
const accountsLoading = ref(true)
const checkingAccounts = computed(() => accounts.value.filter(a => a.accountType === 'CHECKING'))

const fromAccount = ref(null)
const toAccount   = ref(null)
const extIban     = ref('')
const amount      = ref('')
const description = ref('')
const openPicker  = ref(null)

const submitLoading = ref(false)
const submitError   = ref('')
const submitSuccess = ref(false)

const recentRecipients = ref([])
const recipientsLoading = ref(false)

function formatEur(val) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(val || 0))
}

function togglePicker(which) {
  openPicker.value = openPicker.value === which ? null : which
}

function switchTab(tab) {
  activeTab.value = tab
  submitError.value = ''
  submitSuccess.value = false
  openPicker.value = null
}

function useRecipient(r) {
  activeTab.value = 'To another customer'
  extIban.value = r.iban
}

async function submit() {
  submitError.value = ''
  submitSuccess.value = false

  const amt = parseFloat(amount.value)
  if (!amt || amt <= 0)        { submitError.value = 'Enter a valid amount.'; return }
  if (!description.value.trim()) { submitError.value = 'Description is required.'; return }

  if (activeTab.value === 'Between my accounts') {
    if (!fromAccount.value)  { submitError.value = 'Select a source account.'; return }
    if (!toAccount.value)    { submitError.value = 'Select a destination account.'; return }
    if (fromAccount.value.iban === toAccount.value.iban) { submitError.value = 'Source and destination must differ.'; return }
  } else {
    if (!fromAccount.value)             { submitError.value = 'Select a source account.'; return }
    if (!extIban.value.replace(/\s/g, '')) { submitError.value = 'Enter the recipient IBAN.'; return }
  }

  submitLoading.value = true
  try {
    const toIban = activeTab.value === 'Between my accounts'
      ? toAccount.value.iban
      : extIban.value.replace(/\s/g, '')

    await createTransaction({
      type: 'TRANSFER',
      fromIban: fromAccount.value.iban,
      toIban,
      amount: amt,
      description: description.value,
    })

    submitSuccess.value = true
    amount.value = ''
    description.value = ''
    extIban.value = ''

    const data = await getAccounts()
    accounts.value = data.accounts || []
    fromAccount.value = accounts.value.find(a => a.iban === fromAccount.value?.iban) || fromAccount.value
    if (activeTab.value === 'Between my accounts') {
      toAccount.value = accounts.value.find(a => a.iban === toAccount.value?.iban) || toAccount.value
    }
    loadRecipients()
  } catch (err) {
    submitError.value = err.message
  } finally {
    submitLoading.value = false
  }
}

async function loadRecipients() {
  recipientsLoading.value = true
  try {
    const page = await listTransactions({ page: 0, size: 20 })
    const seen = new Set()
    recentRecipients.value = (page.items || [])
      .filter(tx => tx.transactionType === 'TRANSFER' && tx.toAccount?.iban)
      .reduce((acc, tx) => {
        const iban = tx.toAccount.iban
        if (!seen.has(iban)) { seen.add(iban); acc.push({ iban, name: tx.toAccount.name || iban }) }
        return acc
      }, [])
      .slice(0, 4)
  } catch {}
  recipientsLoading.value = false
}

onMounted(async () => {
  try {
    const data = await getAccounts()
    accounts.value = data.accounts || []
    fromAccount.value = checkingAccounts.value[0] || null
    toAccount.value   = accounts.value.find(a => a.iban !== fromAccount.value?.iban) || null
  } catch {}
  accountsLoading.value = false
  loadRecipients()
})
</script>
