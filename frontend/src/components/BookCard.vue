<template>
  <article class="book-card">
    <div class="book-cover-wrap">
      <img
        v-if="book.imageUrl"
        :src="book.imageUrl"
        :alt="`${book.title}封面`"
        class="book-cover"
        width="112"
        height="148"
        loading="lazy"
      />
      <div v-else class="book-cover placeholder" aria-hidden="true">暂无封面</div>
    </div>
    <div class="book-body">
      <div class="book-main">
        <div class="book-top">
          <p class="book-meta">{{ book.categoryName || book.categoryCode || '未分类' }}</p>
          <slot name="badge" />
        </div>
        <h3 class="book-title">{{ book.title }}</h3>
        <p class="book-author">{{ book.author }}</p>
        <dl class="book-facts">
          <div>
            <dt>分类号</dt>
            <dd>{{ book.categoryCode || '—' }}</dd>
          </div>
          <div>
            <dt>馆藏位置</dt>
            <dd>{{ book.location || '—' }}</dd>
          </div>
          <div>
            <dt>库存</dt>
            <dd>{{ book.stock }}</dd>
          </div>
        </dl>
        <p v-if="reason" class="book-reason">{{ reason }}</p>
      </div>
      <div class="book-actions">
        <slot name="actions" />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { BookItem } from '@/types'

defineProps<{
  book: BookItem
  reason?: string
}>()
</script>
