<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <h1 class="t-h1" style="margin:0">Pending approvals</h1>
        <p class="t-body muted" style="margin:6px 0 0">{{ pendingCount }} customers awaiting account creation.</p>
      </div>
      <span class="spacer" />
      <button class="btn btn--secondary"><AppIcon name="filter" :size="16" /> Filter</button>
      <button class="btn btn--primary">Bulk approve…</button>
    </div>

    <div v-if="error" class="banner banner--danger" style="margin-bottom:16px">{{ error }}</div>
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:24px">Customer</th>
            <th>Email</th>
            <th>BSN</th>
            <th>Phone</th>
            <th>Submitted</th>
            <th style="padding-right:24px;text-align:right">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="muted" style="padding:24px;text-align:center">Loading approvals...</td>
          </tr>
          <tr v-else-if="pending.length === 0">
            <td colspan="6" class="muted" style="padding:24px;text-align:center">No pending approvals.</td>
          </tr>
          <tr v-for="c in pending" :key="c.id">
            <td style="padding-left:24px">
              <div class="row">
                <AppAvatar :name="c.name" />
                <div>
                  <div style="font-weight:500;font-size:14px">{{ c.name }}</div>
                  <div class="t-body-sm">
                    {{ c.ago }}
                    <span v-if="c.overdue" style="color:var(--warning);font-weight:500"> · over 24h</span>
                  </div>
                </div>
              </div>
            </td>
            <td class="muted" style="font-size:13px">{{ c.email }}</td>
            <td class="iban">{{ c.bsn }}</td>
            <td class="num muted">{{ c.phone }}</td>
            <td class="num muted">{{ c.submitted }}</td>
            <td style="padding-right:24px;text-align:right">
              <div class="row" style="justify-content:flex-end;gap:6px">
                <button class="btn btn--ghost btn--xs" @click="rejectUser(c.id)" :disabled="loading">Reject</button>
                <!-- Navigate to approval form -->
                <RouterLink :to="`/employee/approvals/${c.id}`" class="btn btn--primary btn--xs">
                  Approve →
                </RouterLink>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import * as userService from '@/services/user'

const pending = ref([])
const pendingCount = ref(0)
const loading = ref(false)
const error = ref(null)

async function fetchPending() {
  loading.value = true
  error.value = null
  try {
    const response = await userService.getAllUsers({
      role: 'CUSTOMER',
      status: 'PENDING_APPROVAL',
      size: 100
    })
    
    pendingCount.value = response.totalElements ?? response.content.length
    pending.value = response.content
      .map(user => {
        const submittedDate = user.registeredAt ? new Date(user.registeredAt) : new Date()
        const diffMs = new Date() - submittedDate
        const diffHours = diffMs / (1000 * 60 * 60)
        
        return {
          id: user.id,
          name: [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || user.email,
          email: user.email,
          bsn: user.bsn || '—',
          phone: user.phoneNumber || '—',
          submitted: submittedDate.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }) + ' · ' + submittedDate.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }),
          ago: diffHours < 1 ? 'Just now' : (diffHours < 24 ? Math.floor(diffHours) + 'h ago' : Math.floor(diffHours/24) + 'd ago'),
          overdue: diffHours > 24
        }
      })
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function rejectUser(id) {
  if (!confirm('Are you sure you want to reject this customer?')) return
  try {
    await userService.approveUser(id, 'REJECTED')
    await fetchPending()
  } catch (err) {
    alert('Failed to reject: ' + err.message)
  }
}

onMounted(() => {
  fetchPending()
})
</script>
