<template>
  <AtmShell>
    <div style="flex:1;display:flex;flex-direction:column;justify-content:center">
      <h2 class="t-h2" style="margin:0 0 6px;text-align:center">Welcome.</h2>
      <p class="t-body muted" style="margin:0 0 28px;text-align:center">Sign in to use this ATM.</p>
      <div class="col" style="gap:16px">
        <AppField label="Email" v-model="form.email" placeholder="name@example.com" />
        <AppField label="Password" v-model="form.password" type="password" placeholder="••••••••" />
        <p v-if="error" style="margin:0;padding:10px 14px;background:var(--red-soft,#fef2f2);color:var(--red,#dc2626);border-radius:8px;font-size:13px">{{ error }}</p>
        <button class="btn btn--primary btn--xl btn--block" style="margin-top:8px" :disabled="loading" @click="handleLogin">
          <span v-if="loading">Signing in…</span>
          <template v-else>Sign in <AppIcon name="arrowRight" :size="20" /></template>
        </button>
        <div class="t-body-sm" style="text-align:center;margin-top:4px">Forgot password? Please see branch staff.</div>
      </div>
    </div>
    <div class="row" style="justify-content:center;margin-top:24px;gap:6px;font-size:11px;color:var(--ink-faint)">
      <AppIcon name="shield" :size="12" /> Encrypted session · Camera active
    </div>
  </AtmShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AtmShell from '@/components/layout/AtmShell.vue'
import AppField from '@/components/shared/AppField.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import { atmLogin, setAtmSession, clearAtmSession, atmGetAccounts, setAtmAccount } from '@/services/atm.js'

const router = useRouter()
const form = ref({ email: '', password: '' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    const response = await atmLogin(form.value.email, form.value.password)
    if (response.user?.role !== 'CUSTOMER') {
      error.value = 'Only customer accounts can use this ATM.'
      return
    }
    if (response.user?.userStatus !== 'APPROVED') {
      error.value = 'Your account is not yet approved. Please visit a branch.'
      return
    }
    setAtmSession(response.accessToken, response.user)
    const accountData = await atmGetAccounts()
    const checking = (accountData.accounts || []).find(a => a.accountType === 'CHECKING')
    if (!checking) {
      clearAtmSession()
      error.value = 'No checking account found. Please visit a branch.'
      return
    }
    setAtmAccount(checking)
    router.push('/atm/home')
  } catch (err) {
    clearAtmSession()
    error.value = err.message || 'Login failed.'
  } finally {
    loading.value = false
  }
}
</script>
