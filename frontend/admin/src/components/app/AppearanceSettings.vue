<script setup lang="ts">
import { Contrast, Monitor, Moon, Sun } from '@lucide/vue'
import { ref } from 'vue'
import AppButton from './AppButton.vue'
import AppDialog from './AppDialog.vue'
import { vPress } from '@/directives/press'
import { setTheme, themeMode } from '@/lib/appearance'

const open = ref(false)
const options = [
  { value: 'light', label: '浅色', icon: Sun },
  { value: 'dark', label: '深色', icon: Moon },
  { value: 'auto', label: '跟随系统', icon: Monitor },
] as const
</script>
<template>
  <button
    v-press
    type="button"
    class="appearance-trigger topbar-icon-button inline-flex"
    aria-label="外观设置"
    title="外观设置"
    @click="open = true"
  >
    <Contrast :size="17" aria-hidden="true" />
  </button>
  <AppDialog :open="open" title="外观设置" size="sm" @close="open = false">
    <p class="mb-4 text-[13px] leading-6 text-ink-muted">
      选择界面主题。跟随系统时，将随系统外观自动切换。
    </p>
    <div role="group" aria-label="主题" class="grid grid-cols-3 gap-2">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        class="theme-option"
        :aria-pressed="themeMode === option.value"
        @click="setTheme(option.value)"
      >
        <component :is="option.icon" :size="22" aria-hidden="true" /><span>{{ option.label }}</span>
      </button>
    </div>
    <p class="mt-4 text-[12px] text-ink-soft">外观偏好仅保存在当前浏览器。</p>
    <template #footer><AppButton @click="open = false">完成</AppButton></template>
  </AppDialog>
</template>
<style scoped>
.appearance-trigger {
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  color: var(--ink-soft);
  transition: background-color 180ms ease;
}
.appearance-trigger:hover {
  background: var(--hover-surface);
}
.appearance-trigger:focus-visible,
.theme-option:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}
.theme-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 18px 6px;
  border: 1px solid var(--line-strong);
  border-radius: 12px;
  color: var(--ink-muted);
  background: var(--input-surface);
  font-size: 13px;
  transition:
    color 180ms ease,
    border-color 180ms ease,
    background-color 180ms ease;
}
.theme-option[aria-pressed='true'] {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand-strong);
}
</style>
