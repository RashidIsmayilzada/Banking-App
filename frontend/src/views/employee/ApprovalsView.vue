<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <h1 class="t-h1" style="margin:0">Pending approvals</h1>
        <p class="t-body muted" style="margin:6px 0 0">{{ pending.length }} customers awaiting account creation.</p>
      </div>
      <span class="spacer" />
      <button class="btn btn--secondary"><AppIcon name="filter" :size="16" /> Filter</button>
      <button class="btn btn--primary">Bulk approve…</button>
    </div>

    <!-- TODO: Fetch from GET /employees/customers?status=PENDING&page=0&size=20 -->
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
const loading = ref(false)
const error = ref(null)

async function fetchPending() {
  loading.value = true
  error.value = null
  try {
    const response = await userService.getAllUsers({
      role: 'CUSTOMER',
      active: false, // Pending users are inactive
      size: 100 // Get all for now
    })
    
    // Filter by customerProfile.status === 'PENDING' if possible, or just use active=false
    pending.value = response.content
      .filter(user => user.customerProfile && user.customerProfile.status === 'PENDING')
      .map(user => {
        const profile = user.customerProfile
        const submittedDate = new Date(user.createdAt)
        const diffMs = new Date() - submittedDate
        const diffHours = diffMs / (1000 * 60 * 60)
        
        return {
          id: user.id,
          name: `${profile.firstName} ${profile.lastName}`,
          email: user.email,
          bsn: profile.bsn,
          phone: profile.phoneNumber,
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
    await userService.approveUser(id, 'DENIED')
    await fetchPending()
  } catch (err) {
    alert('Failed to reject: ' + err.message)
  }
}

onMounted(() => {
  fetchPending()
})
</script>
