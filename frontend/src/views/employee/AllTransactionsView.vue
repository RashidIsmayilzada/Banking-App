<template>
  <EmployeeShell>
    <div class="row" style="margin-bottom:24px">
      <div>
        <h1 class="t-h1" style="margin:0">System transactions</h1>
        <!-- TODO: Fetch totals from GET /employees/transactions summary -->
        <p class="t-body muted" style="margin:6px 0 0">1 084 today · €412k volume</p>
      </div>
      <span class="spacer" />
      <button class="btn btn--secondary btn--sm"><AppIcon name="download" :size="14" /> Export</button>
    </div>

    <!-- Filters -->
    <div class="card" style="margin-bottom:20px;padding:20px">
      <div style="display:grid;grid-template-columns:repeat(5,1fr) auto;gap:12px;align-items:end">
        <AppField label="Start date" v-model="filters.startDate" placeholder="2026-04-01" />
        <AppField label="End date"   v-model="filters.endDate"   placeholder="2026-04-28" />
        <AppField label="From IBAN"  v-model="filters.fromIban"  placeholder="NL.." />
        <AppField label="To IBAN"    v-model="filters.toIban"    placeholder="NL.." />
        <div class="field">
          <label class="field__label">Initiated by</label>
          <select class="select" v-model="filters.channel">
            <option value="">Anyone</option>
            <option value="CUSTOMER">Customer</option>
            <option value="EMPLOYEE">Employee</option>
            <option value="ATM">ATM</option>
          </select>
        </div>
        <!-- TODO: Apply filters → GET /employees/transactions with query params -->
        <button class="btn btn--primary" @click="applyFilters">Apply</button>
      </div>
    </div>

    <!-- TODO: Fetch from GET /employees/transactions with pagination -->
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:24px">Timestamp</th>
            <th>From</th>
            <th>To</th>
            <th>Initiated by</th>
            <th>Type</th>
            <th style="padding-right:24px;text-align:right">Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in transactions" :key="tx.id">
            <td class="num" style="padding-left:24px">{{ tx.timestamp }}</td>
            <td class="iban">{{ tx.from }}</td>
            <td class="iban">{{ tx.to }}</td>
            <td>
              <div class="row" style="gap:8px">
                <AppAvatar :name="tx.initiator" />
                <span style="font-size:13px">{{ tx.initiator }}</span>
              </div>
            </td>
            <td>
              <span :class="['badge', tx.typeTone ? `badge--${tx.typeTone}` : '']">{{ tx.type }}</span>
            </td>
            <td class="num" style="padding-right:24px;text-align:right;font-weight:500">{{ tx.amount }}</td>
          </tr>
        </tbody>
      </table>
      <div style="padding:0 16px 12px">
        <AppPager :current-page="1" :total="109" count="1–7 of 1 084" />
      </div>
    </div>
  </EmployeeShell>
</template>

<script setup>
import { ref } from 'vue'
import EmployeeShell from '@/components/layout/EmployeeShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'
import AppPager from '@/components/shared/AppPager.vue'

const filters = ref({ startDate: '', endDate: '', fromIban: '', toIban: '', channel: '' })

// TODO: GET /employees/transactions with filters as query params
function applyFilters() {}

// TODO: Fetch from GET /employees/transactions
const transactions = [
  { id: 1, timestamp: '28 Apr · 14:22:01', from: 'NL42 INHO …89', to: 'NL91 ABNA …42', initiator: 'Jane Doe',     type: 'Card',     typeTone: 'warn', amount: '€42,18'    },
  { id: 2, timestamp: '28 Apr · 14:18:55', from: 'NL11 INHO …07', to: 'NL55 INHO …14', initiator: 'S. van Berg',  type: 'Employee', typeTone: 'info', amount: '€1 200,00' },
  { id: 3, timestamp: '28 Apr · 13:50:22', from: 'ATM #14',        to: 'NL42 INHO …89', initiator: 'Jane (ATM)',  type: 'Withdraw', typeTone: 'pink', amount: '€100,00'   },
  { id: 4, timestamp: '28 Apr · 11:02:10', from: 'NL77 INHO …40', to: 'NL11 INHO …07', initiator: 'P. de Vries', type: 'Customer', typeTone: '',     amount: '€75,00'    },
  { id: 5, timestamp: '28 Apr · 09:01:44', from: 'NL42 INHO …89', to: 'NL11 RABO …07', initiator: 'Jane Doe',    type: 'Customer', typeTone: '',     amount: '€250,00'   },
  { id: 6, timestamp: '27 Apr · 18:30:11', from: 'NL91 RABO …01', to: 'NL42 INHO …89', initiator: '— external',  type: 'Deposit',  typeTone: 'pink', amount: '€2 400,00' },
  { id: 7, timestamp: '27 Apr · 17:14:09', from: 'ATM #08',        to: 'NL77 INHO …40', initiator: 'Pieter (ATM)',type: 'Deposit',  typeTone: 'pink', amount: '€500,00'   },
]
</script>
