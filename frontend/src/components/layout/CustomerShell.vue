<template>
  <div class="app">
    <AppTopBar
      user="Jane Doe"
      role="Customer"
      initials="JD"
      avatar-tone="avatar--blue"
      @sign-out="handleSignOut"
    />
    <div class="shell">
      <AppSidebar :items="NAV" settings-to="/customer/settings" security-to="/customer/security" />
      <main class="main">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppTopBar from '@/components/shared/AppTopBar.vue'
import AppSidebar from '@/components/shared/AppSidebar.vue'
import { logout } from '@/services/auth.js'

const router = useRouter()

function handleSignOut() {
  logout()
  router.push('/login')
}

const NAV = [
  { label: 'Dashboard',    icon: 'home',       to: '/customer/dashboard' },
  { label: 'Accounts',     icon: 'wallet',     to: '/customer/accounts' },
  { label: 'Transfer',     icon: 'transfer',   to: '/customer/transfer' },
  { label: 'Transactions', icon: 'list',       to: '/customer/transactions' },
  { label: 'Cards',        icon: 'creditCard', to: '/customer/cards' },
]
</script>
