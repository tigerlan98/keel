<script setup lang="ts">
import { ChevronDown, KeyRound, LogOut } from '@lucide/vue'
import {
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuTrigger,
} from 'reka-ui'
import { computed, ref } from 'vue'

const props = defineProps<{
  name: string
  username: string
  superAdmin?: boolean
}>()

const emit = defineEmits<{ changePassword: []; logout: [] }>()
const openingDialog = ref(false)

function selectAction(action: 'changePassword' | 'logout'): void {
  openingDialog.value = true
  if (action === 'changePassword') emit('changePassword')
  else emit('logout')
}

function closeAutoFocus(event: InstanceType<typeof globalThis.Event>): void {
  if (openingDialog.value) event.preventDefault()
  openingDialog.value = false
}

const initials = computed(() => props.name.trim().slice(0, 1).toUpperCase() || 'U')
const roleLabel = computed(() => (props.superAdmin ? '超管' : '管理员'))
</script>

<template>
  <DropdownMenuRoot>
    <DropdownMenuTrigger as-child>
      <button
        type="button"
        class="flex h-10 items-center gap-[9px] rounded-full py-1 pl-1 pr-2.5 text-left outline-none transition-colors hover:bg-ink/5 focus-visible:ring-2 focus-visible:ring-brand/30 data-[state=open]:bg-ink/5"
        :aria-label="`用户菜单：${name}`"
      >
        <span
          class="grid h-8 w-8 place-items-center rounded-full bg-[#bf593b] text-[12px] font-semibold text-white"
        >
          {{ initials }}
        </span>
        <span class="hidden min-w-0 sm:block">
          <span
            class="block max-w-[120px] truncate text-[12.5px] font-[640] leading-[1.2] text-ink"
          >
            {{ name }}
          </span>
          <span class="block text-[11px] leading-[1.2] text-ink-soft">{{ roleLabel }}</span>
        </span>
        <ChevronDown :size="14" :stroke-width="1.6" class="text-ink-soft" aria-hidden="true" />
      </button>
    </DropdownMenuTrigger>

    <DropdownMenuPortal>
      <DropdownMenuContent
        align="end"
        :side-offset="8"
        :collision-padding="12"
        class="user-menu-content z-[120] w-[220px] max-w-[calc(100vw-24px)] rounded-2xl p-1.5 text-[13px] text-ink"
        @close-auto-focus="closeAutoFocus"
      >
        <div class="mb-1.5 border-b border-line-soft px-2.5 pb-3 pt-2.5">
          <p class="truncate font-[680]">{{ name }}</p>
          <p class="truncate text-[12px] text-ink-soft">@{{ username }}</p>
        </div>
        <DropdownMenuItem
          class="flex h-9 cursor-pointer items-center gap-2.5 rounded-lg px-2.5 text-left outline-none transition-colors data-[highlighted]:bg-[var(--hover-surface)]"
          @select="selectAction('changePassword')"
        >
          <KeyRound :size="16" :stroke-width="1.6" aria-hidden="true" />
          修改密码
        </DropdownMenuItem>
        <DropdownMenuItem
          class="flex h-9 cursor-pointer items-center gap-2.5 rounded-lg px-2.5 text-left text-[#e0443b] outline-none transition-colors data-[highlighted]:bg-[var(--hover-surface)]"
          @select="selectAction('logout')"
        >
          <LogOut :size="16" :stroke-width="1.6" aria-hidden="true" />
          退出登录
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>

<style>
.user-menu-content {
  border: 1px solid var(--glass-edge);
  background: rgba(246, 248, 247, 0.86);
  box-shadow:
    var(--shadow-e3),
    inset 0 0 0 1px rgba(120, 132, 135, 0.13),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(28px) saturate(1.08);
}

.dark .user-menu-content {
  background: rgba(30, 34, 37, 0.9);
  box-shadow:
    var(--shadow-e3),
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

@media (prefers-reduced-transparency: reduce) {
  .user-menu-content {
    background: var(--surface-solid);
    backdrop-filter: none;
  }
}
</style>
