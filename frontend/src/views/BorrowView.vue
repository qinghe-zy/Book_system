<template>
  <div class="app-page">
    <PageHeader
      :eyebrow="auth.isAdmin ? '借阅管理' : '我的借阅'"
      :title="auth.isAdmin ? '按状态处理借阅申请、待取书与逾期记录' : '按当前状态查看借阅进度，明确下一步动作'"
      :description="auth.isAdmin ? '管理员视角强调待办聚合和状态分组，减少在整表中反复查找。' : '用户视角突出当前状态、应还时间、提取码与可执行操作。'"
    >
      <template #actions>
        <button type="button" @click="openApplyModal">发起借阅</button>
      </template>
    </PageHeader>

    <p v-if="notice" class="msg success" aria-live="polite">{{ notice }}</p>
    <p v-if="error" class="msg error" aria-live="polite">{{ error }}</p>

    <section class="notice-card">
      <h3>借阅方式说明</h3>
      <p>线上借阅：提交申请后等待管理员审核，通过后凭提取码取书并确认取书。线下借阅：现场登记后直接借出，系统同步记录应还时间与续借状态。</p>
    </section>

    <section class="page-card">
      <div class="segmented">
        <button v-for="tab in userTabs" :key="tab.value" type="button" :class="{ active: currentUserTab === tab.value }" @click="currentUserTab = tab.value">
          {{ tab.label }}（{{ countMine(tab.value) }}）
        </button>
      </div>

      <div v-if="filteredMine.length" class="book-grid" style="margin-top: 18px;">
        <article v-for="r in filteredMine" :key="r.id" class="page-card" style="padding: 18px;">
          <div class="toolbar" style="justify-content: space-between; align-items: flex-start;">
            <div>
              <h3 style="margin: 0; font-size: 20px;">{{ r.bookTitle }}</h3>
              <p class="muted" style="margin-top: 8px;">{{ borrowTypeText(r.borrowType) }} · 记录 #{{ r.id }}</p>
            </div>
            <StatusBadge :label="statusText(r.status)" :tone="statusTone(r.status)" />
          </div>
          <div class="detail-list" style="margin-top: 16px;">
            <div><dt>审核时间</dt><dd>{{ formatDateTime(r.approvedTime) }}</dd></div>
            <div><dt>借出时间</dt><dd>{{ formatDateTime(r.borrowTime) }}</dd></div>
            <div><dt>应还时间</dt><dd>{{ formatDateTime(r.dueTime) }}</dd></div>
            <div><dt>提取码</dt><dd>{{ r.pickupCode || '—' }}</dd></div>
          </div>
          <p class="book-reason" style="margin-top: 16px;">{{ userActionHint(r) }}</p>
          <div class="toolbar" style="margin-top: 16px;">
            <button type="button" class="secondary" @click="openUserActionModal(r)">更多操作</button>
          </div>
        </article>
      </div>
      <EmptyState v-else title="当前分类下暂无借阅记录" description="你可以切换状态分组查看其他记录，或直接发起新的借阅申请。" icon="🧾">
        <template #action>
          <button type="button" @click="openApplyModal">发起借阅</button>
        </template>
      </EmptyState>
    </section>

    <section v-if="auth.isAdmin" class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="table-title">管理员处理台</div>
          <div class="table-helper">按待审核、待取书、借阅中、逾期等状态组织记录，便于快速处理当前待办。</div>
        </div>
      </div>
      <div class="segmented" style="padding: 0 20px 18px;">
        <button v-for="tab in adminTabs" :key="tab.value" type="button" :class="{ active: currentAdminTab === tab.value }" @click="currentAdminTab = tab.value">
          {{ tab.label }}（{{ countAdmin(tab.value) }}）
        </button>
      </div>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>用户</th>
              <th>书名</th>
              <th>方式</th>
              <th>状态</th>
              <th>应还时间</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in filteredAdmin" :key="r.id">
              <td>{{ r.id }}</td>
              <td>{{ r.username }}</td>
              <td>{{ r.bookTitle }}</td>
              <td>{{ borrowTypeText(r.borrowType) }}</td>
              <td><StatusBadge :label="statusText(r.status)" :tone="statusTone(r.status)" /></td>
              <td>{{ formatDateTime(r.dueTime) }}</td>
              <td>{{ r.reviewComment || '—' }}</td>
              <td><button type="button" @click="openReviewModal(r)">处理申请</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-if="!filteredAdmin.length" title="当前没有待处理记录" description="切换其他状态标签，或稍后再回来查看新的借阅流转。" icon="✅" />
    </section>

    <teleport to="body">
      <div v-if="showApplyModal" class="modal-mask">
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="apply-title">
          <h2 id="apply-title" style="margin-top: 0;">发起借阅申请</h2>
          <form class="modal-form" @submit.prevent="submitApply">
            <div class="form-field">
              <label for="apply-book">选择图书</label>
              <select id="apply-book" v-model.number="applyForm.bookId" name="bookId" required>
                <option :value="undefined">请选择可借图书</option>
                <option v-for="b in books" :key="b.id" :value="b.id">{{ b.title }}（库存 {{ b.stock }}）</option>
              </select>
            </div>
            <div class="form-field">
              <label for="apply-type">借阅方式</label>
              <select id="apply-type" v-model="applyForm.borrowType" name="borrowType" required>
                <option value="ONLINE">线上借阅（管理员审核后取书）</option>
                <option value="OFFLINE">线下借阅（现场登记直接借出）</option>
              </select>
            </div>
            <div class="modal-actions">
              <button type="button" class="secondary" @click="showApplyModal = false">取消</button>
              <button type="submit">提交借阅申请</button>
            </div>
          </form>
        </div>
      </div>

      <div v-if="showUserActionModal && activeRecord" class="modal-mask">
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="user-action-title">
          <h2 id="user-action-title" style="margin-top: 0;">借阅操作</h2>
          <p class="muted">记录 #{{ activeRecord.id }} · {{ activeRecord.bookTitle }}</p>
          <p class="book-reason" style="margin-top: 16px;">{{ userActionHint(activeRecord) }}</p>
          <div class="detail-list" style="margin-top: 16px;">
            <div><dt>当前状态</dt><dd>{{ statusText(activeRecord.status) }}</dd></div>
            <div><dt>提取码</dt><dd>{{ activeRecord.pickupCode || '—' }}</dd></div>
            <div><dt>应还时间</dt><dd>{{ formatDateTime(activeRecord.dueTime) }}</dd></div>
            <div><dt>续借次数</dt><dd>{{ activeRecord.renewCount }}</dd></div>
          </div>
          <div class="modal-actions" style="margin-top: 18px; justify-content: flex-start;">
            <button type="button" class="secondary" @click="showUserActionModal = false">关闭</button>
            <button v-if="activeRecord.status === 'APPLIED'" type="button" class="danger" @click="cancelApply(activeRecord.id)">撤销申请</button>
            <button v-if="activeRecord.status === 'APPROVED'" type="button" @click="confirmPickup(activeRecord.id)">确认取书</button>
            <button v-if="['BORROWED', 'OVERDUE'].includes(activeRecord.status)" type="button" class="secondary" @click="renew(activeRecord.id)">续借图书</button>
            <button v-if="['BORROWED', 'OVERDUE'].includes(activeRecord.status)" type="button" @click="returnBook(activeRecord.id)">归还图书</button>
          </div>
        </div>
      </div>

      <div v-if="showReviewModal && reviewTarget" class="modal-mask">
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="review-title">
          <h2 id="review-title" style="margin-top: 0;">处理借阅申请</h2>
          <p class="muted">记录 #{{ reviewTarget.id }} · 用户 {{ reviewTarget.username }} · {{ reviewTarget.bookTitle }}</p>
          <form class="modal-form" @submit.prevent="submitReview">
            <div class="form-field">
              <label for="review-action">处理动作</label>
              <select id="review-action" v-model="reviewForm.action" name="action">
                <option v-for="option in reviewActionOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </div>
            <div v-if="reviewForm.action === 'approve'" class="form-field">
              <label for="loan-days">借阅天数</label>
              <input id="loan-days" v-model.number="reviewForm.loanDays" name="loanDays" type="number" min="1" max="60" inputmode="numeric" placeholder="请输入借阅天数…" />
            </div>
            <div class="form-field">
              <label for="review-comment">处理备注</label>
              <textarea id="review-comment" v-model="reviewForm.comment" name="comment" rows="3" placeholder="请输入备注信息…" />
            </div>
            <div class="modal-actions">
              <button type="button" class="secondary" @click="showReviewModal = false">取消</button>
              <button type="submit">提交处理结果</button>
            </div>
          </form>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, BookItem, BorrowItem } from '@/types'
