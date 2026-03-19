<template>
  <div class="app-shell">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <aside class="sidebar">
      <div class="brand-box">
        <p class="brand-eyebrow">智能图书管理系统</p>
        <h1>Library OS</h1>
        <p>{{ auth.isAdmin ? '管理员运营视角' : '用户任务视角' }}</p>
      </div>

      <nav class="side-nav" aria-label="主导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="side-link">
          <span aria-hidden="true">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <section class="side-panel">
  <p class="side-title">当前身份</p>
  <strong>{{ auth.username }}</strong>
  <p class="muted">
    {{ auth.isAdmin ? '管理员可处理图书、借阅与用户数据。' : '你可以找书、借书、查看推荐与维护个人资料。' }}
  </p>
  <button type="button" class="danger" style="margin-top: 12px; width: 100%;" @click="logout">
    退出登录
  </button>
</section>
    </aside>

    <div class="main-shell">
      <header class="topbar">
        <div>
          <p class="topbar-label">{{ auth.isAdmin ? '管理工作台' : '欢迎回来' }}</p>
          <p class="topbar-title">{{ auth.isAdmin ? '今天先处理待办，再查看运营概览。' : '先看借阅状态，再继续找书与查看推荐。' }}</p>
        </div>
        <div class="topbar-actions">
          <RouterLink class="top-link" to="/profile">个人中心</RouterLink>
          <button class="secondary avatar-button" type="button" aria-label="打开账户菜单" @click="toggleMenu">
            {{ initials }}
          </button>
        </div>
        <div v-if="showMenu" ref="menuWrap" class="menu-card" role="menu">
          <div class="menu-header">
            <strong>{{ auth.username }}</strong>
            <span>{{ auth.role }}</span>
          </div>
          <button type="button" class="menu-btn" @click="goProfile">进入个人中心</button>
          <button type="button" class="menu-btn danger-text" @click="logout">退出登录</button>
        </div>
      </header>

      <main id="main-content" class="content-shell">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const showMenu = ref(false)
const menuWrap = ref<HTMLElement | null>(null)

const navItems = computed(() => {
  const common = [
    { to: '/dashboard', label: '首页', icon: '🏠' },
    { to: '/books', label: auth.isAdmin ? '图书管理' : '图书检索', icon: '📚' },
    { to: '/borrow', label: auth.isAdmin ? '借阅管理' : '我的借阅', icon: '🔄' },
    { to: '/recommendations', label: auth.isAdmin ? '推荐统计' : '我的推荐', icon: '✨' },
    { to: '/profile', label: '个人中心', icon: '👤' }
  ]
  if (auth.isAdmin) {
    common.splice(3, 0,
      { to: '/categories', label: '分类管理', icon: '🗂️' },
      { to: '/users', label: '用户管理', icon: '👥' }
    )
  }
  return common
})

const initials = computed(() => (auth.username || 'U').slice(0, 1).toUpperCase())

function toggleMenu() {
  showMenu.value = !showMenu.value
}

function closeMenu(event: MouseEvent) {
  if (!showMenu.value) return
  if (!menuWrap.value?.contains(event.target as Node)) {
    showMenu.value = false
  }
}

function goProfile() {
  showMenu.value = false
  router.push('/profile')
}

function logout() {
  auth.logout()
  router.replace('/login')
}

onMounted(() => {
  window.addEventListener('click', closeMenu)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeMenu)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
}

.sidebar {
  position: sticky;
  top: 0;
  align-self: start;
  min-height: 100vh;
  padding: 24px 18px;
  border-right: 1px solid rgba(219, 229, 240, 0.8);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
}

.brand-box {
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(160deg, rgba(219, 234, 254, 0.9), rgba(255, 255, 255, 0.96));
  box-shadow: var(--shadow-sm);
}

.brand-box h1,
.brand-box p,
.brand-eyebrow,
.topbar-label,
.topbar-title {
  margin: 0;
}

.brand-eyebrow {
  color: var(--primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.brand-box h1 {
  margin-top: 10px;
  font-size: 28px;
}

.brand-box p:last-child {
  margin-top: 10px;
  color: var(--text-muted);
  line-height: 1.6;
}

.side-nav {
  display: grid;
  gap: 8px;
  margin-top: 20px;
}

.side-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  color: var(--text-soft);
  font-weight: 700;
}

.side-link:hover,
.side-link.router-link-active {
  background: rgba(219, 234, 254, 0.86);
  color: var(--primary-hover);
}

.side-panel {
  margin-top: 18px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid var(--border);
}

.side-title {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 13px;
}

.main-shell {
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 28px;
  border-bottom: 1px solid rgba(219, 229, 240, 0.7);
  background: rgba(243, 247, 251, 0.75);
  backdrop-filter: blur(14px);
}

.topbar-label {
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.topbar-title {
  margin-top: 4px;
  color: var(--text-soft);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-link {
  font-weight: 700;
}

.avatar-button {
  width: 42px;
  height: 42px;
  padding: 0;
  border-radius: 50%;
}

.menu-card {
  position: absolute;
  right: 28px;
  top: 74px;
  width: 220px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.menu-header {
  padding: 16px;
  background: #f8fafc;
  display: grid;
  gap: 4px;
}

.menu-header span {
  color: var(--text-muted);
  font-size: 13px;
}

.menu-btn {
  width: 100%;
  border-radius: 0;
  padding: 12px 16px;
  background: #fff;
  color: var(--text-soft);
  justify-content: flex-start;
}

.menu-btn:hover {
  background: #f8fafc;
}

.danger-text {
  color: var(--danger);
}

.content-shell {
  padding: 24px 28px 40px;
}

@media (max-width: 1080px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    min-height: auto;
    border-right: 0;
    border-bottom: 1px solid rgba(219, 229, 240, 0.8);
  }
}
</style>
