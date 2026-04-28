<template>
  <header class="topbar">
    <RouterLink to="/" class="topbar__logo">
      <div class="logo-mark">€</div>
      <span>InHolland</span>
    </RouterLink>
    <span class="spacer" />
    <button class="btn btn--ghost btn--sm" aria-label="Notifications">
      <AppIcon name="bell" :size="18" />
    </button>
    <div class="row" style="gap:10px">
      <div
        :class="['avatar', avatarTone]"
        style="width:36px;height:36px;font-size:13px"
      >{{ initials }}</div>
      <div style="line-height:1.15">
        <div style="font-size:14px;font-weight:500">{{ user }}</div>
        <div style="font-size:12px;color:var(--ink-faint)">{{ role }}</div>
      </div>
    </div>
    <button class="btn btn--ghost btn--sm" @click="$emit('sign-out')">
      <AppIcon name="logout" :size="16" /> Sign out
    </button>
  </header>
</template>

<script setup>
import AppIcon from './AppIcon.vue'

defineProps({
  user:      { type: String, default: 'Jane Doe' },
  role:      { type: String, default: 'Customer' },
  initials:  { type: String, default: 'JD' },
  avatarTone:{ type: String, default: 'avatar--blue' },
})
defineEmits(['sign-out'])
</script>
