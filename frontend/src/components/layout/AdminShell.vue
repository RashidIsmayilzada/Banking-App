<template>
  <div class="app">
    <AppTopBar
        :user="displayName"
        role="System Administrator"
        :initials="initials"
        avatar-tone="avatar--pink"
        @sign-out="handleSignOut"
    />
    <div class="shell">
      <AppSidebar :items="NAV" settings-to="/admin/settings" security-to="/admin/security" />
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

const displayName = computed(() => authStore.user.value?.username || authStore.user.value?.email || 'Admin')
const initials = computed(() => displayName.value.split(/[\s.@_-]+/).filter(Boolean).slice(0, 2).map(part => part[0]?.toUpperCase()).join('') || 'A')

async function handleSignOut() {
  try {
    await logout()
  } catch {
    // Failsafe in case backend is unreachable
  } finally {
    authStore.clearSession()
    router.replace('/login')
  }
}

const NAV = [
  { label: 'Dashboard',    icon: 'pieChart',   to: '/admin/dashboard' },
  { label: 'Employees',    icon: 'users',      to: '/admin/employees' },
  { label: 'Accounts',     icon: 'wallet',     to: '/admin/accounts' },
  { label: 'Transactions', icon: 'list',       to: '/admin/transactions' },
  { label: 'Audit Logs',   icon: 'shield',     to: '/admin/audit-logs' },
]
</script>