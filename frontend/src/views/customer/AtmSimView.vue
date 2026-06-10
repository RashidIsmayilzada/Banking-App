<template>
  <CustomerShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <h1 class="t-h1" style="margin:0 0 4px">ATM</h1>
        <p class="t-body muted" style="margin:0">Simulate a cash withdrawal or deposit.</p>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1.2fr 1fr;gap:20px;align-items:flex-start">

      <!-- Left: ATM card -->
      <div>
        <!-- Balance card -->
        <div class="card card--dark" style="padding:28px;margin-bottom:20px">
          <div class="t-label" style="margin-bottom:6px">{{ account ? (account.accountType === 'CHECKING' ? 'Checking account' : 'Savings account') : 'Account' }}</div>
          <div v-if="accountsLoading" class="muted t-body-sm">Loading…</div>
          <template v-else-if="account">
            <div class="t-display" style="font-size:52px;color:#fff;margin:0 0 8px">
              {{ formatEur(account.balance?.amount) }}
            </div>
            <div class="iban" style="color:rgba(255,255,255,0.55)">{{ account.iban }}</div>
          </template>
          <div v-else class="muted t-body-sm">No checking account found.</div>
        </div>

        <!-- Mode toggle -->
        <div class="card" style="padding:20px;margin-bottom:20px">
          <div class="tabs" style="margin-bottom:20px">
            <div :class="['tab', mode === 'withdraw' ? 'tab--active' : '']" @click="switchMode('withdraw')">
              <AppIcon name="arrowUpRight" :size="14" /> Withdraw
            </div>
            <div :class="['tab', mode === 'deposit' ? 'tab--active' : '']" @click="switchMode('deposit')">
              <AppIcon name="arrowDown" :size="14" /> Deposit
            </div>
          </div>

          <div class="field" style="margin-bottom:16px">
            <label class="field__label">Amount</label>
            <input class="input input--xl" v-model="amount" placeholder="0.00" type="number" min="0.01" step="0.01" />
          </div>

          <div class="row" style="gap:8px;flex-wrap:wrap;margin-bottom:20px">
            <button v-for="p in presets" :key="p"
              :class="['amount-chip', Number(amount) === p ? 'amount-chip--active' : '']"
              @click="amount = String(p)">
              {{ formatEur(p) }}
            </button>
          </div>

          <div v-if="mode === 'withdraw' && account" class="banner banner--info" style="margin-bottom:20px">
            <AppIcon name="info" :size="16" class="banner__icon" />
            <div>Daily limit: <strong>{{ formatEur(account.dailyTransferLimit?.amount) }}</strong></div>
          </div>

          <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">
            <AppIcon name="alert" :size="16" class="banner__icon" /><div>{{ error }}</div>
          </div>

          <button class="btn btn--primary btn--lg btn--block" :disabled="loading || !account || !amount" @click="execute">
            <span v-if="loading">Processing…</span>
            <template v-else>
              {{ mode === 'withdraw' ? 'Withdraw cash' : 'Deposit cash' }}
              <AppIcon name="arrowRight" :size="16" />
            </template>
          </button>
        </div>
      </div>

      <!-- Right: receipt / history -->
      <div class="col" style="gap:16px">

        <!-- Last transaction receipt -->
        <div v-if="lastTx" class="card">
          <div class="row" style="margin-bottom:16px">
            <div style="width:40px;height:40px;border-radius:50%;background:var(--teal);color:#fff;display:grid;place-items:center;flex-shrink:0">
              <AppIcon name="check" :size="20" :stroke="2.5" />
            </div>
            <div>
              <div style="font-weight:500;font-size:15px">{{ lastTx.mode === 'withdraw' ? 'Withdrawal' : 'Deposit' }} complete</div>
              <div class="t-body-sm muted">{{ lastTx.time }}</div>
            </div>
          </div>
          <div class="col" style="gap:8px;font-size:14px">
            <div class="row"><span class="muted">Amount</span><span class="spacer" /><strong>{{ formatEur(lastTx.amount) }}</strong></div>
            <div class="row"><span class="muted">Account</span><span class="spacer" /><span class="iban" style="font-size:12px">{{ lastTx.iban }}</span></div>
            <div class="row"><span class="muted">New balance</span><span class="spacer" /><strong style="font-size:16px">{{ formatEur(lastTx.newBalance) }}</strong></div>
            <div class="row"><span class="muted">Ref</span><span class="spacer" /><span class="iban" style="font-size:12px">#{{ lastTx.ref }}</span></div>
          </div>
        </div>

        <!-- ATM info card -->
        <div class="card card--soft">
          <h4 class="t-h4" style="margin:0 0 12px">How it works</h4>
          <div class="col" style="gap:10px;font-size:13px">
            <div class="row" style="gap:8px;align-items:flex-start">
              <div class="icon-box icon-box--blue" style="width:28px;height:28px;border-radius:6px;flex-shrink:0"><AppIcon name="arrowUpRight" :size="12" /></div>
              <div><strong>Withdraw</strong> — deducts from your checking account, subject to your daily limit.</div>
            </div>
            <div class="row" style="gap:8px;align-items:flex-start">
              <div class="icon-box icon-box--teal" style="width:28px;height:28px;border-radius:6px;flex-shrink:0"><AppIcon name="arrowDown" :size="12" /></div>
              <div><strong>Deposit</strong> — adds cash to your checking account immediately.</div>
            </div>
            <div class="row" style="gap:8px;align-items:flex-start">
              <div class="icon-box icon-box--ink" style="width:28px;height:28px;border-radius:6px;flex-shrink:0"><AppIcon name="shield" :size="12" /></div>
              <div>All transactions are recorded and visible in your transaction history.</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </CustomerShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import { getAccounts } from '@/services/accounts.js'
import { createTransaction } from '@/services/transaction.js'

const accounts      = ref([])
const accountsLoading = ref(true)
const account       = computed(() => accounts.value.find(a => a.accountType === 'CHECKING') || null)

const mode   = ref('withdraw')
const amount = ref('')
const presets = [20, 50, 100, 200, 500]
const loading = ref(false)
const error   = ref('')
const lastTx  = ref(null)

function formatEur(val) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(val || 0))
}

function switchMode(m) {
  mode.value = m
  error.value = ''
  amount.value = ''
}

async function execute() {
  error.value = ''
  const amt = parseFloat(amount.value)
  if (!amt || amt <= 0) { error.value = 'Enter a valid amount.'; return }
  if (!account.value)   { error.value = 'No account found.'; return }

  loading.value = true
  try {
    const result = await createTransaction({
      type: mode.value === 'withdraw' ? 'WITHDRAWAL' : 'DEPOSIT',
      iban: account.value.iban,
      amount: amt,
      description: mode.value === 'withdraw' ? 'ATM withdrawal' : 'ATM deposit',
      channel: 'ATM',
    })

    const newBalance = mode.value === 'withdraw'
      ? result.sourceBalance?.amount
      : result.sourceBalance?.amount

    lastTx.value = {
      mode:       mode.value,
      amount:     amt,
      iban:       account.value.iban,
      newBalance: newBalance,
      ref:        result.transaction?.transactionId || '—',
      time:       new Date().toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
    }
    amount.value = ''

    const data = await getAccounts()
    accounts.value = data.accounts || []
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const data = await getAccounts()
    accounts.value = data.accounts || []
  } catch {}
  accountsLoading.value = false
})
</script>
