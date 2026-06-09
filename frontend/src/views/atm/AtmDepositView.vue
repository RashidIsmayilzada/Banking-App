<template>
  <AtmShell>
    <div class="row" style="margin-bottom:20px">
      <RouterLink to="/atm/home" class="btn btn--ghost btn--sm" style="padding:8px">
        <AppIcon name="arrowLeft" :size="16" />
      </RouterLink>
      <span class="spacer" />
      <span class="badge badge--success">Deposit</span>
    </div>

    <div class="t-label">Current balance</div>
    <div class="t-h2" style="margin:4px 0 24px">{{ formatEur(account?.balance?.amount) }}</div>

    <label class="field__label">Amount</label>
    <div class="input input--xl" style="margin-top:6px">{{ selectedAmount ? `€${selectedAmount}` : 'Select an amount' }}</div>

    <div class="row" style="gap:8px;flex-wrap:wrap;margin-top:14px;margin-bottom:18px">
      <button v-for="n in presets" :key="n" :class="['amount-chip', selectedAmount === n ? 'amount-chip--active' : '']" @click="selectedAmount = n">€{{ n }}</button>
    </div>

    <p v-if="error" style="margin:0 0 12px;padding:10px 14px;background:var(--red-soft,#fef2f2);color:var(--red,#dc2626);border-radius:8px;font-size:13px">{{ error }}</p>

    <div class="col" style="gap:8px;margin-top:auto">
      <button class="btn btn--primary btn--xl btn--block" :disabled="!selectedAmount || loading" @click="confirm">
        {{ loading ? 'Processing…' : 'Confirm deposit' }}
      </button>
      <RouterLink to="/atm/home" class="btn btn--ghost btn--block">Cancel</RouterLink>
    </div>
  </AtmShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AtmShell from '@/components/layout/AtmShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import { getAtmAccount, setAtmAccount, setAtmLastTx, atmCreateTransaction, getAtmUser } from '@/services/atm.js'

const router = useRouter()
if (!getAtmUser()) router.push('/atm/login')

const account = ref(getAtmAccount())
const presets = [20, 50, 100, 200, 500]
const selectedAmount = ref(null)
const loading = ref(false)
const error = ref('')

function formatEur(amount) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(amount || 0))
}

async function confirm() {
  error.value = ''
  loading.value = true
  try {
    const result = await atmCreateTransaction({
      type: 'DEPOSIT',
      iban: account.value.iban,
      amount: selectedAmount.value,
      channel: 'ATM',
      description: 'ATM deposit',
    })
    setAtmLastTx({ ...result, mode: 'deposit', displayAmount: selectedAmount.value })
    if (result.sourceBalance) {
      setAtmAccount({ ...account.value, balance: result.sourceBalance })
    }
    router.push('/atm/confirm')
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
