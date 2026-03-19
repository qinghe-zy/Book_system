<template>
  <div class="app-page">
    <PageHeader eyebrow="个人中心" title="维护基础资料、兴趣偏好与账户安全" description="这里既是用户资料中心，也是推荐画像的重要输入来源。建议定期更新兴趣标签与邮箱信息。" />

    <p v-if="message" class="msg success" aria-live="polite">{{ message }}</p>

    <div class="grid-3">
      <section class="page-card">
        <h2 style="margin-top: 0;">基础信息</h2>
        <form class="modal-form" @submit.prevent="save">
          <div class="form-field">
            <label for="profile-email">电子邮箱</label>
            <input id="profile-email" v-model="form.email" name="email" type="email" autocomplete="email" placeholder="例如 name@example.com…" required spellcheck="false" />
          </div>
          <button type="submit">保存基础信息</button>
        </form>
      </section>

      <section class="page-card">
        <h2 style="margin-top: 0;">兴趣偏好</h2>
        <form class="modal-form" @submit.prevent="save">
          <div class="form-field">
            <label for="profile-tags">兴趣标签</label>
            <input id="profile-tags" v-model="form.interestTags" name="interestTags" autocomplete="off" placeholder="例如 Java, 文学, 产品设计…" />
          </div>
          <p class="muted">推荐模块会优先结合你的兴趣标签与借阅历史，生成更自然的推荐理由。</p>
          <button type="submit" class="secondary">保存兴趣偏好</button>
        </form>
      </section>

      <section class="page-card">
        <h2 style="margin-top: 0;">账户安全</h2>
        <form class="modal-form" @submit.prevent="save">
          <div class="form-field">
            <label for="profile-password">修改密码</label>
            <input id="profile-password" v-model="form.password" name="password" type="password" autocomplete="new-password" placeholder="留空则不修改…" />
          </div>
          <p class="muted">为保证账号安全，建议定期更新密码，并保持邮箱信息可用。</p>
          <button type="submit" class="secondary">保存安全设置</button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import http from '@/api/http'
import type { ApiResponse, UserItem } from '@/types'

const form = reactive({ email: '', password: '', interestTags: '' })
const message = ref('')

async function load() {
  const { data } = await http.get<ApiResponse<UserItem>>('/api/users/me')
  form.email = data.data.email
  form.interestTags = data.data.interestTags || ''
}

async function save() {
  await http.put('/api/users/me', form)
  message.value = '个人资料更新成功。'
  form.password = ''
}

onMounted(load)
</script>
