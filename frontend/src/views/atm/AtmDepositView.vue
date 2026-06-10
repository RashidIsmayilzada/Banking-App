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
    <div style="position:relative;margin-top:6px">
      <span style="position:absolute;left:16px;top:50%;transform:translateY(-50%);font-size:20px;color:var(--ink-faint);pointer-events:none">€</span>
      <input
        class="input input--xl"
        style="padding-left:32px"
        type="number"
        min="0.01"
        step="0.01"
        placeholder="0.00"
        :value="customAmount"
        @input="onCustomInput"
      />
    </div>

    <div class="row" style="gap:8px;flex-wrap:wrap;margin-top:14px;margin-bottom:18px">
      <button v-for="n in presets" :key="n" :class="['amount-chip', selectedAmount === n && !customAmount ? 'amount-chip--active' : '']" @click="selectPreset(n)">€{{ n }}</button>
    </div>

    <p v-if="error" style="margin:0 0 12px;padding:10px 14px;background:var(--red-soft,#fef2f2);color:var(--red,#dc2626);border-radius:8px;font-size:13px">{{ error }}</p>

    <div class="col" style="gap:8px;margin-top:auto">
      <button class="btn btn--primary btn--xl btn--block" :disabled="!activeAmount || activeAmount <= 0 || loading" @click="confirm">
        {{ loading ? 'Processing…' : 'Confirm deposit' }}
      </button>
      <RouterLink to="/atm/home" class="btn btn--ghost btn--block">Cancel</RouterLink>
    </div>
  </AtmShell>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AtmShell from '@/components/layout/AtmShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import { getAtmAccount, setAtmAccount, setAtmLastTx, atmCreateTransaction, getAtmUser } from '@/services/atm.js'

const router = useRouter()
if (!getAtmUser()) router.push('/atm/login')

const account = ref(getAtmAccount())
const presets = [20, 50, 100, 200, 500]
const selectedAmount = ref(null)
const customAmount = ref('')
const loading = ref(false)
const error = ref('')

function selectPreset(n) {
  selectedAmount.value = n
  customAmount.value = ''
}

function onCustomInput(e) {
  customAmount.value = e.target.value
  selectedAmount.value = null
}

const activeAmount = computed(() => {
  if (customAmount.value) return parseFloat(customAmount.value)
  return selectedAmount.value
})

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
      amount: activeAmount.value,
      channel: 'ATM',
      description: 'ATM deposit',
    })
    setAtmLastTx({ ...result, mode: 'deposit', displayAmount: activeAmount.value })
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
