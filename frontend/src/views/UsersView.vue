<template>
  <div class="app-page">
    <PageHeader eyebrow="用户管理" title="按用户资料与角色集中维护系统账号" description="管理员可以在这里快速检索用户、调整邮箱与角色，并结合注册时间了解用户基础情况。" />

    <section class="filter-card">
      <div class="toolbar">
        <div class="field">
          <label for="user-keyword">关键词</label>
          <input id="user-keyword" v-model="keyword" name="keyword" autocomplete="off" placeholder="按用户名或邮箱筛选…" />
        </div>
        <div class="field">
          <label for="user-role">角色筛选</label>
          <select id="user-role" v-model="roleFilter" name="roleFilter" autocomplete="off">
            <option value="ALL">全部角色</option>
            <option value="USER">普通用户</option>
            <option value="ADMIN">管理员</option>
          </select>
        </div>
      </div>
    </section>

    <section class="table-card">
      <div class="table-toolbar">
        <div>
          <div class="table-title">用户列表</div>
          <div class="table-helper">共 {{ filteredUsers.length }} 位用户。保存时仅提交当前行编辑后的邮箱与角色。</div>
        </div>
      </div>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id">
              <td>{{ u.id }}</td>
              <td>{{ u.username }}</td>
              <td><input v-model="u.email" :name="`email-${u.id}`" type="email" autocomplete="off" :aria-label="`修改${u.username}的邮箱`" /></td>
              <td>
                <select v-model="u.role" :name="`role-${u.id}`" :aria-label="`修改${u.username}的角色`">
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </td>
              <td>{{ formatDateTime(u.registerTime) }}</td>
              <td><button type="button" class="secondary" @click="save(u)">保存更改</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import http from '@/api/http'
import type { ApiResponse, UserItem } from '@/types'
import { formatDateTime } from '@/composables/useFormat'

const users = ref<UserItem[]>([])
const keyword = ref('')
const roleFilter = ref<'ALL' | 'USER' | 'ADMIN'>('ALL')

const filteredUsers = computed(() => users.value.filter((u) => {
  const keywordValue = keyword.value.trim().toLowerCase()
  const matchesKeyword = !keywordValue || u.username.toLowerCase().includes(keywordValue) || u.email.toLowerCase().includes(keywordValue)
  const matchesRole = roleFilter.value === 'ALL' || u.role === roleFilter.value
  return matchesKeyword && matchesRole
}))

async function load() {
  const { data } = await http.get<ApiResponse<UserItem[]>>('/api/admin/users')
  users.value = data.data
}

async function save(u: UserItem) {
  await http.put(`/api/admin/users/${u.id}`, { email: u.email, role: u.role })
  await load()
}

onMounted(load)
</script>
