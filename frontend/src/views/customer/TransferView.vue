<template>
  <CustomerShell>
    <h1 class="t-h1" style="margin:0 0 8px">Send money</h1>
    <p class="t-body muted" style="margin:0 0 24px">Move money between your accounts or send to another customer.</p>

    <div class="tabs" style="margin-bottom:24px">
      <div
        v-for="tab in tabs"
        :key="tab"
        :class="['tab', activeTab === tab ? 'tab--active' : '']"
        @click="activeTab = tab"
      >{{ tab }}</div>
    </div>

    <div style="display:grid;grid-template-columns:1.4fr 1fr;gap:20px;align-items:flex-start">
      <!-- Main form -->
      <div class="card" style="padding:28px">
        <h3 class="t-h3" style="margin:0 0 20px">Move funds</h3>

        <!-- From account -->
        <!-- TODO: Fetch accounts from GET /api/accounts -->
        <div class="field" style="margin-bottom:16px">
          <label class="field__label">From</label>
          <div class="input row" style="justify-content:space-between;cursor:pointer">
            <div class="row">
              <div class="icon-box icon-box--ink" style="width:32px;height:32px;border-radius:8px"><AppIcon name="wallet" :size="14" /></div>
              <div>
                <div style="font-weight:500;font-size:14px">Checking</div>
                <div class="iban">NL42 INHO …89</div>
              </div>
            </div>
            <div class="row" style="gap:8px">
              <span style="font-weight:500">€6 218,40</span>
              <AppIcon name="chevronDown" :size="14" />
            </div>
          </div>
        </div>

        <div style="display:flex;justify-content:center;margin:-4px 0">
          <div style="width:36px;height:36px;border-radius:50%;background:var(--surface);border:1.5px solid var(--line);display:grid;place-items:center;z-index:1;position:relative">
            <AppIcon name="arrowDown" :size="14" />
          </div>
        </div>

        <!-- To account -->
        <div class="field" style="margin-bottom:24px">
          <label class="field__label">To</label>
          <div class="input row" style="justify-content:space-between;cursor:pointer">
            <div class="row">
              <div class="icon-box icon-box--teal" style="width:32px;height:32px;border-radius:8px"><AppIcon name="pieChart" :size="14" /></div>
              <div>
                <div style="font-weight:500;font-size:14px">Savings</div>
                <div class="iban">NL42 INHO …21</div>
              </div>
            </div>
            <div class="row" style="gap:8px">
              <span style="font-weight:500">€2 203,15</span>
              <AppIcon name="chevronDown" :size="14" />
            </div>
          </div>
        </div>

        <!-- Amount -->
        <div class="field" style="margin-bottom:16px">
          <label class="field__label">Amount</label>
          <input class="input input--xl" v-model="form.amount" placeholder="€ 0,00" />
          <div class="row" style="gap:8px;margin-top:8px">
            <button
              v-for="preset in amountPresets"
              :key="preset"
              :class="['amount-chip', form.amount === preset ? 'amount-chip--active' : '']"
              style="font-size:13px;padding:8px 14px"
              @click="form.amount = preset"
            >{{ preset }}</button>
          </div>
        </div>

        <AppField label="Description (optional)" v-model="form.description" placeholder="e.g. Save for holiday" />

        <!-- Limit info banner -->
        <div class="banner banner--info" style="margin-top:20px">
          <AppIcon name="info" :size="16" class="banner__icon" />
          <div>This transfer will use <strong>{{ form.amount || '€0' }}</strong> of your daily <strong>€2 500,00</strong> limit.</div>
        </div>

        <div class="row" style="margin-top:24px">
          <button class="btn btn--ghost" @click="$router.back()">Cancel</button>
          <span class="spacer" />
          <!-- TODO: POST /api/transfers with form data -->
          <button class="btn btn--primary btn--lg">Review &amp; confirm <AppIcon name="arrowRight" :size="16" /></button>
        </div>
      </div>

      <!-- Right: customer lookup -->
      <div class="col" style="gap:16px">
        <div class="card">
          <h3 class="t-h3" style="margin:0 0 6px">To another customer</h3>
          <p class="t-body-sm" style="margin:0 0 16px">Look up by name to find their IBAN.</p>
          <div class="col" style="gap:12px">
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
              <AppField label="First name" v-model="lookup.firstName" />
              <AppField label="Last name"  v-model="lookup.lastName" />
            </div>
            <!-- TODO: GET /api/customers/search?firstName=…&lastName=… -->
            <button class="btn btn--secondary btn--sm" style="align-self:flex-start" @click="searchCustomer">
              <AppIcon name="search" :size="14" /> Search
            </button>
            <template v-if="lookupResult">
              <hr class="divider" />
              <div class="t-body-sm">Match found:</div>
              <div class="card card--soft" style="padding:12px">
                <div class="row">
                  <AppAvatar :name="lookupResult.name" />
                  <div>
                    <div style="font-weight:500;font-size:14px">{{ lookupResult.name }}</div>
                    <div class="iban">{{ lookupResult.iban }}</div>
                  </div>
                  <span class="spacer" />
                  <AppIcon name="check" :size="16" style="color:var(--teal)" />
                </div>
              </div>
              <AppField label="Amount" v-model="lookup.amount" />
              <div v-if="limitExceeded" class="banner banner--danger">
                <AppIcon name="alert" :size="16" class="banner__icon" />
                <div>Exceeds absolute limit by <strong>€40,00</strong>. Reduce amount.</div>
              </div>
              <!-- TODO: POST /api/transfers with lookup data -->
              <button class="btn btn--primary btn--block" :disabled="limitExceeded">Send transfer</button>
            </template>
          </div>
        </div>

        <!-- Recent recipients -->
        <!-- TODO: Fetch recent recipients from GET /api/transfers/recipients -->
        <div class="card card--soft">
          <h4 class="t-h4" style="margin:0 0 10px">Recent recipients</h4>
          <div
            v-for="r in recentRecipients"
            :key="r.name"
            class="row"
            style="padding:8px 0;border-bottom:1px dashed var(--line)"
          >
            <AppAvatar :name="r.name" />
            <div>
              <div style="font-weight:500;font-size:13px">{{ r.name }}</div>
              <div class="iban">{{ r.iban }}</div>
            </div>
            <span class="spacer" />
            <button class="btn btn--ghost btn--xs"><AppIcon name="arrowRight" :size="12" /></button>
          </div>
        </div>
      </div>
    </div>
  </CustomerShell>
</template>

<script setup>
import { ref } from 'vue'
import CustomerShell from '@/components/layout/CustomerShell.vue'
import AppIcon from '@/components/shared/AppIcon.vue'
import AppField from '@/components/shared/AppField.vue'
import AppAvatar from '@/components/shared/AppAvatar.vue'

const tabs = ['Between my accounts', 'To another customer', 'External (SEPA)']
const activeTab = ref('Between my accounts')

const form = ref({ amount: '€ 300,00', description: '' })
const amountPresets = ['€50', '€100', '€250', '€500', '€1 000']

const lookup = ref({ firstName: '', lastName: '', amount: '' })
const lookupResult = ref(null)
const limitExceeded = ref(false)

// TODO: GET /api/customers/search?firstName=…&lastName=…
function searchCustomer() {
  lookupResult.value = { name: 'Maarten Janssen', iban: 'NL11 INHO 0034 9921 07' }
}

// TODO: Fetch recent recipients from GET /api/transfers/recipients
const recentRecipients = [
  { name: 'Maarten Janssen', iban: 'NL11 INHO …07' },
  { name: 'Sara El-Amin',    iban: 'NL55 INHO …14' },
  { name: 'Pieter de Vries', iban: 'NL77 INHO …40' },
]
</script>
