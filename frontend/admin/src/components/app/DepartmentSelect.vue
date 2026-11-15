<script setup lang="ts">
import { Check, ChevronDown, Search } from '@lucide/vue'
import { PopoverRoot, PopoverTrigger, PopoverPortal, PopoverContent } from 'reka-ui'
import { computed, ref } from 'vue'
import type { DepartmentOption } from '@/api/departments'
import { flattenDepartments } from '@/lib/departments'
import AppInput from './AppInput.vue'
const props = withDefaults(
  defineProps<{
    options: DepartmentOption[]
    invalid?: boolean
    disabled?: boolean
    excludeIds?: number[]
    preserveId?: number
    placeholder?: string
    id?: string
  }>(),
  {
    invalid: false,
    disabled: false,
    excludeIds: () => [],
    preserveId: undefined,
    placeholder: '请选择部门',
    id: undefined,
  },
)
const model = defineModel<number>()
const open = ref(false)
const keyword = ref('')
const all = computed(() => flattenDepartments(props.options))
const selected = computed(() => all.value.find((item) => item.id === model.value))
const filtered = computed(() =>
  all.value.filter((item) => item.path.toLowerCase().includes(keyword.value.trim().toLowerCase())),
)
function choose(id: number) {
  model.value = id
  open.value = false
  keyword.value = ''
}
</script>
<template>
  <PopoverRoot v-model:open="open">
    <PopoverTrigger as-child>
      <button
        :id="id"
        type="button"
        :disabled="disabled"
        class="app-select-trigger"
        :class="{ 'is-invalid': invalid }"
        :aria-invalid="invalid || undefined"
      >
        <span class="truncate">{{
          selected
            ? selected.name + (selected.status === 'DISABLED' ? '（已停用）' : '')
            : placeholder
        }}</span><ChevronDown :size="15" aria-hidden="true" />
      </button>
    </PopoverTrigger>
    <PopoverPortal
      ><PopoverContent
        class="app-select-content w-[min(360px,calc(100vw-32px))]"
        align="start"
        :side-offset="6"
        :collision-padding="12"
        aria-label="选择部门"
      >
        <AppInput v-model="keyword" placeholder="搜索部门名称或路径" aria-label="搜索部门"
          ><template #leading><Search :size="14" aria-hidden="true" /></template
        ></AppInput>
        <div class="mt-2 max-h-64 overflow-y-auto">
          <button
            v-for="item in filtered"
            :key="item.id"
            type="button"
            class="flex min-h-9 w-full items-center gap-2 rounded-lg py-2 pr-2 text-left text-[13px] text-ink hover:bg-brand/10 focus-visible:outline-2 focus-visible:outline-brand disabled:cursor-not-allowed disabled:opacity-45"
            :style="{ paddingLeft: `${10 + item.depth * 12}px` }"
            :disabled="excludeIds.includes(item.id) || (!item.selectable && preserveId !== item.id)"
            :aria-pressed="model === item.id"
            :title="item.path"
            @click="choose(item.id)"
          >
            <span class="min-w-0 flex-1 truncate"
              >{{ item.name
              }}<span v-if="!item.selectable" class="text-ink-soft">（已停用）</span></span
            ><Check v-if="model === item.id" :size="14" class="text-brand" aria-hidden="true" />
          </button>
          <p v-if="!filtered.length" class="px-3 py-4 text-center text-xs text-ink-soft">
            暂无匹配部门
          </p>
        </div>
      </PopoverContent></PopoverPortal
    >
  </PopoverRoot>
</template>
