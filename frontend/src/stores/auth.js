import { reactive, computed } from 'vue'

const state = reactive({
  token: localStorage.getItem('auth_token') || null,
  user: JSON.parse(localStorage.getItem('auth_user') || 'null'),
})

export function useAuth() {
  const isLoggedIn = computed(() => !!state.token)
  const role = computed(() => state.user?.role || null)
  const user = computed(() => state.user)
  const token = computed(() => state.token)

  function setSession(loginResponse) {
    state.token = loginResponse.accessToken
    state.user = loginResponse.user
    localStorage.setItem('auth_token', loginResponse.accessToken)
    localStorage.setItem('auth_user', JSON.stringify(loginResponse.user))
  }

  function clearSession() {
    state.token = null
    state.user = null
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
  }

  return { isLoggedIn, role, user, token, setSession, clearSession }
}
