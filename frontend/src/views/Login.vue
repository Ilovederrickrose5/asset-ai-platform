<template>
  <div class="login-container">
    <div class="ambient ambient--indigo"></div>
    <div class="ambient ambient--bronze"></div>

    <div class="login-shell">
      <section class="login-story">
        <div class="story-glow story-glow--a"></div>
        <div class="story-glow story-glow--b"></div>

        <header class="story-brand">
          <span class="brand-mark">EA</span>
          <div class="brand-copy">
            <strong>企业固定资产</strong>
            <span>Asset Management</span>
          </div>
        </header>

        <div class="story-content">
          <p class="story-eyebrow">LIFECYCLE · INSIGHT · CONTROL</p>
          <h1>让每一件资产<br /><em>都有迹可循。</em></h1>
          <p class="story-lede">
            从采购入库、领用流转，到折旧盘点与报废处置，
            用一套清晰的流程，让资产管理更从容。
          </p>

          <ul class="story-features">
            <li>
              <span class="feature-icon"><el-icon><CircleCheck /></el-icon></span>
              <div>
                <strong>全生命周期</strong>
                <span>入库、领用、维修、报废全程可追溯</span>
              </div>
            </li>
            <li>
              <span class="feature-icon"><el-icon><DataAnalysis /></el-icon></span>
              <div>
                <strong>数据洞察</strong>
                <span>资产分布、状态与折旧一目了然</span>
              </div>
            </li>
            <li>
              <span class="feature-icon"><el-icon><Lock /></el-icon></span>
              <div>
                <strong>权限与审计</strong>
                <span>基于角色的访问控制与操作留痕</span>
              </div>
            </li>
          </ul>
        </div>

        <footer class="story-footer">
          <span class="pulse-dot"></span>
          <span>安全 · 稳定 · 可追溯</span>
          <span class="story-meta">EAM / 2026</span>
        </footer>
      </section>

      <section class="login-panel">
        <div class="panel-inner">
          <div class="panel-heading">
            <p class="panel-eyebrow">WELCOME BACK</p>
            <h2>欢迎回来</h2>
            <p class="panel-subtitle">登录以继续管理企业资产</p>
          </div>

          <el-form
            :model="loginForm"
            :rules="rules"
            ref="loginFormRef"
            label-position="top"
            class="login-form"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
                autocomplete="username"
                clearable
                class="login-input"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                autocomplete="current-password"
                show-password
                @keyup.enter="handleLogin"
                class="login-input"
              />
            </el-form-item>

            <el-form-item class="login-submit-item">
              <el-button
                type="primary"
                :loading="isLoading"
                @click="handleLogin"
                class="login-button"
              >
                <span>{{ isLoading ? '正在登录' : '登录' }}</span>
                <el-icon v-if="!isLoading" class="login-button-icon"><ArrowRight /></el-icon>
              </el-button>
            </el-form-item>

            <transition name="login-alert">
              <div v-if="error" class="login-error" role="alert">
                <span class="error-dot"></span>
                <span>{{ error }}</span>
              </div>
            </transition>

            <div class="login-hint">
              <span class="hint-line"></span>
              <span>如有登录问题，请联系系统管理员</span>
            </div>
          </el-form>

          <footer class="panel-footnote">
            <span>© 2026 企业资产智能管理平台</span>
            <span>保留所有权利</span>
          </footer>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from '../utils/request'
