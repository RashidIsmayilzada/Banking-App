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
      <div style="padding:0 16px 12px">
        <AppPager :current-page="1" :total="36" count="1–7 of 247" />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref } from 'vue'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import AppPager from '@/components/shared/AppPager.vue'

const search = ref('')
const statusFilter = ref('')

// TODO: Fetch from GET /employees/customers with filters and pagination
function fetchCustomers() {}

const customers = [
  { id: '00247', name: 'Jane Doe',        email: 'jane.doe@example.com',    ibans: 'NL42 INHO …89  ·  …21', status: 'active',  joined: '12 Jan 2026' },
  { id: '00198', name: 'Maarten Janssen', email: 'maarten.j@example.com',   ibans: 'NL11 INHO …07  ·  …08', status: 'active',  joined: '04 Mar 2026' },
  { id: '00231', name: 'Sara El-Amin',    email: 'sara.e@example.com',      ibans: 'NL55 INHO …14',          status: 'active',  joined: '21 Mar 2026' },
  { id: '00248', name: 'Tom Bakker',      email: 'tom.b@example.com',       ibans: '— (no accounts yet)',    status: 'pending', joined: '27 Apr 2026' },
  { id: '00112', name: 'Lisa Vermeer',    email: 'lisa.v@example.com',      ibans: 'NL08 INHO …62  ·  …63', status: 'closed',  joined: '02 Feb 2025' },
  { id: '00219', name: 'Pieter de Vries', email: 'pieter.dv@example.com',   ibans: 'NL77 INHO …40',          status: 'active',  joined: '15 Apr 2026' },
  { id: '00249', name: 'Yusuf Demir',     email: 'yusuf.d@example.com',     ibans: '— (no accounts yet)',    status: 'pending', joined: '26 Apr 2026' },
]
</script>