import { formatDateTime } from '@/composables/useFormat'

const auth = useAuthStore()
const books = ref<BookItem[]>([])
const mine = ref<BorrowItem[]>([])
const all = ref<BorrowItem[]>([])
const notice = ref('')
const error = ref('')
const showApplyModal = ref(false)
const showUserActionModal = ref(false)
const showReviewModal = ref(false)
const activeRecord = ref<BorrowItem>()
const reviewTarget = ref<BorrowItem>()
const currentUserTab = ref<'all' | 'pending' | 'pickup' | 'borrowing' | 'history'>('all')
const currentAdminTab = ref<'pending' | 'pickup' | 'borrowing' | 'overdue' | 'all'>('pending')

type ReviewAction = 'approve' | 'reject' | 'cancelApproved' | 'forceReturn'

const userTabs = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '待审核' },
  { value: 'pickup', label: '待取书' },
  { value: 'borrowing', label: '借阅中' },
  { value: 'history', label: '历史记录' }
] as const

const adminTabs = [
  { value: 'pending', label: '待审核' },
  { value: 'pickup', label: '待取书' },
  { value: 'borrowing', label: '借阅中' },
  { value: 'overdue', label: '逾期' },
  { value: 'all', label: '全部' }
] as const

const applyForm = reactive({ bookId: undefined as number | undefined, borrowType: 'ONLINE' as 'ONLINE' | 'OFFLINE' })
const reviewForm = reactive({ action: 'approve' as ReviewAction, loanDays: 14, comment: '' })

