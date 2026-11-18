<script setup lang="ts">
import {
  ArrowLeft,
  ArrowRight,
  ChevronRight,
  Command,
  Menu,
  Pause,
  Play,
  RefreshCw,
  Shield,
  Users,
  X,
} from '@lucide/vue'
import { computed, onMounted, onUnmounted, ref, useId, watch } from 'vue'
import {
  resourceHolders,
  roleResourceIds,
  type PermissionGraph,
} from '@/composables/usePermissionGraph'
import PermissionTraceModule from './PermissionTraceModule.vue'

const props = defineProps<{ graph: PermissionGraph }>()
const focus = defineModel<string | null>('focus', { default: null })
type Kind = 'user' | 'role' | 'menu'
interface Entry {
  id: string
  name: string
  sub: string
  kind: Kind
  disabled: boolean
}
const labels: Record<Kind, string> = { user: '用户', role: '角色', menu: '菜单' }
const colors: Record<Kind, string> = { user: '#4387d9', role: '#9474d8', menu: '#4aab70' }
const icons = { user: Users, role: Shield, menu: Menu }
const cluster = ref<Kind | null>(null)
const hovered = ref<Kind | null>(null)
const paused = ref(false)
const replay = ref(0)
const stage = ref<InstanceType<typeof globalThis.SVGSVGElement> | null>(null)
const screenQuery = globalThis.matchMedia('(max-width: 640px)')
const narrow = ref(screenQuery.matches)
function resize(): void {
  narrow.value = screenQuery.matches
}
onMounted(() => screenQuery.addEventListener('change', resize))
onUnmounted(() => screenQuery.removeEventListener('change', resize))
watch(paused, (value) => {
  if (value) stage.value?.pauseAnimations()
  else stage.value?.unpauseAnimations()
})
const core = computed(() => ({ x: 600, y: narrow.value ? 400 : 385 }))
const question = ref('')
const answer = ref('')
const answerEntries = ref<Entry[]>([])
const id = useId().replaceAll(':', '')
const selected = computed(() => entries.value.find((entry) => entry.id === focus.value))
const entries = computed<Entry[]>(() => [
  ...props.graph.users.map((user) => ({
    id: `user:${user.id}`,
    name: user.nickname || user.username,
    sub: `@${user.username} · ${(user.roles ?? []).length} 个角色`,
    kind: 'user' as const,
    disabled: user.status !== 'ENABLED',
  })),
  ...props.graph.roles.map((role) => ({
    id: `role:${role.id}`,
    name: role.name,
    sub: role.code,
    kind: 'role' as const,
    disabled: role.status !== 'ENABLED',
  })),
  ...props.graph.resources.map((node) => ({
    id: `menu:${node.id}`,
    name: node.name,
    sub: node.code,
    kind: 'menu' as const,
    disabled: node.status !== 'ENABLED',
  })),
])
watch(
  focus,
  (value) => {
    if (!value) return
    const match = /^(user|role|menu):[1-9]\d*$/.exec(value)
    if (match) cluster.value = match[1] as Kind
    else {
      focus.value = null
      cluster.value = null
    }
  },
  { immediate: true },
)
watch(
  () => props.graph,
  () => {
    if (focus.value && !selected.value) focus.value = null
    answer.value = ''
    answerEntries.value = []
  },
)

