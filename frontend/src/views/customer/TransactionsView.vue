<template>
  <CustomerShell>
    <div class="row" style="margin-bottom:24px">
      <h1 class="t-h1" style="margin:0">Transactions</h1>
      <span class="spacer" />
      <button class="btn btn--secondary btn--sm"><AppIcon name="download" :size="14" /> Export CSV</button>
    </div>

    <!-- Filters -->
    <div class="card" style="margin-bottom:20px;padding:20px">
      <div class="row" style="margin-bottom:14px">
        <AppIcon name="filter" :size="16" />
        <h3 class="t-h4" style="margin:0">Search &amp; filter</h3>
        <span class="spacer" />
        <a style="font-size:13px;color:var(--ink-soft);cursor:pointer" @click="clearFilters">Clear all</a>
      </div>
      <div style="display:grid;grid-template-columns:repeat(4,1fr) auto;gap:12px;align-items:end">
        <AppField label="Start date" v-model="filters.startDate" placeholder="2026-04-01" />
        <AppField label="End date" v-model="filters.endDate" placeholder="2026-04-28" />
        <div class="field">
          <label class="field__label">Amount</label>
          <div class="row" style="gap:6px">
            <select class="select" style="width:70px;padding:12px 8px" v-model="filters.amountOp">
              <option>=</option><option>></option><option>&lt;</option>
            </select>
            <input class="input" placeholder="€ 100,00" v-model="filters.amount" />
          </div>
        </div>
        <AppField label="Counterparty IBAN" v-model="filters.iban" placeholder="NL.. INHO .." />
        <!-- TODO: Apply filters and call GET /api/transactions with query params -->
        <button class="btn btn--primary" @click="applyFilters">Apply</button>
      </div>
      <div v-if="activeFilters.length" class="row" style="margin-top:14px;gap:6px;flex-wrap:wrap">
        <span class="t-body-sm" style="color:var(--ink-faint)">Active:</span>
        <span v-for="f in activeFilters" :key="f" class="badge badge--dark">
          {{ f }} <AppIcon name="x" :size="10" />
        </span>
      </div>
    </div>

    <!-- Table -->
    <!-- TODO: Fetch transactions from GET /api/transactions with pagination and filters -->
    <div class="card" style="padding:0">
      <table class="table">
        <thead>
          <tr>
            <th style="padding-left:20px">Date</th>
            <th>Description</th>
            <th>From</th>
            <th>To</th>
            <th style="text-align:right;padding-right:20px">Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in transactions" :key="tx.id">
            <td class="num" style="padding-left:20px">{{ tx.date }}</td>
            <td style="font-weight:500">{{ tx.description }}</td>
            <td class="iban">{{ tx.from }}</td>
            <td class="iban">{{ tx.to }}</td>
            <td
              class="num"
              style="text-align:right;padding-right:20px;font-weight:500;font-size:14px"
              :style="{ color: tx.amount.startsWith('+') ? 'var(--teal)' : 'var(--ink)' }"
            >{{ tx.amount }}</td>
          </tr>
        </tbody>
      </table>
      <div style="padding:0 16px 12px">
        <AppPager :current-page="1" :total="7" count="1–8 of 64" />
      </div>
    </div>
  </CustomerShell>
</template>

<script setup>
import { ref } from 'vue'
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppPager from '@/components/shared/AppPager.vue'

const filters = ref({ startDate: '', endDate: '', amountOp: '=', amount: '', iban: '' })
const activeFilters = ref([])

function clearFilters() { filters.value = { startDate: '', endDate: '', amountOp: '=', amount: '', iban: '' }; activeFilters.value = [] }
// TODO: Call GET /api/transactions with filters as query params
function applyFilters() { activeFilters.value = Object.entries(filters.value).filter(([k,v]) => v && k !== 'amountOp').map(([,v]) => v) }

// TODO: Fetch transactions from GET /api/transactions
const transactions = [
  { id: 1, date: '28 Apr · 14:22', description: 'Card · Albert Heijn',    from: 'NL42 INHO …89', to: 'NL91 ABNA …42', amount: '−€42,18'    },
  { id: 2, date: '28 Apr · 09:01', description: 'Transfer · M. Janssen',  from: 'NL42 INHO …89', to: 'NL11 RABO …07', amount: '−€250,00'   },
  { id: 3, date: '27 Apr · 18:30', description: 'Salary · ACME BV',       from: 'NL91 RABO …01', to: 'NL42 INHO …89', amount: '+€2 400,00' },
  { id: 4, date: '26 Apr · 12:10', description: 'Transfer to savings',     from: 'NL42 INHO …89', to: 'NL42 INHO …21', amount: '−€300,00'   },
  { id: 5, date: '25 Apr · 20:45', description: 'ATM withdrawal #14',      from: '—',              to: 'NL42 INHO …89', amount: '−€100,00'   },
  { id: 6, date: '24 Apr · 11:02', description: 'Spotify subscription',    from: 'NL42 INHO …89', to: 'NL11 INGB …55', amount: '−€9,99'     },
  { id: 7, date: '23 Apr · 16:40', description: 'Refund · Bol.com',        from: 'NL55 ABNA …88', to: 'NL42 INHO …89', amount: '+€18,40'    },
  { id: 8, date: '22 Apr · 08:15', description: 'Transfer from savings',   from: 'NL42 INHO …21', to: 'NL42 INHO …89', amount: '+€150,00'   },
]
</script>
