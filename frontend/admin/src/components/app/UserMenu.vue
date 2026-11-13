<script setup lang="ts">
import { ChevronDown, KeyRound, LogOut } from '@lucide/vue'
import {
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from 'reka-ui'
import { computed } from 'vue'

const props = defineProps<{
  name: string
  username: string
  superAdmin?: boolean
}>()

const emit = defineEmits<{ changePassword: []; logout: [] }>()

const initials = computed(() => props.name.trim().slice(0, 1).toUpperCase() || 'U')
const roleLabel = computed(() => (props.superAdmin ? '超管' : '管理员'))
</script>

<template>
  <DropdownMenuRoot>
    <DropdownMenuTrigger as-child>
      <button
        type="button"
        class="flex h-10 items-center gap-[9px] rounded-full py-1 pl-1 pr-2.5 text-left outline-none transition-colors hover:bg-ink/5 focus-visible:ring-2 focus-visible:ring-brand/30"
        :aria-label="`用户菜单：${name}`"
      >
        <span
          class="grid h-8 w-8 place-items-center rounded-full bg-[#bf593b] text-[12px] font-semibold text-white"
        >
          {{ initials }}
        </span>
        <span class="hidden min-w-0 sm:block">
          <span class="block max-w-[120px] truncate text-[12px] font-semibold leading-4 text-ink">
            {{ name }}
          </span>
          <span class="block text-[10px] leading-4 text-ink-soft">{{ roleLabel }}</span>
        </span>
        <ChevronDown :size="13" class="text-ink-soft" aria-hidden="true" />
      </button>
    </DropdownMenuTrigger>

    <DropdownMenuPortal>
      <DropdownMenuContent
        align="end"
        :side-offset="8"
        class="z-[80] min-w-[196px] rounded-xl border border-line-strong bg-[var(--floating-surface)] p-1.5 shadow-[var(--shadow-e3)] backdrop-blur-xl"
      >
        <div class="px-2.5 py-2">
          <p class="truncate text-[12px] font-semibold text-ink">{{ name }}</p>
          <p class="truncate text-[10.5px] text-ink-soft">@{{ username }}</p>
        </div>
        <DropdownMenuSeparator class="my-1 h-px bg-line-soft" />
        <DropdownMenuItem
          class="flex cursor-pointer items-center gap-2 rounded-lg px-2.5 py-2 text-[12px] text-ink outline-none data-[highlighted]:bg-brand/10 data-[highlighted]:text-brand-strong"
          @select="emit('changePassword')"
        >
          <KeyRound :size="14" aria-hidden="true" />
          修改密码
        </DropdownMenuItem>
        <DropdownMenuItem
          class="flex cursor-pointer items-center gap-2 rounded-lg px-2.5 py-2 text-[12px] text-destructive outline-none data-[highlighted]:bg-destructive/10"
          @select="emit('logout')"
        >
          <LogOut :size="14" aria-hidden="true" />
          退出登录
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>
