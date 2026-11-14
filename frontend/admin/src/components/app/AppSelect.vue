<script setup lang="ts">
import { Check, ChevronDown } from '@lucide/vue'
import {
  SelectContent,
  SelectIcon,
  SelectItem,
  SelectItemIndicator,
  SelectItemText,
  SelectPortal,
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectViewport,
} from 'reka-ui'
import { computed } from 'vue'

export interface SelectOption {
  label: string
  value: string | number
  disabled?: boolean
  depth?: number
}
defineOptions({ inheritAttrs: false })
const props = withDefaults(
  defineProps<{
    options: readonly SelectOption[]
    invalid?: boolean
    disabled?: boolean
    size?: 'sm' | 'md'
    placeholder?: string
  }>(),
  { invalid: false, disabled: false, size: 'md', placeholder: '请选择' },
)
const model = defineModel<string | number>()
// 编码后交给 Reka，空字符串选项和数字值仍保持原来的业务语义。
const encode = (value: string | number) => JSON.stringify([typeof value, value])
const selected = computed(() => (model.value === undefined ? undefined : encode(model.value)))
function update(value: unknown): void {
  const option = props.options.find((option) => encode(option.value) === value)
  if (option) model.value = option.value
}
</script>
<template>
  <SelectRoot :model-value="selected" :disabled="disabled" @update:model-value="update">
    <SelectTrigger
      v-bind="$attrs"
      class="app-select-trigger"
      :class="{ 'is-invalid': invalid, 'is-small': size === 'sm' }"
      :aria-invalid="invalid || undefined"
    >
      <SelectValue :placeholder="placeholder" class="select-value" />
      <SelectIcon as-child
        ><ChevronDown :size="15" class="select-caret" aria-hidden="true"
      /></SelectIcon>
    </SelectTrigger>
    <SelectPortal>
      <SelectContent
        class="app-select-content"
        position="popper"
        align="start"
        :side-offset="6"
        :collision-padding="8"
        :avoid-collisions="true"
      >
        <SelectViewport class="app-select-options">
          <SelectItem
            v-for="option in options"
            :key="encode(option.value)"
            :value="encode(option.value)"
            :disabled="option.disabled"
            class="app-select-option"
            :style="option.depth ? { paddingLeft: `${10 + option.depth * 16}px` } : undefined"
          >
            <SelectItemText class="select-label">{{ option.label }}</SelectItemText>
            <SelectItemIndicator as-child
              ><Check :size="14" class="shrink-0" aria-hidden="true"
            /></SelectItemIndicator>
          </SelectItem>
          <div v-if="!options.length" class="select-empty">暂无选项</div>
        </SelectViewport>
      </SelectContent>
    </SelectPortal>
  </SelectRoot>
</template>
<style>
/* 浮层通过 Portal 与多层原语转发，使用组件专属类名定义全局样式，避免作用域属性丢失。 */
.app-select-trigger {
  display: inline-flex;
  width: 100%;
  height: 36px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: var(--input-surface);
  color: var(--ink);
  padding: 0 12px;
  text-align: left;
  font-size: 13px;
  outline: none;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease;
  cursor: pointer;
}
.app-select-trigger.is-small {
  height: 32px;
}
.app-select-trigger:hover {
  border-color: var(--line-strong);
}
.app-select-trigger:focus-visible,
.app-select-trigger[data-state='open'] {
  border-color: var(--brand);
  box-shadow: 0 0 0 2px var(--brand-soft);
}
.app-select-trigger.is-invalid {
  border-color: var(--destructive);
}
.app-select-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.select-value {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.select-caret {
  flex: none;
  color: var(--ink-soft);
}
.app-select-content {
  z-index: 120;
  min-width: max(160px, var(--reka-select-trigger-width));
  max-width: calc(100vw - 16px);
  max-height: min(300px, var(--reka-select-content-available-height));
  overflow: hidden;
  padding: 6px;
  border-radius: 16px;
  border: 1px solid var(--glass-edge);
  background: var(--floating-surface);
  box-shadow: var(--shadow-e3), var(--inset-edge);
  backdrop-filter: blur(28px) saturate(1.08);
  color: var(--ink);
  transform-origin: var(--reka-select-content-transform-origin);
}
.app-select-content[data-state='open'] {
  animation: select-open 320ms cubic-bezier(0.16, 1, 0.3, 1);
}
.app-select-content[data-state='closed'] {
  animation: select-close 140ms cubic-bezier(0.55, 0.085, 0.68, 0.53);
}
.app-select-options {
  max-height: min(280px, var(--reka-select-content-available-height));
  overflow-y: auto;
  overscroll-behavior: contain;
}
.app-select-option {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border-radius: 8px;
  outline: none;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  transition:
    background-color 150ms ease,
    color 150ms ease;
}
.app-select-option[data-highlighted] {
  background: var(--hover-surface);
}
.app-select-option[data-state='checked'] {
  color: var(--brand);
  font-weight: 620;
}
.app-select-option[data-disabled] {
  opacity: 0.45;
  cursor: not-allowed;
}
.select-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.select-empty {
  padding: 14px 10px;
  font-size: 12px;
  color: var(--ink-soft);
  text-align: center;
}
@keyframes select-open {
  from {
    opacity: 0;
    transform: translateY(-6px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
@keyframes select-close {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-4px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .app-select-content {
    animation: none !important;
  }
}
@media (prefers-reduced-transparency: reduce) {
  .app-select-content {
    background: var(--surface-solid);
    backdrop-filter: none;
  }
}
</style>
