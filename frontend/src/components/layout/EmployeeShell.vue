<template>
  <div class="app">
    <AppTopBar
      user="Sven van Berg"
      role="Employee · Branch 014"
      initials="SB"
      avatar-tone="avatar--brown"
      @sign-out="router.push('/login')"
    />
    <div class="shell">
      <AppSidebar :items="NAV" settings-to="/employee/settings" security-to="/employee/security" />
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
  { label: 'Overview',          icon: 'pieChart',  to: '/employee/overview' },
  { label: 'Customers',         icon: 'users',     to: '/employee/customers' },
  { label: 'Pending Approvals', icon: 'clock',     to: '/employee/approvals', badge: 12 },
  { label: 'Transactions',      icon: 'list',      to: '/employee/transactions' },
  { label: 'Transfer',          icon: 'transfer',  to: '/employee/transfer' },
  { label: 'Limits',            icon: 'shield',    to: '/employee/limits' },
]
</script>
