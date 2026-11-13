<script setup lang="ts">
import { Activity, ArrowRight, FileClock, RefreshCw } from '@lucide/vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { format, startOfDay, startOfHour, subDays, subHours } from 'date-fns'
import { ApiError } from '@/api/http'
import { fetchLoginLogs, fetchOperationLogs } from '@/api/logs'
import type { LoginLogSummary, OperationLogSummary } from '@/api/types'
import AppButton from '@/components/app/AppButton.vue'
import AnimatedNumber from '@/components/app/AnimatedNumber.vue'
import AppErrorState from '@/components/app/AppErrorState.vue'
import AppStatusTag from '@/components/app/AppStatusTag.vue'
import TrendChart from '@/components/app/TrendChart.vue'
import { formatDateTime } from '@/lib/format'
import { useAuthStore } from '@/stores/auth'
import { firstTimeInSession, motionSkipped, settleFrames } from '@/lib/motion'

const auth = useAuthStore()
const grid = ref<InstanceType<typeof globalThis.HTMLElement> | null>(null)
const entrances: InstanceType<typeof globalThis.Animation>[] = []
let entranceFrame = 0
function finishHidden(): void {
  if (globalThis.document.hidden) entrances.forEach((animation) => animation.finish())
}
const range = ref<24 | 7 | 30>(24)
const now = ref(new Date())
const loginLogs = ref<LoginLogSummary[]>([])
const operationLogs = ref<OperationLogSummary[]>([])
const loginTotal = ref(0)
const operationTotal = ref(0)
const loading = ref(false)
const hasLoaded = ref(false)
const initialLoading = computed(() => !hasLoaded.value)
const loginError = ref('')
const operationError = ref('')
const canLogin = computed(() => auth.hasPermission('audit:login:list'))
const canOperation = computed(() => auth.hasPermission('audit:operation:list'))
const name = computed(() => auth.user?.nickname || auth.user?.username || '管理员')
const greeting = computed(() => {
  const hour = now.value.getHours()
  return hour < 6
    ? '夜深了'
    : hour < 12
      ? '上午好'
      : hour < 14
        ? '中午好'
        : hour < 18
          ? '下午好'
          : '晚上好'
})
const rangeText = computed(() => (range.value === 24 ? '近 24 小时' : `近 ${range.value} 天`))
const dateStamp = computed(
  () => `${format(now.value, 'yyyy 年 M 月 d 日')} · 星期${'日一二三四五六'[now.value.getDay()]}`,
)
const cutoff = computed(() =>
  range.value === 24
    ? startOfHour(subHours(now.value, 23))
    : startOfDay(subDays(now.value, range.value - 1)),
)
const logins = computed(() =>
  loginLogs.value.filter(
    (log) => log.action === 'LOGIN' && log.success && new Date(log.createdAt) >= cutoff.value,
  ),
)
const recentOperations = computed(() =>
  operationLogs.value.filter((log) => new Date(log.createdAt) >= subHours(now.value, 24)),
)
const successRate = computed(() =>
  recentOperations.value.length
    ? Math.round(
        (recentOperations.value.filter((log) => log.success).length /
          recentOperations.value.length) *
          1000,
      ) / 10
    : null,
)
const buckets = computed(() =>
  Array.from({ length: range.value === 24 ? 24 : range.value }, (_, index) => {
    const date =
      range.value === 24
        ? subHours(now.value, 23 - index)
        : subDays(now.value, range.value - 1 - index)
    return {
      key: format(date, range.value === 24 ? 'yyyy-MM-dd HH' : 'yyyy-MM-dd'),
      label: format(date, range.value === 24 ? 'HH:00' : 'M/d'),
    }
  }),
)
const trend = computed(() =>
  buckets.value.map(
    (bucket) =>
      logins.value.filter(
        (log) =>
          format(new Date(log.createdAt), range.value === 24 ? 'yyyy-MM-dd HH' : 'yyyy-MM-dd') ===
          bucket.key,
      ).length,
  ),
)
const peak = computed(() => Math.max(0, ...trend.value))
const daily = computed(() =>
  Array.from({ length: 7 }, (_, index) => {
    const date = subDays(now.value, 6 - index)
    const logs = operationLogs.value.filter(
      (log) => log.createdAt.slice(0, 10) === format(date, 'yyyy-MM-dd'),
    )
    return {
      label: index === 6 ? '今天' : `周${'日一二三四五六'[date.getDay()]}`,
      total: logs.length,
      failed: logs.filter((log) => !log.success).length,
    }
  }),
)
const dailyMax = computed(() => Math.max(1, ...daily.value.map((day) => day.total)))
const modules = computed(() => {
  const counts = new Map<string, number>()
  for (const log of operationLogs.value) counts.set(log.module, (counts.get(log.module) ?? 0) + 1)
  return [...counts]
    .map(([label, value]) => ({
      label,
      value,
      percent: Math.round((value / Math.max(1, operationLogs.value.length)) * 100),
    }))
    .sort((a, b) => b.value - a.value)
})
const lanes = computed(() =>
  modules.value.slice(0, 5).map((module) => ({
    label: module.label,
    logs: operationLogs.value.filter(
      (log) =>
        log.module === module.label &&
        log.createdAt.slice(0, 10) === format(now.value, 'yyyy-MM-dd'),
    ),
  })),
)
const ticks = Array.from({ length: 60 }, (_, index) => {
  const angle = ((index * 6 - 90) * Math.PI) / 180
  return {
    x1: 100 + Math.cos(angle) * 76,
    y1: 100 + Math.sin(angle) * 76,
    x2: 100 + Math.cos(angle) * 90,
    y2: 100 + Math.sin(angle) * 90,
  }
})
function lanePosition(date: string): number {
  const parsed = new Date(date)
  return ((parsed.getHours() * 60 + parsed.getMinutes()) / 1440) * 100
}
async function load(): Promise<void> {
  if (loading.value) return
  loading.value = true
  loginError.value = ''
  operationError.value = ''
  const end = new Date()
  const params = {
    page: 1,
    size: 100,
    sort: 'createdAt,desc',
    startTime: format(subDays(end, 30), "yyyy-MM-dd'T'HH:mm:ss"),
    endTime: format(end, "yyyy-MM-dd'T'HH:mm:ss"),
  }
  const [login, operation] = await Promise.allSettled([
    canLogin.value ? fetchLoginLogs(params) : Promise.resolve(null),
    canOperation.value ? fetchOperationLogs(params) : Promise.resolve(null),
  ])
  now.value = end
  if (login.status === 'fulfilled' && login.value) {
    loginLogs.value = login.value.items
    loginTotal.value = login.value.total
  } else if (login.status === 'rejected') {
    loginLogs.value = []
    loginError.value = login.reason instanceof ApiError ? login.reason.message : '登录记录加载失败'
  }
  if (operation.status === 'fulfilled' && operation.value) {
    operationLogs.value = operation.value.items
    operationTotal.value = operation.value.total
  } else if (operation.status === 'rejected') {
    operationLogs.value = []
    operationError.value =
      operation.reason instanceof ApiError ? operation.reason.message : '操作记录加载失败'
  }
  loading.value = false
  hasLoaded.value = true
}
onMounted(() => {
  void load()
  globalThis.document.addEventListener('visibilitychange', finishHidden)
  if (motionSkipped()) return
  const intro = firstTimeInSession('workbench')
  entranceFrame = globalThis.requestAnimationFrame(() => {
    if (!grid.value) return
    const bounds = grid.value.getBoundingClientRect()
    const centerX = bounds.left + bounds.width / 2
    const centerY = bounds.top + Math.min(bounds.height / 2, 300)
    const cards = [
      ...grid.value.querySelectorAll<InstanceType<typeof globalThis.HTMLElement>>(
        '.workbench-card',
      ),
    ].map((element) => ({ element, bounds: element.getBoundingClientRect() }))
    cards.forEach(({ element, bounds: card }, index) => {
      entrances.push(
        element.animate(
          intro
            ? settleFrames(
                centerX - card.left - card.width / 2,
                centerY - card.top - card.height / 2,
              )
            : [
                { transform: 'translateY(18px)', opacity: 0 },
                { transform: 'translateY(0)', opacity: 1 },
              ],
          {
            id: intro ? 'workbench-intro' : 'workbench-return',
            duration: 720,
            delay: intro ? 50 + index * 50 : 100 + Math.abs(index - (cards.length - 1) / 2) * 45,
            easing: intro ? 'linear' : 'cubic-bezier(.16, 1, .3, 1)',
            fill: 'backwards',
          },
        ),
      )
    })
  })
})
onUnmounted(() => {
  globalThis.document.removeEventListener('visibilitychange', finishHidden)
  globalThis.cancelAnimationFrame(entranceFrame)
  entrances.forEach((animation) => animation.cancel())
})
</script>

