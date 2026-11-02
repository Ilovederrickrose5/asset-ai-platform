<template>
  <div class="page-topbar">
    <div class="page-topbar__inner">
      <div class="page-head">
        <button
          v-if="showBack"
          class="page-back"
          type="button"
          aria-label="返回首页"
          @click="goBack"
        >
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="page-heading">
          <p v-if="eyebrow" class="eam-eyebrow">{{ eyebrow }}</p>
          <h1>{{ title }}</h1>
          <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
        </div>
      </div>

      <div v-if="$slots.actions || $slots.default" class="page-actions">
        <slot name="actions" />
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()

const goBack = () => {
  router.push('/dashboard')
}
</script>

<style scoped>
.page-topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(248, 249, 251, 0.82);
  border-bottom: 1px solid var(--eam-line-soft);
  backdrop-filter: blur(18px) saturate(1.15);
  -webkit-backdrop-filter: blur(18px) saturate(1.15);
}

.page-topbar__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: 100%;
  max-width: 1480px;
  margin: 0 auto;
  padding: 18px 32px;
}

.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.page-back {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  color: var(--eam-ink-500);
  background: var(--eam-surface);
  border: 1px solid var(--eam-line);
  border-radius: var(--eam-radius-sm);
  cursor: pointer;
  box-shadow: var(--eam-shadow-xs);
  transition: color var(--eam-duration-fast) var(--eam-ease),
    border-color var(--eam-duration-fast) var(--eam-ease),
    transform var(--eam-duration-fast) var(--eam-ease),
    box-shadow var(--eam-duration-fast) var(--eam-ease);
}

.page-back:hover {
  color: var(--eam-primary);
  border-color: rgba(61, 86, 128, 0.24);
  transform: translateX(-2px);
  box-shadow: var(--eam-shadow-sm);
}

.page-heading {
  min-width: 0;
}

.page-heading h1 {
  margin: 2px 0 0;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: -0.03em;
  color: var(--eam-ink-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--eam-ink-400);
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

@media (max-width: 900px) {
  .page-topbar__inner {
    padding: 14px 18px;
  }

  .page-heading h1 {
    font-size: 20px;
  }

  .page-subtitle {
    display: none;
  }
}

@media (max-width: 560px) {
  .page-heading h1 {
    font-size: 18px;
  }

  .page-back {
    width: 34px;
    height: 34px;
  }
}
</style>
