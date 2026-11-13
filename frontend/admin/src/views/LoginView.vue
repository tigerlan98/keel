<script setup lang="ts">
import {
  CircleAlert,
  Eye,
  EyeOff,
  LoaderCircle,
  LockKeyhole,
  ScrollText,
  ShieldCheck,
  SlidersHorizontal,
  UserRound,
  Users,
  type LucideIcon,
} from '@lucide/vue'
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { computed, nextTick, onMounted, onUnmounted, ref, useTemplateRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

type StatusState = 'ready' | 'loading' | 'error'

interface ClockTick {
  index: number
  angle: number
  major: boolean
  active: boolean
  current: boolean
  opacity: number
}

interface FeatureItem {
  icon: LucideIcon
  title: string
  description: string
}

interface Particle {
  left: string
  top: string
  size: string
  opacity: number
}

const WEEKDAYS = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'] as const
const ACTIVE_SECONDS = 14

const STATUS_TEXT: Record<StatusState, string> = {
  ready: '就绪',
  loading: '连接中',
  error: '连接中断',
}

const STATUS_NOTE: Record<StatusState, string> = {
  ready: '等待登录',
  loading: '正在验证凭据',
  error: '请检查后端服务',
}

const features: FeatureItem[] = [
  {
    icon: Users,
    title: '用户与角色',
    description: '账号、角色与按钮级权限统一管理。',
  },
  {
    icon: ShieldCheck,
    title: '资源与菜单',
    description: '资源树、权限码与菜单同源维护。',
  },
  {
    icon: SlidersHorizontal,
    title: '系统参数',
    description: 'Flyway 迁移维护，Redis 缓存读取。',
  },
  {
    icon: ScrollText,
    title: '审计日志',
    description: '操作日志异步写入，登录记录可追溯。',
  },
]

const particles: Particle[] = [
  { left: '15%', top: '36%', size: '3px', opacity: 0.45 },
  { left: '28%', top: '64%', size: '2px', opacity: 0.3 },
  { left: '47%', top: '17%', size: '2px', opacity: 0.42 },
  { left: '74%', top: '25%', size: '2px', opacity: 0.38 },
  { left: '86%', top: '58%', size: '3px', opacity: 0.28 },
  { left: '57%', top: '76%', size: '2px', opacity: 0.24 },
]

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const submitting = ref(false)
const submitError = ref('')
const showPassword = ref(false)
const passwordRef = useTemplateRef<{ focus: () => void }>('passwordRef')
const statusState = ref<StatusState>('ready')

const statusText = computed(() => STATUS_TEXT[statusState.value])
const statusNote = computed(() => STATUS_NOTE[statusState.value])

const now = ref(new Date())
let clockTimer: ReturnType<typeof globalThis.setInterval> | undefined

const timeText = computed(() => {
  const date = now.value
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
})

const dateText = computed(() => {
  const date = now.value
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} · ${WEEKDAYS[date.getDay()]}`
})

const clockTicks = computed<ClockTick[]>(() => {
  const seconds = now.value.getSeconds()

  return Array.from({ length: 60 }, (_, index) => {
    const diff = (seconds - index + 60) % 60
    const major = index % 5 === 0
    const active = diff < ACTIVE_SECONDS
    const strength = active ? 1 - diff / ACTIVE_SECONDS : 0

    return {
      index,
      angle: index * 6,
      major,
      active,
      current: diff === 0,
      opacity: active ? 0.55 + strength * 0.45 : major ? 0.45 : 0.2,
    }
  })
})

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

function updateClock(): void {
  now.value = new Date()
}

onMounted(() => {
  updateClock()
  clockTimer = globalThis.setInterval(updateClock, 1000)
})

onUnmounted(() => {
  if (clockTimer !== undefined) {
    globalThis.clearInterval(clockTimer)
  }
})

const validationSchema = toTypedSchema(
  z.object({
    username: z.string().min(1, '请输入用户名'),
    password: z.string().min(1, '请输入密码'),
  }),
)

const { defineField, errors, handleSubmit } = useForm({
  validationSchema,
  initialValues: { username: '', password: '' },
})

const [username, usernameAttrs] = defineField('username')
const [password, passwordAttrs] = defineField('password')

const passwordType = computed(() => (showPassword.value ? 'text' : 'password'))

watch([username, password], () => {
  submitError.value = ''
  if (!submitting.value) {
    statusState.value = 'ready'
  }
})

async function togglePassword(): Promise<void> {
  showPassword.value = !showPassword.value
  await nextTick()
  passwordRef.value?.focus()
}

const onSubmit = handleSubmit(async (values) => {
  if (submitting.value) {
    return
  }

  submitError.value = ''
  submitting.value = true
  statusState.value = 'loading'

  try {
    await auth.login(values.username, values.password)
  } catch (error) {
    if (error instanceof ApiError && error.status !== undefined) {
      statusState.value = 'ready'
      submitError.value = error.message
    } else {
      statusState.value = 'error'
      submitError.value =
        error instanceof ApiError
          ? '无法连接服务，请确认后端已启动或稍后重试。'
          : '登录失败，请稍后重试'
    }
    return
  } finally {
    submitting.value = false
  }

  const redirect = route.query.redirect
  const target =
    typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
      ? redirect
      : '/'

  await router.push(target)
})
</script>

<template>
  <div class="login-page">
    <main class="shell">
      <section class="hero" aria-label="Keel 管理控制台">
        <div class="hero-glow" aria-hidden="true"></div>
        <div class="particles" aria-hidden="true">
          <span
            v-for="particle in particles"
            :key="particle.left + particle.top"
            :style="{
              left: particle.left,
              top: particle.top,
              width: particle.size,
              height: particle.size,
              opacity: particle.opacity,
            }"
          ></span>
        </div>

        <div class="hero-top">
          <span class="brand-mark" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path
                d="M6.5 4v16"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="square"
              />
              <path
                d="M7 11.5 19 4"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="square"
              />
              <path
                d="M7 12.5 19 20"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="square"
              />
              <circle cx="7" cy="12" r="2" fill="#6AC4B3" />
            </svg>
          </span>
          <div>
            <span class="wordmark">KEEL</span>
            <span class="brand-sub">ADMIN CONSOLE · 管理控制台</span>
          </div>
        </div>

        <div class="hero-main">
          <div class="hero-copy">
            <span class="eyebrow">后台底座 · v1.0.0</span>
            <p class="hero-title">
              <span>统一的后台底座，</span>
              <span>专注业务本身。</span>
            </p>
            <p class="hero-copy-text">
              认证、RBAC、系统参数与审计日志开箱即用；<br />
              新业务模块只需关注字段与流程。
            </p>
          </div>

          <div class="clock" aria-hidden="true">
            <svg viewBox="0 0 260 260" fill="none">
              <circle
                cx="130"
                cy="130"
                r="86"
                stroke="rgba(245, 244, 239, 0.06)"
                stroke-width="1"
              />
              <g transform="translate(130 130)">
                <line
                  v-for="tick in clockTicks"
                  :key="tick.index"
                  class="clock-tick"
                  :class="{ major: tick.major, active: tick.active, current: tick.current }"
                  x1="0"
                  y1="-102"
                  x2="0"
                  :y2="tick.major ? -119 : -112"
                  stroke="rgba(245, 244, 239, 0.42)"
                  stroke-linecap="round"
                  :transform="`rotate(${tick.angle})`"
                  :opacity="tick.opacity"
                />
              </g>
            </svg>
            <div class="clock-readout">
              <span class="clock-time">{{ timeText }}</span>
              <span class="clock-date">{{ dateText }}</span>
            </div>
          </div>
        </div>

        <div class="hero-bottom">
          <div class="status-card" :data-state="statusState" role="status" aria-live="polite">
            <span class="status-label">服务连接</span>
            <div class="status-main">
              <span class="status-dot" aria-hidden="true"></span>
              <span>{{ statusText }}</span>
            </div>
            <span class="status-note">{{ statusNote }}</span>
          </div>

          <div class="feature-panel">
            <ul class="features">
              <li v-for="feature in features" :key="feature.title" class="feature">
                <span class="feature-icon" aria-hidden="true">
                  <component :is="feature.icon" :size="15" :stroke-width="1.8" />
                </span>
                <div>
                  <p class="feature-title">{{ feature.title }}</p>
                  <p class="feature-desc">{{ feature.description }}</p>
                </div>
              </li>
            </ul>
          </div>
        </div>

        <div class="hero-footer">© 2026 Keel · 管理端</div>
      </section>

      <section class="auth">
        <div class="login-card">
          <header class="login-head">
            <span class="login-kicker">账号登录</span>
            <h1>登录</h1>
            <p>使用管理员账号进入 Keel 管理控制台</p>
          </header>

          <form class="login-form" novalidate @submit="onSubmit">
            <div class="field">
              <label for="username">用户名</label>
              <div class="input-shell">
                <span class="input-icon" aria-hidden="true">
                  <UserRound :size="16" :stroke-width="1.7" />
                </span>
                <input
                  id="username"
                  v-model="username"
                  v-bind="usernameAttrs"
                  type="text"
                  autocomplete="username"
                  placeholder="请输入用户名"
                  :aria-invalid="errors.username ? 'true' : 'false'"
                  :aria-describedby="errors.username ? 'username-error' : undefined"
                />
              </div>
              <p v-if="errors.username" id="username-error" class="field-error" role="alert">
                <CircleAlert :size="13" :stroke-width="1.8" aria-hidden="true" />
                <span>{{ errors.username }}</span>
              </p>
            </div>

            <div class="field has-reveal">
              <label for="password">密码</label>
              <div class="input-shell">
                <span class="input-icon" aria-hidden="true">
                  <LockKeyhole :size="16" :stroke-width="1.7" />
                </span>
                <input
                  id="password"
                  ref="passwordRef"
                  v-model="password"
                  v-bind="passwordAttrs"
                  :type="passwordType"
                  autocomplete="current-password"
                  placeholder="请输入密码"
                  :aria-invalid="errors.password ? 'true' : 'false'"
                  :aria-describedby="errors.password ? 'password-error' : undefined"
                />
                <button
                  type="button"
                  class="reveal"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  :aria-pressed="showPassword"
                  @click="togglePassword"
                >
                  <EyeOff v-if="showPassword" :size="17" :stroke-width="1.7" />
                  <Eye v-else :size="17" :stroke-width="1.7" />
                </button>
              </div>
              <p v-if="errors.password" id="password-error" class="field-error" role="alert">
                <CircleAlert :size="13" :stroke-width="1.8" aria-hidden="true" />
                <span>{{ errors.password }}</span>
              </p>
            </div>

            <div v-if="submitError" class="form-error" role="alert">
              <CircleAlert :size="14" :stroke-width="1.8" aria-hidden="true" />
              <span>{{ submitError }}</span>
            </div>

            <button class="submit" type="submit" :disabled="submitting" :aria-busy="submitting">
              <LoaderCircle v-if="submitting" :size="15" class="animate-spin" aria-hidden="true" />
              <span>{{ submitting ? '正在登录' : '登录' }}</span>
            </button>
          </form>

          <p class="form-note">
            <LockKeyhole :size="12" :stroke-width="1.7" aria-hidden="true" />
            <span>仅限授权管理员访问。连续失败 5 次将锁定 15 分钟。</span>
          </p>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.login-page {
  --lp-ink: #0b1312;
  --lp-ink-raise: #16302b;
  --lp-verdigris: #3e8b7e;
  --lp-verdigris-bright: #6ac4b3;
  --lp-verdigris-ring: #8adbc9;
  --lp-canvas: #eef0eb;
  --lp-card: #ffffff;
  --lp-paper: #f5f4ef;
  --lp-line: #dcdfd8;
  --lp-muted: #5f6a66;
  --lp-danger: #b4433b;
  --lp-font-sans:
    'Inter', 'Segoe UI Variable Text', 'Segoe UI', -apple-system, BlinkMacSystemFont, 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  --lp-font-display:
    'Bahnschrift', 'DIN Alternate', 'Avenir Next Condensed', 'Segoe UI', -apple-system,
    'PingFang SC', 'Microsoft YaHei', sans-serif;

  min-height: 100dvh;
  overflow-x: clip;
  font-family: var(--lp-font-sans);
  color: var(--lp-ink);
  background: var(--lp-canvas);
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

.shell {
  display: grid;
  grid-template-columns: minmax(0, 1.38fr) minmax(0, 1fr);
  min-height: 100dvh;
  padding: 14px;
  background:
    radial-gradient(ellipse 55% 60% at 78% 45%, #f8f9f5 0%, transparent 70%), var(--lp-canvas);
}

/* ---------- 左侧品牌面板 ---------- */

.hero {
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: clamp(26px, 3vw, 42px);
  color: var(--lp-paper);
  background:
    radial-gradient(ellipse 60% 55% at 62% 44%, rgba(62, 139, 126, 0.25), transparent 68%),
    linear-gradient(145deg, #0d1b18 0%, #0a1211 48%, #0e1e1a 100%);
  border-radius: 20px;
}

.hero::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(rgba(245, 244, 239, 0.042) 1px, transparent 1px),
    linear-gradient(90deg, rgba(245, 244, 239, 0.042) 1px, transparent 1px);
  background-size: 72px 72px;
  -webkit-mask-image: radial-gradient(ellipse 78% 72% at 55% 45%, #000 25%, transparent 88%);
  mask-image: radial-gradient(ellipse 78% 72% at 55% 45%, #000 25%, transparent 88%);
  pointer-events: none;
}

.hero > * {
  position: relative;
  z-index: 1;
}

.hero-glow {
  position: absolute;
  z-index: 0;
  top: 4%;
  left: 38%;
  width: min(72vw, 820px);
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(62, 139, 126, 0.23) 0%,
    rgba(62, 139, 126, 0.07) 38%,
    transparent 68%
  );
  filter: blur(6px);
  pointer-events: none;
}

.particles span {
  position: absolute;
  z-index: 0;
  border-radius: 50%;
  background: var(--lp-verdigris-bright);
  box-shadow: 0 0 10px rgba(106, 196, 179, 0.55);
  pointer-events: none;
}

.hero-top {
  display: flex;
  align-items: center;
  gap: 13px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  flex: none;
  color: var(--lp-paper);
  background: rgba(245, 244, 239, 0.045);
  border: 1px solid rgba(245, 244, 239, 0.14);
  border-radius: 9px;
}

.brand-mark svg {
  display: block;
}

.wordmark {
  font-family: var(--lp-font-display);
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.32em;
}

.brand-sub {
  display: block;
  margin-top: 6px;
  font-size: 10px;
  line-height: 1;
  letter-spacing: 0.14em;
  color: rgba(245, 244, 239, 0.46);
}

.hero-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(180px, 0.8fr);
  align-items: center;
  gap: clamp(22px, 3.2vw, 56px);
  padding: 28px 0 20px;
}

.hero-copy {
  max-width: 560px;
  animation: rise 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) 0.08s both;
}

.eyebrow {
  display: block;
  font-size: 10px;
  line-height: 1.4;
  letter-spacing: 0.16em;
  color: rgba(245, 244, 239, 0.42);
}

.hero-title {
  margin: 18px 0 0;
  font-size: clamp(30px, 3vw, 56px);
  font-weight: 700;
  line-height: 1.14;
  letter-spacing: -0.035em;
}

.hero-title span {
  display: block;
  white-space: nowrap;
}

.hero-copy-text {
  margin: 20px 0 0;
  max-width: 35ch;
  font-size: clamp(12.5px, 1vw, 14.5px);
  line-height: 1.85;
  color: rgba(245, 244, 239, 0.58);
}

/* ---------- 实时时钟 ---------- */

.clock {
  position: relative;
  width: min(100%, clamp(220px, 20vw, 300px));
  aspect-ratio: 1;
  margin: 0 auto;
  animation: rise 0.8s cubic-bezier(0.22, 0.61, 0.36, 1) 0.18s both;
}

.clock svg {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
}

.clock-tick {
  stroke-width: 1.1;
  transition:
    stroke 0.35s ease,
    opacity 0.35s ease,
    stroke-width 0.35s ease,
    filter 0.35s ease;
}

.clock-tick.major {
  stroke-width: 1.6;
}

.clock-tick.active {
  stroke: var(--lp-verdigris-ring);
  stroke-width: 1.8;
  filter: drop-shadow(0 0 3px rgba(138, 219, 201, 0.55));
}

.clock-tick.current {
  stroke-width: 2.6;
}

.clock-readout {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
}

.clock-time {
  font-family: var(--lp-font-display);
  font-size: clamp(28px, 2.2vw, 38px);
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0.04em;
  color: var(--lp-paper);
  font-variant-numeric: tabular-nums;
}

.clock-date {
  font-size: 10px;
  letter-spacing: 0.16em;
  color: rgba(245, 244, 239, 0.5);
  font-variant-numeric: tabular-nums;
}

/* ---------- 底部状态与能力 ---------- */

.hero-bottom {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  gap: 12px;
  animation: rise 0.8s cubic-bezier(0.22, 0.61, 0.36, 1) 0.3s both;
}

.status-card,
.feature-panel {
  background: rgba(245, 244, 239, 0.035);
  border: 1px solid rgba(245, 244, 239, 0.09);
  border-radius: 13px;
  backdrop-filter: blur(10px);
}

.status-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  min-height: 118px;
}

.status-label {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: rgba(245, 244, 239, 0.42);
}

.status-main {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(245, 244, 239, 0.9);
}

.status-dot {
  width: 7px;
  height: 7px;
  flex: none;
  border-radius: 50%;
  background: var(--lp-verdigris-bright);
  box-shadow: 0 0 0 4px rgba(106, 196, 179, 0.12);
  transition:
    background-color 0.25s ease,
    box-shadow 0.25s ease;
}

.status-note {
  margin-top: auto;
  font-size: 10px;
  line-height: 1.5;
  color: rgba(245, 244, 239, 0.4);
}

.status-card[data-state='loading'] .status-dot {
  background: #d9b36c;
  box-shadow: 0 0 0 4px rgba(217, 179, 108, 0.14);
  animation: status-pulse 1.4s ease-in-out infinite;
}

.status-card[data-state='error'] .status-dot {
  background: #e07166;
  box-shadow: 0 0 0 4px rgba(224, 113, 102, 0.14);
}

.status-card[data-state='error'] .status-note {
  color: rgba(240, 164, 155, 0.72);
}

.feature-panel {
  padding: 14px 16px;
}

.features {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 26px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.feature {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.feature-icon {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  flex: none;
  color: var(--lp-verdigris-bright);
  background: rgba(106, 196, 179, 0.1);
  border: 1px solid rgba(106, 196, 179, 0.14);
  border-radius: 7px;
}

.feature-icon svg {
  display: block;
}

.feature-title {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  color: rgba(245, 244, 239, 0.88);
}

.feature-desc {
  margin: 4px 0 0;
  font-size: 10.5px;
  line-height: 1.55;
  color: rgba(245, 244, 239, 0.43);
}

.hero-footer {
  margin-top: 14px;
  font-size: 10px;
  letter-spacing: 0.04em;
  color: rgba(245, 244, 239, 0.3);
}

/* ---------- 右侧登录卡 ---------- */

.auth {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 4vw, 64px);
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: clamp(28px, 2.4vw, 34px);
  background: var(--lp-card);
  border: 1px solid rgba(12, 23, 22, 0.06);
  border-radius: 16px;
  box-shadow:
    0 34px 90px -46px rgba(12, 23, 22, 0.5),
    0 2px 8px rgba(12, 23, 22, 0.03);
  animation: rise 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) 0.16s both;
}

.login-kicker {
  display: block;
  font-size: 10.5px;
  letter-spacing: 0.14em;
  color: var(--lp-muted);
}

.login-head h1 {
  margin: 6px 0 0;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.03em;
}

.login-head p {
  margin: 9px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--lp-muted);
}

.login-form {
  margin-top: 24px;
}

.field + .field {
  margin-top: 16px;
}

.field label {
  display: block;
  margin-bottom: 7px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.input-shell {
  position: relative;
}

.input-icon {
  position: absolute;
  top: 50%;
  left: 12px;
  display: grid;
  place-items: center;
  color: #7a8580;
  pointer-events: none;
  transform: translateY(-50%);
}

.input-shell input {
  width: 100%;
  height: 44px;
  padding: 0 12px 0 38px;
  font: inherit;
  font-size: 14px;
  color: var(--lp-ink);
  background: #fbfbf9;
  border: 1px solid var(--lp-line);
  border-radius: 8px;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.input-shell input::placeholder {
  color: #64706b;
}

.input-shell input:hover {
  border-color: #c6cac2;
}

.input-shell input:focus {
  outline: none;
  background: var(--lp-card);
  border-color: var(--lp-verdigris);
  box-shadow: 0 0 0 3px rgba(62, 139, 126, 0.14);
}

.input-shell input[aria-invalid='true'] {
  border-color: var(--lp-danger);
}

.input-shell input[aria-invalid='true']:focus {
  box-shadow: 0 0 0 3px rgba(180, 67, 59, 0.12);
}

.has-reveal input {
  padding-right: 44px;
}

.reveal {
  position: absolute;
  top: 50%;
  right: 3px;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  padding: 0;
  color: #7a8580;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
  transform: translateY(-50%);
  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

.reveal:hover {
  color: var(--lp-ink);
  background: #f0f1ec;
}

.reveal:focus-visible {
  outline: 2px solid var(--lp-verdigris);
  outline-offset: 1px;
}

.reveal svg {
  display: block;
}

.field-error {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin: 6px 0 0;
  font-size: 11.5px;
  line-height: 1.45;
  color: var(--lp-danger);
}

.field-error svg {
  flex: none;
  margin-top: 1px;
}

.form-error {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin-top: 16px;
  padding: 9px 11px;
  font-size: 12px;
  line-height: 1.5;
  color: #8e332d;
  background: rgba(180, 67, 59, 0.065);
  border: 1px solid rgba(180, 67, 59, 0.2);
  border-left: 3px solid var(--lp-danger);
  border-radius: 6px;
}

.form-error svg {
  flex: none;
  margin-top: 1px;
}

.submit {
  width: 100%;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  margin-top: 20px;
  padding: 0;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--lp-paper);
  background: var(--lp-ink);
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.submit:hover:not(:disabled) {
  background: var(--lp-ink-raise);
  box-shadow: 0 12px 26px -18px rgba(62, 139, 126, 0.65);
}

.submit:active:not(:disabled) {
  transform: translateY(1px);
}

.submit:focus-visible {
  outline: 2px solid var(--lp-verdigris);
  outline-offset: 3px;
}

.submit:disabled {
  cursor: progress;
  opacity: 0.86;
}

.form-note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 18px;
  padding-top: 14px;
  font-size: 10.5px;
  line-height: 1.6;
  color: var(--lp-muted);
  border-top: 1px solid #e6e8e2;
}

.form-note svg {
  flex: none;
  margin-top: 1px;
}

/* ---------- 动效 ---------- */

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes status-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 4px rgba(217, 179, 108, 0.12);
  }
  50% {
    box-shadow: 0 0 0 7px rgba(217, 179, 108, 0.04);
  }
}

/* ---------- 响应式 ---------- */

@media (max-width: 1180px) {
  .hero-main {
    grid-template-columns: minmax(0, 1.2fr) minmax(180px, 0.8fr);
    gap: 24px;
  }

  .clock {
    width: min(100%, clamp(200px, 19vw, 250px));
  }

  .features {
    gap: 12px 18px;
  }

  .feature-desc {
    font-size: 10px;
  }
}

@media (max-width: 1024px) {
  .shell {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px;
  }

  .hero {
    padding: 26px 24px;
  }

  .hero-main {
    display: block;
    padding: 44px 0 30px;
  }

  .hero-copy {
    max-width: 560px;
  }

  .hero-title {
    font-size: clamp(32px, 6vw, 44px);
  }

  .hero-copy-text {
    max-width: 46ch;
  }

  .clock,
  .hero-bottom,
  .hero-footer {
    display: none;
  }

  .auth {
    padding: 32px 20px 48px;
  }

  .login-card {
    max-width: 420px;
  }
}

@media (max-width: 420px) {
  .hero {
    padding: 22px 20px;
  }

  .hero-title {
    font-size: 30px;
  }

  .login-card {
    padding: 24px 22px;
  }

  .login-head h1 {
    font-size: 26px;
  }
}

@media (max-width: 360px) {
  .hero-copy-text {
    font-size: 11.5px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.001ms !important;
    animation-delay: 0ms !important;
    transition-duration: 0.001ms !important;
  }
}
</style>
