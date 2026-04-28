<template>
  <div class="spark" :style="{ width: width ? width + 'px' : '100%', height: height + 'px' }">
    <div
      v-for="(h, i) in bars"
      :key="i"
      :class="['spark__bar', dimEarly && i < bars.length - 5 ? 'dim' : '']"
      :style="{ height: h + '%', background: accentLast && i === bars.length - 1 ? 'var(--teal)' : undefined }"
    />
  </div>
</template>

<script setup>
defineProps({
  bars:       { type: Array,   default: () => [35,50,30,65,45,70,55,80,60,75,90,85] },
  height:     { type: Number,  default: 56 },
  width:      { type: Number,  default: 0 },
  dimEarly:   { type: Boolean, default: false },
  accentLast: { type: Boolean, default: false },
})
</script>
