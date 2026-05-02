<template>
  <div class="pager">
    <span style="font-size:13px;color:var(--ink-faint)">Showing {{ count }}</span>
    <span class="spacer" />
    <button class="pager__btn" aria-label="prev">
      <AppIcon name="chevronLeft" :size="14" />
    </button>
    <button
      v-for="n in pages"
      :key="n"
      :class="['pager__btn', n === currentPage ? 'pager__btn--active' : '']"
    >{{ n }}</button>
    <span v-if="total > 5" style="color:var(--ink-faint);padding:0 4px">…</span>
    <button v-if="total > 5" class="pager__btn">{{ total }}</button>
    <button class="pager__btn" aria-label="next">
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

const pages = computed(() => Array.from({ length: Math.min(props.total, 5) }, (_, i) => i + 1))
</script>
