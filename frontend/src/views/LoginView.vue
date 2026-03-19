<template>
  <div class="auth-shell">
    <section class="auth-hero">
      <p class="auth-eyebrow">智能图书管理系统</p>
      <h1>登录后继续找书、借书与管理馆藏</h1>
      <p>系统围绕图书检索、借阅流转、用户管理与个性化推荐构建，支持用户端与管理员端双角色访问。</p>
    </section>

    <section class="auth-card">
      <h2>登录系统</h2>
      <form class="auth-form" @submit.prevent="submit">
        <div class="form-field">
          <label for="username">用户名</label>
          <input id="username" v-model="form.username" name="username" autocomplete="username" placeholder="请输入用户名…" required spellcheck="false" />
        </div>
        <div class="form-field">
          <label for="password">密码</label>
          <input id="password" v-model="form.password" name="password" type="password" autocomplete="current-password" placeholder="请输入密码…" required />
        </div>
        <button type="submit">登录系统</button>
      </form>
      <p v-if="error" class="msg error" aria-live="polite">{{ error }}</p>
      <p class="auth-tip">还没有账号？<RouterLink to="/register">去注册</RouterLink></p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, AuthPayload } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: '', password: '' })
const error = ref('')

async function submit() {
  error.value = ''
  try {
    const { data } = await http.post<ApiResponse<AuthPayload>>('/api/auth/login', form)
    auth.setAuth(data.data)
    router.push('/dashboard')
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '登录失败，请检查用户名和密码后重试。'
  }
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr minmax(320px, 460px);
  gap: 24px;
  align-items: center;
  padding: 28px;
}

.auth-hero,
.auth-card {
  border-radius: 28px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.auth-eyebrow,
.auth-hero h1,
.auth-hero p,
.auth-card h2,
.auth-tip {
  margin: 0;
}

.auth-eyebrow {
  color: var(--primary);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
}

.auth-hero h1 {
  margin-top: 14px;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.1;
  text-wrap: balance;
}

.auth-hero p {
  margin-top: 16px;
  color: var(--text-soft);
  line-height: 1.8;
  max-width: 48ch;
}

.auth-form {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.auth-tip {
  margin-top: 18px;
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
}
</style>
