<script setup lang="ts">
import { LayoutDashboard } from '@lucide/vue'
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import type { MenuNode } from '@/api/types'
import MenuTree from './MenuTree.vue'
import { animateMotion, motionFrames, motionSkipped, slideThumb } from '@/lib/motion'

const props = withDefaults(defineProps<{ menus: MenuNode[]; collapsed?: boolean }>(), {
  collapsed: false,
})
const route = useRoute()
const root = ref<InstanceType<typeof globalThis.HTMLElement> | null>(null)
const thumb = ref<InstanceType<typeof globalThis.HTMLElement> | null>(null)
let observer: InstanceType<typeof globalThis.ResizeObserver> | undefined
let animation: InstanceType<typeof globalThis.Animation> | undefined
let initialized = false
const pressed = new Set<InstanceType<typeof globalThis.HTMLElement>>()
const presses = new Map<
  InstanceType<typeof globalThis.HTMLElement>,
  InstanceType<typeof globalThis.Animation>
>()
function press(
  element: InstanceType<typeof globalThis.HTMLElement>,
  to: number,
  release: boolean,
): void {
  const from = Number.parseFloat(globalThis.getComputedStyle(element).scale) || 1
  presses.get(element)?.cancel()
  element.style.scale = String(to)
  if (motionSkipped()) return
  const next = animateMotion(
    element,
    motionFrames((t) => ({ scale: String(from + (to - from) * t) }), release ? 'settle' : 'press'),
    release ? 460 : 140,
    0,
    'navigation-press',
  )
  presses.set(element, next)
}
function pointerDown(event: InstanceType<typeof globalThis.PointerEvent>): void {
  if (event.button !== 0 || motionSkipped()) return
  const target =
    event.target instanceof globalThis.Element ? event.target.closest('.nav-item') : null
  if (!(target instanceof globalThis.HTMLElement) || !root.value?.contains(target)) return
  pressed.add(target)
  press(target, 0.955, false)
}
function release(): void {
  pressed.forEach((element) => press(element, 1, true))
  pressed.clear()
}
async function sync(instant = false): Promise<void> {
  await nextTick()
  const target = root.value?.querySelector<InstanceType<typeof globalThis.HTMLElement>>(
    '.nav-item[aria-current="page"]',
  )
  if (!thumb.value) return
  if (!target) {
    thumb.value.style.opacity = '0'
    return
  }
  animation = slideThumb(thumb.value, target, instant || !initialized, animation)
  thumb.value.style.opacity = '1'
  initialized = true
}
watch(
  () => route.path,
  () => {
    void sync()
  },
  { flush: 'post' },
)
watch(
  () => [props.menus, props.collapsed],
  () => {
    void sync(true)
  },
  { deep: true, flush: 'post' },
)
onMounted(() => {
  void sync(true)
  observer = new globalThis.ResizeObserver(() => {
    void sync(true)
  })
  if (root.value) observer.observe(root.value)
  globalThis.addEventListener('pointerup', release)
  globalThis.addEventListener('pointercancel', release)
  globalThis.addEventListener('blur', release)
})
onUnmounted(() => {
  observer?.disconnect()
  animation?.cancel()
  presses.forEach((animation) => animation.cancel())
  globalThis.removeEventListener('pointerup', release)
  globalThis.removeEventListener('pointercancel', release)
  globalThis.removeEventListener('blur', release)
})
</script>
<template>
  <div ref="root" class="sidebar-navigation" @pointerdown="pointerDown">
    <span ref="thumb" class="nav-selection" aria-hidden="true"></span>
    <div class="navigation-items">
      <RouterLink
        to="/"
        class="nav-item workbench-link"
        :class="{ 'is-compact': collapsed }"
        :title="collapsed ? '工作台' : undefined"
        :aria-label="collapsed ? '工作台' : undefined"
        ><LayoutDashboard :size="18" class="nav-icon" aria-hidden="true" /><span v-if="!collapsed"
          >工作台</span
        ></RouterLink
      >
      <MenuTree :menus="menus" :collapsed="collapsed" @layout="sync(true)" />
      <p
        v-if="menus.length === 0 && !collapsed"
        class="px-3 py-4 text-[11px] leading-5 text-ink-soft"
      >
        暂无可见菜单
      </p>
    </div>
  </div>
</template>
<style scoped>
.sidebar-navigation {
  position: relative;
  isolation: isolate;
}
.navigation-items {
  position: relative;
  z-index: 1;
}
.nav-selection {
  position: absolute;
  z-index: 0;
  inset: 0 0 auto;
  height: 38px;
  opacity: 0;
  pointer-events: none;
  border-radius: 12px;
  border: 1px solid var(--glass-edge);
  background: var(--pill-surface);
  box-shadow: var(--shadow-e2), var(--inset-edge);
}
.workbench-link {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 38px;
  padding: 0 11px;
  margin: 4px 0 8px;
  border-radius: 12px;
  color: var(--ink-muted);
  font-size: 13px;
}
.workbench-link[aria-current='page'] {
  color: var(--ink);
  font-weight: 650;
}
.workbench-link:hover {
  color: var(--ink);
}
.workbench-link[aria-current='page'] .nav-icon {
  color: var(--brand);
}
.workbench-link.is-compact {
  justify-content: center;
  padding: 0;
}
.workbench-link:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}
</style>
