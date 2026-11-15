<script setup lang="ts">
import { ChevronDown } from '@lucide/vue'
import { PopoverRoot, PopoverTrigger, PopoverPortal, PopoverContent } from 'reka-ui'
import { computed, ref } from 'vue'
import type { DepartmentOption } from '@/api/departments'
import { flattenDepartments } from '@/lib/departments'
import DepartmentTree from './DepartmentTree.vue'
const props = defineProps<{ nodes: DepartmentOption[] }>()
const model = defineModel<number | 'unassigned'>()
const open = ref(false)
const label = computed(() =>
  model.value === undefined
    ? '全部部门'
    : model.value === 'unassigned'
      ? '未分配部门'
      : (flattenDepartments(props.nodes).find((row) => row.id === model.value)?.name ??
        '请选择部门'),
)
function choose(value: number | 'unassigned' | undefined) {
  model.value = value
  open.value = false
}
</script>
<template>
  <PopoverRoot v-model:open="open"
    ><PopoverTrigger class="app-select-trigger" aria-label="按部门筛选"
      ><span class="truncate">{{ label }}</span
      ><ChevronDown :size="15" aria-hidden="true" /></PopoverTrigger
    ><PopoverPortal
      ><PopoverContent
        class="app-select-content w-[min(360px,calc(100vw-32px))]"
        align="start"
        :collision-padding="12"
        :side-offset="6"
        aria-label="按部门筛选"
        ><DepartmentTree
          :nodes="nodes"
          :model-value="model"
          @update:model-value="choose" /></PopoverContent></PopoverPortal
  ></PopoverRoot>
</template>
