<template>
  <div class="login-overlay">
    <!-- 装饰光斑 -->
    <div class="glow glow-1"></div>
    <div class="glow glow-2"></div>
    <div class="glow glow-3"></div>

    <div class="login-card">
      <!-- Logo 区 -->
      <div class="logo-area">
        <div class="logo-icon">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="3"/>
            <circle cx="8.5" cy="8.5" r="1.5"/>
            <path d="m21 15-5-5L5 21"/>
          </svg>
        </div>
        <h1 class="login-title">DiagramGPT</h1>
        <p class="login-subtitle">AI 驱动的语音画图工具</p>
      </div>

      <!-- Tab 切换 -->
      <div class="login-tabs">
        <button
          :class="{ active: tab === 'login' }"
          @click="tab = 'login'"
        >登录</button>
        <button
          :class="{ active: tab === 'register' }"
          @click="tab = 'register'"
        >注册</button>
      </div>

      <!-- 表单 -->
      <form @submit.prevent="handleSubmit" class="login-form">
        <div class="field">
          <div class="input-wrapper">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <input
              v-model="username"
              type="text"
              placeholder="用户名"
              autocomplete="username"
              :disabled="loading"
            />
          </div>
        </div>
        <div class="field">
          <div class="input-wrapper">
            <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <input
              v-model="password"
              type="password"
              placeholder="密码"
              autocomplete="current-password"
              :disabled="loading"
            />
          </div>
        </div>

        <p class="login-error" v-if="errorMsg">{{ errorMsg }}</p>

        <button class="login-submit" type="submit" :disabled="loading || !canSubmit">
          <span v-if="loading" class="spinner"></span>
          <span v-else>{{ tab === 'login' ? '登 录' : '注 册' }}</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { login, register } from '../api/authApi.js'

const tab = ref('login')
const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

const canSubmit = computed(() => {
  return username.value.trim().length >= 3 && password.value.length >= 6
})

const emit = defineEmits(['loggedIn'])

async function handleSubmit() {
  if (!canSubmit.value || loading.value) return
  loading.value = true
  errorMsg.value = ''

  try {
    if (tab.value === 'login') {
      await login(username.value.trim(), password.value)
    } else {
      await register(username.value.trim(), password.value)
      await login(username.value.trim(), password.value)
    }
    emit('loggedIn')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0a14;
  z-index: 9999;
  overflow: hidden;
}

/* ---- 装饰光斑 ---- */
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  pointer-events: none;
}
.glow-1 {
  width: 500px; height: 500px;
  background: #7c5cfc;
  top: -20%; left: -15%;
  animation: float1 12s ease-in-out infinite;
}
.glow-2 {
  width: 400px; height: 400px;
  background: #3b82f6;
  bottom: -15%; right: -10%;
  animation: float2 14s ease-in-out infinite;
}
.glow-3 {
  width: 300px; height: 300px;
  background: #a78bfa;
  top: 40%; left: 55%;
  animation: float3 10s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(60px, 40px) scale(1.08); }
  66% { transform: translate(-30px, -30px) scale(0.94); }
}
@keyframes float2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-50px, -30px) scale(1.05); }
  66% { transform: translate(40px, 20px) scale(0.95); }
}
@keyframes float3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-20px, -50px) scale(1.1); }
}

/* ---- 卡片 ---- */
.login-card {
  position: relative;
  width: 400px;
  max-width: 92vw;
  padding: 44px 40px 36px;
  border-radius: 20px;
  background: rgba(15, 15, 30, 0.7);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow:
    0 0 80px rgba(124, 92, 252, 0.08),
    0 20px 60px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255,255,255,0.04);
  animation: cardIn 0.5s ease-out;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ---- Logo 区 ---- */
.logo-area {
  text-align: center;
  margin-bottom: 28px;
}
.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px; height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #7c5cfc, #5b8bf7);
  color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(124, 92, 252, 0.3);
}
.login-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
  letter-spacing: -0.3px;
}
.login-subtitle {
  color: rgba(255,255,255,0.45);
  font-size: 13px;
  margin: 0;
}

/* ---- Tab ---- */
.login-tabs {
  display: flex;
  background: rgba(255,255,255,0.04);
  border-radius: 10px;
  padding: 3px;
  margin-bottom: 24px;
}
.login-tabs button {
  flex: 1;
  padding: 9px 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255,255,255,0.4);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all .25s;
}
.login-tabs button.active {
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

/* ---- 表单 ---- */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
}
.input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border-radius: 10px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  transition: border-color .25s, background .25s;
}
.input-wrapper:focus-within {
  border-color: #7c5cfc;
  background: rgba(255,255,255,0.06);
  box-shadow: 0 0 0 3px rgba(124, 92, 252, 0.1);
}
.input-icon {
  color: rgba(255,255,255,0.3);
  flex-shrink: 0;
}
.input-wrapper:focus-within .input-icon {
  color: #7c5cfc;
}
.input-wrapper input {
  flex: 1;
  padding: 12px 0;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 14px;
  outline: none;
}
.input-wrapper input:-webkit-autofill,
.input-wrapper input:-webkit-autofill:hover,
.input-wrapper input:-webkit-autofill:focus {
  -webkit-text-fill-color: #fff !important;
  caret-color: #fff;
  box-shadow: 0 0 0 1000px transparent inset !important;
  transition: background-color 9999s ease-in-out 0s;
}
.input-wrapper input::placeholder {
  color: rgba(255,255,255,0.25);
}

/* ---- 错误提示 ---- */
.login-error {
  color: #f87171;
  font-size: 13px;
  margin: -4px 0 -4px;
  text-align: center;
  padding: 6px 10px;
  background: rgba(248, 113, 113, 0.08);
  border-radius: 8px;
}

/* ---- 提交按钮 ---- */
.login-submit {
  margin-top: 6px;
  padding: 13px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #7c5cfc, #5b8bf7);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all .25s;
  box-shadow: 0 4px 16px rgba(124, 92, 252, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
}
.login-submit:hover:not(:disabled) {
  box-shadow: 0 6px 24px rgba(124, 92, 252, 0.4);
  transform: translateY(-1px);
}
.login-submit:active:not(:disabled) {
  transform: translateY(0);
}
.login-submit:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  box-shadow: none;
}

.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin .6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
