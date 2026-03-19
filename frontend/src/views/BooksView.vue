<template>
  <div class="app-page">
    <PageHeader
      :eyebrow="auth.isAdmin ? '图书管理' : '图书检索'"
      :title="auth.isAdmin ? '集中处理图书资料、库存、位置与封面信息' : '从书名、作者、分类号中快速找到适合你的图书'"
      :description="auth.isAdmin ? '管理员视角保留表格与批量操作，便于高效维护馆藏。' : '用户视角突出图书信息表达、可借状态与详情查看，减少纯表格感。'"
    >
      <template #actions>
        <button v-if="auth.isAdmin" type="button" @click="openCreateModal">新增图书</button>
      </template>
    </PageHeader>

    <p v-if="errorMessage" class="msg error" aria-live="polite">{{ errorMessage }}</p>
    <p v-if="successMessage" class="msg success" aria-live="polite">{{ successMessage }}</p>

    <section class="filter-card">
      <div class="toolbar">
        <div class="field">
          <label for="book-keyword">关键词</label>
          <input
            id="book-keyword"
            v-model="keyword"
            name="keyword"
            autocomplete="off"
            placeholder="按书名或作者检索…"
            @keyup.enter="load"
          />
        </div>
        <div class="field">
          <label for="book-category">分类号</label>
          <input
            id="book-category"
            v-model="categoryCode"
            name="categoryCode"
            autocomplete="off"
            placeholder="例如 TP312…"
            @keyup.enter="load"
          />
        </div>
        <div class="field" v-if="auth.isAdmin">
          <label for="book-stock-filter">库存状态</label>
          <select id="book-stock-filter" v-model="stockFilter" name="stockFilter" autocomplete="off">
            <option value="all">全部库存</option>
            <option value="available">仅可借</option>
            <option value="low">库存紧张</option>
          </select>
        </div>
        <div class="toolbar">
          <button type="button" @click="load">立即搜索</button>
          <button type="button" class="secondary" @click="resetFilters">重置筛选</button>
        </div>
      </div>
    </section>

    <section v-if="auth.isAdmin && filteredBooks.length" class="page-card">
      <h2 style="margin-top: 0;">批量操作</h2>
      <div class="toolbar">
        <div class="field">
          <label for="batch-location">新位置</label>
          <input
            id="batch-location"
            v-model="batchLocation"
            name="batchLocation"
            autocomplete="off"
            placeholder="例如 A-01-03…"
          />
        </div>
        <div class="toolbar">
          <button type="button" class="secondary" @click="batchUpdateLocation">批量改位置</button>
          <button type="button" class="danger" @click="batchDelete">批量删除</button>
        </div>
      </div>
      <p class="muted">已选中 {{ selectedIds.length }} 本图书。批量操作仅对当前勾选记录生效。</p>
    </section>

    <section v-if="!auth.isAdmin" class="page-card">
      <div class="table-toolbar" style="padding: 0 0 16px; border-bottom: 0;">
        <div>
          <div class="table-title">检索结果</div>
          <div class="table-helper">
            共 {{ filteredBooks.length }} 本图书，点击“查看详情”可了解位置、库存与相似图书。
          </div>
        </div>
      </div>

      <div v-if="filteredBooks.length" class="book-grid">
        <BookCard v-for="item in filteredBooks" :key="item.id" :book="item">
          <template #badge>
            <StatusBadge
              :label="item.stock > 0 ? '可借' : '库存不足'"
              :tone="item.stock > 0 ? 'success' : 'danger'"
            />
          </template>
          <template #actions>
            <RouterLink :to="`/books?bookId=${item.id}`" class="button-link secondary-link">
              查看详情
            </RouterLink>
            <button type="button" :disabled="item.stock <= 0" @click="goBorrow">立即借阅</button>
          </template>
        </BookCard>
      </div>

      <EmptyState
        v-else
        title="没有找到匹配图书"
        description="请换一个关键词、作者名或分类号后再试，系统会继续保留你的检索入口。"
        icon="🔎"
      />
    </section>

    <section v-else class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="table-title">图书列表</div>
          <div class="table-helper">
            共 {{ filteredBooks.length }} 本图书。管理员可在此维护图书资料、上传封面与批量处理位置。
          </div>
        </div>
      </div>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>选择</th>
              <th>封面</th>
              <th>ID</th>
              <th>书名</th>
              <th>作者</th>
              <th>分类号</th>
              <th>分类</th>
              <th>库存</th>
              <th>位置</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredBooks" :key="item.id">
              <td>
                <input
                  v-model="selectedIds"
                  :value="item.id"
                  type="checkbox"
                  :aria-label="`选择图书${item.title}`"
                />
              </td>
              <td>
                <img
                  v-if="item.imageUrl"
                  :src="item.imageUrl"
                  :alt="`${item.title}封面`"
                  class="cover"
                  width="48"
                  height="64"
                  loading="lazy"
                />
                <span v-else class="muted">暂无封面</span>
              </td>
              <td>{{ item.id }}</td>
              <td>{{ item.title }}</td>
              <td>{{ item.author }}</td>
              <td>{{ item.categoryCode }}</td>
              <td>{{ item.categoryName || '—' }}</td>
              <td>
                <StatusBadge
                  :label="item.stock > 0 ? `${item.stock} 本` : '无库存'"
                  :tone="item.stock > 0 ? (item.stock <= 2 ? 'warning' : 'success') : 'danger'"
                />
              </td>
              <td>{{ item.location }}</td>
              <td>
                <div class="toolbar" style="gap: 8px;">
                  <RouterLink :to="`/books?bookId=${item.id}`" class="button-link secondary-link">
                    详情
                  </RouterLink>
                  <button type="button" class="secondary" @click="edit(item)">编辑</button>
                  <button type="button" class="danger" @click="remove(item.id)">删除</button>
                </div>

                <div class="toolbar" style="margin-top: 8px; gap: 8px;">
                  <input
                    :id="`cover-${item.id}`"
                    type="file"
                    accept=".jpg,.jpeg,.png,image/png,image/jpeg"
                    :aria-label="`为${item.title}选择封面文件`"
                    style="max-width: 220px;"
                    @change="pickCover(item.id, $event)"
                  />
                  <button type="button" class="secondary" @click="uploadCover(item.id)">上传封面</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <EmptyState
        v-if="!filteredBooks.length"
        title="暂无图书数据"
        description="请先新增图书，或调整检索条件重新查看。"
        icon="📚"
      />
    </section>

    <teleport to="body">
      <div v-if="showDetail && detailBook" class="modal-mask">
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="book-detail-title">
          <div class="grid-2">
            <div>
              <img
                v-if="detailBook.imageUrl"
                :src="detailBook.imageUrl"
                :alt="`${detailBook.title}封面`"
                class="detail-cover"
                width="240"
                height="320"
              />
              <div v-else class="detail-cover placeholder" aria-hidden="true">暂无封面</div>
            </div>

            <div>
              <h2 id="book-detail-title" style="margin-top: 0;">{{ detailBook.title }}</h2>
              <p class="muted" style="margin-top: 8px;">{{ detailBook.author }}</p>

              <div class="detail-list" style="margin-top: 18px;">
                <div>
                  <dt>分类号</dt>
                  <dd>{{ detailBook.categoryCode || '—' }}</dd>
                </div>
                <div>
                  <dt>分类名称</dt>
                  <dd>{{ detailBook.categoryName || '未分类' }}</dd>
                </div>
                <div>
                  <dt>馆藏位置</dt>
                  <dd>{{ detailBook.location || '—' }}</dd>
                </div>
                <div>
                  <dt>库存状态</dt>
                  <dd>{{ detailBook.availabilityText || detailReason }}</dd>
                </div>
              </div>

              <p class="book-reason" style="margin-top: 16px;">
                {{ detailBook.summary || detailReason }}
              </p>

              <div v-if="detailBook.similarBooks?.length" style="margin-top: 18px;">
                <h3 style="margin: 0 0 12px;">相似图书</h3>
                <div class="toolbar">
                  <button
                    v-for="similar in detailBook.similarBooks"
                    :key="similar.id"
                    type="button"
                    class="secondary"
                    @click="openDetail(similar.id)"
                  >
                    {{ similar.title }}
                  </button>
                </div>
              </div>

              <div class="modal-actions" style="margin-top: 20px; justify-content: flex-start;">
                <button type="button" class="secondary" @click="closeDetail">关闭详情</button>
                <button type="button" :disabled="detailBook.stock <= 0" @click="goBorrow">
                  去借阅中心
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="auth.isAdmin && showModal" class="modal-mask">
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="book-modal-title">
          <h2 id="book-modal-title" style="margin-top: 0;">{{ editId ? '编辑图书' : '新增图书' }}</h2>

          <form class="modal-form" @submit.prevent="saveBook">
            <div class="form-grid">
              <div class="form-field">
                <label for="book-title">书名</label>
                <input
                  id="book-title"
                  v-model="form.title"
                  name="title"
                  autocomplete="off"
                  placeholder="请输入书名…"
                  required
                />
              </div>

              <div class="form-field">
                <label for="book-author">作者</label>
                <input
                  id="book-author"
                  v-model="form.author"
                  name="author"
                  autocomplete="off"
                  placeholder="请输入作者名…"
                  required
                />
              </div>

              <div class="form-field">
                <label for="book-category-code">分类号</label>
                <input
                  id="book-category-code"
                  v-model="form.categoryCode"
                  name="categoryCode"
                  autocomplete="off"
                  placeholder="例如 TP312…"
                  required
                />
              </div>

              <div class="form-field">
                <label for="book-category-select">图书分类</label>
                <select
                  id="book-category-select"
                  v-model.number="form.categoryId"
                  name="categoryId"
                  autocomplete="off"
                >
                  <option :value="undefined">选择分类（可选）</option>
                  <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>

              <div class="form-field">
                <label for="book-stock">库存</label>
                <input
                  id="book-stock"
                  v-model.number="form.stock"
                  name="stock"
                  type="number"
                  min="0"
                  inputmode="numeric"
                  placeholder="请输入库存…"
                  required
                />
              </div>

              <div class="form-field">
                <label for="book-location">馆藏位置</label>
                <input
                  id="book-location"
                  v-model="form.location"
                  name="location"
                  autocomplete="off"
                  placeholder="例如 A-01-02…"
                  required
                />
              </div>
            </div>

            <div class="modal-actions">
              <button type="button" class="secondary" @click="closeModal">取消</button>
              <button type="submit">{{ editId ? '保存图书信息' : '创建图书' }}</button>
            </div>
          </form>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import BookCard from '@/components/BookCard.vue'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, BookDetailItem, BookItem, CategoryItem } from '@/types'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const list = ref<BookItem[]>([])