const assignedUsers = computed(
  () => props.graph.users.filter((user) => (user.roles ?? []).length > 0).length,
)
const heldRoles = computed(
  () => new Set(props.graph.users.flatMap((user) => (user.roles ?? []).map((role) => role.id))),
)
const ordinaryGrants = computed(
  () =>
    new Set(
      props.graph.roles
        .filter((role) => role.code !== 'SUPER_ADMIN' && role.status === 'ENABLED')
        .flatMap((role) => props.graph.grants[role.id] ?? []),
    ),
)
const orphanMenus = computed(() =>
  props.graph.resources.filter(
    (node) =>
      node.type === 'MENU' && node.status === 'ENABLED' && !ordinaryGrants.value.has(node.id),
  ),
)
const idleRoles = computed(() => props.graph.roles.filter((role) => !heldRoles.value.has(role.id)))
const unassignedUsers = computed(() =>
  props.graph.users.filter((user) => !(user.roles ?? []).length),
)
const grantCount = computed(
  () =>
    Object.values(props.graph.grants).reduce((n, ids) => n + ids.length, 0) +
    props.graph.users.reduce((n, user) => n + (user.roles ?? []).length, 0),
)
const dials = computed(() => [
  {
    kind: 'user' as const,
    x: narrow.value ? 450 : 350,
    y: narrow.value ? 450 : 425,
    count: props.graph.usersVisible ? props.graph.users.length : null,
    ratio: props.graph.users.length ? assignedUsers.value / props.graph.users.length : 0,
    sub: '刻度：已分配角色占比',
  },
  {
    kind: 'role' as const,
    x: 600,
    y: narrow.value ? 270 : 220,
    count: props.graph.roles.length,
    ratio:
      props.graph.usersVisible && props.graph.roles.length
        ? heldRoles.value.size / props.graph.roles.length
        : 0,
    sub: props.graph.usersVisible ? '刻度：有人持有占比' : '持有用户不可见',
  },
  {
    kind: 'menu' as const,
    x: narrow.value ? 750 : 850,
    y: narrow.value ? 450 : 425,
    count: props.graph.resources.length,
    ratio: props.graph.resources.length
      ? props.graph.resources.filter((node) => ordinaryGrants.value.has(node.id)).length /
        props.graph.resources.length
      : 0,
    sub: '刻度：已被授权资源占比',
  },
])
function entryRows(kind: Kind): Entry[] {
  return entries.value.filter((entry) => entry.kind === kind)
}
const rays = computed(() =>
  dials.value.flatMap((dial) => {
    const list = entryRows(dial.kind).slice(0, 128)
    return list.map((entry, index) => {
      const ring = Math.floor(index / 20)
      const count = Math.min(20, list.length - ring * 20)
      const spread = Math.min(2.9, 0.5 + count * 0.14)
      const away = dial.kind === 'user' ? Math.PI : dial.kind === 'role' ? -Math.PI / 2 : 0
      const angle = away - spread / 2 + (count > 1 ? (index % 20) / (count - 1) : 0.5) * spread
      const radius = 88 + ring * 18
      return {
        ...entry,
        x: dial.x + Math.cos(angle) * radius,
        y: dial.y + Math.sin(angle) * radius,
        fromX: dial.x + Math.cos(angle) * 52,
        fromY: dial.y + Math.sin(angle) * 52,
        color: colors[dial.kind],
      }
    })
  }),
)
function home(): void {
  focus.value = null
  cluster.value = null
}
function back(): void {
  if (focus.value) focus.value = null
  else home()
}
function chooseKind(kind: Kind): void {
  cluster.value = kind
  focus.value = null
  answer.value = ''
}
function choose(entry: Entry): void {
  focus.value = entry.id
  cluster.value = entry.kind
}
function clearAnswer(): void {
  answer.value = ''
  answerEntries.value = []
}
function chooseId(value: string): void {
  const entry = entries.value.find((item) => item.id === value)
  if (entry) choose(entry)
}

