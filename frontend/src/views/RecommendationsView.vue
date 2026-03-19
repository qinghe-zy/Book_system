<template>
  <div class="app-page">
    <PageHeader
      :eyebrow="auth.isAdmin ? '推荐统计' : '我的推荐'"
      :title="auth.isAdmin ? '用关键指标观察推荐覆盖与命中情况' : '查看系统为什么推荐这些图书给你，并直接完成后续借阅'"
      :description="auth.isAdmin ? '这里保留必要统计，不做过度复杂的运营大屏。' : '推荐结果会结合借阅历史、兴趣标签、热门图书与可借库存进行展示。'"
    >
      <template #actions>
        <button v-if="!auth.isAdmin" type="button" @click="refreshMine">换一批推荐</button>
        <button v-else type="button" class="secondary" @click="loadStats">刷新统计</button>
      </template>
    </PageHeader>

    <section v-if="!auth.isAdmin" class="filter-card">
      <div class="toolbar">
        <div class="field">
          <label for="topN">推荐数量</label>
          <input id="topN" v-model.number="topN" name="topN" type="number" min="1" max="20" inputmode="numeric" placeholder="例如 8…" />
        </div>
        <div class="toolbar">
          <button type="button" class="secondary" @click="load">重新加载</button>
          <button type="button" @click="refreshMine">刷新推荐</button>
        </div>
      </div>
    </section>

    <section v-if="!auth.isAdmin" class="page-card">
      <div v-if="list.length" class="book-grid">
        <BookCard v-for="r in list" :key="`${r.userId}-${r.bookId}-${r.generatedTime}`" :book="toBook(r)" :reason="r.reason || defaultReason">
          <template #badge>
            <StatusBadge label="推荐中" tone="info" />
          </template>
          <template #actions>
            <RouterLink :to="`/books?bookId=${r.bookId}`"><button type="button" class="secondary">查看详情</button></RouterLink>
            <button type="button" @click="goTo('/borrow')">立即借阅</button>
          </template>
        </BookCard>
      </div>
      <EmptyState v-else title="暂时还没有推荐结果" description="你可以先去借阅几本图书，或补充兴趣标签，系统会逐步生成更合适的推荐。" icon="✨">
        <template #action>
          <button type="button" @click="goTo('/books')">去找书</button>
        </template>
      </EmptyState>
    </section>

    <template v-else>
      <div class="stat-grid">
        <article class="stat-card">
          <div class="label">推荐总数</div>
          <div class="value">{{ stats?.totalRecommendations ?? 0 }}</div>
          <div class="hint">系统累计输出的推荐记录</div>
        </article>
        <article class="stat-card">
          <div class="label">命中数</div>
          <div class="value">{{ stats?.hitRecommendations ?? 0 }}</div>
          <div class="hint">推荐后被借阅的图书数量</div>
        </article>
        <article class="stat-card">
          <div class="label">命中率</div>
          <div class="value">{{ formatPercent(stats?.overallHitRate) }}</div>
          <div class="hint">推荐转化效果摘要</div>
        </article>
        <article class="stat-card">
          <div class="label">覆盖用户</div>
          <div class="value">{{ stats?.coveredUsers ?? 0 }}</div>
          <div class="hint">获得推荐的用户数量</div>
        </article>
      </div>

      <section class="page-card">
        <h2 style="margin-top: 0;">推荐概况说明</h2>
        <p class="book-reason" style="margin-top: 0;">推荐模块采用“兴趣标签 + 借阅历史 + 热门馆藏 + 新书补位 + 可借库存”的渐进式策略，先保证推荐可用，再逐步优化排序质量与命中率。</p>
      </section>

      <section class="table-card">
        <div class="table-toolbar">
          <div>
            <div class="table-title">Top 图书推荐效果</div>
            <div class="table-helper">观察哪些图书被推荐得更多、命中更好，用于后续优化推荐策略与馆藏曝光。</div>
          </div>
        </div>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>书籍 ID</th>
                <th>书名</th>
                <th>被推荐次数</th>
                <th>命中次数</th>
                <th>命中率</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="b in stats?.topBooks || []" :key="b.bookId">
                <td>{{ b.bookId }}</td>
                <td>{{ b.title }}</td>
                <td>{{ b.recommendedCount }}</td>
                <td>{{ b.hitCount }}</td>
                <td>{{ formatPercent(b.hitRate) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <EmptyState v-if="!(stats?.topBooks?.length)" title="暂无推荐统计明细" description="当推荐记录逐步积累后，这里会展示更稳定的命中表现。" icon="📊" />
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import BookCard from '@/components/BookCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, BookItem, RecommendationItem, RecommendationStatsItem } from '@/types'
import { formatPercent } from '@/composables/useFormat'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const list = ref<RecommendationItem[]>([])
const stats = ref<RecommendationStatsItem>()
const books = ref<BookItem[]>([])
const topN = ref(Number(route.query.topN || 8))
const defaultReason = '结合你的借阅历史、兴趣标签与当前馆藏热度生成。'

watch(topN, (value) => {
  router.replace({ query: { ...route.query, topN: String(value || 8) } })
})

function toBook(rec: RecommendationItem): BookItem {
  const match = books.value.find((item) => item.id === rec.bookId)
  return match || {
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

async function loadBooks() {
  const { data } = await http.get<ApiResponse<BookItem[]>>('/api/books')
  books.value = data.data
}

async function load() {
  const { data } = await http.get<ApiResponse<RecommendationItem[]>>('/api/recommendations/my', { params: { topN: topN.value || 8 } })
  list.value = data.data
}

async function refreshMine() {
  await http.post('/api/recommendations/refresh/me', null, { params: { topN: topN.value || 8 } })
  await load()
}

async function loadStats() {
  if (!auth.isAdmin) return
  const { data } = await http.get<ApiResponse<RecommendationStatsItem>>('/api/recommendations/admin/stats')
  stats.value = data.data
}

function goTo(path: string) {
  router.push(path)
}

onMounted(async () => {
  await loadBooks()
  if (auth.isAdmin) {
    await loadStats()
  } else {
    await load()
  }
})
</script>
