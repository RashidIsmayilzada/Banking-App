<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">Customers</h1>
      <span class="spacer" />
      <div class="row" style="gap:8px;position:relative">
        <AppIcon name="search" :size="14" style="position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--ink-faint)" />
        <!-- TODO: GET /employees/customers?q=… to search -->
        <input class="input" placeholder="Search name, email, IBAN…" style="width:320px;padding-left:38px" v-model="search" @input="fetchCustomers" />
        <select class="select" style="width:160px" v-model="statusFilter" @change="fetchCustomers">
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="PENDING">Pending</option>
          <option value="CLOSED">Closed</option>
        </select>
      </div>
    </div>

    <!-- TODO: Fetch customers from GET /employees/customers?q=search&status=statusFilter&page=page&size=10 -->
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:24px">Customer</th>
            <th>Email</th>
            <th>IBAN(s)</th>
            <th>Status</th>
            <th>Joined</th>
            <th style="padding-right:24px;text-align:right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in customers" :key="c.id">
            <td style="padding-left:24px">
              <div class="row">
                <AppAvatar :name="c.name" />
                <div>
                  <div style="font-weight:500;font-size:14px">{{ c.name }}</div>
                  <div class="t-body-sm">#{{ c.id }}</div>
                </div>
              </div>
            </td>
            <td class="muted" style="font-size:13px">{{ c.email }}</td>
            <td class="iban">{{ c.ibans }}</td>
            <td><AppStatus :kind="c.status" /></td>
            <td class="num muted">{{ c.joined }}</td>
            <td style="padding-right:24px;text-align:right">
              <div class="row" style="justify-content:flex-end;gap:6px">
                <RouterLink
                  v-if="c.status === 'pending'"
                  :to="`/employee/approvals/${c.id}`"
                  class="btn btn--primary btn--xs"
                >Approve →</RouterLink>
                <template v-else-if="c.status === 'active'">
                  <RouterLink :to="`/employee/customers/${c.id}`" class="btn btn--ghost btn--xs">View</RouterLink>
                  <button class="btn btn--ghost-danger btn--xs">Close</button>
                </template>
                <RouterLink v-else :to="`/employee/customers/${c.id}`" class="btn btn--ghost btn--xs">View</RouterLink>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div style="padding:0 16px 12px" v-if="totalPages > 0">
        <AppPager 
          :current-page="page + 1" 
          :total="totalElements" 
          :count="`${page * 10 + 1}–${Math.min((page + 1) * 10, totalElements)} of ${totalElements}`"
          @change="handlePageChange"
        />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import AppPager from '@/components/shared/AppPager.vue'
import * as userService from '@/services/user'

const search = ref('')
const statusFilter = ref('')
const customers = ref([])
const loading = ref(false)
const error = ref(null)
const page = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)

async function fetchCustomers() {
  loading.value = true
  error.value = null
  try {
    const params = {
      role: 'CUSTOMER',
      page: page.value,
      size: 10,
      search: search.value
    }
    
    if (statusFilter.value === 'ACTIVE') params.active = true
    if (statusFilter.value === 'PENDING') params.active = false
    
    const response = await userService.getAllUsers(params)
    customers.value = response.content.map(user => ({
      id: user.id,
      name: user.customerProfile ? `${user.customerProfile.firstName} ${user.customerProfile.lastName}` : user.username,
      email: user.email,
      ibans: user.accounts && user.accounts.length > 0 ? user.accounts.map(a => a.iban).join(', ') : '— (no accounts yet)',
      status: user.active ? 'active' : (user.customerProfile && user.customerProfile.status === 'DENIED' ? 'denied' : 'pending'),
      joined: user.createdAt ? new Date(user.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
    }))
    totalElements.value = response.totalElements
    totalPages.value = response.totalPages
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function handlePageChange(newPage) {
  page.value = newPage - 1
  fetchCustomers()
}

onMounted(() => {
  fetchCustomers()
})
</script>
