<script setup lang="ts">
import { ChevronDown } from '@lucide/vue'
import { PopoverRoot, PopoverTrigger, PopoverPortal, PopoverContent } from 'reka-ui'
import { computed } from 'vue'
import type { RoleSummary } from '@/api/types'
import RolePicker from './RolePicker.vue'
const props = withDefaults(
  defineProps<{
    roles: Pick<RoleSummary, 'id' | 'name' | 'code' | 'status'>[]
    disabled?: boolean
  }>(),
  {
    disabled: false,
  },
)
const model = defineModel<number[]>({ default: () => [] })
const label = computed(
  () =>
    props.roles
      .filter((role) => model.value.includes(role.id))
      .map((role) => role.name)
      .join('、') || '请选择角色（选填）',
)
</script>
<template>
  <PopoverRoot
    ><PopoverTrigger :disabled="disabled" class="app-select-trigger" aria-label="选择角色"
      ><span class="truncate">{{ label }}</span
      ><ChevronDown :size="15" aria-hidden="true" /></PopoverTrigger
    ><PopoverPortal
      ><PopoverContent
        class="app-select-content w-[min(380px,calc(100vw-32px))]"
        align="start"
        :side-offset="6"
        :collision-padding="12"
        aria-label="选择角色"
        ><RolePicker
          v-model="model"
          :roles="roles"
          :allow-select-all="false" /></PopoverContent></PopoverPortal
  ></PopoverRoot>
</template>
