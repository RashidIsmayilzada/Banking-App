<template>
  <div class="field">
    <label v-if="label" class="field__label">{{ label }}</label>
    <slot>
      <input
          :class="[
          'input',
          size === 'lg' ? 'input--lg' : '',
          size === 'xl' ? 'input--xl' : '',
          error ? 'input--error' : '',
          disabled ? 'input--disabled' : ''
        ]"
          :type="type"
          :value="modelValue"
          :placeholder="placeholder"
          :disabled="disabled"
          @input="$emit('update:modelValue', $event.target.value)"
      />
    </slot>
    <div v-if="error" class="field__error">
      <AppIcon name="alert" :size="12" />
      {{ error }}
    </div>
    <div v-else-if="hint" class="field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
import AppIcon from './AppIcon.vue'

defineProps({
  label:       { type: String,  default: '' },
  modelValue:  { type: String,  default: '' },
  placeholder: { type: String,  default: '' },
  hint:        { type: String,  default: '' },
  error:       { type: String,  default: '' },
  type:        { type: String,  default: 'text' },
  size:        { type: String,  default: '' },

  disabled:    { type: Boolean, default: false }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
/* 2. Added a visual lock state  */
.input--disabled {
  background-color: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
  opacity: 0.7;
}
</style>