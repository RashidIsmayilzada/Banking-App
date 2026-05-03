<template>
  <div class="app">
    <AppTopBar
      user="Jane Doe"
      role="Customer"
      initials="JD"
      avatar-tone="avatar--blue"
      @sign-out="router.push('/login')"
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

const router = useRouter()

const NAV = [
  { label: 'Dashboard',    icon: 'home',       to: '/customer/dashboard' },
  { label: 'Accounts',     icon: 'wallet',     to: '/customer/accounts' },
  { label: 'Transfer',     icon: 'transfer',   to: '/customer/transfer' },
  { label: 'Transactions', icon: 'list',       to: '/customer/transactions' },
  { label: 'Cards',        icon: 'creditCard', to: '/customer/cards' },
]
</script>
