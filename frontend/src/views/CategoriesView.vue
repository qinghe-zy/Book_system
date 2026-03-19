<template>
  <div class="app-page">
    <PageHeader eyebrow="分类管理" title="轻量维护图书分类名称与编码映射" description="分类模块只保留基础维护能力，不扩展为复杂运营系统，确保稳定、清晰、快进快出。" />

    <section class="page-card">
      <form class="toolbar" @submit.prevent="save">
        <div class="field">
          <label for="category-name">分类名称</label>
          <input id="category-name" v-model="name" name="name" autocomplete="off" placeholder="请输入分类名称…" required />
        </div>
        <div class="toolbar">
          <button type="submit">{{ editId ? '保存分类' : '新增分类' }}</button>
          <button type="button" class="secondary" @click="reset">重置</button>
        </div>
      </form>
    </section>

    <section class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="table-title">分类列表</div>
          <div class="table-helper">共 {{ list.length }} 个分类。点击编辑后可直接修改分类名称。</div>
        </div>
      </div>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>分类名</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.name }}</td>
              <td>
                <div class="toolbar" style="gap: 8px;">
                  <button type="button" class="secondary" @click="edit(item)">编辑</button>
                  <button type="button" class="danger" @click="remove(item.id)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import http from '@/api/http'
import type { ApiResponse, CategoryItem } from '@/types'

const list = ref<CategoryItem[]>([])
const name = ref('')
const editId = ref<number | null>(null)

async function load() {
  const { data } = await http.get<ApiResponse<CategoryItem[]>>('/api/categories')
  list.value = data.data
}

function reset() {
  editId.value = null
  name.value = ''
}

function edit(item: CategoryItem) {
  editId.value = item.id
  name.value = item.name
}

async function save() {
  if (editId.value) {
    await http.put(`/api/categories/${editId.value}`, { name: name.value })
  } else {
    await http.post('/api/categories', { name: name.value })
  }
  reset()
  await load()
}

async function remove(id: number) {
  if (!window.confirm('删除分类后将无法恢复，确认继续吗？')) return
  await http.delete(`/api/categories/${id}`)
  await load()
}

onMounted(load)
</script>
