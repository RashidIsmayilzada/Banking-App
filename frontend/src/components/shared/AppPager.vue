<template>
  <div class="pager">
    <span style="font-size:13px;color:var(--ink-faint)">Showing {{ count }}</span>
    <span class="spacer" />
    <button class="pager__btn" aria-label="prev" :disabled="currentPage <= 1" @click="emit('change', currentPage - 1)">
      <AppIcon name="chevronLeft" :size="14" />
    </button>
    <button
      v-for="n in pages"
      :key="n"
      :class="['pager__btn', n === currentPage ? 'pager__btn--active' : '']"
      @click="emit('change', n)"
    >{{ n }}</button>
    <span v-if="total > 5" style="color:var(--ink-faint);padding:0 4px">…</span>
    <button v-if="total > 5" class="pager__btn" @click="emit('change', total)">{{ total }}</button>
    <button class="pager__btn" aria-label="next" :disabled="currentPage >= total" @click="emit('change', currentPage + 1)">
      <AppIcon name="chevronRight" :size="14" />
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  currentPage: { type: Number, default: 1 },
  total:       { type: Number, default: 7 },
  count:       { type: String, default: '1–10 of 64' },
})

const emit = defineEmits(['change'])

const pages = computed(() => Array.from({ length: Math.min(props.total, 5) }, (_, i) => i + 1))
</script>
