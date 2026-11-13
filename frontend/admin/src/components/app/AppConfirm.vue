<script setup lang="ts">
import AppButton from './AppButton.vue'
import AppDialog from './AppDialog.vue'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    message: string
    confirmText?: string
    cancelText?: string
    tone?: 'danger' | 'primary'
    loading?: boolean
  }>(),
  {
    confirmText: '确认',
    cancelText: '取消',
    tone: 'danger',
    loading: false,
  },
)

const emit = defineEmits<{ close: []; confirm: [] }>()
</script>

<template>
  <AppDialog :open="open" :title="title" size="sm" @close="emit('close')">
    <p class="text-[13px] leading-6 text-ink-muted">{{ message }}</p>
    <template #footer>
      <AppButton variant="outline" @click="emit('close')">{{ cancelText }}</AppButton>
      <AppButton
        :variant="tone === 'danger' ? 'danger' : 'primary'"
        :loading="loading"
        @click="emit('confirm')"
      >
        {{ confirmText }}
      </AppButton>
    </template>
  </AppDialog>
</template>
