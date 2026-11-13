<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { motionSkipped } from '@/lib/motion'

const props = withDefaults(defineProps<{ value: number | null; decimals?: number }>(), {
  decimals: 0,
})
const ready = ref(false)
let frame = 0
let fallback: ReturnType<typeof globalThis.setTimeout> | undefined
const text = computed(() =>
  props.value === null
    ? '—'
    : props.value.toLocaleString('en-US', {
        minimumFractionDigits: props.decimals,
        maximumFractionDigits: props.decimals,
      }),
)
const digits = computed(() =>
  [...text.value].map((character, index, list) => ({
    character,
    key: list.length - index,
    value: /\d/.test(character) ? Number(character) : null,
  })),
)
watch(
  () => props.value,
  (value, previous) => {
    globalThis.cancelAnimationFrame(frame)
    globalThis.clearTimeout(fallback)
    if (motionSkipped() || value === null || (previous !== null && previous !== undefined)) {
      ready.value = true
      return
    }
    ready.value = false
    void nextTick(() => {
      frame = globalThis.requestAnimationFrame(() => {
        ready.value = true
      })
      fallback = globalThis.setTimeout(() => {
        ready.value = true
      }, 120)
    })
  },
  { immediate: true },
)
onUnmounted(() => {
  globalThis.cancelAnimationFrame(frame)
  globalThis.clearTimeout(fallback)
})
</script>
<template>
  <span
    class="animated-number"
    :data-ready="ready"
    :aria-label="value === null ? '暂无数据' : text"
    role="img"
    ><span
      v-for="digit in digits"
      :key="digit.key"
      class="digit-cell"
      :class="{ 'is-separator': digit.value === null }"
      aria-hidden="true"
      ><span
        v-if="digit.value !== null"
        class="digit-strip"
        :style="{
          transform: `translateY(${-(ready ? digit.value : 0) * 10}%)`,
          transitionDelay: `${(digit.key % 6) * 40}ms`,
        }"
        ><span v-for="number in 10" :key="number">{{ number - 1 }}</span></span
      ><template v-else>{{ digit.character }}</template></span
    ></span
  >
</template>
<style scoped>
.animated-number {
  display: inline-flex;
  height: 1em;
  line-height: 1;
  vertical-align: baseline;
  font-variant-numeric: tabular-nums;
}
.digit-cell {
  display: inline-block;
  position: relative;
  height: 1em;
  width: 0.62em;
  overflow: hidden;
}
.digit-cell.is-separator {
  width: auto;
  overflow: visible;
}
.digit-strip {
  display: flex;
  flex-direction: column;
  transition: transform 0.9s cubic-bezier(0.16, 1, 0.3, 1);
}
.digit-strip > span {
  display: block;
  flex: none;
  height: 1em;
  line-height: 1;
  text-align: center;
}
</style>
