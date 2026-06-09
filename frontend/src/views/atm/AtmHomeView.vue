<template>
  <AtmShell>
    <div class="card card--dark" style="padding:20px;margin-bottom:20px;text-align:center">
      <div class="t-label" style="margin-bottom:8px">Available · Checking</div>
      <div class="t-display" style="font-size:44px;color:#fff;margin:0">{{ formatEur(account?.balance?.amount) }}</div>
      <div class="iban" style="color:rgba(255,255,255,0.6);margin-top:8px">{{ account?.iban || '—' }}</div>
    </div>

    <div class="col" style="gap:12px;flex:1">
      <RouterLink to="/atm/deposit" class="btn btn--secondary" style="height:84px;font-size:22px;justify-content:flex-start;padding:0 24px;text-decoration:none">
        <div class="icon-box" style="background:var(--teal-tint);color:var(--teal)">
          <AppIcon name="deposit" :size="22" />
        </div>
        <div style="text-align:left">
          <div>Deposit</div>
          <div style="font-size:12px;color:var(--ink-faint);font-weight:400;font-family:var(--font-body)">Add cash to your account</div>
        </div>
      </RouterLink>
      <RouterLink to="/atm/withdraw" class="btn btn--primary" style="height:84px;font-size:22px;justify-content:flex-start;padding:0 24px;text-decoration:none">
        <div class="icon-box" style="background:rgba(255,255,255,0.15);color:#fff">
          <AppIcon name="withdraw" :size="22" />
        </div>
        <div style="text-align:left">
          <div>Withdraw</div>
          <div style="font-size:12px;color:rgba(255,255,255,0.7);font-weight:400;font-family:var(--font-body)">Take cash out</div>
        </div>
      </RouterLink>
    </div>

    <div class="row" style="margin-top:20px">
      <button class="btn btn--ghost btn--sm" @click="signOut">
        <AppIcon name="logout" :size="14" /> Sign out
      </button>
      <span class="spacer" />
      <span class="t-body-sm">Hi, {{ firstName }}</span>
    </div>
  </AtmShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AtmShell from '@/components/layout/AtmShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import { getAtmUser, getAtmAccount, setAtmAccount, atmGetAccounts, atmLogout, clearAtmSession } from '@/services/atm.js'

const router = useRouter()
const account = ref(getAtmAccount())
const user = getAtmUser()
const firstName = computed(() => user?.firstName || user?.username || 'there')

function formatEur(amount) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(Number(amount || 0))
}

async function signOut() {
  await atmLogout()
  clearAtmSession()
  router.push('/atm/login')
}

onMounted(async () => {
  if (!getAtmUser()) { router.push('/atm/login'); return }
  try {
    const data = await atmGetAccounts()
    const checking = (data.accounts || []).find(a => a.accountType === 'CHECKING')
    if (checking) { setAtmAccount(checking); account.value = checking }
  } catch {}
})
</script>