const categories = ref<CategoryItem[]>([])
const keyword = ref(String(route.query.keyword || ''))
const categoryCode = ref(String(route.query.categoryCode || ''))
const stockFilter = ref<'all' | 'available' | 'low'>((route.query.stockFilter as 'all' | 'available' | 'low') || 'all')
const selectedIds = ref<number[]>([])
const selectedCoverFiles = reactive<Record<number, File | undefined>>({})
const batchLocation = ref('')
const editId = ref<number | null>(null)
const showModal = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const showDetail = ref(false)
const detailBook = ref<BookDetailItem | null>(null)

const form = reactive({
  title: '',
  author: '',
  categoryCode: '',
  stock: 1,
  location: '',
  categoryId: undefined as number | undefined
})

watch([keyword, categoryCode, stockFilter], () => {
  router.replace({
    query: {
      ...route.query,
      keyword: keyword.value || undefined,
      categoryCode: categoryCode.value || undefined,
      stockFilter: auth.isAdmin && stockFilter.value !== 'all' ? stockFilter.value : undefined,
      bookId: route.query.bookId || undefined
    }
  })
})

watch(
  () => route.query.bookId,
  async (value) => {
    if (value) {
      await openDetail(Number(value))
    } else {
      hideDetail(false)
    }
  }
)

