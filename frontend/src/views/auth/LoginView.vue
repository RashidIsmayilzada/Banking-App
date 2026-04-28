<template>
  <div class="app" style="display:grid;grid-template-columns:1fr 1.1fr;height:100vh">
    <!-- Left: form -->
    <div style="padding:40px 48px;display:flex;flex-direction:column">
      <RouterLink to="/" class="topbar__logo" style="margin-bottom:64px">
        <div class="logo-mark">€</div>
        <span>InHolland</span>
      </RouterLink>

      <div style="max-width:360px;margin-top:32px">
        <h1 class="t-h1" style="margin:0 0 12px">Welcome back.</h1>
        <p class="t-body muted" style="margin:0 0 32px">
          Sign in to manage your accounts, transfers, and cards.
        </p>
        <div class="col" style="gap:16px">
          <AppField label="Email" v-model="form.email" placeholder="jane.doe@example.com" />
          <AppField label="Password" v-model="form.password" type="password" placeholder="••••••••" />
          <div class="row" style="justify-content:space-between;margin-top:4px">
            <label :class="['checkbox', keepSigned ? 'checkbox--checked' : '']" @click="keepSigned = !keepSigned">
              <span class="checkbox__box">
                <AppIcon v-if="keepSigned" name="check" :size="12" :stroke="2.5" />
              </span>
              Keep me signed in
            </label>
            <a style="font-size:13px;color:var(--ink);font-weight:500;text-decoration:none;border-bottom:1px solid currentColor;cursor:pointer">
              Forgot password?
            </a>
          </div>
          <button class="btn btn--primary btn--lg btn--block" style="margin-top:8px" @click="handleLogin">
            Sign in <AppIcon name="arrowRight" :size="16" />
          </button>
          <div class="row" style="justify-content:center;font-size:14px;color:var(--ink-soft);margin-top:8px">
            New to InHolland?&nbsp;
            <RouterLink to="/register" style="color:var(--ink);font-weight:500;text-decoration:none;border-bottom:1.5px solid currentColor">
              Create account
            </RouterLink>
          </div>
        </div>
      </div>
      <span class="spacer" />
      <div class="row" style="font-size:12px;color:var(--ink-faint);gap:8px">
        <AppIcon name="shield" :size="14" /> Secured · 256-bit · TLS 1.3
      </div>
    </div>

    <!-- Right: brand panel -->
    <div style="background:var(--ink);color:#fff;padding:48px;display:flex;flex-direction:column;position:relative;overflow:hidden">
      <div style="font-size:13px;letter-spacing:0.06em;text-transform:uppercase;opacity:0.6">The bank of Holland</div>
      <span class="spacer" />
      <h2 class="t-display" style="margin:0;font-size:76px;color:#fff">
        Money,<br />without the friction.
      </h2>
      <div class="row" style="margin-top:32px;gap:12px;flex-wrap:wrap">
        <span class="badge" style="background:rgba(255,255,255,0.1);color:#fff">Instant SEPA</span>
        <span class="badge" style="background:rgba(255,255,255,0.1);color:#fff">0,00 € fees</span>
        <span class="badge" style="background:rgba(255,255,255,0.1);color:#fff">24/7 support</span>
      </div>
      <span class="spacer" />
      <!-- Decorative card -->
      <div style="position:absolute;right:-60px;top:80px;width:320px;height:200px;background:linear-gradient(135deg,var(--teal),#006a52);border-radius:20px;transform:rotate(8deg);padding:24px;color:#fff">
        <div style="font-size:12px;letter-spacing:0.1em;text-transform:uppercase;opacity:0.85">InHolland · Premium</div>
        <div style="font-family:var(--font-mono);font-size:17px;margin-top:60px;letter-spacing:0.15em">•••• •••• •••• 4892</div>
        <div class="row" style="margin-top:8px;font-size:11px;opacity:0.9">
          <span>JANE DOE</span><span class="spacer" /><span>04/29</span>
        </div>
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

const form = ref({ email: '', password: '' })
const keepSigned = ref(true)

// TODO: POST /api/auth/login with form.value, then route based on returned role
function handleLogin() {
  // Temporary demo routing — replace with real auth
  router.push('/customer/dashboard')
}
</script>
