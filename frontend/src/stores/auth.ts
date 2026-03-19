import { defineStore } from 'pinia'

interface AuthState {
  token: string
  userId: number | null
  username: string
  role: 'USER' | 'ADMIN' | ''
}

const STORAGE_KEY = 'library-auth'

function loadState(): AuthState {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return { token: '', userId: null, username: '', role: '' }
  }
  try {
    return JSON.parse(raw) as AuthState
  } catch {
    return { token: '', userId: null, username: '', role: '' }
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => loadState(),
  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => state.role === 'ADMIN'
  },
  actions: {
    setAuth(payload: { token: string; userId: number; username: string; role: 'USER' | 'ADMIN' }) {
      this.token = payload.token
      this.userId = payload.userId
      this.username = payload.username
      this.role = payload.role
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.$state))
    },
    logout() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.role = ''
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
