<template>
  <AdminShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">Transaction Reversal</h1>
    </div>

    <div class="card" style="max-width: 600px; margin-bottom: 24px;">
      <h2 class="t-h3" style="margin-top: 0;">Force Reverse Transaction</h2>
      <p class="muted" style="margin-bottom: 24px; font-size: 14px;">
        Warning: This action will forcefully deduct funds from the recipient and return them to the sender. This cannot be undone.
      </p>

      <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">
        <AppIcon name="alert" :size="16" class="banner__icon" />
        <div>{{ error }}</div>
      </div>

      <div v-if="successData" class="banner banner--success" style="margin-bottom:16px; background: var(--teal-soft, #f0fdfa); color: var(--teal, #0d9488); border-radius: 8px; padding: 12px;">
        <AppIcon name="check" :size="16" class="banner__icon" />
        <div>
          <strong>Success!</strong> Transaction #{{ successData.originalTransactionId }} was reversed.<br/>
          New Reversal Record: #{{ successData.reversalTransactionId }}<br/>
          Amount Reversed: {{ formatEur(successData.amount) }}
        </div>
      </div>

      <div class="row" style="gap:12px; align-items: flex-end;">
        <div style="flex: 1;">
          <AppField
              label="Original Transaction ID"
              v-model="transactionId"
              placeholder="e.g. 1042"
              type="number"
          />
        </div>
        <button
            class="btn btn--danger"
            style="margin-bottom: 12px;"
            :disabled="loading || !transactionId"
            @click="handleReverse"
        >
          {{ loading ? 'Processing...' : 'Reverse Transaction' }}
        </button>
      </div>
    </div>
  </AdminShell>
</template>

<script setup>
import { ref } from 'vue'
import AdminShell from '@/components/layout/AdminShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import * as adminService from '@/services/admin'

const transactionId = ref('')
const loading = ref(false)
const error = ref(null)
const successData = ref(null)

async function handleReverse() {
  if (!confirm(`Are you absolutely sure you want to reverse Transaction #${transactionId.value}?`)) return

  loading.value = true
  error.value = null
  successData.value = null

  try {
    const response = await adminService.reverseTransaction(transactionId.value)
    successData.value = response
    transactionId.value = '' // clear the input on success
  } catch (err) {
    error.value = err.message || 'Failed to reverse transaction.'
  } finally {
    loading.value = false
  }
}

function formatEur(amount) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(amount)
}
</script>