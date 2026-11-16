<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, useId, watch } from 'vue'
import { Menu, Shield, Users, X } from '@lucide/vue'
import {
  resourceHolders,
  roleResourceIds,
  type PermissionGraph,
} from '@/composables/usePermissionGraph'

type Kind = 'user' | 'role' | 'menu'
interface Dial {
  kind: Kind
  count: number | null
  ratio: number
  sub: string
}
interface Item {
  id: string
  name: string
  sub: string
  disabled: boolean
}
interface Point {
  x: number
  y: number
}
const props = defineProps<{ graph: PermissionGraph; kind: Kind; dials: Dial[]; paused: boolean }>()
const emit = defineEmits<{ select: [id: string]; cluster: [kind: Kind]; home: [] }>()
const labels = { user: '用户', role: '角色', menu: '菜单' }
const icons = { user: Users, role: Shield, menu: Menu }
const colors = { user: '#4387d9', role: '#9474d8', menu: '#4aab70' }
const root = ref<InstanceType<typeof globalThis.HTMLElement> | null>(null)
const svg = ref<InstanceType<typeof globalThis.SVGSVGElement> | null>(null)
const size = ref({ width: 1200, height: 700 })
const id = useId().replaceAll(':', '')
const showingAll = ref(false)
const query = ref('')
let observer: InstanceType<typeof globalThis.ResizeObserver> | undefined
onMounted(() => {
  if (!root.value) return
  observer = new globalThis.ResizeObserver(([entry]) => {
    if (entry) size.value = { width: entry.contentRect.width, height: entry.contentRect.height }
  })
  observer.observe(root.value)
})
onUnmounted(() => observer?.disconnect())
watch(
  [svg, () => props.paused],
  ([element, paused]) => {
    if (paused) element?.pauseAnimations()
    else element?.unpauseAnimations()
  },
  { flush: 'post' },
)
watch(
  () => props.kind,
  () => {
    showingAll.value = false
    query.value = ''
  },
)
const narrow = computed(() => size.value.width < 640)
const centerY = computed(() => 76 + (size.value.height - 76 - 96) / 2)
const core = computed(() => ({
  x: narrow.value ? 37 : Math.max(84, size.value.width * 0.07),
  y: centerY.value,
}))
const columnX = computed(() => core.value.x + (narrow.value ? 77 : 150))
const hubs = computed(() => {
  const others = props.dials.filter((dial) => dial.kind !== props.kind)
  return [others[0]!, props.dials.find((dial) => dial.kind === props.kind)!, others[1]!].map(
    (dial, index) => ({
      ...dial,
      x: columnX.value,
      y: centerY.value + (narrow.value ? [-133, -10, 123][index]! : [-180, -10, 170][index]!),
      radius: dial.kind === props.kind ? (narrow.value ? 36 : 56) : narrow.value ? 24 : 36,
      active: dial.kind === props.kind,
    }),
  )
})
const activeHub = computed(() => hubs.value.find((hub) => hub.active)!)
const fanX = computed(() =>
  narrow.value
    ? 196
    : Math.min(Math.max(columnX.value + 300, size.value.width * 0.42), size.value.width - 320),
)
const items = computed<Item[]>(() => {
  if (props.kind === 'role')
    return props.graph.roles.map((role) => {
      const holders = props.graph.users.filter((user) =>
        (user.roles ?? []).some((assigned) => assigned.id === role.id),
      ).length
      const grants = new Set(roleResourceIds(props.graph, role))
      const pages = props.graph.resources.filter(
        (node) => node.type === 'MENU' && grants.has(node.id),
      ).length
      return {
        id: `role:${role.id}`,
        name: role.name,
        sub: `${props.graph.usersVisible ? `${holders} 人持有` : '用户不可见'} · ${pages} 个页面`,
        disabled: role.status !== 'ENABLED',
      }
    })
  if (props.kind === 'user')
    return props.graph.users.map((user) => ({
      id: `user:${user.id}`,
      name: user.nickname || user.username,
      sub: `${user.departmentName || '未分配部门'} · ${(user.roles ?? []).length} 个角色`,
      disabled: user.status !== 'ENABLED',
    }))
  return props.graph.resources
    .filter((node) => node.type === 'MENU')
    .map((node) => {
      const roles = resourceHolders(props.graph, node.id).roles.filter(
        (role) => role.code !== 'SUPER_ADMIN',
      )
      return {
        id: `menu:${node.id}`,
        name: node.name,
        sub: roles.length ? `${roles.length} 个角色授予` : '仅超级管理员可见',
        disabled: node.status !== 'ENABLED',
      }
    })
})
const cap = computed(() =>
  Math.max(4, Math.floor((size.value.height - 76 - 120) / (narrow.value ? 48 : 34))),
)
const visibleItems = computed(() => {
  const rows = items.value.slice(0, cap.value)
  const count = rows.length + (items.value.length > rows.length ? 1 : 0)
  const gap =
    count > 1 ? Math.min(narrow.value ? 54 : 46, (size.value.height - 76 - 140) / (count - 1)) : 0
  const y0 = centerY.value - (gap * (count - 1)) / 2
  return rows.map((item, index) => ({ ...item, x: fanX.value, y: y0 + index * gap }))
})
const more = computed(() => items.value.length - visibleItems.value.length)
const moreY = computed(() => (visibleItems.value.at(-1)?.y ?? centerY.value) + 46)
const filtered = computed(() =>
  items.value.filter((item) =>
    `${item.name} ${item.sub}`.toLowerCase().includes(query.value.trim().toLowerCase()),
  ),
)
function thread(from: Point, to: Point): string {
  const middle = (from.x + to.x) / 2
  return `M${from.x},${from.y} C${middle},${from.y} ${middle},${to.y} ${to.x},${to.y}`
}
const background = computed(() => {
  const w = size.value.width,
    h = size.value.height
  const nodes = new Map<string, Point>()
  const users = props.graph.users.slice(0, 100),
    roles = props.graph.roles.slice(0, 60),
    resources = props.graph.resources.slice(0, 160)
  users.forEach((user, i) =>
    nodes.set(`user:${user.id}`, {
      x: w * 0.245 + i * Math.min(10, (w * 0.12) / Math.max(1, users.length)),
      y: h * 0.55 + i * Math.min(14, (h * 0.27) / Math.max(1, users.length)),
    }),
  )
  roles.forEach((role, i) =>
    nodes.set(`role:${role.id}`, {
      x: w * 0.47 + i * 22,
      y: h * 0.22 - Math.sin((i / Math.max(1, roles.length - 1)) * Math.PI) * 22,
    }),
  )
  resources.forEach((node, i) =>
    nodes.set(`menu:${node.id}`, {
      x: w * 0.735 + (i % 7) * 13,
      y: h * 0.46 + i * Math.min(7, (h * 0.32) / Math.max(1, resources.length)),
    }),
  )
  const links: { id: string; from: Point; to: Point; color: string }[] = []
  users.forEach((user) =>
    (user.roles ?? []).forEach((role) => {
      const from = nodes.get(`user:${user.id}`),
        to = nodes.get(`role:${role.id}`)
      if (from && to) links.push({ id: `u${user.id}-r${role.id}`, from, to, color: colors.user })
    }),
  )
  roles.forEach((role) =>
    roleResourceIds(props.graph, role).forEach((resourceId) => {
      const from = nodes.get(`role:${role.id}`),
        to = nodes.get(`menu:${resourceId}`)
      if (from && to && links.length < 2000)
        links.push({ id: `r${role.id}-m${resourceId}`, from, to, color: colors[props.kind] })
    }),
  )
  return {
    nodes: [...nodes].map(([key, point]) => ({
      key,
      ...point,
      color: colors[key.split(':')[0] as Kind],
    })),
    links,
  }
})
</script>

