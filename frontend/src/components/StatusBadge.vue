<template>
  <span class="status-badge" :class="`status-${tone}`">{{ label }}</span>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}>()
</script>
