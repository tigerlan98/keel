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
              class="h-3.5 animate-pulse rounded-full bg-ink/10"
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
        <tr v-for="(row, index) in rows" :key="rowKeyValue(row, index, rowKey)">
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
