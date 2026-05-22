<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:20px">
      <RouterLink to="/employee/customers" class="row" style="gap:6px;color:var(--ink-soft);font-size:13px;font-weight:500;text-decoration:none">
        <AppIcon name="arrowLeft" :size="14" /> All customers
      </RouterLink>
      <span class="spacer" />
      <RouterLink :to="`/employee/limits`" class="btn btn--secondary btn--sm">
        <AppIcon name="shield" :size="14" /> Set transfer limits
      </RouterLink>
      <button class="btn btn--ghost-danger btn--sm">Close customer</button>
    </div>

    <div v-if="loading" style="padding: 40px; text-align: center;">Loading customer details...</div>
    <div v-else-if="error" class="banner banner--danger" style="margin-bottom: 24px;">{{ error }}</div>
    <div v-else-if="customer">
      <div class="row" style="margin-bottom:24px;gap:20px">
        <AppAvatar :name="customer.customerProfile ? `${customer.customerProfile.firstName} ${customer.customerProfile.lastName}` : customer.username" size="lg" />
        <div>
          <h1 class="t-h1" style="margin:0">{{ customer.customerProfile ? `${customer.customerProfile.firstName} ${customer.customerProfile.lastName}` : customer.username }}</h1>
          <div class="row" style="gap:12px;margin-top:6px">
            <span class="t-body-sm">Customer #{{ customer.id }}</span>
            <span class="t-body-sm">·</span>
            <span class="t-body-sm">Joined {{ customer.createdAt ? new Date(customer.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—' }}</span>
            <AppStatus :kind="customer.active ? 'active' : (customer.customerProfile && customer.customerProfile.status === 'DENIED' ? 'denied' : 'pending')" />
          </div>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:24px">
        <div class="card" style="padding:0">
          <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
            <h3 class="t-h4" style="margin:0">Personal information</h3>
          </div>
          <div
            v-for="([k, v, mono], i) in personalInfo"
            :key="k"
            class="row"
            :style="{ padding:'12px 20px', borderBottom: i < personalInfo.length - 1 ? '1px solid var(--line)' : 'none' }"
          >
            <span class="muted" style="font-size:13px">{{ k }}</span>
            <span class="spacer" />
            <span :class="mono ? 'iban' : ''" style="font-size:14px;font-weight:500;color:var(--ink)">{{ v }}</span>
          </div>
        </div>

        <div class="card" style="padding:0">
          <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
            <h3 class="t-h4" style="margin:0">Accounts</h3>
          </div>
          <div style="padding:16px;display:flex;flex-direction:column;gap:10px">
            <template v-if="customer.accounts && customer.accounts.length > 0">
              <div 
                v-for="acc in customer.accounts" 
                :key="acc.iban"
                class="card card--flat" 
                :style="{ padding:'14px', background: acc.type === 'SAVINGS' ? 'var(--teal-tint)' : undefined }"
              >
                <div class="row">
                  <span class="badge" :class="acc.type === 'SAVINGS' ? '' : 'badge--dark'" :style="acc.type === 'SAVINGS' ? 'background:var(--teal);color:#fff' : ''">
                    {{ acc.type.charAt(0).toUpperCase() + acc.type.slice(1).toLowerCase() }}
                  </span>
                  <span class="iban">{{ acc.iban }}</span>
                  <span class="spacer" />
                  <span style="font-weight:500">€{{ acc.balance.toLocaleString('nl-NL', { minimumFractionDigits: 2 }) }}</span>
                </div>
              </div>
            </template>
            <div v-else class="muted" style="padding: 10px; text-align: center; font-size: 13px;">No accounts yet.</div>
            
            <hr class="divider" style="margin:6px 0" />
            <!-- Note: Limits are usually per account or per user, here we show them if available -->
            <div class="row" style="font-size:13px">
              <span class="muted">Daily transfer limit</span>
              <span class="spacer" />
              <span style="font-weight:500">€2 500,00</span>
            </div>
            <div class="row" style="font-size:13px">
              <span class="muted">Absolute limit</span>
              <span class="spacer" />
              <span style="font-weight:500">−€500,00</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Transaction history -->
    <!-- TODO: Fetch from GET /employees/customers/{customerUserId}/transactions -->
    <h3 class="t-h3" style="margin:0 0 12px">Transaction history</h3>
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:24px">Date</th>
            <th>From</th>
            <th>To</th>
            <th>Initiated by</th>
            <th style="padding-right:24px;text-align:right">Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in transactions" :key="tx.id">
            <td class="num" style="padding-left:24px">{{ tx.date }}</td>
            <td class="iban">{{ tx.from }}</td>
            <td class="iban">{{ tx.to }}</td>
            <td>
              <div class="row" style="gap:8px">
                <AppAvatar :name="tx.initiator" />
                <span style="font-size:13px">{{ tx.initiator }}</span>
              </div>
            </td>
            <td class="num" style="padding-right:24px;text-align:right;font-weight:500"
                :style="{ color: tx.amount.startsWith('+') ? 'var(--teal)' : 'var(--ink)' }">
              {{ tx.amount }}
            </td>
          </tr>
        </tbody>
      </table>
      <div style="padding:0 16px 12px">
        <AppPager :current-page="1" count="1–5 of 138" />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppStatus from '@/components/shared/AppStatus.vue'
import AppPager from '@/components/shared/AppPager.vue'
import * as userService from '@/services/user'

const route = useRoute()
const customer = ref(null)
const loading = ref(false)
const error = ref(null)

const personalInfo = computed(() => {
  if (!customer.value || !customer.value.customerProfile) return []
  const profile = customer.value.customerProfile
  return [
    ['Email',         customer.value.email,           false],
    ['Phone',         profile.phoneNumber,            false],
    ['BSN',           profile.bsn,                    true ],
    ['Date of birth', profile.dateOfBirth || '—',     false],
    // ['Address',       '—',                         false],
  ]
})

// TODO: Fetch from actual transactions endpoint when available
const transactions = ref([
  { id: 1, date: '28 Apr · 14:22', from: 'NL42 INHO …89', to: 'NL91 ABNA …42', initiator: 'Jane Doe',    amount: '−€42,18'   },
  { id: 2, date: '28 Apr · 09:01', from: 'NL42 INHO …89', to: 'NL11 INHO …07', initiator: 'Jane Doe',    amount: '−€250,00'  },
  { id: 3, date: '27 Apr · 18:30', from: 'NL91 RABO …01', to: 'NL42 INHO …89', initiator: '— external',  amount: '+€2 400,00'},
  { id: 4, date: '25 Apr · 20:45', from: 'ATM #14',        to: 'NL42 INHO …89', initiator: 'Jane (ATM)', amount: '−€100,00'  },
  { id: 5, date: '22 Apr · 08:15', from: 'NL42 INHO …21', to: 'NL42 INHO …89', initiator: 'Jane Doe',    amount: '+€150,00'  },
])

async function fetchCustomer() {
  loading.value = true
  try {
    customer.value = await userService.getUserById(route.params.id)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCustomer()
})
</script>
