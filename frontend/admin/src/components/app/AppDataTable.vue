<script setup lang="ts" generic="T extends object">
import AppEmptyState from './AppEmptyState.vue'

interface Column {
  key: string
  label: string
  align?: 'left' | 'right'
  width?: string
  sticky?: boolean
}

withDefaults(
  defineProps<{
    columns: Column[]
    rows: T[]
    rowKey?: string
    loading?: boolean
    minWidth?: number
    emptyTitle?: string
    emptyDescription?: string
  }>(),
  {
    rowKey: 'id',
    loading: false,
    minWidth: 900,
    emptyTitle: '暂无数据',
    emptyDescription: '',
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
</script>

<template>
  <div class="relative overflow-x-auto" :aria-busy="loading">
    <div v-if="loading" class="table-loading-bar" role="status">
      <span class="sr-only">正在加载数据</span>
    </div>
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

      <tbody v-if="loading">
        <tr v-for="rowIndex in 6" :key="rowIndex">
          <td
            v-for="column in columns"
            :key="column.key"
            :class="[column.align === 'right' && 'text-right', column.sticky && 'col-actions']"
          >
            <div
              class="table-skeleton h-3.5 rounded-full"
              :class="column.align === 'right' ? 'ml-auto w-16' : 'w-2/5'"
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

      <tbody v-else>
        <tr
          v-for="(row, index) in rows"
          :key="rowKeyValue(row, index, rowKey)"
          class="data-row"
          :style="{ '--row-delay': `${Math.min(index, 10) * 24}ms` }"
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
.table-loading-bar {
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  z-index: 20;
  overflow: hidden;
  background: var(--brand-soft);
}
.table-loading-bar::after {
  content: '';
  display: block;
  width: 35%;
  height: 100%;
  background: var(--brand);
  animation: loading-progress 1.1s ease-in-out infinite;
}
.table-skeleton {
  background: linear-gradient(
    100deg,
    rgba(46, 58, 62, 0.06) 25%,
    rgba(46, 58, 62, 0.13) 45%,
    rgba(46, 58, 62, 0.06) 65%
  );
  background-size: 250% 100%;
  animation: skeleton-sweep 1.5s ease-in-out infinite;
}
.data-row {
  animation: row-arrive 350ms cubic-bezier(0.16, 1, 0.3, 1) var(--row-delay) both;
}
@keyframes loading-progress {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(390%);
  }
}
@keyframes skeleton-sweep {
  from {
    background-position: 100% 0;
  }
  to {
    background-position: -100% 0;
  }
}
@keyframes row-arrive {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