import { User, Lock, CircleCheck, DataAnalysis, ArrowRight } from '@element-plus/icons-vue'
import { normalizeUser } from '../utils/common'
import { useUserStore } from '../stores/userStore'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const userStore = useUserStore()
    const loginFormRef = ref(null)
    const loginForm = ref({
      username: '',
      password: ''
    })
    const isLoading = ref(false)
    const error = ref('')

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }

    const handleLogin = async () => {
      if (!loginFormRef.value) return

      await loginFormRef.value.validate(async (valid) => {
        if (!valid) return

        isLoading.value = true
        error.value = ''

        try {
          const response = await axios.post('/auth/login', loginForm.value)

          if (response.data.code === 200) {
            // 使用字段映射函数统一用户数据字段名
            const normalizedUser = normalizeUser(response.data.data)

            // 写入 store + localStorage（统一入口）
            userStore.login(normalizedUser, response.data.data.token)

            // 登录成功后跳转到 redirect 参数指定的页面，否则回首页
            const redirect = route.query.redirect
            if (redirect) {
              router.push(decodeURIComponent(redirect))
            } else {
              router.push('/home')
            }
          } else {
            error.value = response.data.message
          }
        } catch (err) {
          console.error('登录错误:', err)
          if (err.response?.data?.message && err.response.data.message.includes('User is disabled')) {
            error.value = '您现在暂时无法登录，请联系系统管理员 13800138001'
          } else {
            error.value = err.response?.data?.message || '登录失败，请检查网络连接'
          }
        } finally {
          isLoading.value = false
        }
      })
    }

    return {
      loginFormRef,
      loginForm,
      isLoading,
      error,
      rules,
      User,
      Lock,
      CircleCheck,
      DataAnalysis,
      ArrowRight,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 32px;
  overflow: hidden;
  background:
    radial-gradient(1100px 620px at 12% 8%, #e7ebf2 0%, transparent 62%),
    radial-gradient(900px 520px at 88% 92%, #efe9e0 0%, transparent 58%),
    var(--eam-canvas);
}

.ambient {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  opacity: 0.55;
  animation: ambient-drift 22s var(--eam-ease-soft) infinite alternate;
}

.ambient--indigo {
  width: 520px;
  height: 520px;
  top: -220px;
  left: -140px;
  background: rgba(84, 108, 150, 0.22);
}

.ambient--bronze {
  width: 460px;
  height: 460px;
  right: -180px;
  bottom: -200px;
  background: rgba(169, 136, 95, 0.18);
  animation-delay: -8s;
}

@keyframes ambient-drift {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(26px, 20px, 0) scale(1.06); }
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.06fr 0.94fr;
  width: 100%;
  max-width: 1180px;
  min-height: 700px;
  overflow: hidden;
  background: var(--eam-surface);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--eam-radius-xl);
  box-shadow: var(--eam-shadow-lg);
  animation: shell-in 0.9s var(--eam-ease) both;
}

@keyframes shell-in {
  from {
    opacity: 0;
    transform: translate3d(0, 26px, 0) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

/* ---------- Story panel ---------- */

.login-story {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(158deg, #0c1119 0%, #131b28 54%, #1b283b 100%);
}

.login-story::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 76px 76px;
  mask-image: radial-gradient(ellipse at 28% 22%, #000 0%, transparent 76%);
  pointer-events: none;
}

.story-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
}

.story-glow--a {
  width: 380px;
  height: 380px;
  top: -120px;
  right: -80px;
  background: rgba(72, 104, 156, 0.38);
}

.story-glow--b {
  width: 320px;
  height: 320px;
  bottom: -140px;
  left: -100px;
  background: rgba(169, 136, 95, 0.22);
}

.story-brand,
.story-content,
.story-footer {
  position: relative;
  z-index: 1;
}

.story-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  animation: reveal-up 0.8s var(--eam-ease) 0.05s both;
}

.brand-mark {
  display: inline-grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 13px;
  font-family: var(--eam-font-mono);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #efe6d6;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.04));
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.brand-copy span {
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.42);
}

.story-content {
  max-width: 460px;
}

.story-eyebrow {
  margin: 0 0 20px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: rgba(216, 198, 166, 0.82);
  animation: reveal-up 0.8s var(--eam-ease) 0.16s both;
}

.story-content h1 {
  margin: 0 0 22px;
  font-size: clamp(36px, 3.6vw, 50px);
  font-weight: 600;
  line-height: 1.16;
  letter-spacing: -0.035em;
  color: #f6f7f9;
  animation: reveal-up 0.9s var(--eam-ease) 0.24s both;
}

.story-content h1 em {
  font-style: normal;
  color: #d9c7a7;
}

.story-lede {
  margin: 0 0 34px;
  max-width: 420px;
  font-size: 14px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.56);
  animation: reveal-up 0.9s var(--eam-ease) 0.32s both;
}

.story-features {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
  animation: reveal-up 0.9s var(--eam-ease) 0.4s both;
}

.story-features li {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 16px;
  border-radius: var(--eam-radius-md);
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(8px);
  transition: transform var(--eam-duration) var(--eam-ease),
    background-color var(--eam-duration) var(--eam-ease),
    border-color var(--eam-duration) var(--eam-ease);
}

.story-features li:hover {
  transform: translateX(4px);
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.12);
}

.feature-icon {
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  color: #d9c7a7;
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.story-features li div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.story-features strong {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.story-features li div span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.44);
}

.story-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.4);
  animation: reveal-up 0.9s var(--eam-ease) 0.5s both;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8fbfa8;
  box-shadow: 0 0 0 0 rgba(143, 191, 168, 0.5);
  animation: pulse 2.6s var(--eam-ease-soft) infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(143, 191, 168, 0.42); }
  50% { box-shadow: 0 0 0 7px rgba(143, 191, 168, 0); }
}

.story-meta {
  margin-left: auto;
  font-family: var(--eam-font-mono);
  color: rgba(255, 255, 255, 0.26);
}

/* ---------- Form panel ---------- */

