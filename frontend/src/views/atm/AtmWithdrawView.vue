<template>
  <AtmShell>
    <div class="row" style="margin-bottom:20px">
      <RouterLink to="/atm/home" class="btn btn--ghost btn--sm" style="padding:8px">
        <AppIcon name="arrowLeft" :size="16" />
      </RouterLink>
      <span class="spacer" />
      <span class="badge badge--dark">Withdraw</span>
    </div>

    <!-- TODO: Fetch balance from session account via GET /api/accounts -->
    <div class="t-label">Available</div>
    <div class="t-h2" style="margin:4px 0 24px">€6 218,40</div>

    <label class="field__label">Amount</label>
    <div class="input input--xl" style="margin-top:6px">{{ selectedAmount ? `€${selectedAmount}` : 'Select an amount' }}</div>

    <div class="row" style="gap:8px;flex-wrap:wrap;margin-top:14px;margin-bottom:18px">
      <button
        v-for="n in presets"
        :key="n"
        :class="['amount-chip', selectedAmount === n ? 'amount-chip--active' : '']"
        @click="selectedAmount = n"
      >€{{ n }}</button>
    </div>

    <div class="banner banner--warn" style="margin-bottom:20px">
      <AppIcon name="info" :size="16" class="banner__icon" />
      <div>Daily limit <strong>€2 500</strong> · used <strong>€100</strong> today.</div>
    </div>

    <div class="col" style="gap:8px;margin-top:auto">
      <!-- TODO: POST /atm/transactions/withdrawals with accountId and amount -->
      <button class="btn btn--primary btn--xl btn--block" :disabled="!selectedAmount" @click="confirm">
        Confirm withdrawal
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

const router = useRouter()
const presets = [20, 50, 100, 200, 500]
const selectedAmount = ref(null)

// TODO: POST /atm/transactions/withdrawals → navigate to /atm/confirm on success
function confirm() {
  router.push('/atm/confirm')
}
</script>
