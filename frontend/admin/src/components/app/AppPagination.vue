<script setup lang="ts">
import { ChevronLeft, ChevronRight } from '@lucide/vue'
import { computed } from 'vue'

import AppSelect from './AppSelect.vue'

const props = withDefaults(
  defineProps<{
    page: number
    pageSize: number
    total: number
    loading?: boolean
    pageSizes?: number[]
  }>(),
  {
    loading: false,
    pageSizes: () => [10, 20, 50, 100],
  },
)

const emit = defineEmits<{
  'update:page': [page: number]
  'update:page-size': [pageSize: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const rangeStart = computed(() =>
  props.total === 0 ? 0 : Math.min((props.page - 1) * props.pageSize + 1, props.total),
)
const rangeEnd = computed(() => Math.min(props.page * props.pageSize, props.total))

type PageItem = number | 'ellipsis'

const pageItems = computed<PageItem[]>(() => {
  const count = totalPages.value
  if (count <= 7) {
    return Array.from({ length: count }, (_, index) => index + 1)
  }

  const items: PageItem[] = [1]
  if (props.page > 4) {
    items.push('ellipsis')
  }

  const start = Math.max(2, props.page - 1)
  const end = Math.min(count - 1, props.page + 1)
  for (let page = start; page <= end; page += 1) {
    items.push(page)
  }

  if (props.page < count - 3) {
    items.push('ellipsis')
  }
  items.push(count)
  return items
})

function go(page: number): void {
  if (page < 1 || page > totalPages.value || page === props.page || props.loading) {
    return
  }
  emit('update:page', page)
}

function changePageSize(value: string | number | undefined): void {
  if (value === undefined) {
    return
  }
  const size = Number(value)
  if (!Number.isFinite(size) || size <= 0) {
    return
  }
  if (size !== props.pageSize) {
    emit('update:page-size', size)
  }
}
</script>

<template>
  <div class="flex flex-wrap items-center justify-between gap-3 text-[12px]">
    <div class="flex items-center gap-2 text-ink-soft">
      <span>第 {{ rangeStart }}–{{ rangeEnd }} 条</span>
      <span class="text-ink-faint">·</span>
      <span class="tnum">{{ page }} / {{ totalPages }}</span>
    </div>

    <div class="flex items-center gap-2">
      <label class="flex items-center gap-1.5 text-ink-soft">
        每页
        <span class="w-[70px]">
          <AppSelect
            :model-value="pageSize"
            size="sm"
            aria-label="每页条数"
            :options="pageSizes.map((size) => ({ label: String(size), value: size }))"
            @update:model-value="changePageSize"
          />
        </span>
      </label>

      <button
        type="button"
        class="inline-flex h-8 w-8 items-center justify-center rounded-[8px] text-ink-muted transition-colors hover:bg-ink/5 hover:text-ink disabled:pointer-events-none disabled:opacity-35"
        aria-label="上一页"
        :disabled="page <= 1 || loading"
        @click="go(page - 1)"
      >
        <ChevronLeft :size="15" aria-hidden="true" />
      </button>

      <template v-for="(item, index) in pageItems" :key="`${item}-${index}`">
        <span v-if="item === 'ellipsis'" class="px-0.5 text-ink-faint">…</span>
        <button
          v-else
          type="button"
          class="h-8 min-w-8 rounded-[8px] px-2 text-[12px] font-medium transition-colors"
          :class="
            item === page
              ? 'bg-[var(--action-bg)] text-[var(--action-ink)] shadow-[var(--shadow-e1)]'
              : 'text-ink-muted hover:bg-ink/5 hover:text-ink'
          "
          :aria-current="item === page ? 'page' : undefined"
          @click="go(item)"
        >
          {{ item }}
        </button>
      </template>

      <button
        type="button"
        class="inline-flex h-8 w-8 items-center justify-center rounded-[8px] text-ink-muted transition-colors hover:bg-ink/5 hover:text-ink disabled:pointer-events-none disabled:opacity-35"
        aria-label="下一页"
        :disabled="page >= totalPages || loading"
        @click="go(page + 1)"
      >
        <ChevronRight :size="15" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>