const related = computed(() => {
  const all = { user: [] as Entry[], role: [] as Entry[], menu: [] as Entry[] }
  if (!selected.value) return all
  const [kind, rawId] = selected.value.id.split(':')
  const entityId = Number(rawId)
  let userIds = new Set<number>(),
    roleIds = new Set<number>(),
    resourceIds = new Set<number>()
  if (kind === 'user') {
    const user = props.graph.users.find((item) => item.id === entityId)
    userIds.add(entityId)
    roleIds = new Set((user?.roles ?? []).map((role) => role.id))
    resourceIds = new Set(
      props.graph.roles
        .filter(
          (role) => roleIds.has(role.id) && role.status === 'ENABLED' && user?.status === 'ENABLED',
        )
        .flatMap((role) => roleResourceIds(props.graph, role)),
    )
  } else if (kind === 'role') {
    const role = props.graph.roles.find((item) => item.id === entityId)
    roleIds.add(entityId)
    userIds = new Set(
      props.graph.users
        .filter((user) => (user.roles ?? []).some((item) => item.id === entityId))
        .map((user) => user.id),
    )
    resourceIds = new Set(role ? roleResourceIds(props.graph, role) : [])
  } else {
    resourceIds.add(entityId)
    const holders = resourceHolders(props.graph, entityId)
    roleIds = new Set(holders.roles.map((role) => role.id))
    userIds = new Set(holders.users.map((user) => user.id))
  }
  all.user = entries.value.filter(
    (entry) => entry.kind === 'user' && userIds.has(Number(entry.id.split(':')[1])),
  )
  all.role = entries.value.filter(
    (entry) => entry.kind === 'role' && roleIds.has(Number(entry.id.split(':')[1])),
  )
  all.menu = entries.value.filter(
    (entry) => entry.kind === 'menu' && resourceIds.has(Number(entry.id.split(':')[1])),
  )
  return all
})
const relatedResources = computed(() => related.value.menu.filter((entry) => !entry.disabled))
function asEntries(kind: Kind, ids: number[]): Entry[] {
  const set = new Set(ids.map((value) => `${kind}:${value}`))
  return entries.value.filter((entry) => set.has(entry.id))
}
function ask(value = question.value): void {
  question.value = value
  answerEntries.value = []
  const query = value
    .trim()
    .toLowerCase()
    .replace(/[？?「」“”\s]/g, '')
  if (!query) {
    answer.value = '输入用户、角色、菜单名称或权限标识，查看真实授权关系。'
    return
  }
  if (
    query.includes('角色') &&
    !query.includes('用户') &&
    (query.includes('没') || query.includes('闲置'))
  ) {
    if (!props.graph.usersVisible) {
      answer.value = '当前没有用户列表权限，无法判断角色是否有人使用。'
      return
    }
    answerEntries.value = asEntries(
      'role',
      idleRoles.value.map((role) => role.id),
    )
    answer.value = `有 ${answerEntries.value.length} 个角色没有分配给用户。`
    return
  }
  if (query.includes('用户') && (query.includes('没') || query.includes('无角色'))) {
    if (!props.graph.usersVisible) {
      answer.value = '当前没有用户列表权限，无法查看用户角色分配。'
      return
    }
    answerEntries.value = asEntries(
      'user',
      unassignedUsers.value.map((user) => user.id),
    )
    answer.value = `有 ${answerEntries.value.length} 个用户尚未分配角色。`
    return
  }
  if (query.includes('孤立') || query.includes('未授权')) {
    answerEntries.value = asEntries(
      'menu',
      orphanMenus.value.map((node) => node.id),
    )
    answer.value = `有 ${answerEntries.value.length} 个启用菜单未分配给普通角色；超级管理员仍按内置规则拥有权限。`
    return
  }
  const actions = [
    { key: '删除', code: 'delete' },
    { key: '新增', code: 'create' },
    { key: '编辑', code: 'update' },
    { key: '授权', code: 'assign' },
  ]
  const action = actions.find((item) => query.includes(item.key))
  const scopes = [
    { name: '用户', code: ':user:' },
    { name: '角色', code: ':role:' },
    { name: '部门', code: ':department:' },
  ]
  const scope = scopes.find((item) => query.includes(item.name))
  const resourceMatches = props.graph.resources.filter((node) =>
    action
      ? node.code.includes(action.code) && (!scope || node.code.includes(scope.code))
      : node.code.toLowerCase() === query || node.name.toLowerCase() === query,
  )
  if (
    (query.includes('谁') || query.includes('哪些角色') || query.includes('能')) &&
    resourceMatches.length
  ) {
    const roles = new Set<number>(),
      users = new Set<number>()
    resourceMatches.forEach((node) => {
      const holders = resourceHolders(props.graph, node.id)
      holders.roles.forEach((role) => roles.add(role.id))
      holders.users.forEach((user) => users.add(user.id))
    })
    answerEntries.value = [...asEntries('role', [...roles]), ...asEntries('user', [...users])]
    answer.value = `${resourceMatches.map((node) => node.name).join('、')}：${roles.size} 个启用角色${props.graph.usersVisible ? `、${users.size} 个启用用户具有对应权限` : '具有对应权限；用户信息无权查看'}。超级管理员按内置规则计算。`
    return
  }
  answerEntries.value = entries.value.filter((entry) =>
    `${entry.name} ${entry.sub}`.toLowerCase().includes(query),
  )
  answer.value = answerEntries.value.length
    ? `找到 ${answerEntries.value.length} 项，点击查看授权链路。`
    : '未找到匹配项。可输入名称、权限编码，或选择下方示例问题。'
}
const examples = [
  '谁能删除用户？',
  '哪些角色没人用？',
  '哪些用户还没分配角色？',
  '哪些菜单未授权？',
]
function line(from: { x: number; y: number }, to: { x: number; y: number }): string {
  return `M${from.x},${from.y} C${from.x},${(from.y + to.y) / 2} ${to.x},${(from.y + to.y) / 2} ${to.x},${to.y}`
}
function restart(): void {
  paused.value = false
  replay.value += 1
}
</script>

