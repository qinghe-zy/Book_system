<template>
  <div class="app-page">
    <PageHeader
      :eyebrow="auth.isAdmin ? '管理首页' : '用户首页'"
      :title="auth.isAdmin ? '今天先处理待办，再掌握整体运行状态' : '先看借阅状态，再继续发现适合你的图书'"
      :description="auth.isAdmin ? '这里聚合了借阅待办、库存提醒、推荐效果与新增图书，方便你快速进入管理动作。' : '这里聚合了当前借阅、即将到期提醒、热门图书与推荐图书，让你更快完成找书与借阅。'"
    />

    <p v-if="errorMessage" class="msg error" aria-live="polite">{{ errorMessage }}</p>

    <div class="stat-grid">
      <article v-for="card in overview?.statCards || []" :key="card.label" class="stat-card">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}</div>
        <div class="hint">{{ card.hint }}</div>
      </article>
    </div>

    <div class="grid-2">
      <section class="notice-card">
        <h3>{{ overview?.noticeSummary?.title || (auth.isAdmin ? '当前待办' : '当前提醒') }}</h3>
        <p>{{ overview?.noticeSummary?.content || fallbackNotice }}</p>
        <div class="toolbar" style="margin-top: 16px;">
          <button type="button" @click="goTo('/borrow')">{{ auth.isAdmin ? '进入借阅管理' : '查看我的借阅' }}</button>
          <button type="button" class="secondary" @click="goTo('/books')">{{ auth.isAdmin ? '进入图书管理' : '继续找书' }}</button>
        </div>
      </section>

      <section class="page-card">
        <h2 style="margin-top: 0;">{{ auth.isAdmin ? '最近新增图书' : '最近借阅记录' }}</h2>
        <template v-if="summaryItems.length">
          <div class="detail-list">
            <div v-for="item in summaryItems" :key="item.id">
              <dt>{{ item.title }}</dt>
              <dd>{{ item.subtitle }}</dd>
            </div>
          </div>
        </template>
        <EmptyState v-else title="暂无数据" description="这里会展示最近发生的业务记录，方便你快速回顾系统状态。" icon="🗃️" />
      </section>
    </div>

    <div class="grid-2">
      <section class="page-card">
        <h2 style="margin-top: 0;">{{ auth.isAdmin ? '库存紧张图书' : '热门与新上架图书' }}</h2>
        <div v-if="displayBooks.length" class="book-grid">
          <BookCard v-for="book in displayBooks" :key="book.id" :book="book">
            <template #badge>
              <StatusBadge :label="book.stock > 0 ? '可借' : '库存不足'" :tone="book.stock > 0 ? 'success' : 'danger'" />
            </template>
            <template #actions>
              <RouterLink :to="`/books?bookId=${book.id}`" class="button-link secondary-link">查看图书</RouterLink>
            </template>
          </BookCard>
        </div>
        <EmptyState v-else title="暂无图书数据" description="等图书数据加载后，这里会展示热门图书、新书或库存提醒。" icon="📘" />
      </section>

      <section class="page-card">
        <h2 style="margin-top: 0;">{{ auth.isAdmin ? '推荐效果摘要' : '为你推荐' }}</h2>
        <template v-if="auth.isAdmin">
          <div class="detail-list" v-if="overview?.recommendationStats">
            <div><dt>推荐总数</dt><dd>{{ overview.recommendationStats.totalRecommendations }}</dd></div>
            <div><dt>命中数</dt><dd>{{ overview.recommendationStats.hitRecommendations }}</dd></div>
            <div><dt>命中率</dt><dd>{{ formatPercent(overview.recommendationStats.overallHitRate) }}</dd></div>
            <div><dt>覆盖用户</dt><dd>{{ overview.recommendationStats.coveredUsers }}</dd></div>
          </div>
          <EmptyState v-else title="暂无统计数据" description="推荐统计会在生成推荐后展示整体效果。" icon="📈" />
        </template>
        <template v-else>
          <div v-if="overview?.recommendations?.length" class="book-grid">
            <BookCard
              v-for="rec in overview.recommendations.slice(0, 2)"
              :key="`${rec.userId}-${rec.bookId}`"
              :book="toBook(rec)"
              :reason="rec.reason || '结合你的借阅历史与热门馆藏生成。'"
            >
              <template #badge>
                <StatusBadge label="推荐中" tone="info" />
              </template>
              <template #actions>
                <RouterLink :to="`/books?bookId=${rec.bookId}`" class="button-link secondary-link">查看详情</RouterLink>
                <button type="button" @click="goTo('/recommendations')">更多推荐</button>
              </template>
            </BookCard>
          </div>
          <EmptyState v-else title="暂时还没有推荐" description="系统会根据你的借阅与兴趣标签逐步生成更合适的图书推荐。" icon="✨" />
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import BookCard from '@/components/BookCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, BookItem, BorrowItem, DashboardOverviewItem, RecommendationItem } from '@/types'
import { formatDate, formatPercent } from '@/composables/useFormat'

const auth = useAuthStore()
const router = useRouter()
const overview = ref<DashboardOverviewItem>()
const errorMessage = ref('')

const fallbackNotice = computed(() => auth.isAdmin
  ? '这里会聚合借阅待办、库存提醒与推荐效果，方便你快速进入关键动作。'
  : '这里会聚合你的借阅状态、即将到期提醒与推荐图书，让你更快完成找书与借阅。')

const displayBooks = computed(() => overview.value?.highlightBooks || [])

const summaryItems = computed(() => {
  if (!overview.value) return []
  if (auth.isAdmin) {
    return (overview.value.recentBooks || []).map((item) => ({
      id: item.id,
      title: item.title,
      subtitle: formatDate(item.createdTime)
    }))
  }
  return (overview.value.recentBorrowRecords || []).map((item: BorrowItem) => ({
    id: item.id,
    title: item.bookTitle,
    subtitle: statusText(item.status)
  }))
})

function statusText(status: BorrowItem['status']) {
  return {
    APPLIED: '待审核',
    APPROVED: '待取书',
    BORROWED: '借阅中',
    RETURNED: '已归还',
    REJECTED: '已驳回',
    CANCELED: '已撤销',
    OVERDUE: '已逾期'
  }[status] || status
}

function toBook(rec: RecommendationItem): BookItem {
  const book = overview.value?.highlightBooks?.find((item) => item.id === rec.bookId)
  return book || {
    id: rec.bookId,
    title: rec.bookTitle,
    author: '馆藏图书',
    categoryCode: '',
    stock: 0,
    location: '',
    imageUrl: rec.imageUrl,
    createdTime: rec.generatedTime
  }
}

async function load() {
  try {
    const { data } = await http.get<ApiResponse<DashboardOverviewItem>>('/api/dashboard/overview')
    overview.value = data.data
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '首页数据加载失败，请稍后再试。'
  }
}

function goTo(path: string) {
  router.push(path)
}

onMounted(load)
</script>