<template>
  <div class="workbench" :aria-busy="loading">
    <header class="workbench-heading">
      <p class="workbench-date">{{ dateStamp }}</p>
      <h1>{{ greeting }}，{{ name }}</h1>
      <p>系统运行概况、登录脉搏与今日操作轨迹。</p>
    </header>
    <p class="stat-scope">
      统计范围：近 30 天最新 {{ loginLogs.length }} 条登录记录 /
      {{ operationLogs.length }} 条操作记录，图表仅统计这些记录。<span
        v-if="loginTotal > 100 || operationTotal > 100"
        >完整记录请查看日志。</span
      >
    </p>
    <AppErrorState
      v-if="loginError || operationError"
      :message="[loginError, operationError].filter(Boolean).join('；')"
      @retry="load"
    />
    <div ref="grid" class="workbench-grid" :class="{ 'is-loading': initialLoading }">
      <section class="workbench-card pulse-card" aria-label="登录脉搏">
        <header class="card-heading">
          <h2>登录脉搏</h2>
          <div class="range-switch" role="group" aria-label="时间范围">
            <button
              v-for="option in [24, 7, 30] as const"
              :key="option"
              type="button"
              :aria-pressed="range === option"
              @click="range = option"
            >
              {{ option === 24 ? '24 小时' : `${option} 天` }}
            </button>
          </div>
        </header>
        <div v-if="!canLogin" class="dashboard-empty">暂无登录日志查看权限</div>
        <template v-else>
          <div class="pulse-body">
            <div class="pulse-figure">
              <div class="metric">
                <AnimatedNumber :value="initialLoading || loginError ? null : logins.length" /><span
                  >次</span
                >
              </div>
              <p class="metric-caption">{{ rangeText }}成功登录</p>
              <dl class="pulse-readouts">
                <div>
                  <dt>峰值</dt>
                  <dd>{{ peak }}</dd>
                </div>
                <div>
                  <dt>均值</dt>
                  <dd>{{ (logins.length / trend.length).toFixed(1) }}</dd>
                </div>
                <div>
                  <dt>范围</dt>
                  <dd>{{ rangeText }}</dd>
                </div>
              </dl>
            </div>
            <div class="pulse-chart">
              <TrendChart
                :values="hasLoaded ? trend : []"
                :labels="buckets.map((bucket) => bucket.label)"
              />
            </div>
          </div>
          <div class="card-actions">
            <RouterLink to="/audit/login-logs" class="dashboard-link"
              >查看登录日志 <ArrowRight :size="14" /></RouterLink
            ><AppButton variant="outline" :loading="loading" @click="load"
              ><RefreshCw :size="14" />刷新</AppButton
            >
          </div>
        </template>
      </section>
      <section class="workbench-card side-card">
        <header class="card-heading">
          <h2>登录账号 · {{ rangeText }}</h2>
          <Activity :size="17" class="side-icon" />
        </header>
        <div class="metric side-metric">
          <AnimatedNumber
            :value="
              !canLogin || loginError || initialLoading
                ? null
                : new Set(logins.map((log) => log.username)).size
            "
          /><span>人</span>
        </div>
        <p class="metric-caption">
          {{ canLogin ? '所统计记录中的成功登录账号' : '暂无登录日志查看权限' }}
        </p>
      </section>
      <section class="workbench-card side-card">
        <header class="card-heading">
          <h2>操作记录 · 近 24 小时</h2>
          <FileClock :size="17" class="side-icon" />
        </header>
        <div class="metric side-metric">
          <AnimatedNumber
            :value="
              !canOperation || operationError || initialLoading ? null : recentOperations.length
            "
          /><span>次</span>
        </div>
        <p class="metric-caption">
          {{ canOperation ? '所统计记录中的操作次数' : '暂无操作日志查看权限' }}
        </p>
      </section>
      <section class="workbench-card third-card">
        <header class="card-heading">
          <h2>近 7 天操作量</h2>
          <span>{{ daily.reduce((sum, day) => sum + day.total, 0) }} 次</span>
        </header>
        <div v-if="!canOperation || operationError" class="dashboard-empty">
          {{ operationError ? '操作记录加载失败' : '暂无操作日志查看权限' }}
        </div>
        <div
          v-else-if="hasLoaded"
          class="week-chart"
          role="img"
          :aria-label="
            daily.map((day) => `${day.label} ${day.total}次，失败${day.failed}次`).join('；')
          "
        >
          <div v-for="(day, index) in daily" :key="index" class="week-column">
            <div class="week-track">
              <div
                class="week-bar"
                :class="{ 'is-today': index === 6 }"
                :style="{
                  height: `${(day.total / dailyMax) * 100}%`,
                  '--bar-delay': `${index * 45}ms`,
                }"
              >
                <span
                  v-if="day.failed"
                  class="week-failed"
                  :style="{ height: `${(day.failed / day.total) * 100}%` }"
                ></span
                ><span v-if="day.total" class="bar-value">{{ day.total }}</span>
              </div>
            </div>
            <span>{{ day.label }}</span>
          </div>
        </div>
      </section>
      <section class="workbench-card third-card">
        <header class="card-heading">
          <h2>操作成功率</h2>
          <span>近 24 小时</span>
        </header>
        <div class="success-body">
          <div class="success-ring">
            <svg viewBox="0 0 200 200" aria-hidden="true">
              <line
                v-for="(tick, index) in ticks"
                :key="index"
                v-bind="tick"
                :class="{
                  'is-success':
                    canOperation &&
                    !operationError &&
                    successRate !== null &&
                    index < (successRate / 100) * 60,
                }"
              />
            </svg>
            <div>
              <strong
                >{{
                  !canOperation || operationError || initialLoading || successRate === null
                    ? '—'
                    : successRate
                }}<small v-if="successRate !== null && canOperation && !operationError"
                  >%</small
                ></strong
              ><span>{{
                !canOperation ? '暂无权限' : successRate === null ? '暂无操作记录' : '成功'
              }}</span>
            </div>
          </div>
          <dl>
            <dt>操作</dt>
            <dd>{{ canOperation && !operationError ? recentOperations.length : '—' }}</dd>
            <dt>成功率</dt>
            <dd>
              {{
                canOperation && !operationError && successRate !== null ? `${successRate}%` : '—'
              }}
            </dd>
          </dl>
        </div>
      </section>
      <section class="workbench-card third-card">
        <header class="card-heading">
          <h2>操作模块分布</h2>
          <span>近期记录</span>
        </header>
        <div v-if="!canOperation || operationError || !modules.length" class="dashboard-empty">
          {{
            !canOperation
              ? '暂无操作日志查看权限'
              : operationError
                ? '操作记录加载失败'
                : '暂无操作记录'
          }}
        </div>
        <div v-else class="module-list">
          <div v-for="(module, index) in modules" :key="module.label" class="module-row">
            <span>{{ module.label }}</span>
            <div class="module-track">
              <span
                :style="{
                  width: `${module.percent}%`,
                  background: ['#a38c57', '#658bb6', '#8b78b2', '#bc7e8e'][index % 4],
                }"
              ></span>
            </div>
            <strong
              >{{ module.value }} <small>· {{ module.percent }}%</small></strong
            >
          </div>
        </div>
      </section>
      <section class="workbench-card timeline-card">
        <header class="card-heading">
          <h2>今日操作泳道</h2>
          <RouterLink v-if="canOperation" to="/audit/operation-logs" class="dashboard-link light"
            >操作日志 <ArrowRight :size="14"
          /></RouterLink>
        </header>
        <div
          v-if="!canOperation || operationError || !lanes.some((lane) => lane.logs.length)"
          class="dashboard-empty"
        >
          {{
            !canOperation
              ? '暂无操作日志查看权限'
              : operationError
                ? '操作记录加载失败'
                : '今天暂无操作记录'
          }}
        </div>
        <div v-else class="timeline">
          <div class="timeline-ruler">
            <span v-for="hour in [0, 6, 12, 18, 24]" :key="hour">{{ hour }}:00</span>
          </div>
          <div v-for="(lane, index) in lanes" :key="lane.label" class="timeline-row">
            <span>{{ lane.label }}</span>
            <div>
              <span
                v-for="log in lane.logs"
                :key="log.id"
                class="timeline-event"
                :class="{ 'is-failed': !log.success }"
                :style="{
                  left: `${lanePosition(log.createdAt)}%`,
                  '--lane-color': ['#f6e7c4', '#d7e7fb', '#e4dcf7'][index % 3],
                }"
                tabindex="0"
                :aria-label="`${log.username} ${log.action} ${formatDateTime(log.createdAt)} ${log.success ? '成功' : '失败'}`"
                ><span class="event-tooltip"
                  >{{ log.username }} · {{ log.action }} · {{ formatDateTime(log.createdAt) }}</span
                ></span
              >
            </div>
          </div>
        </div>
      </section>
      <section class="workbench-card recent-card">
        <header class="card-heading"><h2>最近登录</h2></header>
        <div v-if="!canLogin || loginError || !loginLogs.length" class="dashboard-empty">
          {{
            !canLogin ? '暂无登录日志查看权限' : loginError ? '登录记录加载失败' : '暂无登录记录'
          }}
        </div>
        <ul v-else class="recent-logins">
          <li v-for="(log, index) in loginLogs.slice(0, 5)" :key="log.id">
            <span
              class="login-avatar"
              :style="{ background: ['#bf593b', '#8058d4', '#419c43'][index % 3] }"
              >{{ log.username.slice(0, 1).toUpperCase() }}</span
            >
            <div>
              <strong>{{ log.username }}</strong><span>{{
                log.action === 'LOGOUT'
                  ? '退出系统'
                  : log.action === 'LOGIN_FAILED'
                    ? '登录失败'
                    : '登录后台管理系统'
              }}</span
              ><time>{{ formatDateTime(log.createdAt) }}</time>
            </div>
            <AppStatusTag
              :tone="log.success ? 'success' : 'danger'"
              :label="log.success ? '成功' : '失败'"
            />
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>
<style scoped src="@/assets/workbench.css"></style>