<template>
  <div
    ref="root"
    class="trace-module-scene"
    :class="{ 'is-narrow': narrow }"
    :style="{ '--cluster-color': colors[kind] }"
  >
    <p class="module-hint">点击任一条目，展开它的完整授权链路</p>
    <svg
      ref="svg"
      class="module-stage"
      :viewBox="`0 0 ${size.width} ${size.height}`"
      aria-hidden="true"
    >
      <defs>
        <radialGradient :id="`${id}-pearl`" cx="30%" cy="23%" r="85%">
          <stop offset="0" stop-color="#fff" />
          <stop offset=".33" stop-color="#f1f6f4" />
          <stop offset=".7" stop-color="#d3e2d5" />
          <stop offset="1" stop-color="#c1d0cf" />
        </radialGradient>
        <filter :id="`${id}-shadow`" x="-60%" y="-60%" width="220%" height="220%">
          <feDropShadow dx="0" dy="7" stdDeviation="9" flood-color="#45565c" flood-opacity=".16" />
        </filter>
      </defs>
      <g v-if="!narrow" class="module-network">
        <line
          v-for="link in background.links"
          :key="link.id"
          :x1="link.from.x"
          :y1="link.from.y"
          :x2="link.to.x"
          :y2="link.to.y"
          :stroke="link.color"
          stroke-width=".6"
          stroke-opacity=".16"
        />
        <circle
          v-for="node in background.nodes"
          :key="node.key"
          :cx="node.x"
          :cy="node.y"
          r="2"
          :fill="node.color"
          fill-opacity=".25"
        />
      </g>
      <g v-for="hub in hubs" :key="hub.kind" :opacity="hub.active ? 1 : 0.26">
        <path
          :d="thread(core, hub)"
          fill="none"
          :stroke="colors[hub.kind]"
          :stroke-opacity="hub.active ? 0.65 : 0.22"
          stroke-width="1"
        />
        <circle
          :cx="hub.x"
          :cy="hub.y"
          :r="hub.radius"
          fill="var(--surface-solid)"
          :filter="`url(#${id}-shadow)`"
        />
        <g :transform="`translate(${hub.x} ${hub.y})`">
          <line
            v-for="tick in 60"
            :key="tick"
            x1="0"
            :y1="-hub.radius + 6"
            x2="0"
            :y2="-hub.radius + 13"
            :transform="`rotate(${tick * 6})`"
            :stroke="tick <= hub.ratio * 60 ? colors[hub.kind] : 'var(--surface-edge)'"
            stroke-width="1.3"
          />
        </g>
      </g>
      <g>
        <line
          v-for="tick in 72"
          :key="tick"
          x1="0"
          :y1="narrow ? -31 : -44"
          x2="0"
          :y2="narrow ? -27 : -38"
          :transform="`translate(${core.x} ${core.y}) rotate(${tick * 5})`"
          stroke="var(--ink-faint)"
          stroke-opacity=".22"
          stroke-width="1"
        />
        <circle
          :cx="core.x"
          :cy="core.y"
          :r="narrow ? 23 : 32"
          :fill="`url(#${id}-pearl)`"
          stroke="#bccdca"
          stroke-opacity=".5"
          :filter="`url(#${id}-shadow)`"
        />
        <circle
          :cx="core.x - (narrow ? 28 : 54)"
          :cy="core.y - 12"
          r="7"
          fill="#82dd2d"
          fill-opacity=".16"
        />
        <circle :cx="core.x - (narrow ? 28 : 54)" :cy="core.y - 12" r="3" fill="#82dd2d" />
      </g>
      <g v-for="(item, index) in visibleItems" :key="item.id">
        <path
          :d="thread(activeHub, { x: item.x + 12, y: item.y })"
          fill="none"
          :stroke="colors[kind]"
          stroke-width="1.1"
          stroke-opacity=".8"
        />
        <circle r="2.5" :fill="colors[kind]" class="module-pulse">
          <animateMotion
            :dur="`${4.4 + index * 0.3}s`"
            :begin="`${-index * 0.4}s`"
            repeatCount="indefinite"
            :path="thread(activeHub, { x: item.x + 12, y: item.y })"
          />
        </circle>
      </g>
    </svg>
    <button
      type="button"
      class="module-core-target"
      :style="{ left: `${core.x}px`, top: `${core.y}px` }"
      aria-label="返回权限全景"
      @click="emit('home')"
    >
      <span>权限全景</span>
    </button>
    <button
      v-for="hub in hubs"
      :key="hub.kind"
      type="button"
      class="module-hub"
      :class="{ 'is-active': hub.active }"
      :style="{
        left: `${hub.x}px`,
        top: `${hub.y}px`,
        width: `${hub.radius * 2}px`,
        height: `${hub.radius * 2}px`,
        '--hub-color': colors[hub.kind],
      }"
      :aria-label="`查看${labels[hub.kind]}`"
      @click="emit('cluster', hub.kind)"
    >
      <component
        :is="icons[hub.kind]"
        :size="hub.active ? 13 : 11"
        :stroke-width="1.6"
        aria-hidden="true"
      /><strong>{{ hub.count ?? '—' }}</strong
      ><span class="module-hub-caption" :style="{ top: `${hub.radius * 2 + 13}px` }"
        ><b>{{ labels[hub.kind] }}</b
        ><small v-if="hub.active">{{ hub.sub }}</small></span
      >
    </button>
    <button
      v-for="item in visibleItems"
      :key="item.id"
      type="button"
      class="module-capsule"
      :class="{ 'is-off': item.disabled }"
      :style="{ left: `${item.x}px`, top: `${item.y}px` }"
      :aria-label="`${item.name}，${item.sub}`"
      @click="emit('select', item.id)"
    >
      <i aria-hidden="true" /><span
        ><strong>{{ item.name }}</strong
        ><small>{{ item.sub }}{{ item.disabled ? ' · 停用' : '' }}</small></span
      >
    </button>
    <button
      v-if="more"
      type="button"
      class="module-more"
      :style="{ left: `${fanX}px`, top: `${moreY}px` }"
      @click="showingAll = true"
    >
      还有 {{ more }} 项，查看全部
    </button>
    <p
      v-if="!items.length"
      class="module-empty"
      :style="{ left: `${fanX}px`, top: `${centerY}px` }"
    >
      {{
        kind === 'user' && !graph.usersVisible ? '需要用户列表权限才能查看用户关系' : '暂无关联条目'
      }}
    </p>
    <div v-if="showingAll" class="module-all trace-glass">
      <header>
        <strong>全部{{ labels[kind] }}</strong
        ><button type="button" aria-label="关闭全部条目" @click="showingAll = false">
          <X :size="15" />
        </button>
      </header>
      <input v-model="query" :aria-label="`搜索${labels[kind]}`" placeholder="搜索名称或编码" />
      <div>
        <button
          v-for="item in filtered"
          :key="item.id"
          type="button"
          @click="emit('select', item.id)"
        >
          <strong>{{ item.name }}</strong><small>{{ item.sub }}</small>
        </button>
        <p v-if="!filtered.length">暂无匹配项</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.trace-module-scene {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.module-stage {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.module-hint {
  position: absolute;
  top: 23px;
  left: 50%;
  translate: -50% 0;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 550;
  white-space: nowrap;
}
.module-core-target {
  position: absolute;
  width: 74px;
  height: 74px;
  translate: -50% -50%;
  border-radius: 50%;
}
.module-core-target > span {
  position: absolute;
  top: 95px;
  left: 50%;
  translate: -50% 0;
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
}
.module-hub {
  position: absolute;
  translate: -50% -50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border-radius: 50%;
  color: var(--hub-color);
  opacity: 0.28;
}
.module-hub.is-active {
  opacity: 1;
}
.module-hub strong {
  font-size: 18px;
  font-weight: 650;
  line-height: 1.1;
  color: var(--ink);
}
.module-hub.is-active strong {
  font-size: 26px;
}
.module-hub-caption {
  position: absolute;
  left: 50%;
  translate: -50% 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  white-space: nowrap;
  gap: 2px;
}
.module-hub-caption b {
  font-size: 13px;
  font-weight: 650;
  color: var(--ink);
}
.module-hub-caption small {
  font-size: 11px;
  font-weight: 550;
  color: var(--ink-soft);
}
.module-capsule {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 220px;
  padding: 5px 11px 5px 6px;
  translate: 0 -50%;
  border-radius: 999px;
  border: 1px solid var(--surface-edge);
  background: var(--floating-surface);
  box-shadow: var(--inset-edge), var(--shadow-e1);
  text-align: left;
  transition:
    background 0.2s,
    border-color 0.2s,
    box-shadow 0.2s;
}
.module-capsule:hover {
  border-color: var(--cluster-color);
  box-shadow: var(--shadow-e2), var(--inset-edge);
}
.module-capsule > i {
  width: 14px;
  height: 14px;
  border: 1.5px solid var(--cluster-color);
  border-radius: 50%;
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--cluster-color) 12%, transparent);
  flex: none;
}
.module-capsule > span {
  min-width: 0;
  line-height: 1.25;
}
.module-capsule strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  font-weight: 620;
}
.module-capsule small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10.5px;
  color: var(--ink-soft);
  font-weight: 550;
  margin-top: 2px;
}
.module-capsule.is-off {
  opacity: 0.5;
}
.module-more {
  position: absolute;
  translate: 0 -50%;
  padding: 8px;
  color: var(--ink-soft);
  font-size: 11px;
}
.module-empty {
  position: absolute;
  max-width: 260px;
  translate: 0 -50%;
  font-size: 12px;
  color: var(--ink-soft);
}
.module-all {
  position: absolute;
  z-index: 6;
  top: 75px;
  right: 20px;
  bottom: 90px;
  width: min(360px, calc(100% - 40px));
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--glass-edge);
  background: var(--floating-surface);
  box-shadow: var(--shadow-e3);
  backdrop-filter: blur(28px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.module-all header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.module-all input {
  width: 100%;
  border: 1px solid var(--surface-edge);
  border-radius: 10px;
  background: var(--input-surface);
  padding: 8px 10px;
  font-size: 12px;
}
.module-all > div {
  overflow-y: auto;
  display: grid;
  gap: 6px;
}
.module-all > div button {
  display: block;
  text-align: left;
  padding: 8px;
  border-radius: 10px;
  background: var(--hover-surface);
}
.module-all strong {
  font-size: 12px;
}
.module-all small {
  display: block;
  font-size: 10.5px;
  color: var(--ink-soft);
  margin-top: 3px;
}
.trace-module-scene button:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 4px;
}
.is-narrow .module-hint {
  top: 64px;
  font-size: 10px;
}
.is-narrow .module-capsule {
  max-width: calc(100% - 201px);
  padding: 6px 7px;
  gap: 6px;
}
.is-narrow .module-capsule strong {
  font-size: 11px;
}
.is-narrow .module-capsule small {
  font-size: 9px;
}
.is-narrow .module-capsule > i {
  width: 10px;
  height: 10px;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--cluster-color) 12%, transparent);
}
.is-narrow .module-core-target {
  width: 50px;
  height: 50px;
}
.is-narrow .module-core-target > span {
  top: 65px;
  font-size: 10px;
}
.is-narrow .module-hub strong {
  font-size: 14px;
}
.is-narrow .module-hub.is-active strong {
  font-size: 20px;
}
.is-narrow .module-hub-caption b {
  font-size: 11px;
}
.is-narrow .module-hub-caption {
  width: 104px;
}
.is-narrow .module-hub-caption small {
  font-size: 8px;
  max-width: 100px;
  white-space: normal;
  text-align: center;
}
.is-narrow .module-empty {
  max-width: calc(100% - 210px);
  font-size: 11px;
}
.is-narrow .module-more {
  max-width: calc(100% - 205px);
  font-size: 10px;
  text-align: left;
}
@media (prefers-reduced-motion: reduce) {
  .module-pulse {
    display: none;
  }
  .module-capsule {
    transition: none;
  }
}
</style>