<template>
  <section
    class="permission-trace"
    :class="{ 'is-paused': paused, 'has-detail': !!selected, 'is-module': !!cluster && !selected }"
    aria-label="权限溯源图"
    @keydown.esc.stop="back"
  >
    <nav class="trace-crumbs trace-glass" aria-label="溯源路径">
      <button v-if="cluster" type="button" aria-label="返回上一层" @click="back">
        <ArrowLeft :size="14" />
      </button>
      <button type="button" :aria-current="!cluster ? 'page' : undefined" @click="home">
        <Shield :size="13" :stroke-width="1.6" />权限全景
      </button>
      <template v-if="cluster"
        ><ChevronRight :size="11" /><button type="button" @click="focus = null">
          {{ labels[cluster] }}
        </button></template
      >
      <template v-if="selected"
        ><ChevronRight :size="11" /><strong>{{ selected.name }}</strong></template
      >
    </nav>
    <dl v-if="!cluster" class="trace-legend trace-glass">
      <div v-for="dial in dials" :key="dial.kind">
        <dt><i :style="{ background: colors[dial.kind] }" />{{ labels[dial.kind] }}</dt>
        <dd>{{ dial.count ?? '—' }}</dd>
      </div>
      <div class="is-warn">
        <dt>闲置角色</dt>
        <dd>{{ graph.usersVisible ? idleRoles.length : '—' }}</dd>
      </div>
      <div class="is-warn">
        <dt>孤立菜单</dt>
        <dd>{{ orphanMenus.length }}</dd>
      </div>
      <div class="is-warn">
        <dt>无角色用户</dt>
        <dd>{{ graph.usersVisible ? unassignedUsers.length : '—' }}</dd>
      </div>
    </dl>
    <div v-if="!selected" class="trace-controls trace-glass">
      <button type="button" @click="restart"><RefreshCw :size="12" />重播</button
      ><button type="button" :aria-pressed="paused" @click="paused = !paused">
        <component :is="paused ? Play : Pause" :size="12" />{{ paused ? '继续' : '暂停' }}
      </button>
    </div>
    <svg
      v-if="!cluster"
      :key="replay"
      ref="stage"
      class="trace-stage"
      :viewBox="narrow ? '390 180 420 400' : '0 0 1200 700'"
      role="group"
      aria-label="用户、角色与菜单授权全景"
    >
      <defs>
        <radialGradient :id="`${id}-pearl`" cx="30%" cy="23%" r="85%">
          <stop offset="0" stop-color="#fff" />
          <stop offset=".33" stop-color="#f1f6f4" />
          <stop offset=".7" stop-color="#d3e2d5" />
          <stop offset="1" stop-color="#c1d0cf" />
        </radialGradient>
        <radialGradient :id="`${id}-shine`" cx="32%" cy="20%" r="80%">
          <stop offset="0" stop-color="white" stop-opacity=".95" />
          <stop offset=".7" stop-color="white" stop-opacity="0" />
        </radialGradient>
        <filter :id="`${id}-shadow`" x="-60%" y="-60%" width="220%" height="220%">
          <feDropShadow dx="0" dy="7" stdDeviation="9" flood-color="#45565c" flood-opacity=".16" />
        </filter>
      </defs>
      <g class="trace-rays">
        <g
          v-for="ray in rays"
          :key="ray.id"
          :opacity="hovered && hovered !== ray.kind ? 0.16 : 1"
          @pointerenter="hovered = ray.kind"
          @pointerleave="hovered = null"
        >
          <line
            :x1="ray.fromX"
            :y1="ray.fromY"
            :x2="ray.x"
            :y2="ray.y"
            :stroke="ray.color"
            :stroke-opacity="hovered === ray.kind ? 0.38 : 0.13"
            stroke-width=".7"
          />
          <circle
            :cx="ray.x"
            :cy="ray.y"
            :r="hovered === ray.kind ? 2.1 : 1.5"
            :fill="ray.color"
            fill-opacity=".85"
          >
            <title>{{ ray.name }} · {{ ray.sub }}</title>
          </circle>
        </g>
      </g>
      <g v-for="dial in dials" :key="dial.kind" :style="{ '--dial-color': colors[dial.kind] }">
        <path
          :d="line(core, dial)"
          fill="none"
          :stroke="colors[dial.kind]"
          stroke-width=".75"
          stroke-opacity=".6"
        />
        <circle r="2.5" :fill="colors[dial.kind]" class="trace-pulse">
          <animateMotion
            :dur="`${5 + dials.indexOf(dial)}s`"
            repeatCount="indefinite"
            :path="line(core, dial)"
          />
        </circle>
        <g
          class="trace-dial"
          :transform="`translate(${dial.x} ${dial.y})`"
          role="button"
          tabindex="0"
          :aria-label="`${labels[dial.kind]}，${dial.count ?? '无读取权限'}，点击查看`"
          @click="chooseKind(dial.kind)"
          @keydown.enter.prevent="chooseKind(dial.kind)"
          @keydown.space.prevent="chooseKind(dial.kind)"
          @pointerenter="hovered = dial.kind"
          @pointerleave="hovered = null"
        >
          <circle r="46" fill="var(--surface-solid)" :filter="`url(#${id}-shadow)`" />
          <line
            v-for="tick in 60"
            :key="tick"
            x1="0"
            y1="-40"
            x2="0"
            y2="-35"
            :transform="`rotate(${tick * 6})`"
            :stroke="tick <= dial.ratio * 60 ? colors[dial.kind] : 'var(--surface-edge)'"
            stroke-width="1.15"
          />
          <foreignObject x="-6" y="-17" width="12" height="12"
            ><component
              :is="icons[dial.kind]"
              :size="12"
              :stroke-width="1.6"
              :color="colors[dial.kind]"
          /></foreignObject>
          <text y="12" text-anchor="middle" class="trace-dial-count">{{ dial.count ?? '—' }}</text>
          <rect
            x="-56"
            y="53"
            width="112"
            height="32"
            rx="8"
            fill="var(--surface-solid)"
            fill-opacity=".75"
          />
          <text y="65" text-anchor="middle" class="trace-dial-label">{{ labels[dial.kind] }}</text>
          <text y="78" text-anchor="middle" class="trace-dial-sub">{{ dial.sub }}</text>
        </g>
      </g>
      <g :transform="`translate(${core.x} ${core.y})`" class="trace-core">
        <line
          v-for="tick in 90"
          :key="tick"
          x1="0"
          y1="-58"
          x2="0"
          y2="-53"
          :transform="`rotate(${tick * 4})`"
          stroke="var(--ink-faint)"
          stroke-opacity=".2"
          stroke-width="1"
        />
        <circle
          r="43"
          :fill="`url(#${id}-pearl)`"
          stroke="#bccdca"
          stroke-opacity=".6"
          :filter="`url(#${id}-shadow)`"
        />
        <circle r="39" :fill="`url(#${id}-shine)`" />
        <circle cx="-35" cy="-53" r="5" fill="#82dd2d" fill-opacity=".2" />
        <circle cx="-35" cy="-53" r="2.5" fill="#82dd2d" />
        <text y="77" text-anchor="middle" class="trace-dial-label">权限全景</text>
        <text y="90" text-anchor="middle" class="trace-dial-sub">{{ grantCount }} 条授权</text>
      </g>
    </svg>
    <PermissionTraceModule
      v-else-if="!selected && cluster"
      :key="`${cluster}-${replay}`"
      :graph="graph"
      :kind="cluster"
      :dials="dials"
      :paused="paused"
      @select="chooseId"
      @cluster="chooseKind"
      @home="home"
    />
    <div v-else-if="selected" class="trace-entity">
      <div class="trace-chain" aria-label="授权链路">
        <div
          v-for="kind in ['user', 'role', 'menu'] as const"
          :key="kind"
          class="trace-chain-column"
        >
          <h3 :style="{ color: colors[kind] }">
            <component :is="icons[kind]" :size="15" />{{ labels[kind]
            }}<span>{{ kind === 'user' && !graph.usersVisible ? '—' : related[kind].length }}</span>
          </h3>
          <button
            v-for="entry in related[kind]"
            :key="entry.id"
            type="button"
            class="trace-entry"
            :class="{ 'is-selected': entry.id === focus, 'is-off': entry.disabled }"
            @click="choose(entry)"
          >
            <span
              ><strong>{{ entry.name }}</strong
              ><small>{{ entry.sub }}</small></span
            ><ChevronRight :size="12" />
          </button>
          <p v-if="!related[kind].length" class="trace-no-results">
            {{ kind === 'user' && !graph.usersVisible ? '无读取权限' : '暂无关联' }}
          </p>
        </div>
      </div>
      <aside class="trace-insight trace-glass">
        <button type="button" class="trace-insight-close" aria-label="关闭详情" @click="back">
          <X :size="16" /></button
        ><span :style="{ color: colors[selected.kind] }">{{ labels[selected.kind] }}</span>
        <h2>{{ selected.name }}</h2>
        <p>{{ selected.sub }}</p>
        <div class="trace-insight-metric">
          <strong>{{
            selected.kind === 'menu'
              ? graph.usersVisible
                ? related.user.length
                : '—'
              : relatedResources.length
          }}</strong><span>{{
            selected.kind === 'menu' ? '具有权限的启用用户' : '关联启用资源'
          }}</span>
        </div>
        <p v-if="selected.disabled" class="trace-warning">
          该{{ labels[selected.kind] }}已停用，不作为普通账号有效权限的来源。
        </p>
        <p v-else-if="selected.sub === 'SUPER_ADMIN'" class="trace-info">
          内置超级管理员按项目规则拥有全部权限。
        </p>
        <p v-if="!graph.usersVisible" class="trace-info">
          没有用户列表权限，用户关系与数量不可见。
        </p>
        <p class="trace-info">
          图中展示角色分配与资源授权关系；停用状态和超级管理员规则会影响最终可用权限。
        </p>
      </aside>
    </div>
    <div class="trace-dock trace-glass" :class="{ 'is-open': answer, 'is-side': !!cluster }">
      <div v-if="answer" class="trace-answer" role="status">
        <button type="button" aria-label="关闭查询结果" @click="clearAnswer">
          <X :size="13" />
        </button>
        <p>{{ answer }}</p>
        <div v-if="answerEntries.length" class="trace-answer-entries">
          <button
            v-for="entry in answerEntries"
            :key="entry.id"
            type="button"
            @click="choose(entry)"
          >
            {{ entry.name }}<ChevronRight :size="11" />
          </button>
        </div>
      </div>
      <form class="trace-ask" @submit.prevent="ask()">
        <Command :size="13" /><input
          v-model="question"
          aria-label="查询权限关系"
          placeholder="问问权限…例如「谁能删除用户」"
          maxlength="100"
        /><button type="submit" aria-label="查询权限"><ArrowRight :size="16" /></button>
      </form>
      <div class="trace-examples">
        <button v-for="example in examples" :key="example" type="button" @click="ask(example)">
          {{ example }}
        </button>
      </div>
    </div>
    <p v-if="!graph.usersVisible && !cluster" class="trace-visibility">
      用户信息无读取权限，相关计数不展示
    </p>
  </section>