const filteredMine = computed(() => mine.value.filter((item) => {
  if (currentUserTab.value === 'pending') return item.status === 'APPLIED'
  if (currentUserTab.value === 'pickup') return item.status === 'APPROVED'
  if (currentUserTab.value === 'borrowing') return ['BORROWED', 'OVERDUE'].includes(item.status)
  if (currentUserTab.value === 'history') return ['RETURNED', 'REJECTED', 'CANCELED'].includes(item.status)
  return true
}))

const filteredAdmin = computed(() => all.value.filter((item) => {
  if (currentAdminTab.value === 'pending') return item.status === 'APPLIED'
  if (currentAdminTab.value === 'pickup') return item.status === 'APPROVED'
  if (currentAdminTab.value === 'borrowing') return item.status === 'BORROWED'
  if (currentAdminTab.value === 'overdue') return item.status === 'OVERDUE'
  return true
}))

const reviewActionOptions = computed(() => {
  const target = reviewTarget.value
  if (!target) return []
  if (target.status === 'APPLIED') return [
    { value: 'approve' as ReviewAction, label: '审核通过并生成提取码' },
    { value: 'reject' as ReviewAction, label: '驳回申请' }
  ]
  if (target.status === 'APPROVED') return [{ value: 'cancelApproved' as ReviewAction, label: '撤销通过并回补库存' }]
  if (target.status === 'BORROWED' || target.status === 'OVERDUE') return [{ value: 'forceReturn' as ReviewAction, label: '管理员代归还' }]
  return []
})

function countMine(tab: typeof userTabs[number]['value']) {
  currentUserTab.value = currentUserTab.value
  return mine.value.filter((item) => {
    if (tab === 'pending') return item.status === 'APPLIED'
    if (tab === 'pickup') return item.status === 'APPROVED'
    if (tab === 'borrowing') return ['BORROWED', 'OVERDUE'].includes(item.status)
    if (tab === 'history') return ['RETURNED', 'REJECTED', 'CANCELED'].includes(item.status)
    return true
  }).length
}

function countAdmin(tab: typeof adminTabs[number]['value']) {
  return all.value.filter((item) => {
    if (tab === 'pending') return item.status === 'APPLIED'
    if (tab === 'pickup') return item.status === 'APPROVED'
    if (tab === 'borrowing') return item.status === 'BORROWED'
    if (tab === 'overdue') return item.status === 'OVERDUE'
    return true
  }).length
}

function statusText(status: BorrowItem['status']) {
  return { APPLIED: '待审核', APPROVED: '待取书', BORROWED: '借阅中', RETURNED: '已归还', REJECTED: '已驳回', CANCELED: '已撤销', OVERDUE: '已逾期' }[status] || status
}

function statusTone(status: BorrowItem['status']) {
  if (status === 'APPLIED') return 'info'
  if (status === 'APPROVED') return 'warning'
  if (status === 'BORROWED' || status === 'RETURNED') return 'success'
  if (status === 'OVERDUE' || status === 'REJECTED' || status === 'CANCELED') return 'danger'
  return 'neutral'
}

function borrowTypeText(type: BorrowItem['borrowType']) {
  return type === 'ONLINE' ? '线上借阅' : '线下借阅'
}

