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
import { getCurrentUser, homePathFor, isAuthenticated, restoreAuth } from '@/services/auth.js'

const routes = [
  { path: '/', redirect: '/login' },

  // ── Auth ───────────────────────────────────────────
  { path: '/login',    component: LoginView,    meta: { guestOnly: true } },
  { path: '/register', component: RegisterView, meta: { guestOnly: true } },
  { path: '/pending',  component: PendingView,  meta: { requiresAuth: true } },

  // ── Customer ───────────────────────────────────────
  { path: '/customer/dashboard',    component: DashboardView,     meta: { requiresAuth: true, role: 'CUSTOMER' } },
  { path: '/customer/accounts',     component: AccountDetailView, meta: { requiresAuth: true, role: 'CUSTOMER' } },
  { path: '/customer/transfer',     component: TransferView,      meta: { requiresAuth: true, role: 'CUSTOMER' } },
  { path: '/customer/transactions', component: TransactionsView,  meta: { requiresAuth: true, role: 'CUSTOMER' } },

  // ── ATM ────────────────────────────────────────────
  { path: '/atm/login',    component: AtmLoginView    },
  { path: '/atm/home',     component: AtmHomeView     },
  { path: '/atm/withdraw', component: AtmWithdrawView },
  { path: '/atm/confirm',  component: AtmConfirmView  },

  // ── Employee ───────────────────────────────────────
  { path: '/employee/overview',                 component: OverviewView,         meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/customers',                component: CustomersView,        meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/customers/:id',            component: CustomerDetailView,   meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/approvals',                component: ApprovalsView,        meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/approvals/:id',            component: ApproveFormView,      meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/transactions',             component: AllTransactionsView,  meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/transfer',                 component: EmployeeTransferView, meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/limits',                   component: SetLimitsView,        meta: { requiresAuth: true, role: 'EMPLOYEE' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  let user = getCurrentUser()

  if (isAuthenticated() && !user) {
    user = await restoreAuth()
  }

  if (to.meta.requiresAuth && !isAuthenticated()) {
    return '/login'
  }

  if (to.meta.guestOnly && isAuthenticated()) {
    return homePathFor(user)
  }

  if (to.meta.role && user?.role !== to.meta.role) {
    return homePathFor(user)
  }
})

export default router
