import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/LoginView.vue') },
    { path: '/register', component: () => import('@/views/RegisterView.vue') },
    {
      path: '/',
      component: () => import('@/views/DashboardLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: () => import('@/views/HomeView.vue') },
        { path: 'books', component: () => import('@/views/BooksView.vue') },
        { path: 'borrow', component: () => import('@/views/BorrowView.vue') },
        { path: 'recommendations', component: () => import('@/views/RecommendationsView.vue') },
        { path: 'profile', component: () => import('@/views/ProfileView.vue') },
        { path: 'categories', component: () => import('@/views/CategoriesView.vue'), meta: { admin: true } },
        { path: 'users', component: () => import('@/views/UsersView.vue'), meta: { admin: true } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const isPublic = ['/login', '/register'].includes(to.path)

  if (!isPublic && !auth.isLogin) {
    return '/login'
  }

  if (to.path === '/login' && auth.isLogin) {
    return '/dashboard'
  }

  if (to.meta.admin && !auth.isAdmin) {
    return '/dashboard'
  }

  return true
})

export default router
