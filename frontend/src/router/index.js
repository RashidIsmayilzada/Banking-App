import { createRouter, createWebHistory } from 'vue-router'
import { getAtmToken } from '@/services/atm.js'

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
import AtmDepositView  from '@/views/atm/AtmDepositView.vue'
import AtmWithdrawView from '@/views/atm/AtmWithdrawView.vue'
import AtmConfirmView  from '@/views/atm/AtmConfirmView.vue'

// Employee
import OverviewView          from '@/views/employee/OverviewView.vue'
import UserDirectoryView     from '@/views/employee/UserDirectoryView.vue'
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
  { path: '/customer/dashboard',      component: DashboardView,     meta: { requiresAuth: true, role: 'CUSTOMER' } },
  { path: '/customer/accounts',       redirect: '/customer/dashboard' },
  { path: '/customer/accounts/:iban', component: AccountDetailView, meta: { requiresAuth: true, role: 'CUSTOMER' } },
  { path: '/customer/transfer',       component: TransferView,      meta: { requiresAuth: true, role: 'CUSTOMER' } },
  { path: '/customer/transactions',   component: TransactionsView,  meta: { requiresAuth: true, role: 'CUSTOMER' } },

  // ── ATM ────────────────────────────────────────────
  { path: '/atm/login',    component: AtmLoginView    },
  { path: '/atm/home',     component: AtmHomeView,     meta: { requiresAtmSession: true } },
  { path: '/atm/deposit',  component: AtmDepositView,  meta: { requiresAtmSession: true } },
  { path: '/atm/withdraw', component: AtmWithdrawView, meta: { requiresAtmSession: true } },
  { path: '/atm/confirm',  component: AtmConfirmView,  meta: { requiresAtmSession: true } },

  // ── Employee ───────────────────────────────────────
  { path: '/employee/overview',      component: OverviewView,         meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/users',         component: UserDirectoryView,    props: { title: 'All users' }, meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/employees',     component: UserDirectoryView,    props: { title: 'Employees', fixedRole: 'EMPLOYEE' }, meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/customers',     component: CustomersView,        meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/customers/:id', component: CustomerDetailView,   meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/approvals',     component: ApprovalsView,        meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/approvals/:id', component: ApproveFormView,      meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/transactions',  component: AllTransactionsView,  meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/transfer',      component: EmployeeTransferView, meta: { requiresAuth: true, role: 'EMPLOYEE' } },
  { path: '/employee/limits/:id?',   component: SetLimitsView,        meta: { requiresAuth: true, role: 'EMPLOYEE' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.requiresAtmSession) {
    if (!getAtmToken()) return '/atm/login'
  }

  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('auth_token')
    const user  = JSON.parse(localStorage.getItem('auth_user') || 'null')
    if (!token || !user) return '/login'
    if (to.meta.role && user.role !== to.meta.role) return '/login'
  }
})

export default router
