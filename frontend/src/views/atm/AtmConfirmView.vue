<template>
  <AtmShell>
    <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center">
      <div style="width:80px;height:80px;border-radius:50%;background:var(--teal);color:#fff;display:grid;place-items:center;margin-bottom:20px">
        <AppIcon name="check" :size="40" :stroke="2.5" />
      </div>
      <h2 class="t-h1" style="margin:0 0 8px">
        {{ isDeposit ? `€${tx?.displayAmount} deposited` : `€${tx?.displayAmount} withdrawn` }}
      </h2>
      <p class="t-body muted" style="margin:0 0 24px">
        {{ isDeposit ? 'Your balance has been updated.' : 'Please take your cash from the slot below.' }}
      </p>

      <div class="card card--soft" style="width:100%;padding:18px;margin-bottom:20px">
        <div class="summary-row"><span class="muted">Account</span><span class="iban" style="color:var(--ink)">{{ tx?.transaction?.fromAccount?.iban || tx?.transaction?.toAccount?.iban || account?.iban || '—' }}</span></div>
        <div class="summary-row"><span class="muted">Amount</span><span style="font-weight:500">{{ formatEur(tx?.displayAmount) }}</span></div>
        <div class="summary-row"><span class="muted">New balance</span><span style="font-weight:500;font-size:18px">{{ formatEur(tx?.sourceBalance?.amount) }}</span></div>
        <div class="summary-row"><span class="muted">Reference</span><span class="iban" style="color:var(--ink)">#{{ tx?.transaction?.transactionId || '—' }}</span></div>
      </div>

      <div class="col" style="width:100%;gap:8px">
        <RouterLink to="/atm/home" class="btn btn--secondary btn--block">Another transaction</RouterLink>
        <button class="btn btn--ghost btn--block" @click="endSession">End session</button>
      </div>
    </div>
  </AtmShell>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AtmShell from '@/components/layout/AtmShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import { getAtmLastTx, getAtmAccount, atmLogout, clearAtmSession } from '@/services/atm.js'

const router = useRouter()
const tx = getAtmLastTx()
const account = getAtmAccount()
const isDeposit = computed(() => tx?.mode === 'deposit')

if (!tx) router.push('/atm/home')

function formatEur(amount) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(amount || 0))
}

async function endSession() {
  await atmLogout()
  clearAtmSession()
  router.push('/atm/login')
}
</script>
