<template>
  <aside class="sidebar">
    <div class="nav-section">Menu</div>
    <RouterLink
      v-for="item in items"
      :key="item.label"
      :to="item.to"
      :class="['nav-item', isActive(item) ? 'active' : '']"
    >
      <AppIcon :name="item.icon" :size="18" class="nav-item__icon" />
      <span>{{ item.label }}</span>
      <span v-if="item.badge" class="spacer" />
      <span
        v-if="item.badge"
        class="badge badge--warn"
        style="padding:2px 8px;font-size:11px"
      >{{ item.badge }}</span>
    </RouterLink>
    <span class="spacer" />
    <div class="nav-section">Account</div>
    <RouterLink :to="settingsTo" class="nav-item">
      <AppIcon name="settings" :size="18" class="nav-item__icon" />
      <span>Settings</span>
    </RouterLink>
    <RouterLink :to="securityTo" class="nav-item">
      <AppIcon name="shield" :size="18" class="nav-item__icon" />
      <span>Security</span>
    </RouterLink>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  items:      { type: Array,  required: true },
  settingsTo: { type: String, default: '#' },
  securityTo: { type: String, default: '#' },
})

const route = useRoute()
const isActive = (item) => route.path.startsWith(item.to)
</script>
