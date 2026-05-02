<template>
  <div :class="['avatar', toneClass, size === 'lg' ? 'avatar--lg' : '']">
    {{ initials }}
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: String, default: 'md' },
})

const TONES = ['', 'avatar--blue', 'avatar--teal', 'avatar--pink', 'avatar--brown']

const toneClass = computed(() => {
  let h = 0
  for (const c of props.name) h = (h * 31 + c.charCodeAt(0)) | 0
  return TONES[Math.abs(h) % TONES.length]
})

const initials = computed(() =>
  props.name.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase()
)
</script>