function userActionHint(record: BorrowItem) {
  if (record.status === 'APPLIED') return '当前处于待审核阶段。你可以等待管理员处理，或主动撤销申请后重新选择其他图书。'
  if (record.status === 'APPROVED') return '管理员已审核通过。请凭提取码到馆取书，完成后点击“确认取书”。'
  if (record.status === 'BORROWED') return '你已成功借出图书。建议关注应还时间，必要时可续借或直接归还。'
  if (record.status === 'OVERDUE') return '当前借阅已经逾期。请尽快归还，或联系管理员确认后续处理方式。'
  if (record.status === 'RETURNED') return '这条借阅已经完成归还，可继续浏览推荐或再次借阅同类图书。'
  return '这条记录当前无需额外操作，你可以切换到其他状态查看更多借阅进度。'
}

function resetMessage() {
  notice.value = ''
  error.value = ''
}

async function loadBooks() {
  const { data } = await http.get<ApiResponse<BookItem[]>>('/api/books')
  books.value = data.data.filter((item) => item.stock > 0)
}

async function loadMine() {
  const { data } = await http.get<ApiResponse<BorrowItem[]>>('/api/borrow/my')
  mine.value = data.data
}

async function loadAll() {
  if (!auth.isAdmin) return
  const { data } = await http.get<ApiResponse<BorrowItem[]>>('/api/borrow/admin/all')
  all.value = data.data
}

async function refreshData() {
  await Promise.all([loadBooks(), loadMine(), loadAll()])
}

function openApplyModal() {
  applyForm.bookId = undefined
  applyForm.borrowType = 'ONLINE'
  showApplyModal.value = true
}

async function submitApply() {
  if (!applyForm.bookId) return
  resetMessage()
  try {
    const { data } = await http.post<ApiResponse<BorrowItem>>('/api/borrow/apply', applyForm)
    notice.value = `借阅申请已提交，当前状态：${statusText(data.data.status)}。`
    showApplyModal.value = false
    await refreshData()
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '提交借阅申请失败。'
  }
}

function openUserActionModal(record: BorrowItem) {
  activeRecord.value = record
  showUserActionModal.value = true
}

function openReviewModal(record: BorrowItem) {
  reviewTarget.value = record
  reviewForm.action = record.status === 'APPLIED' ? 'approve' : record.status === 'APPROVED' ? 'cancelApproved' : 'forceReturn'
  reviewForm.loanDays = 14
  reviewForm.comment = ''
  showReviewModal.value = true
}

async function cancelApply(recordId: number) {
  resetMessage()
  try {
    await http.put(`/api/borrow/${recordId}/cancel`)
    notice.value = '申请已撤销。'
    showUserActionModal.value = false
    await refreshData()
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '撤销申请失败。'
  }
}

async function confirmPickup(recordId: number) {
  resetMessage()
  try {
    await http.put(`/api/borrow/${recordId}/pickup`)
    notice.value = '你已确认取书，记录已进入借阅中状态。'
    showUserActionModal.value = false
    await refreshData()
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '确认取书失败。'
  }
}

async function returnBook(recordId: number) {
  resetMessage()
  try {
    await http.put(`/api/borrow/${recordId}/return`)
    notice.value = '图书归还成功。'
    showUserActionModal.value = false
    await refreshData()
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '归还图书失败。'
  }
}

async function renew(recordId: number) {
  resetMessage()
  try {
    await http.put(`/api/borrow/${recordId}/renew`)
    notice.value = '续借成功，请留意新的应还时间。'
    showUserActionModal.value = false
    await refreshData()
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '续借失败。'
  }
}

async function submitReview() {
  if (!reviewTarget.value) return
  resetMessage()
  try {
    if (reviewForm.action === 'approve') {
      await http.put(`/api/borrow/${reviewTarget.value.id}/approve`, { loanDays: reviewForm.loanDays, comment: reviewForm.comment })
      notice.value = '审核通过，系统已生成提取码。'
    } else if (reviewForm.action === 'reject') {
      await http.put(`/api/borrow/${reviewTarget.value.id}/reject`, { comment: reviewForm.comment })
      notice.value = '申请已驳回。'
    } else if (reviewForm.action === 'cancelApproved') {
      await http.put(`/api/borrow/${reviewTarget.value.id}/reject`, { comment: reviewForm.comment || '管理员撤销通过' })
      notice.value = '已撤销通过并回补库存。'
    } else {
      await http.put(`/api/borrow/${reviewTarget.value.id}/return`)
      notice.value = '管理员已代用户完成归还。'
    }
    showReviewModal.value = false
    await refreshData()
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '处理借阅记录失败。'
  }
}

onMounted(refreshData)
</script>
