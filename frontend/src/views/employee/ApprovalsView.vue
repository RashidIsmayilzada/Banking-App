<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <h1 class="t-h1" style="margin:0">Pending approvals</h1>
        <!-- TODO: Fetch count from GET /employees/customers?status=PENDING -->
        <p class="t-body muted" style="margin:6px 0 0">12 customers awaiting account creation.</p>
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
                <!-- TODO: POST /employees/customers/{customerUserId}/approval with decision=REJECTED -->
                <button class="btn btn--ghost btn--xs">Reject</button>
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
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'

// TODO: Fetch from GET /employees/customers?status=PENDING
const pending = [
  { id: '00248', name: 'Tom Bakker',  email: 'tom.b@example.com',    bsn: '987-654-321', phone: '+31 6 5544 3322', submitted: '27 Apr · 14:02', ago: '20h ago', overdue: false },
  { id: '00250', name: 'Anna Visser', email: 'a.visser@example.com', bsn: '112-233-445', phone: '+31 6 1212 3434', submitted: '27 Apr · 11:45', ago: '23h ago', overdue: false },
  { id: '00249', name: 'Yusuf Demir', email: 'yusuf.d@example.com',  bsn: '556-778-990', phone: '+31 6 7788 1010', submitted: '26 Apr · 09:30', ago: '2d ago',  overdue: true  },
  { id: '00251', name: 'Mira Patel',  email: 'mira.p@example.com',   bsn: '334-556-778', phone: '+31 6 9090 1212', submitted: '25 Apr · 16:08', ago: '3d ago',  overdue: true  },
]
</script>
