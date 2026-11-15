<script setup lang="ts">
import { ChevronRight, Search } from '@lucide/vue'
import { computed, ref } from 'vue'
import type { DepartmentOption } from '@/api/departments'
import { flattenDepartments } from '@/lib/departments'
import AppInput from './AppInput.vue'
const props = defineProps<{ nodes: DepartmentOption[] }>()
const model = defineModel<number | 'unassigned'>()
const search = ref('')
const collapsed = ref(new Set<number>())
const rows = computed(() => {
  const all = flattenDepartments(props.nodes),
    term = search.value.trim().toLowerCase()
  if (term) {
    const ids = new Set<number>()
    for (const row of all.filter((item) => item.name.toLowerCase().includes(term))) {
      let current: typeof row | undefined = row
      while (current) {
        ids.add(current.id)
        current = all.find((item) => item.id === current?.parentId)
      }
    }
    return all.filter((item) => ids.has(item.id))
  }
  return all.filter((row) => {
    let parent = all.find((item) => item.id === row.parentId)
    while (parent) {
      if (collapsed.value.has(parent.id)) return false
      parent = all.find((item) => item.id === parent?.parentId)
    }
    return true
  })
})
function toggle(id: number) {
  const next = new Set(collapsed.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  collapsed.value = next
}
</script>
<template>
  <nav aria-label="部门筛选" class="space-y-2">
    <AppInput v-model="search" placeholder="搜索部门" aria-label="搜索部门"
      ><template #leading><Search :size="14" aria-hidden="true" /></template
    ></AppInput>
    <div class="max-h-[60vh] space-y-0.5 overflow-y-auto pt-2">
      <button
        type="button"
        class="w-full rounded-lg px-3 py-2 text-left text-[13px] hover:bg-brand/10 focus-visible:outline-2 focus-visible:outline-brand"
        :class="model === undefined ? 'bg-brand/10 font-semibold text-brand' : 'text-ink-muted'"
        :aria-current="model === undefined ? 'true' : undefined"
        @click="model = undefined"
      >
        全部部门
      </button>
      <button
        type="button"
        class="w-full rounded-lg px-3 py-2 text-left text-[13px] hover:bg-brand/10 focus-visible:outline-2 focus-visible:outline-brand"
        :class="model === 'unassigned' ? 'bg-brand/10 font-semibold text-brand' : 'text-ink-muted'"
        :aria-current="model === 'unassigned' ? 'true' : undefined"
        @click="model = 'unassigned'"
      >
        未分配部门
      </button>
      <div
        v-for="row in rows"
        :key="row.id"
        class="flex items-center rounded-lg"
        :class="model === row.id && 'bg-brand/10'"
        :style="{ paddingLeft: `${row.depth * 14}px` }"
      >
        <button
          v-if="row.children.length"
          type="button"
          class="grid h-8 w-6 shrink-0 place-items-center rounded text-ink-soft focus-visible:outline-2 focus-visible:outline-brand"
          :aria-label="`${collapsed.has(row.id) ? '展开' : '折叠'}${row.name}`"
          :disabled="!!search.trim()"
          :aria-expanded="!!search.trim() || !collapsed.has(row.id)"
          @click="toggle(row.id)"
        >
          <ChevronRight
            :size="13"
            class="transition-transform"
            :class="(search.trim() || !collapsed.has(row.id)) && 'rotate-90'"
            aria-hidden="true"
          /></button
        ><span v-else class="w-6 shrink-0" />
        <button
          type="button"
          class="min-w-0 flex-1 truncate rounded-lg py-2 pr-2 text-left text-[13px] hover:text-brand focus-visible:outline-2 focus-visible:outline-brand"
          :class="model === row.id ? 'font-semibold text-brand' : 'text-ink'"
          :aria-current="model === row.id ? 'true' : undefined"
          :title="row.path"
          @click="model = row.id"
        >
          {{ row.name }}<span v-if="row.status === 'DISABLED'" class="text-ink-soft">（停用）</span>
        </button>
      </div>
      <p v-if="search && !rows.length" class="py-4 text-center text-xs text-ink-soft">
        暂无匹配部门
      </p>
    </div>
  </nav>
</template>