</template>

<style scoped>
.permission-trace {
  position: relative;
  height: calc(100dvh - 210px);
  min-height: 650px;
  overflow: hidden;
  border: 1px solid var(--surface-edge);
  border-radius: 24px;
  background:
    radial-gradient(ellipse at 50% 50%, rgba(64, 125, 224, 0.045), transparent 65%),
    radial-gradient(ellipse at 4% 100%, rgba(130, 221, 45, 0.06), transparent 36%),
    radial-gradient(circle, rgba(90, 110, 124, 0.16) 0.65px, transparent 0.8px),
    var(--surface-solid);
  background-size:
    auto,
    auto,
    22px 22px,
    auto;
  box-shadow: var(--shadow-e1);
  color: var(--ink);
}
.trace-glass {
  border: 1px solid var(--glass-edge);
  background: var(--floating-surface);
  box-shadow:
    var(--shadow-e2),
    inset 0 0 0 1px rgba(120, 132, 135, 0.1),
    var(--inset-edge);
  backdrop-filter: blur(28px) saturate(1.08);
}
.trace-crumbs {
  position: absolute;
  z-index: 3;
  top: 18px;
  left: 18px;
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: calc(100% - 36px);
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
}
.trace-crumbs button {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 22px;
  padding: 0 4px;
  white-space: nowrap;
}
.trace-crumbs strong {
  overflow: hidden;
  max-width: 140px;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}
