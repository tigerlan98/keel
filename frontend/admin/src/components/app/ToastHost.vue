<script setup lang="ts">
import { ref, watch } from 'vue'
import { animateMotion, motionFrames, motionSkipped } from '@/lib/motion'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const hovered = ref(false)
const focused = ref(false)
watch(
  () => toast.items.length,
  (count) => {
    if (count === 0) {
      hovered.value = false
      focused.value = false
      toast.resume()
    }
  },
)
function syncPause(): void {
  if (hovered.value || focused.value) toast.pause()
  else toast.resume()
}
function pointerEnter(): void {
  hovered.value = true
  syncPause()
}
function pointerLeave(): void {
  hovered.value = false
  syncPause()
}
function focusIn(): void {
  focused.value = true
  syncPause()
}
function focusOut(event: InstanceType<typeof globalThis.FocusEvent>): void {
  if (
    event.currentTarget instanceof globalThis.Node &&
    event.relatedTarget instanceof globalThis.Node &&
    event.currentTarget.contains(event.relatedTarget)
  )
    return
  focused.value = false
  syncPause()
}
function enter(element: InstanceType<typeof globalThis.Element>, done: () => void): void {
  const card = element.querySelector('.toast')
  if (!card || motionSkipped()) return done()
  const animation = animateMotion(
    card,
    motionFrames(
      (t) => ({
        opacity: Math.max(0, Math.min(1, t)),
        transform: `translateY(${16 * (1 - t)}px) scale(${0.9 + 0.1 * t})`,
      }),
      'settle',
    ),
    500,
  )
  animation.addEventListener('finish', done, { once: true })
  animation.addEventListener('cancel', done, { once: true })
}
function leave(element: InstanceType<typeof globalThis.Element>, done: () => void): void {
  const card = element.querySelector('.toast')
  if (!card || motionSkipped()) return done()
  const animation = animateMotion(
    card,
    motionFrames(
      (t) => ({ opacity: 1 - t, transform: `translateY(${8 * t}px) scale(${1 - 0.04 * t})` }),
      'exit',
    ),
    200,
  )
  animation.addEventListener('finish', done, { once: true })
  animation.addEventListener('cancel', done, { once: true })
}
</script>

<template>
  <Teleport to="body">
    <div
      class="toast-host"
      :class="{ 'is-expanded': toast.paused }"
      role="status"
      aria-live="polite"
      @pointerenter="pointerEnter"
      @pointerleave="pointerLeave"
      @focusin="focusIn"
      @focusout="focusOut"
    >
      <TransitionGroup :css="false" @enter="enter" @leave="leave">
        <div
          v-for="(item, index) in toast.items"
          :key="item.id"
          class="toast-slot"
          :style="{ '--depth': toast.items.length - 1 - index }"
        >
          <div class="toast" :class="[`tone-${item.type}`, { 'is-paused': toast.paused }]">
            <span class="toast-dot" aria-hidden="true" />
            <span class="toast-msg" :title="item.message">{{ item.message }}</span>
            <button
              type="button"
              class="toast-close"
              aria-label="关闭提示"
              @click="toast.dismiss(item.id)"
            >
              <svg class="toast-ring" width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
                <circle class="ring-track" cx="9" cy="9" r="7" />
                <circle
                  class="ring-time"
                  cx="9"
                  cy="9"
                  r="7"
                  :style="{ animationDuration: `${item.duration}ms` }"
                />
                <path d="M6.6 6.6l4.8 4.8M11.4 6.6l-4.8 4.8" />
              </svg>
            </button>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-host {
  --toast-h: 44px;
  position: fixed;
  left: 50%;
  bottom: calc(28px + env(safe-area-inset-bottom, 0px));
  z-index: 400;
  width: min(460px, calc(100vw - 32px));
  height: var(--toast-h);
  translate: -50% 0;
  pointer-events: none;
}
.toast-slot {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  pointer-events: auto;
  transform-origin: 50% 100%;
  transform: translateY(calc(var(--depth) * -9px)) scale(calc(1 - var(--depth) * 0.05));
  opacity: calc(1 - var(--depth) * 0.28);
  z-index: calc(10 - var(--depth));
  transition:
    transform 0.42s cubic-bezier(0.16, 1, 0.3, 1),
    opacity 0.3s ease;
}
.toast-host.is-expanded .toast-slot {
  transform: translateY(calc(var(--depth) * (var(--toast-h) + 8px) * -1));
  opacity: 1;
}
.toast-slot:nth-last-child(n + 4) {
  opacity: 0;
}
.toast-host.is-expanded .toast-slot:nth-last-child(n + 4) {
  opacity: 1;
}
.toast {
  --toast-close-color: #99a2a5;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  max-width: 100%;
  min-height: var(--toast-h);
  padding: 8px 8px 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 560;
  color: var(--ink);
  border: 1px solid var(--glass-edge);
  background: rgba(246, 248, 247, 0.86);
  box-shadow:
    var(--shadow-e3),
    inset 0 0 0 1px rgba(120, 132, 135, 0.13),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(28px) saturate(1.08);
}
:global(.dark) .toast {
  --toast-close-color: #6c7679;
  background: rgba(30, 34, 37, 0.9);
  box-shadow:
    var(--shadow-e3),
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
.toast-msg {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.toast-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex: none;
  background: var(--brand);
}
.tone-success .toast-dot {
  background: var(--brand-bright);
  box-shadow: 0 0 0 4px rgba(130, 221, 45, 0.14);
}
.tone-info .toast-dot {
  box-shadow: 0 0 0 4px rgba(35, 109, 233, 0.1);
}
.tone-error .toast-dot {
  background: #ff5a50;
  box-shadow: 0 0 0 4px rgba(255, 90, 80, 0.11);
}
.toast-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  color: var(--toast-close-color);
  flex: none;
  transition:
    background 0.2s ease,
    color 0.2s ease;
}
.toast-close:hover {
  background: var(--hover-surface);
  color: var(--ink);
}
.toast-close:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 2px;
}
.toast-ring {
  overflow: visible;
}
.toast-ring circle {
  fill: none;
  stroke-width: 1.6;
}
.ring-track {
  stroke: var(--surface-edge);
}
.ring-time {
  stroke: currentColor;
  stroke-dasharray: 44;
  stroke-dashoffset: 0;
  transform: rotate(-90deg);
  transform-origin: 50% 50%;
  animation: toast-count linear forwards;
}
.is-paused .ring-time {
  animation-play-state: paused;
}
.toast-ring path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.5;
  stroke-linecap: round;
}
@keyframes toast-count {
  to {
    stroke-dashoffset: 44;
  }
}
@media (prefers-reduced-motion: reduce) {
  .toast-slot {
    transition: none;
  }
  .ring-time {
    animation: none;
  }
}
@media (prefers-reduced-transparency: reduce) {
  .toast {
    backdrop-filter: none;
    background: var(--surface-solid);
  }
}
</style>
