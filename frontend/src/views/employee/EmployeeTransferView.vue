<template>
  <EmployeeShell>
    <h1 class="t-h1" style="margin:0 0 6px">Transfer between customers</h1>
    <p class="t-body muted" style="margin:0 0 24px">Move funds between two customer checking accounts. Always audited.</p>

    <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:20px;align-items:flex-start">
      <div class="card" style="padding:28px">
        <!-- From customer -->
        <!-- TODO: GET /employees/customers?q=… to search customers -->
        <div class="field" style="margin-bottom:16px">
          <label class="field__label">From customer (checking)</label>
          <input class="input" placeholder="Search by name, email, or IBAN" v-model="fromSearch" @input="searchFrom" />
          <div v-if="fromCustomer" class="card card--soft" style="padding:12px;margin-top:8px">
            <div class="row">
              <AppAvatar :name="fromCustomer.name" />
              <div>
                <div style="font-weight:500;font-size:14px">{{ fromCustomer.name }}</div>
                <div class="iban">{{ fromCustomer.iban }}</div>
              </div>
              <span class="spacer" />
              <div style="text-align:right">
                <div style="font-weight:500">{{ fromCustomer.balance }}</div>
                <div class="t-body-sm">Available</div>
              </div>
            </div>
          </div>
        </div>

        <div style="display:flex;justify-content:center;margin:4px 0">
          <div style="width:36px;height:36px;border-radius:50%;border:1.5px solid var(--line);display:grid;place-items:center">
            <AppIcon name="arrowDown" :size="14" />
          </div>
        </div>

        <!-- To customer -->
        <div class="field" style="margin-bottom:20px">
          <label class="field__label">To customer (checking)</label>
          <input class="input" placeholder="Search by name, email, or IBAN" v-model="toSearch" @input="searchTo" />
          <div v-if="toCustomer" class="card card--soft" style="padding:12px;margin-top:8px">
            <div class="row">
              <AppAvatar :name="toCustomer.name" />
              <div>
                <div style="font-weight:500;font-size:14px">{{ toCustomer.name }}</div>
                <div class="iban">{{ toCustomer.iban }}</div>
              </div>
              <span class="spacer" />
              <div style="text-align:right">
                <div style="font-weight:500">{{ toCustomer.balance }}</div>
                <div class="t-body-sm">Available</div>
              </div>
            </div>
          </div>
        </div>

        <div class="field" style="margin-bottom:14px">
          <label class="field__label">Amount</label>
          <input class="input input--lg" v-model="form.amount" placeholder="€ 0,00" />
        </div>

        <AppField
          label="Reason / note (audit log)"
          v-model="form.reason"
          placeholder="Required for employee transfers"
        />

        <div v-if="form.amount && form.reason" class="banner banner--success" style="margin-top:20px">
          <AppIcon name="check" :size="16" class="banner__icon" />
          <div>Within limits. Daily €2 500 · Absolute −€500. Source has sufficient funds.</div>
        </div>

        <div class="row" style="margin-top:24px">
          <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
          <span class="spacer" />
          <!-- TODO: POST /employees/transfers with fromAccountId, toAccountId, amount, description -->
          <button class="btn btn--primary btn--lg" @click="submitTransfer">
            Submit transfer <AppIcon name="arrowRight" :size="16" />
          </button>
        </div>
      </div>

      <!-- Recent employee transfers -->
      <!-- TODO: Fetch from GET /employees/transactions?initiatedByUserId=employeeId -->
      <div class="card" style="padding:0">
        <div style="padding:16px 20px;border-bottom:1px solid var(--line)">
          <h3 class="t-h4" style="margin:0">Recent employee transfers</h3>
        </div>
        <div
          v-for="t in recentTransfers"
          :key="t.id"
          style="padding:14px 20px;border-bottom:1px solid var(--line)"
        >
          <div class="row">
            <span class="num muted">{{ t.date }}</span>
            <span class="spacer" />
            <span style="font-weight:500">{{ t.amount }}</span>
          </div>
          <div class="row" style="margin-top:4px;font-size:13px">
            <span>{{ t.from }} → {{ t.to }}</span>
            <span class="spacer" />
            <span class="t-body-sm">by {{ t.by }}</span>
          </div>
        </div>
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'

const router = useRouter()

const fromSearch = ref('')
const toSearch   = ref('')
const form = ref({ amount: '', reason: '' })

const fromCustomer = ref({ name: 'Jane Doe',        iban: 'NL42 INHO 0123 4567 89', balance: '€6 218,40' })
const toCustomer   = ref({ name: 'Maarten Janssen', iban: 'NL11 INHO 0034 9921 07', balance: '€1 042,80' })

// TODO: GET /employees/customers?q=fromSearch to populate dropdown
function searchFrom() {}
// TODO: GET /employees/customers?q=toSearch to populate dropdown
function searchTo()   {}

// TODO: POST /employees/transfers with fromAccountId, toAccountId, amount, description (audit reason)
function submitTransfer() { router.push('/employee/overview') }

// TODO: Fetch from GET /employees/transactions?channel=EMPLOYEE
const recentTransfers = [
  { id: 1, date: '28 Apr', from: 'Janssen', to: 'El-Amin',  amount: '€1 200,00', by: 'Sven van Berg' },
  { id: 2, date: '25 Apr', from: 'Doe',     to: 'de Vries', amount: '€450,00',   by: 'L. Hartog'     },
  { id: 3, date: '23 Apr', from: 'Vermeer', to: 'Patel',    amount: '€80,00',    by: 'Sven van Berg' },
  { id: 4, date: '21 Apr', from: 'Demir',   to: 'Janssen',  amount: '€2 100,00', by: 'L. Hartog'     },
]
</script>
