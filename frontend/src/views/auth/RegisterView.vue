<template>
  <div class="app" style="background:var(--surface);overflow:auto;height:100vh">
    <div style="padding:32px 48px;max-width:520px;margin:0 auto;width:100%">
      <div class="row" style="margin-bottom:32px">
        <RouterLink to="/" class="topbar__logo">
          <div class="logo-mark">€</div>
          <span>InHolland</span>
        </RouterLink>
        <span class="spacer" />
        <span class="t-body-sm">Step 1 of 2</span>
      </div>

      <h1 class="t-h1" style="margin:0 0 8px">Create your account.</h1>
      <p class="t-body muted" style="margin:0 0 28px">
        We'll set up checking &amp; savings once an employee approves your details.
      </p>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <AppField label="First name" v-model="form.firstName" />
        <AppField label="Last name" v-model="form.lastName" />
        <div style="grid-column:1/-1">
          <AppField
            label="Email"
            v-model="form.email"
            :error="errors.email"
          />
        </div>
        <div style="grid-column:1/-1">
          <AppField
            label="Username"
            v-model="form.username"
            placeholder="3–50 characters"
            :error="errors.username"
          />
        </div>
        <AppField
          label="BSN"
          v-model="form.bsn"
          placeholder="9-digit number"
          :error="errors.bsn"
          hint="Encrypted at rest"
        />
        <AppField
          label="Phone"
          v-model="form.phoneNumber"
          placeholder="+31 6 …"
          hint="Used for SMS verification"
        />
        <div style="grid-column:1/-1">
          <AppField
            label="Password"
            v-model="form.password"
            type="password"
            :error="errors.password"
            hint="Min 12 chars · 1 number · 1 symbol"
          />
        </div>
      </div>

      <div style="margin-top:24px">
        <label :class="['checkbox', agreed ? 'checkbox--checked' : '']" @click="agreed = !agreed">
          <span class="checkbox__box">
            <AppIcon v-if="agreed" name="check" :size="12" :stroke="2.5" />
          </span>
          I agree to the
          <a style="color:var(--ink);text-decoration:underline;text-underline-offset:3px">Terms of Service</a>
          and
          <a style="color:var(--ink);text-decoration:underline;text-underline-offset:3px">Privacy Policy</a>.
        </label>
      </div>

      <p v-if="serverError" style="margin:16px 0 0;padding:10px 14px;background:var(--red-soft,#fef2f2);color:var(--red,#dc2626);border-radius:8px;font-size:13px">
        {{ serverError }}
      </p>

      <div class="row" style="margin-top:28px">
        <RouterLink to="/login" class="btn btn--ghost">
          <AppIcon name="arrowLeft" :size="16" /> Back
        </RouterLink>
        <span class="spacer" />
        <button class="btn btn--primary btn--lg" :disabled="loading" @click="handleRegister">
          <span v-if="loading">Creating account…</span>
          <template v-else>Continue <AppIcon name="arrowRight" :size="16" /></template>
        </button>
      </div>

      <div class="row" style="margin-top:32px;gap:8px;font-size:12px;color:var(--ink-faint)">
        <AppIcon name="shield" :size="14" /> Your BSN is encrypted at rest. We comply with DNB &amp; GDPR.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import { register } from '@/services/auth.js'

const router = useRouter()

const form = ref({
  firstName: '', lastName: '', email: '',
  username: '', bsn: '', phoneNumber: '', password: '',
})
const errors = ref({ email: '', bsn: '', password: '', username: '' })
const agreed = ref(false)
const serverError = ref('')
const loading = ref(false)

function validate() {
  errors.value = { email: '', bsn: '', password: '', username: '' }
  let valid = true

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) {
    errors.value.email = 'Enter a valid email address'
    valid = false
  }
  if (!/^\d{9}$/.test(form.value.bsn)) {
    errors.value.bsn = 'BSN must be exactly 9 digits'
    valid = false
  }
  if (form.value.password.length < 12) {
    errors.value.password = 'Password must be at least 12 characters'
    valid = false
  }
  if (form.value.username.length < 3) {
    errors.value.username = 'Username must be at least 3 characters'
    valid = false
  }
  return valid
}

async function handleRegister() {
  serverError.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const userData = await register({ ...form.value, role: 'CUSTOMER' })
    sessionStorage.setItem('pending_user', JSON.stringify(userData))
    router.push('/pending')
  } catch (err) {
    serverError.value = err.message || 'Registration failed. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>
