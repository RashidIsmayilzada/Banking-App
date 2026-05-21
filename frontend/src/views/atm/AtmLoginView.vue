<template>
  <AtmShell>
    <div style="flex:1;display:flex;flex-direction:column;justify-content:center">
      <h2 class="t-h2" style="margin:0 0 6px;text-align:center">Welcome.</h2>
      <p class="t-body muted" style="margin:0 0 28px;text-align:center">Sign in to use this ATM.</p>
      <div class="col" style="gap:16px">
        <AppField label="Email" v-model="form.email" placeholder="jane.doe@example.com" />
        <AppField label="Password" v-model="form.password" type="password" placeholder="••••••••" />
        <!-- TODO: POST /atm/sessions with form.value (returns X-ATM-Session-Token) -->
        <button class="btn btn--primary btn--xl btn--block" style="margin-top:8px" @click="handleLogin">
          Sign in <AppIcon name="arrowRight" :size="20" />
        </button>
        <div class="t-body-sm" style="text-align:center;margin-top:4px">
          Forgot password? Please see branch staff.
        </div>
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

const router = useRouter()
const form = ref({ email: '', password: '' })

// TODO: POST /atm/sessions → store returned sessionToken, then navigate to /atm/home
function handleLogin() {
  router.push('/atm/home')
}
</script>