const filteredBooks = computed(() =>
  list.value.filter((item) => {
    if (stockFilter.value === 'available' && item.stock <= 0) return false
    if (stockFilter.value === 'low' && item.stock > 2) return false
    return true
  })
)

const detailReason = computed(() => {
  if (!detailBook.value) return ''
  if (detailBook.value.stock > 0) {
    return '这本书当前仍可借阅，适合直接进入借阅流程。你也可以结合分类号和馆藏位置继续查找相似图书。'
  }
  return '这本书当前库存不足，建议先收藏关注，或继续查看同分类图书与推荐结果。'
})

async function load() {
  try {
    const { data } = await http.get<ApiResponse<BookItem[]>>('/api/books', {
      params: {
        keyword: keyword.value || undefined,
        categoryCode: categoryCode.value || undefined
      }
    })
    list.value = data.data
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '图书列表加载失败，请稍后再试。'
  }
}

async function loadCategories() {
  try {
    const { data } = await http.get<ApiResponse<CategoryItem[]>>('/api/categories')
    categories.value = data.data
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '分类列表加载失败。'
  }
}

function resetForm() {
  editId.value = null
  form.title = ''
  form.author = ''
  form.categoryCode = ''
  form.stock = 1
  form.location = ''
  form.categoryId = undefined
}

