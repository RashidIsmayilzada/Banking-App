<template>
  <div class="app">
    <AppTopBar
      :user="displayName"
      role="Employee · Branch 014"
      :initials="initials"
      avatar-tone="avatar--brown"
      @sign-out="handleSignOut"
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
import { computed } from 'vue'
import AppTopBar from '@/components/shared/AppTopBar.vue'
import AppSidebar from '@/components/shared/AppSidebar.vue'
import { logout } from '@/services/auth.js'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const authStore = useAuth()
const displayName = computed(() => authStore.user.value?.username || authStore.user.value?.email || 'Employee')
const initials = computed(() => displayName.value.split(/[\s.@_-]+/).filter(Boolean).slice(0, 2).map(part => part[0]?.toUpperCase()).join('') || 'E')

async function handleSignOut() {
  try {
    await logout()
  } catch {
    // Local session cleanup still signs the user out if the API is unavailable.
  } finally {
    authStore.clearSession()
    sessionStorage.removeItem('pending_user')
    router.replace('/login')
  }
}

const NAV = [
  { label: 'Overview',          icon: 'pieChart',  to: '/employee/overview' },
  { label: 'Customers',         icon: 'users',     to: '/employee/customers' },
  { label: 'Pending Approvals', icon: 'clock',     to: '/employee/approvals', badge: 12 },
  { label: 'Transactions',      icon: 'list',      to: '/employee/transactions' },
  { label: 'Transfer',          icon: 'transfer',  to: '/employee/transfer' },
  { label: 'Limits',            icon: 'shield',    to: '/employee/limits' },
]
</script>