.trace-legend {
  position: absolute;
  z-index: 2;
  right: 18px;
  top: 18px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 17px;
  padding: 16px;
  border-radius: 20px;
}
.trace-legend dt {
  display: flex;
  gap: 7px;
  align-items: center;
  font-size: 11px;
  color: var(--ink-soft);
  font-weight: 600;
  white-space: nowrap;
}
.trace-legend i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.trace-legend dd {
  margin-top: 3px;
  font-size: 21px;
  font-weight: 650;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.trace-legend .is-warn dd {
  color: #cc8b19;
}
.trace-controls {
  position: absolute;
  z-index: 2;
  right: 18px;
  top: 157px;
  display: flex;
  gap: 16px;
  padding: 7px 12px;
  border-radius: 999px;
}
.trace-controls button {
  display: flex;
  gap: 7px;
  align-items: center;
  font-size: 11px;
  color: var(--ink-soft);
}
.trace-stage {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 650px;
}
.trace-dial {
  cursor: pointer;
  outline: none;
}
.trace-dial:focus-visible > circle {
  stroke: var(--brand);
  stroke-width: 2;
}
.trace-dial:hover > circle {
  stroke: var(--dial-color);
  stroke-opacity: 0.25;
}
.trace-dial-count {
  font-size: 20px;
  font-weight: 680;
  fill: var(--ink);
  font-variant-numeric: tabular-nums;
}
.trace-dial-label {
  font-size: 11px;
  font-weight: 650;
  fill: var(--ink);
}
.trace-dial-sub {
  font-size: 9px;
  font-weight: 550;
  fill: var(--ink-soft);
}
.trace-core {
  animation: pearl-arrive 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.trace-dial {
  animation: dial-arrive 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.trace-pulse {
  opacity: 0.7;
}
.is-paused .trace-pulse {
  display: none;
}
.is-paused .trace-core,
.is-paused .trace-dial {
  animation-play-state: paused;
}
.trace-dock {
  position: absolute;
  bottom: 16px;
  left: 50%;
  z-index: 4;
  width: min(440px, calc(100% - 32px));
  padding: 7px;
  border-radius: 21px;
  translate: -50% 0;
}
.trace-ask {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 5px 4px 13px;
  border: 1px solid var(--surface-edge);
  border-radius: 14px;
  background: var(--input-surface);
  color: var(--ink-soft);
}
.trace-ask input {
  flex: 1;
  min-width: 0;
  height: 28px;
  border: 0;
  outline: none;
  background: transparent;
  font-size: 12px;
  color: var(--ink);
}
.trace-ask:focus-within {
  outline: 2px solid var(--brand-ring);
}
.trace-ask > button {
  display: grid;
  place-items: center;
  flex: none;
  width: 29px;
  height: 29px;
  border-radius: 10px;
  background: var(--brand);
  color: white;
  opacity: 0.6;
}
.trace-ask:focus-within > button {
  opacity: 1;
}
.trace-examples {
  display: flex;
  gap: 6px;
  margin-top: 7px;
  overflow-x: auto;
  scrollbar-width: none;
}
.trace-examples button {
  flex: none;
  border: 1px solid var(--surface-edge);
  border-radius: 999px;
  padding: 5px 9px;
  background: var(--hover-surface);
  font-size: 10.5px;
  color: var(--ink-muted);
}
.trace-answer {
  position: relative;
  padding: 8px 10px 12px;
  font-size: 12px;
  line-height: 1.7;
}
.trace-answer > button {
  float: right;
  padding: 3px;
}
.trace-answer p {
  padding-right: 15px;
}
.trace-answer-entries {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  max-height: 130px;
  overflow: auto;
  margin-top: 8px;
}
.trace-answer-entries button {
  display: flex;
  gap: 3px;
  align-items: center;
  padding: 3px 7px;
  background: var(--brand-soft);
  color: var(--brand-strong);
  border-radius: 6px;
  font-size: 11px;
}
.trace-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  width: 100%;
  padding: 10px 13px;
  background: var(--floating-surface);
  border: 1px solid var(--surface-edge);
  border-radius: 12px;
  text-align: left;
  transition:
    background 0.18s,
    border-color 0.18s;
}
.trace-entry:hover,
.trace-entry.is-selected {
  background: var(--brand-soft);
  border-color: var(--brand-ring);
}
.trace-entry > i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex: none;
}
.trace-entry > span {
  flex: 1;
  min-width: 0;
}
.trace-entry strong {
  display: block;
  font-size: 12px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.trace-entry small {
  display: block;
  margin-top: 3px;
  font-size: 10px;
  color: var(--ink-soft);
  overflow-wrap: anywhere;
}
.trace-entry > svg {
  flex: none;
  color: var(--ink-soft);
}
.trace-entry.is-off {
  opacity: 0.6;
}
.trace-no-results {
  padding: 20px 8px;
  color: var(--ink-soft);
  font-size: 12px;
  text-align: center;
}
.trace-entity {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 24px;
  height: 100%;
  padding: 87px 22px 150px;
}
.trace-chain {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  min-height: 0;
}
.trace-chain-column {
  overflow-y: auto;
  padding: 0 3px 10px;
}
.trace-chain-column h3 {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  gap: 7px;
  align-items: center;
  padding: 12px 2px;
  font-size: 12px;
  font-weight: 650;
  background: var(--surface-solid);
}
.trace-chain-column h3 > span {
  margin-left: auto;
  color: var(--ink-soft);
  font-size: 11px;
}
.trace-chain-column .trace-entry {
  margin-bottom: 10px;
  position: relative;
}
.trace-insight {
  position: relative;
  border-radius: 18px;
  padding: 20px;
  overflow-y: auto;
  align-self: start;
  max-height: 100%;
}
.trace-insight > span {
  font-size: 11px;
}
.trace-insight h2 {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 650;
  overflow-wrap: anywhere;
}
.trace-insight > p {
  font-size: 11px;
  color: var(--ink-soft);
  margin-top: 7px;
  overflow-wrap: anywhere;
}
.trace-insight-close {
  position: absolute;
  right: 12px;
  top: 12px;
  padding: 5px;
  color: var(--ink-soft);
}
.trace-insight-metric {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 24px 0;
  margin: 12px 0;
  border-block: 1px solid var(--surface-edge);
}
.trace-insight-metric strong {
  font-size: 38px;
  line-height: 1;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.trace-insight-metric span {
  font-size: 11px;
  color: var(--ink-soft);
}
.trace-insight p.trace-warning {
  padding: 9px;
  background: rgba(204, 139, 25, 0.08);
  color: #a46b0b;
  border-radius: 9px;
  line-height: 1.7;
}
.trace-insight p.trace-info {
  margin-top: 15px;
  line-height: 1.7;
}
.trace-visibility {
  position: absolute;
  bottom: 126px;
  width: 100%;
  text-align: center;
  font-size: 11px;
  color: var(--ink-soft);
}
.permission-trace button:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 3px;
}
@keyframes pearl-arrive {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@keyframes dial-arrive {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
@media (min-width: 1600px) {
  .trace-entity {
    gap: 40px;
    padding-inline: 55px;
  }
  .trace-chain {
    gap: 38px;
  }
}
@media (max-width: 900px) {
  .trace-entity {
    grid-template-columns: 1fr;
    overflow-y: auto;
    padding-bottom: 160px;
  }
  .trace-chain {
    min-height: 250px;
    max-height: 340px;
    gap: 10px;
  }
  .trace-insight {
    max-height: none;
  }
  .trace-insight-metric {
    padding: 14px 0;
  }
}
@media (max-width: 640px) {
  .permission-trace {
    height: 720px;
    min-height: 720px;
    border-radius: 20px;
  }
  .trace-stage {
    height: 330px;
    position: absolute;
    top: 235px;
    min-height: 0;
    transform: none;
  }
  .trace-legend {
    top: 70px;
    left: 16px;
    right: 16px;
    gap: 10px;
    padding: 12px 16px;
  }
  .trace-legend dd {
    font-size: 18px;
  }
  .trace-controls {
    top: 205px;
    right: 16px;
  }
  .trace-crumbs {
    font-size: 11px;
    top: 15px;
    left: 15px;
  }
  .trace-entity {
    padding: 75px 12px 155px;
    gap: 16px;
  }
  .trace-chain {
    gap: 6px;
  }
  .trace-chain-column h3 {
    font-size: 11px;
  }
  .trace-chain-column .trace-entry {
    padding: 8px 6px;
    border-radius: 9px;
  }
  .trace-chain-column .trace-entry strong {
    font-size: 11px;
  }
  .trace-chain-column .trace-entry small {
    font-size: 9px;
  }
  .trace-chain-column .trace-entry > svg {
    display: none;
  }
  .trace-insight {
    padding: 15px;
  }
  .trace-dock {
    bottom: 12px;
    width: calc(100% - 24px);
  }
  .trace-examples button {
    font-size: 10px;
  }
  .trace-visibility {
    bottom: 125px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .trace-core,
  .trace-dial {
    animation: none;
  }
  .trace-pulse {
    display: none;
  }
  .trace-entry {
    transition: none;
  }
}
.is-module .trace-controls {
  top: 18px;
}
.is-module .trace-crumbs > button:last-child {
  color: #6748a6;
  font-weight: 650;
}
.trace-dock.is-side {
  left: 18px;
  translate: 0 0;
}
.is-module .trace-examples {
  display: none;
}
@media (max-width: 640px) {
  .trace-dock.is-side {
    left: 12px;
  }
  .is-module .trace-controls {
    top: 15px;
    right: 12px;
    padding: 7px 8px;
    gap: 8px;
  }
  .is-module .trace-crumbs {
    max-width: calc(100% - 135px);
    gap: 1px;
    padding-inline: 6px;
  }
  .is-module .trace-crumbs button {
    font-size: 10px;
    gap: 4px;
  }
}
</style>
