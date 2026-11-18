<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, useId, watch } from 'vue'
import { motionSkipped } from '@/lib/motion'
const props = defineProps<{ values: number[]; labels: string[] }>()
const gradientId = useId()
const chart = ref<InstanceType<typeof globalThis.SVGSVGElement> | null>(null)
const width = ref(810)
let renderedWidth = width.value
let observer: InstanceType<typeof globalThis.ResizeObserver> | undefined
const stroke = ref<InstanceType<typeof globalThis.SVGPathElement> | null>(null)
const fill = ref<InstanceType<typeof globalThis.SVGPathElement> | null>(null)
let frame = 0
let drawn = false
let drawing: InstanceType<typeof globalThis.Animation>[] = []
onMounted(() => {
  if (!chart.value) return
  observer = new globalThis.ResizeObserver(([entry]) => {
    if (entry) width.value = Math.max(280, Math.round(entry.contentRect.width))
  })
  observer.observe(chart.value)
})
onUnmounted(() => {
  observer?.disconnect()
  globalThis.cancelAnimationFrame(frame)
  drawing.forEach((animation) => animation.cancel())
  globalThis.document.removeEventListener('visibilitychange', finishHidden)
})
const maximum = computed(() => Math.ceil(Math.max(3, ...props.values) / 3) * 3)
const points = computed(() =>
  props.values.map((value, index) => ({
    x: 36 + (index * (width.value - 60)) / Math.max(1, props.values.length - 1),
    y: 184 - (value / maximum.value) * 150,
  })),
)
const shown = shallowRef<{ x: number; y: number }[]>([])
const latestPoint = computed(() => shown.value.at(-1))
function finishHidden(): void {
  if (!globalThis.document.hidden) return
  globalThis.cancelAnimationFrame(frame)
  shown.value = points.value
  drawing.forEach((animation) => animation.finish())
}
onMounted(() => globalThis.document.addEventListener('visibilitychange', finishHidden))
watch(
  points,
  async (target) => {
    globalThis.cancelAnimationFrame(frame)
    if (!target.length) {
      shown.value = []
      return
    }
    const resized = width.value !== renderedWidth
    renderedWidth = width.value
    if (!drawn || motionSkipped() || resized) {
      shown.value = target
      const first = !drawn
      drawn = true
      if (first && !motionSkipped()) {
        await nextTick()
        const length = stroke.value?.getTotalLength() ?? 0
        drawing = [
          ...(stroke.value
            ? [
                stroke.value.animate(
                  [
                    { strokeDasharray: `${length}`, strokeDashoffset: `${length}` },
                    { strokeDasharray: `${length}`, strokeDashoffset: '0' },
                  ],
                  { duration: 1100, easing: 'cubic-bezier(.65,0,.35,1)' },
                ),
              ]
            : []),
          ...(fill.value
            ? [
                fill.value.animate([{ opacity: 0 }, { opacity: 1 }], {
                  duration: 800,
                  delay: 500,
                  easing: 'ease-out',
                  fill: 'backwards',
                }),
              ]
            : []),
        ]
      }
      return
    }
    const previous = shown.value
    const from = target.map((_, index) => {
      const position = (index / Math.max(1, target.length - 1)) * (previous.length - 1)
      const first = previous[Math.floor(position)] ?? target[index]!
      const last = previous[Math.ceil(position)] ?? first
      const t = position - Math.floor(position)
      return { x: first.x + (last.x - first.x) * t, y: first.y + (last.y - first.y) * t }
    })
    const started = globalThis.performance.now()
    function morph(time: number): void {
      const progress = Math.min(1, (time - started) / 720)
      const t = progress < 0.5 ? 4 * progress ** 3 : 1 - (-2 * progress + 2) ** 3 / 2
      shown.value = target.map((point, index) => ({
        x: from[index]!.x + (point.x - from[index]!.x) * t,
        y: from[index]!.y + (point.y - from[index]!.y) * t,
      }))
      if (progress < 1) frame = globalThis.requestAnimationFrame(morph)
    }
    frame = globalThis.requestAnimationFrame(morph)
  },
  { immediate: true },
)
const line = computed(() =>
  shown.value
    .map((point, index, all) => {
      if (!index) return `M ${point.x},${point.y}`
      const previous = all[index - 1]!
      const middle = (previous.x + point.x) / 2
      return `C ${middle},${previous.y} ${middle},${point.y} ${point.x},${point.y}`
    })
    .join(' '),
)
const area = computed(() =>
  points.value.length ? `${line.value} L ${width.value - 24},184 L 36,184 Z` : '',
)
const labelIndexes = computed(() =>
  props.labels.flatMap((label, index) =>
    index % Math.max(1, Math.ceil(props.labels.length / (width.value < 400 ? 4 : 7))) === 0 ||
    index === props.labels.length - 1
      ? [{ label, index }]
      : [],
  ),
)
</script>
<template>
  <svg
    ref="chart"
    class="trend-chart"
    :viewBox="`0 0 ${width} 220`"
    role="img"
    aria-label="登录记录趋势，横轴为时间，纵轴为登录次数"
  >
    <defs>
      <linearGradient :id="gradientId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="currentColor" stop-opacity="0.2" />
        <stop offset="100%" stop-color="currentColor" stop-opacity="0.01" />
      </linearGradient>
    </defs>
    <g v-for="step in 4" :key="step">
      <line
        x1="36"
        :x2="width - 24"
        :y1="184 - (step - 1) * 50"
        :y2="184 - (step - 1) * 50"
        stroke="currentColor"
        stroke-opacity="0.12"
        stroke-dasharray="1 4"
      />
      <text x="26" :y="188 - (step - 1) * 50" text-anchor="end">
        {{ Math.round((maximum * (step - 1)) / 3) }}
      </text>
    </g>
    <path ref="fill" :d="area" :fill="`url(#${gradientId})`" />
    <path
      ref="stroke"
      class="trend-line"
      :d="line"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
    />
    <g v-if="latestPoint" class="trend-endpoint" aria-hidden="true">
      <circle :cx="latestPoint.x" :cy="latestPoint.y" r="8" fill="var(--brand-soft)" />
      <circle
        :cx="latestPoint.x"
        :cy="latestPoint.y"
        r="3"
        fill="var(--surface-solid)"
        stroke="currentColor"
        stroke-width="1.5"
      />
    </g>
    <g v-for="(point, index) in points" :key="index">
      <circle :cx="point.x" :cy="point.y" r="7" fill="transparent">
        <title>{{ labels[index] }}：{{ values[index] }} 次</title>
      </circle>
    </g>
    <text
      v-for="item in labelIndexes"
      :key="item.index"
      :x="points[item.index]?.x"
      y="207"
      text-anchor="middle"
    >
      {{ item.label }}
    </text>
  </svg>
</template>
<style scoped>
.trend-chart {
  display: block;
  width: 100%;
  height: 220px;
  min-width: 0;
  overflow: hidden;
  color: var(--ink);
}
text {
  font-family: var(--font-sans);
  font-size: 11px;
  fill: var(--ink-soft);
}
</style>
