<template>
  <span class="time-text">{{ formattedTime }}</span>
</template>

<script>
export default {
  name: 'TimeFormatter',
  props: {
    time: {
      type: [String, Date],
      default: null
    },
    fallback: {
      type: String,
      default: '-'
    }
  },
  computed: {
    formattedTime() {
      if (!this.time) return this.fallback
      const date = new Date(this.time)
      if (Number.isNaN(date.getTime())) return this.fallback
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.time-text {
  font-size: 12.5px;
  color: var(--eam-ink-400);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>
