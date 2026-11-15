<script setup lang="ts">
import { Search } from '@lucide/vue'
import { computed, ref } from 'vue'

import type { RoleSummary } from '@/api/types'

import AppButton from './AppButton.vue'
import AppInput from './AppInput.vue'

const props = withDefaults(
  defineProps<{
    roles: RoleSummary[]
    allowSelectAll?: boolean
  }>(),
  { allowSelectAll: true },
)

const selected = defineModel<number[]>({ default: () => [] })
const search = ref('')

const filtered = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) {
    return props.roles
  }
  return props.roles.filter(
    (role) =>
      role.name.toLowerCase().includes(keyword) || role.code.toLowerCase().includes(keyword),
  )
})

const allSelected = computed(
  () =>
    filtered.value.length > 0 && filtered.value.every((role) => selected.value.includes(role.id)),
)

function toggleRole(id: number): void {
  const index = selected.value.indexOf(id)
  if (index >= 0) {
    selected.value = selected.value.filter((item) => item !== id)
  } else {
    selected.value = [...selected.value, id]
  }
}

function toggleAll(): void {
  const ids = filtered.value.map((role) => role.id)
  if (allSelected.value) {
    selected.value = selected.value.filter((id) => !ids.includes(id))
  } else {
    selected.value = Array.from(new Set([...selected.value, ...ids]))
  }
}
</script>

<template>
  <div class="overflow-hidden rounded-xl border border-line-soft bg-[var(--input-surface)]">
    <div class="flex items-center gap-2 border-b border-line-soft p-2">
      <div class="min-w-0 flex-1">
        <AppInput v-model="search" placeholder="搜索角色名称或编码">
          <template #leading>
            <Search :size="14" aria-hidden="true" />
          </template>
        </AppInput>
      </div>
      <AppButton
        v-if="allowSelectAll"
        variant="ghost"
        size="sm"
        :disabled="filtered.length === 0"
        @click="toggleAll"
      >
        {{ allSelected ? '取消全选' : '全选' }}
      </AppButton>
    </div>

    <div class="max-h-44 space-y-0.5 overflow-y-auto p-1.5">
      <label
        v-for="role in filtered"
        :key="role.id"
        class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-[12px] transition-colors hover:bg-ink/5"
        :class="selected.includes(role.id) && 'bg-brand/5'"
      >
        <input
          type="checkbox"
          class="h-3.5 w-3.5 shrink-0 accent-[var(--brand)]"
          :checked="selected.includes(role.id)"
          @change="toggleRole(role.id)"
        />
        <span class="min-w-0 flex-1 truncate font-medium text-ink">{{ role.name }}</span>
        <span class="code-chip">{{ role.code }}</span>
      </label>

      <p v-if="filtered.length === 0" class="px-2 py-4 text-center text-[12px] text-ink-soft">
        没有匹配的角色
      </p>
    </div>
  </div>
</template>
