import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const http = axios.create({
  // 关键修改 1：移除具体的 IP 地址，改为空或只保留 /api
  // 这样请求会通过 Vite 的 proxy 转发，解决跨域和 403 握手问题
  baseURL: '', 
  timeout: 10000
})

// 请求拦截器：在发请求前自动塞入 Token
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    // 确保这里的格式与后端 Spring Security 要求的一致 (Bearer )
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 响应拦截器：统一处理错误
http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const auth = useAuthStore()
    
    // 关键修改 2：同时拦截 401 和 403
    // 401: 未授权（没传Token）
    // 403: 被拒绝（Token过期或无效）
    if (error?.response?.status === 401 || error?.response?.status === 403) {
      console.error('身份验证失效，正在跳转登录页...')
      auth.logout() // 清除 Pinia 里的用户信息
      
      // 只有当前不在登录页时才跳转，防止死循环
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default http

/** --- 以下是你的类型定义，保持不变 --- **/

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface AuthPayload {
  token: string
  userId: number
  username: string
  role: 'USER' | 'ADMIN'
}

export interface UserItem {
  id: number
  username: string
  email: string
  interestTags?: string
  role: 'USER' | 'ADMIN'
  registerTime: string
}

export interface CategoryItem {
  id: number
  name: string
}

export interface BookItem {
  id: number
  title: string
  author: string
  categoryCode: string
  stock: number
  location: string
  imageUrl?: string
  categoryId?: number
  categoryName?: string
  createdTime: string
}

export interface BorrowItem {
  id: number
  userId: number
  username: string
  bookId: number
  bookTitle: string
  borrowTime?: string
  returnTime?: string
  dueTime?: string
  approvedTime?: string
  status: 'APPLIED' | 'APPROVED' | 'BORROWED' | 'RETURNED' | 'REJECTED' | 'CANCELED' | 'OVERDUE'
  borrowType: 'ONLINE' | 'OFFLINE'
  renewCount: number
  pickupCode?: string
  reviewComment?: string
}

export interface RecommendationItem {
  userId: number
  bookId: number
  bookTitle: string
  imageUrl?: string
  reason: string
  modelScore?: number
  finalScore?: number
  generatedTime: string
}

export interface RecommendationBookStatItem {
  bookId: number
  title: string
  recommendedCount: number
  hitCount: number
  hitRate: number
}

export interface RecommendationStatsItem {
  totalRecommendations: number
  hitRecommendations: number
  overallHitRate: number
  coveredUsers: number
  topBooks: RecommendationBookStatItem[]
}