.login-panel {
  position: relative;
  display: flex;
  background: linear-gradient(180deg, #fdfdfc 0%, #f7f5f1 100%);
}

.panel-inner {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 396px;
  margin: 0 auto;
  padding: 64px 40px 40px;
  animation: reveal-up 0.9s var(--eam-ease) 0.24s both;
}

.panel-heading {
  margin-bottom: 36px;
}

.panel-eyebrow {
  margin: 0 0 12px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.22em;
  color: var(--eam-bronze);
}

.panel-heading h2 {
  margin: 0 0 10px;
  font-size: 34px;
  font-weight: 600;
  letter-spacing: -0.035em;
  color: var(--eam-ink-900);
}

.panel-subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--eam-ink-400);
}

.login-form {
  width: 100%;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 7px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--eam-ink-500);
}

.login-input :deep(.el-input__wrapper) {
  height: 52px;
  padding: 0 15px;
  border-radius: var(--eam-radius-md);
  background-color: #f1f2f4;
  box-shadow: 0 0 0 1px transparent inset;
  transition: background-color var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration) var(--eam-ease);
}

.login-input :deep(.el-input__wrapper:hover) {
  background-color: #edeef1;
}

.login-input :deep(.el-input__wrapper.is-focus) {
  background-color: var(--eam-surface);
  box-shadow: 0 0 0 1px var(--eam-primary), 0 0 0 4px var(--eam-primary-ghost),
    var(--eam-shadow-xs);
}

.login-input :deep(.el-input__inner) {
  font-size: 14px;
}

.login-input :deep(.el-input__prefix) {
  color: var(--eam-ink-400);
}

.login-submit-item {
  margin-top: 28px;
  margin-bottom: 0 !important;
}

.login-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 52px;
  border: none;
  border-radius: var(--eam-radius-md);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: #f7f8fa;
  background-image: linear-gradient(180deg, #425b86 0%, #354b70 100%);
  box-shadow: 0 14px 28px rgba(47, 68, 102, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.16);
  transition: transform var(--eam-duration) var(--eam-ease),
    box-shadow var(--eam-duration) var(--eam-ease),
    background-image var(--eam-duration) var(--eam-ease);
}

.login-button:hover:not(.is-disabled) {
  transform: translateY(-1px);
  background-image: linear-gradient(180deg, #4a6492 0%, #3b547c 100%);
  box-shadow: 0 18px 34px rgba(47, 68, 102, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.login-button:active:not(.is-disabled) {
  transform: translateY(0);
  box-shadow: 0 8px 18px rgba(47, 68, 102, 0.2);
}

.login-button-icon {
  transition: transform var(--eam-duration) var(--eam-ease);
}

.login-button:hover .login-button-icon {
  transform: translateX(3px);
}

.login-error {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-top: 18px;
  padding: 11px 14px;
  border-radius: var(--eam-radius-sm);
  font-size: 12px;
  line-height: 1.6;
  color: #8f4747;
  background: var(--eam-danger-soft);
  border: 1px solid rgba(164, 87, 87, 0.16);
  animation: error-shake 0.45s var(--eam-ease);
}

.error-dot {
  flex: 0 0 auto;
  width: 6px;
  height: 6px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--eam-danger);
}

@keyframes error-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.login-alert-enter-active,
.login-alert-leave-active {
  transition: opacity var(--eam-duration) var(--eam-ease),
    transform var(--eam-duration) var(--eam-ease);
}

.login-alert-enter-from,
.login-alert-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.login-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 26px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--eam-ink-400);
}

.hint-line {
  width: 18px;
  height: 1px;
  background: var(--eam-line-strong);
}

.panel-footnote {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 48px;
  font-size: 10px;
  letter-spacing: 0.02em;
  color: var(--eam-ink-300);
}

@keyframes reveal-up {
  from {
    opacity: 0;
    transform: translate3d(0, 16px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

/* ---------- Responsive ---------- */

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 620px;
    min-height: auto;
  }

  .login-story {
    min-height: 230px;
    padding: 32px 36px;
  }

  .story-content h1 {
    font-size: 30px;
    margin-bottom: 0;
  }

  .story-eyebrow,
  .story-lede,
  .story-features {
    display: none;
  }

  .story-footer {
    margin-top: 40px;
  }

  .panel-inner {
    max-width: 440px;
    padding: 42px 36px 32px;
  }

  .panel-footnote {
    padding-top: 36px;
  }
}

@media (max-width: 560px) {
  .login-container {
    padding: 0;
  }

  .login-shell {
    min-height: 100vh;
    border: none;
    border-radius: 0;
  }

  .login-story {
    min-height: 190px;
    padding: 26px 24px;
  }

  .story-content h1 {
    font-size: 26px;
  }

  .panel-inner {
    padding: 34px 24px 28px;
  }

  .panel-heading h2 {
    font-size: 28px;
  }

  .panel-footnote {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
