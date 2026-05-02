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
        <AppField
          label="BSN"
          v-model="form.bsn"
          placeholder="9-digit number"
          :error="errors.bsn"
          hint="Encrypted at rest"
        />
        <AppField
          label="Phone"
          v-model="form.phone"
          placeholder="+31 6 …"
          hint="Used for SMS verification"
        />
        <div style="grid-column:1/-1">
          <AppField
            label="Password"
            v-model="form.password"
            type="password"
            :error="errors.password"
            hint="Min 8 chars · 1 number · 1 symbol"
          />
        </div>
        <div style="grid-column:1/-1">
          <AppField label="Date of birth" v-model="form.dob" placeholder="DD / MM / YYYY" />
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

      <div class="row" style="margin-top:28px">
        <RouterLink to="/login" class="btn btn--ghost">
          <AppIcon name="arrowLeft" :size="16" /> Back
        </RouterLink>
        <span class="spacer" />
        <button class="btn btn--primary btn--lg" @click="handleRegister">
          Continue <AppIcon name="arrowRight" :size="16" />
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

const router = useRouter()

const form = ref({
  firstName: '', lastName: '', email: '', bsn: '',
  phone: '', password: '', dob: '',
})
const errors = ref({ email: '', bsn: '', password: '' })
const agreed = ref(false)

// TODO: POST /api/auth/register with form.value, then route to /pending
function handleRegister() {
  router.push('/pending')
}
</script>
