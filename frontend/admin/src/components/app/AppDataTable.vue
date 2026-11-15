<script setup lang="ts" generic="T extends object">
import AppEmptyState from './AppEmptyState.vue'
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { revealRows } from '@/lib/motion'

interface Column {
  key: string
  label: string
  align?: 'left' | 'right'
  width?: string
  sticky?: boolean
}

const props = withDefaults(
  defineProps<{
    columns: Column[]
    rows: T[]
    rowKey?: string
    loading?: boolean
    minWidth?: number
    emptyTitle?: string
    emptyDescription?: string
    animationKey?: string
  }>(),
  {
    rowKey: 'id',
    loading: false,
    minWidth: 900,
    emptyTitle: '暂无数据',
    emptyDescription: '',
    animationKey: undefined,
  },
)

function valueOf(row: T, key: string): unknown {
  return (row as Record<string, unknown>)[key]
}

function cellText(row: T, key: string): string {
  const value = valueOf(row, key)
  return value === null || value === undefined || value === '' ? '—' : String(value)
}

function rowKeyValue(row: T, index: number, key: string): string {
  return String(valueOf(row, key) ?? index)
}

const body = ref<InstanceType<typeof globalThis.HTMLTableSectionElement> | null>(null)
let animations: InstanceType<typeof globalThis.Animation>[] = []
let lastSignature: string | undefined
let generation = 0
watch(
  [() => props.rows, () => props.loading],
  async () => {
    const current = ++generation
    if (props.loading) {
      animations.forEach((animation) => animation.cancel())
      return
    }
    const signature =
      props.animationKey ??
      props.rows.map((row, index) => rowKeyValue(row, index, props.rowKey)).join(',')
    if (signature === lastSignature) return
    lastSignature = signature
    await nextTick()
    if (current !== generation || !body.value) return
    animations.forEach((animation) => animation.cancel())
    animations = revealRows(body.value.querySelectorAll('.data-row'))
  },
  { immediate: true },
)
onUnmounted(() => {
  generation++
  animations.forEach((animation) => animation.cancel())
})
</script>

<template>
  <!-- 裁剪行入场位移，避免横向滚动容器产生短暂的纵向滚动条。 -->
  <div class="app-data-table relative overflow-x-auto overflow-y-hidden" :aria-busy="loading">
    <span v-if="loading" class="sr-only" role="status">正在加载数据</span>
    <table class="data-table" :style="{ minWidth: `${minWidth}px` }">
      <thead>
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            :class="[column.align === 'right' && 'text-right', column.sticky && 'col-actions']"
            :style="column.width ? { width: column.width } : undefined"
            scope="col"
          >
            {{ column.label }}
          </th>
        </tr>
      </thead>

      <tbody v-if="loading && rows.length === 0">
        <tr v-for="rowIndex in 6" :key="rowIndex">
          <td
            v-for="(column, columnIndex) in columns"
            :key="column.key"
            :class="[column.align === 'right' && 'text-right', column.sticky && 'col-actions']"
          >
            <div
              class="table-skeleton h-3 rounded-full"
              :style="{ width: `${36 + ((rowIndex * 7 + columnIndex * 13) % 48)}%` }"
            ></div>
          </td>
        </tr>
      </tbody>

      <tbody v-else-if="rows.length === 0">
        <tr>
          <td :colspan="columns.length">
            <AppEmptyState :title="emptyTitle" :description="emptyDescription" />
          </td>
        </tr>
      </tbody>

      <tbody v-else ref="body">
        <tr
          v-for="(row, index) in rows"
          :key="rowKeyValue(row, index, rowKey)"
          class="data-row"
          :style="loading ? { opacity: 0.55 } : undefined"
        >
          <td
            v-for="column in columns"
            :key="column.key"
            :class="[column.align === 'right' && 'text-right', column.sticky && 'col-actions']"
          >
            <slot :name="`cell-${column.key}`" :row="row" :index="index">
              {{ cellText(row, column.key) }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-skeleton {
  display: block;
  background: var(--line-soft);
  animation: skeleton-pulse 1.4s ease-in-out infinite;
}
@keyframes skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
