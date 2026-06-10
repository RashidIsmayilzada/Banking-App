<template>
  <div class="app">
    <AppTopBar
      :user="displayName"
      role="Customer"
      :initials="initials"
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
import { computed } from 'vue'
import AppTopBar from '@/components/shared/AppTopBar.vue'
import AppSidebar from '@/components/shared/AppSidebar.vue'
import { logout } from '@/services/auth.js'
import { useAuth } from '@/stores/auth'

const router = useRouter()
const authStore = useAuth()
const displayName = computed(() => authStore.user.value?.username || authStore.user.value?.email || 'Customer')
const initials = computed(() => displayName.value.split(/[\s.@_-]+/).filter(Boolean).slice(0, 2).map(part => part[0]?.toUpperCase()).join('') || 'C')

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
  { label: 'Dashboard',    icon: 'home',       to: '/customer/dashboard' },
  { label: 'Transfer',     icon: 'transfer',   to: '/customer/transfer' },
  { label: 'Transactions', icon: 'list',       to: '/customer/transactions' },
{ label: 'Cards',        icon: 'creditCard', to: '/customer/cards' },
]
</script>
