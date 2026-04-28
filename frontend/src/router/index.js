import { createRouter, createWebHistory } from 'vue-router'

// Auth
import LoginView         from '@/views/auth/LoginView.vue'
import RegisterView      from '@/views/auth/RegisterView.vue'
import PendingView       from '@/views/auth/PendingView.vue'

// Customer
import DashboardView     from '@/views/customer/DashboardView.vue'
import AccountDetailView from '@/views/customer/AccountDetailView.vue'
import TransactionsView  from '@/views/customer/TransactionsView.vue'
import TransferView      from '@/views/customer/TransferView.vue'

// ATM
import AtmLoginView    from '@/views/atm/AtmLoginView.vue'
import AtmHomeView     from '@/views/atm/AtmHomeView.vue'
import AtmWithdrawView from '@/views/atm/AtmWithdrawView.vue'
import AtmConfirmView  from '@/views/atm/AtmConfirmView.vue'

// Employee
import OverviewView          from '@/views/employee/OverviewView.vue'
import CustomersView         from '@/views/employee/CustomersView.vue'
import CustomerDetailView    from '@/views/employee/CustomerDetailView.vue'
import ApprovalsView         from '@/views/employee/ApprovalsView.vue'
import ApproveFormView       from '@/views/employee/ApproveFormView.vue'
import AllTransactionsView   from '@/views/employee/AllTransactionsView.vue'
import EmployeeTransferView  from '@/views/employee/EmployeeTransferView.vue'
import SetLimitsView         from '@/views/employee/SetLimitsView.vue'

const routes = [
  { path: '/', redirect: '/login' },

  // ── Auth ───────────────────────────────────────────
  { path: '/login',    component: LoginView    },
  { path: '/register', component: RegisterView },
  { path: '/pending',  component: PendingView  },

  // ── Customer ───────────────────────────────────────
  { path: '/customer/dashboard',    component: DashboardView     },
  { path: '/customer/accounts',     component: AccountDetailView },
  { path: '/customer/transfer',     component: TransferView      },
  { path: '/customer/transactions', component: TransactionsView  },

  // ── ATM ────────────────────────────────────────────
  { path: '/atm/login',    component: AtmLoginView    },
  { path: '/atm/home',     component: AtmHomeView     },
  { path: '/atm/withdraw', component: AtmWithdrawView },
  { path: '/atm/confirm',  component: AtmConfirmView  },

  // ── Employee ───────────────────────────────────────
  { path: '/employee/overview',                 component: OverviewView         },
  { path: '/employee/customers',                component: CustomersView        },
  { path: '/employee/customers/:id',            component: CustomerDetailView   },
  { path: '/employee/approvals',                component: ApprovalsView        },
  { path: '/employee/approvals/:id',            component: ApproveFormView      },
  { path: '/employee/transactions',             component: AllTransactionsView  },
  { path: '/employee/transfer',                 component: EmployeeTransferView },
  { path: '/employee/limits',                   component: SetLimitsView        },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
