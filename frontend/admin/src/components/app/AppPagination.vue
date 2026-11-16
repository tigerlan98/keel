<script setup lang="ts">
import { ChevronLeft, ChevronRight } from '@lucide/vue'
import { computed } from 'vue'
import { vPress } from '@/directives/press'

import AppSelect from './AppSelect.vue'

const props = withDefaults(
  defineProps<{
    page: number
    pageSize: number
    total: number
    loading?: boolean
    pageSizes?: number[]
    appearance?: 'default' | 'user-list'
  }>(),
  {
    loading: false,
    pageSizes: () => [10, 20, 50, 100],
    appearance: 'default',
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
  <nav
    aria-label="分页"
    class="flex flex-wrap items-center justify-between gap-3 text-[12px]"
    :class="appearance === 'user-list' && 'user-list-pagination'"
  >
    <div v-if="appearance === 'user-list'" class="pagination-summary text-ink-soft">
      共 <strong class="text-ink">{{ total }}</strong> 条，当前
      <strong class="tnum text-ink">{{ rangeStart }}–{{ rangeEnd }}</strong>
    </div>
    <div v-else class="flex items-center gap-2 text-ink-soft">
      <span>第 {{ rangeStart }}–{{ rangeEnd }} 条</span>
      <span class="text-ink-faint">·</span>
      <span class="tnum">{{ page }} / {{ totalPages }}</span>
    </div>

    <div class="flex items-center gap-2">
      <label class="flex items-center gap-1.5 text-ink-soft">
        <span v-if="appearance === 'default'">每页</span>
        <span :class="appearance === 'user-list' ? 'w-[110px]' : 'w-[70px]'">
          <AppSelect
            :model-value="pageSize"
            size="sm"
            aria-label="每页条数"
            :options="
              pageSizes.map((size) => ({
                label: appearance === 'user-list' ? `${size} 条/页` : String(size),
                value: size,
              }))
            "
            :disabled="loading"
            @update:model-value="changePageSize"
          />
        </span>
      </label>

      <button
        v-press
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
          v-press
          type="button"
          class="h-8 min-w-8 rounded-[8px] px-2 text-[12px] font-medium transition-colors"
          :class="
            item === page
              ? 'bg-brand/10 text-brand font-semibold'
              : 'text-ink-muted hover:bg-ink/5 hover:text-ink'
          "
          :aria-current="item === page ? 'page' : undefined"
          :aria-label="`第 ${item} 页`"
          :disabled="loading"
          @click="go(item)"
        >
          {{ item }}
        </button>
      </template>

      <button
        v-press
        type="button"
        class="inline-flex h-8 w-8 items-center justify-center rounded-[8px] text-ink-muted transition-colors hover:bg-ink/5 hover:text-ink disabled:pointer-events-none disabled:opacity-35"
        aria-label="下一页"
        :disabled="page >= totalPages || loading"
        @click="go(page + 1)"
      >
        <ChevronRight :size="15" aria-hidden="true" />
      </button>
    </div>
  </nav>
</template>

<style scoped>
.user-list-pagination {
  min-height: 38px;
  font-size: 12px;
  font-weight: 400;
}

.user-list-pagination .pagination-summary strong {
  font-weight: 650;
}

.user-list-pagination button {
  font-size: 12.5px;
  font-weight: 600;
}

.user-list-pagination :deep(.app-select-trigger) {
  height: 36px;
  border-radius: 12px;
  font-size: 13px;
}

.user-list-pagination button[aria-current='page'] {
  border: 1px solid var(--surface-edge);
  background: var(--pill-surface);
  box-shadow: var(--shadow-e1);
  color: var(--ink);
}

.user-list-pagination button:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}

.user-list-pagination button:disabled {
  pointer-events: none;
  opacity: 0.35;
}
</style>
