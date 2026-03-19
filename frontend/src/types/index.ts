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
  code?: string // 建议加上，方便和 BookItem 的 categoryCode 对应
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


export interface DashboardStatCardItem {
  label: string
  value: string
  hint: string
}

export interface DashboardNoticeSummaryItem {
  title: string
  content: string
  primaryCount: number
  secondaryCount: number
}

export interface BookDetailItem extends BookItem {
  summary: string
  availabilityText: string
  similarBooks: BookItem[]
}

export interface DashboardOverviewItem {
  admin: boolean
  statCards: DashboardStatCardItem[]
  noticeSummary: DashboardNoticeSummaryItem
  highlightBooks: BookItem[]
  recentBooks: BookItem[]
  recentBorrowRecords: BorrowItem[]
  recommendations: RecommendationItem[]
  recommendationStats?: RecommendationStatsItem
}
