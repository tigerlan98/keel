<script setup lang="ts">
import { Check, Search, Shield, X } from '@lucide/vue'
import { DialogContent, DialogOverlay, DialogPortal, DialogRoot, DialogTitle } from 'reka-ui'
import { computed, ref } from 'vue'
import type { ResourceNode, RoleSummary } from '@/api/types'
import AppButton from './AppButton.vue'
import AppErrorState from './AppErrorState.vue'
const props = defineProps<{
  role: RoleSummary | null
  options: { node: ResourceNode; depth: number }[]
  loading: boolean
  saving: boolean
  error: string
}>()
const selected = defineModel<number[]>({ required: true })
const emit = defineEmits<{ close: []; save: []; retry: [] }>()
const query = ref('')
const filtered = computed(() =>
  props.options.filter(({ node }) =>
    `${node.name} ${node.code}`.toLowerCase().includes(query.value.toLowerCase()),
  ),
)
const counts = computed(() => ({
  pages: props.options.filter(
    ({ node }) => node.type === 'MENU' && selected.value.includes(node.id),
  ).length,
  buttons: props.options.filter(
    ({ node }) => node.type === 'BUTTON' && selected.value.includes(node.id),
  ).length,
}))
const labels = { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮', API: '接口' }
function toggle(id: number): void {
  if (props.saving || props.loading || props.role?.code === 'SUPER_ADMIN') return
  selected.value = selected.value.includes(id)
    ? selected.value.filter((value) => value !== id)
    : [...selected.value, id]
}
function close(): void {
  if (!props.saving) {
    query.value = ''
    emit('close')
  }
}
</script>
<template>
  <DialogRoot
    :open="!!role"
    @update:open="
      (open) => {
        if (!open) close()
      }
    "
  >
    <DialogPortal>
      <DialogOverlay class="dialog-overlay fixed inset-0 z-[100] bg-[var(--overlay-bg)]" />
      <DialogContent
        class="role-permission-drawer fixed inset-y-0 right-0 z-[101] flex w-[min(560px,100vw)] flex-col border-l border-line-strong bg-surface text-ink shadow-[var(--shadow-e3)] outline-none"
        @escape-key-down="saving && $event.preventDefault()"
        @pointer-down-outside="saving && $event.preventDefault()"
      >
        <header
          class="flex items-start justify-between gap-4 border-b border-line-soft px-6 pb-5 pt-6"
        >
          <div>
            <DialogTitle class="text-[17px] font-bold">分配权限 · {{ role?.name }}</DialogTitle>
            <p class="mt-1 text-[12px] text-ink-soft">{{ role?.code }}</p>
          </div>
          <button
            type="button"
            aria-label="关闭授权"
            :disabled="saving"
            class="grid h-8 w-8 place-items-center rounded-lg text-ink-soft hover:bg-ink/5 focus-visible:outline-2 focus-visible:outline-brand"
            @click="close"
          >
            <X :size="16" />
          </button>
        </header>
        <div class="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          <div class="mb-5 flex flex-wrap items-center justify-between gap-2">
            <span
              class="rounded-full border border-line-soft bg-ink/5 px-3 py-1.5 text-[12px] font-semibold"
              >菜单与按钮</span
            ><span class="text-[11px] text-ink-soft"
              >已选 {{ counts.pages }} 个页面 · {{ counts.buttons }} 个按钮</span
            >
          </div>
          <div v-if="loading" role="status" class="space-y-4 py-3">
            <span
              v-for="index in 8"
              :key="index"
              class="block h-4 animate-pulse rounded-lg bg-ink/5"
              :style="{ width: `${40 + ((index * 13) % 45)}%` }"
            /><span class="sr-only">正在加载权限</span>
          </div>
          <AppErrorState
            v-else-if="error && !options.length"
            :message="error"
            @retry="emit('retry')"
          />
          <template v-else>
            <label
              class="mb-4 flex items-center gap-2 rounded-xl border border-line-soft bg-[var(--input-surface)] px-3 text-ink-soft"
              ><Search :size="14" /><input
                v-model="query"
                class="h-9 min-w-0 flex-1 bg-transparent text-[12px] outline-none"
                placeholder="搜索菜单或权限编码"
                aria-label="搜索授权资源"
            /></label>
            <p
              v-if="role?.code === 'SUPER_ADMIN'"
              class="mb-4 flex items-center gap-2 rounded-xl bg-brand/5 px-3 py-3 text-[12px] text-ink-muted"
            >
              <Shield :size="15" />内置超级管理员拥有全部权限，此处只读。
            </p>
            <div class="overflow-hidden rounded-xl border border-line-soft">
              <label
                v-for="item in filtered"
                :key="item.node.id"
                class="flex min-h-11 items-center gap-2.5 border-b border-line-soft py-2 pr-3 last:border-b-0 hover:bg-ink/5"
                :style="{ paddingLeft: `${item.depth * 18 + 12}px` }"
                ><input
                  type="checkbox"
                  :checked="role?.code === 'SUPER_ADMIN' || selected.includes(item.node.id)"
                  :disabled="saving || role?.code === 'SUPER_ADMIN'"
                  class="h-3.5 w-3.5 shrink-0 accent-[var(--brand)]"
                  @change="toggle(item.node.id)"
                /><span class="min-w-0 flex-1"
                  ><span class="block text-[12px] font-medium">{{ item.node.name }}</span
                  ><span class="block break-all text-[10px] text-ink-soft">{{
                    item.node.code
                  }}</span></span
                ><span class="rounded bg-ink/5 px-1.5 py-0.5 text-[10px] text-ink-soft">{{
                  labels[item.node.type]
                }}</span></label
              >
              <p v-if="!filtered.length" class="px-4 py-10 text-center text-[12px] text-ink-soft">
                暂无匹配的资源
              </p>
            </div>
            <p
              v-if="error"
              role="alert"
              class="mt-4 rounded-xl bg-destructive/5 px-3 py-3 text-[12px] text-destructive"
            >
              {{ error }}
            </p>
          </template>
        </div>
        <footer class="flex shrink-0 justify-end gap-2 border-t border-line-soft px-6 py-4">
          <AppButton variant="ghost" :disabled="saving" @click="close">取消</AppButton
          ><AppButton
            :icon="Check"
            :loading="saving"
            :disabled="loading || (!options.length && !!error) || role?.code === 'SUPER_ADMIN'"
            @click="emit('save')"
            >保存权限</AppButton
          >
        </footer>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
