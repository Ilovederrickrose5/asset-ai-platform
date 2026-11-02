<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">403</h1>
      <p class="error-message">无权访问该页面</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/home')
}
</script>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}

.error-content {
  text-align: center;
}

.error-code {
  font-size: 6rem;
  font-weight: 700;
  color: #f56c6c;
  margin: 0 0 20px 0;
}

.error-message {
  font-size: 1.5rem;
  color: #606266;
  margin: 0 0 30px 0;
}
</style>