function resetFilters() {
  keyword.value = ''
  categoryCode.value = ''
  stockFilter.value = 'all'
  load()
}

function openCreateModal() {
  resetForm()
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  resetForm()
}

function edit(item: BookItem) {
  editId.value = item.id
  form.title = item.title
  form.author = item.author
  form.categoryCode = item.categoryCode
  form.stock = item.stock
  form.location = item.location
  form.categoryId = item.categoryId
  showModal.value = true
}

async function openDetail(bookId: number) {
  try {
    const { data } = await http.get<ApiResponse<BookDetailItem>>(`/api/books/${bookId}`, {
      params: { similarLimit: 4 }
    })
    detailBook.value = data.data
    showDetail.value = true

    if (String(route.query.bookId || '') !== String(bookId)) {
      router.replace({
        query: {
          ...route.query,
          bookId: String(bookId)
        }
      })
    }
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '图书详情加载失败。'
  }
}

/**
 * 内部详情关闭函数。
 * updateRoute = true 时同步移除地址栏中的 bookId；
 * updateRoute = false 时只关闭弹窗，不改路由，避免 watch 循环触发。
 */
function hideDetail(updateRoute = true) {
  showDetail.value = false
  detailBook.value = null

  if (updateRoute && route.query.bookId) {
    const query = { ...route.query }
    delete query.bookId
    router.replace({ query })
  }
}

/**
 * 供模板点击事件直接绑定的关闭函数。
 * 不接收业务参数，避免与 PointerEvent 类型冲突。
 */
function closeDetail() {
  hideDetail(true)
}

function goBorrow() {
  hideDetail(true)
  router.push('/borrow')
}

async function saveBook() {
  successMessage.value = ''
  errorMessage.value = ''

  const payload: Record<string, any> = {
    title: form.title.trim(),
    author: form.author.trim(),
    categoryCode: form.categoryCode.trim(),
    stock: Number(form.stock),
    location: form.location.trim()
  }

  if (typeof form.categoryId === 'number' && Number.isFinite(form.categoryId)) {
    payload.categoryId = form.categoryId
  }

  try {
    if (editId.value) {
      await http.put(`/api/books/${editId.value}`, payload)
      successMessage.value = '图书信息更新成功。'
    } else {
      await http.post('/api/books', payload)
      successMessage.value = '图书创建成功。'
    }

    closeModal()
    await load()
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '保存图书失败，请检查表单后重试。'
  }
}

async function remove(id: number) {
  if (!window.confirm('删除后无法恢复，确认继续吗？')) return

  try {
    await http.delete(`/api/books/${id}`)
    successMessage.value = '图书删除成功。'
    await load()
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '删除图书失败。'
  }
}

async function batchUpdateLocation() {
  if (!selectedIds.value.length || !batchLocation.value.trim()) {
    errorMessage.value = '请先勾选图书，并填写新的馆藏位置。'
    return
  }

  try {
    await http.put('/api/books/batch/location', {
      bookIds: selectedIds.value,
      location: batchLocation.value.trim()
    })
    successMessage.value = '批量位置更新成功。'
    batchLocation.value = ''
    selectedIds.value = []
    await load()
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '批量更新位置失败。'
  }
}

async function batchDelete() {
  if (!selectedIds.value.length) {
    errorMessage.value = '请先勾选要删除的图书。'
    return
  }

  if (!window.confirm(`确认删除已勾选的 ${selectedIds.value.length} 本图书吗？`)) return

  try {
    await http.delete('/api/books/batch', { data: selectedIds.value })
    successMessage.value = '批量删除成功。'
    selectedIds.value = []
    await load()
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '批量删除失败。'
  }
}

function pickCover(id: number, event: Event) {
  const input = event.target as HTMLInputElement
  selectedCoverFiles[id] = input.files?.[0]
}

async function uploadCover(id: number) {
  const file = selectedCoverFiles[id]
  if (!file) {
    errorMessage.value = '请先选择封面图片文件。'
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    await http.post(`/api/books/${id}/cover`, formData)
    successMessage.value = '封面上传成功。'
    await load()
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message ?? '封面上传失败，请确认图片格式与大小。'
  }
}

onMounted(async () => {
  await Promise.all([load(), loadCategories()])
  if (route.query.bookId) {
    await openDetail(Number(route.query.bookId))
  }
})
</script>