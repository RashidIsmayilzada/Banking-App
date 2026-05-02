<template>
  <span :class="['badge', `badge--${tone}`, 'badge--dot']">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  kind: { type: String, default: 'active' },
})

const map = {
  active:  ['success', 'Active'],
  pending: ['warn',    'Pending'],
  closed:  ['danger',  'Closed'],
}

const tone  = computed(() => (map[props.kind] ?? map.active)[0])
const label = computed(() => (map[props.kind] ?? map.active)[1])
</script>
