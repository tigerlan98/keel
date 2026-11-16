<script setup lang="ts">
import {
  Check,
  ChevronDown,
  ChevronRight,
  RotateCcw,
  Shield,
  Users,
  Settings,
  Menu,
  Network,
} from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import { flattenResources, type PermissionGraph } from '@/composables/usePermissionGraph'
import type { ResourceNode } from '@/api/types'
import { menuIcon } from '@/lib/menus'
import AppButton from './AppButton.vue'

export interface GrantChange {
  roleId: number
  resourceIds: number[]
}
const props = defineProps<{ graph: PermissionGraph; editable: boolean; saving: boolean }>()
const emit = defineEmits<{ save: [changes: GrantChange[]] }>()
const draft = ref<Record<number, number[]>>({})
const baseline = ref<Record<number, number[]>>({})
const collapsed = ref<number[]>([])
const hoverColumn = ref<number>()
const hoverRow = ref<number>()
function reset(): void {
  baseline.value = Object.fromEntries(
    Object.entries(props.graph.grants).map(([id, ids]) => [id, [...ids]]),
  )
  draft.value = Object.fromEntries(
    Object.entries(props.graph.grants).map(([id, ids]) => [id, [...ids]]),
  )
}
watch(() => props.graph, reset, { immediate: true })
const rows = computed(() => {
  const result: { node: ResourceNode; depth: number; parents: number[]; descendants: number[] }[] =
    []
  function walk(nodes: ResourceNode[], depth = 0, parents: number[] = []) {
    for (const node of nodes) {
      result.push({
        node,
        depth,
        parents,
        descendants: flattenResources(node.children ?? []).map((child) => child.id),
      })
      walk(node.children ?? [], depth + 1, [...parents, node.id])
    }
  }
  walk(props.graph.tree)
  return result.filter((row) => !row.parents.some((id) => collapsed.value.includes(id)))
})
function cellState(roleId: number, row: (typeof rows.value)[number]): 'on' | 'off' | 'partial' {
  if (props.graph.roles.find((role) => role.id === roleId)?.code === 'SUPER_ADMIN') return 'on'
  const selected = new Set(draft.value[roleId] ?? [])
  if (!row.descendants.length) return selected.has(row.node.id) ? 'on' : 'off'
  const count = row.descendants.filter((id) => selected.has(id)).length
  if (selected.has(row.node.id) && count === row.descendants.length) return 'on'
  return selected.has(row.node.id) || count ? 'partial' : 'off'
}
function highlight(roleId?: number, nodeId?: number): void {
  hoverColumn.value = roleId
  hoverRow.value = nodeId
}
function resourceIcon(node: ResourceNode) {
  if (node.type === 'BUTTON' || node.type === 'API') return null
  if (node.code === 'iam:role:list') return Shield
  if (node.icon) return menuIcon(node.icon)
  if (node.code === 'iam:user:list') return Users
  if (node.code === 'iam:department:list') return Network
  return node.type === 'DIRECTORY' ? Settings : Menu
}
const changes = computed(() =>
  props.graph.roles
    .filter((role) => role.code !== 'SUPER_ADMIN')
    .flatMap((role) => {
      const before = new Set(baseline.value[role.id] ?? [])
      const after = draft.value[role.id] ?? []
      return before.size !== after.length || after.some((id) => !before.has(id))
        ? [{ roleId: role.id, resourceIds: after }]
        : []
    }),
)
const dirty = computed(() => changes.value.length > 0)
const changeCount = computed(() =>
  changes.value.reduce((count, change) => {
    const before = new Set(baseline.value[change.roleId] ?? [])
    const after = new Set(change.resourceIds)
    return (
      count +
      [...before].filter((id) => !after.has(id)).length +
      [...after].filter((id) => !before.has(id)).length
    )
  }, 0),
)
function toggle(roleId: number, resourceId: number): void {
  if (
    !props.editable ||
    props.saving ||
    props.graph.roles.find((role) => role.id === roleId)?.code === 'SUPER_ADMIN'
  )
    return
  const ids = draft.value[roleId] ?? []
  draft.value[roleId] = ids.includes(resourceId)
    ? ids.filter((id) => id !== resourceId)
    : [...ids, resourceId]
}
function collapse(id: number): void {
  collapsed.value = collapsed.value.includes(id)
    ? collapsed.value.filter((item) => item !== id)
    : [...collapsed.value, id]
}
function accept(change: GrantChange): void {
  baseline.value[change.roleId] = [...change.resourceIds]
}
const labels = { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮', API: '接口' }
defineExpose({ dirty, reset, accept })
</script>
<template>
  <section
    class="permission-matrix"
    aria-label="权限矩阵"
    :style="{ '--matrix-role-count': graph.roles.length }"
  >
    <p v-if="!editable" class="sr-only">当前为只读视图，需要角色资源授权权限才能修改。</p>
    <div class="matrix-scroll">
      <table
        v-if="graph.roles.length && graph.resources.length"
        class="matrix-table"
        @pointerleave="highlight()"
      >
        <colgroup>
          <col class="matrix-label-col" />
          <col v-for="role in graph.roles" :key="role.id" />
        </colgroup>
        <thead>
          <tr>
            <th scope="col" class="matrix-label matrix-corner">菜单 × 角色</th>
            <th
              v-for="role in graph.roles"
              :key="role.id"
              scope="col"
              class="matrix-role"
              :class="{
                'is-hover': hoverColumn === role.id,
                'is-builtin': role.code === 'SUPER_ADMIN',
                'is-off': role.status !== 'ENABLED',
              }"
              :title="role.name"
            >
              <span>{{ role.name }}</span><small>{{
                role.code === 'SUPER_ADMIN' ? '内置 · 只读' : `${(draft[role.id] ?? []).length} 项`
              }}</small>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in rows"
            :key="row.node.id"
            :class="[{ 'is-hover': hoverRow === row.node.id }, `is-${row.node.type.toLowerCase()}`]"
          >
            <th scope="row" class="matrix-label" :title="row.node.code">
              <div class="matrix-label-inner" :style="{ paddingLeft: `${14 + row.depth * 18}px` }">
                <button
                  v-if="row.node.children?.length"
                  type="button"
                  class="matrix-toggle"
                  :aria-label="`${collapsed.includes(row.node.id) ? '展开' : '折叠'}${row.node.name}`"
                  :aria-expanded="!collapsed.includes(row.node.id)"
                  @click="collapse(row.node.id)"
                >
                  <component
                    :is="collapsed.includes(row.node.id) ? ChevronRight : ChevronDown"
                    :size="14"
                    :stroke-width="1.6"
                  /></button
                ><span v-else class="matrix-spacer" />
                <component
                  :is="resourceIcon(row.node)"
                  v-if="resourceIcon(row.node)"
                  :size="15"
                  :stroke-width="1.6"
                  class="matrix-resource-icon"
                  aria-hidden="true"
                />
                <span class="matrix-resource-name">{{ row.node.name }}</span><span
                  class="matrix-resource-type"
                  >{{ labels[row.node.type] }}</span
                >
              </div>
            </th>
            <td
              v-for="role in graph.roles"
              :key="role.id"
              :class="{ 'is-hover': hoverColumn === role.id }"
              @pointerenter="highlight(role.id, row.node.id)"
            >
              <button
                type="button"
                class="matrix-cell"
                role="checkbox"
                :aria-label="`${role.name}：${row.node.name}`"
                :aria-checked="
                  cellState(role.id, row) === 'partial' ? 'mixed' : cellState(role.id, row) === 'on'
                "
                :disabled="!editable || saving || role.code === 'SUPER_ADMIN'"
                :class="[
                  `is-${cellState(role.id, row)}`,
                  { 'is-builtin': role.code === 'SUPER_ADMIN' },
                ]"
                :title="
                  row.descendants.length ? '汇总展示下级授权状态；点击切换本项授权' : row.node.code
                "
                @focus="highlight(role.id, row.node.id)"
                @blur="highlight()"
                @click="toggle(role.id, row.node.id)"
              >
                <span class="matrix-mark" aria-hidden="true"
                  ><Check v-if="cellState(role.id, row) === 'on'" :size="12" :stroke-width="1.6"
                /></span>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="matrix-empty">
        {{ graph.roles.length ? '暂无菜单与按钮资源' : '暂无角色，请先新增角色' }}
      </p>
    </div>
    <Teleport to="body"
      ><Transition name="matrix-bar"
        ><footer v-if="dirty" class="matrix-save-bar" role="status">
          <span><i />未保存 {{ changeCount }} 项改动</span>
          <AppButton
            variant="ghost"
            class="text-white/60! hover:bg-white/10! hover:text-white!"
            :icon="RotateCcw"
            :disabled="saving"
            @click="reset"
            >重置</AppButton
          >
          <AppButton :loading="saving" @click="emit('save', changes)">保存权限</AppButton>
        </footer></Transition
      ></Teleport
    >
  </section>
</template>

<style scoped>
.permission-matrix {
  --matrix-label-width: 280px;
  --matrix-role-width: 112px;
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid var(--surface-edge);
  background: var(--surface);
  box-shadow: var(--inset-edge), var(--shadow-e1);
}
.matrix-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgba(126, 136, 140, 0.35) transparent;
  max-height: max(480px, calc(100dvh - 250px));
  min-height: 300px;
  overflow: auto;
  overscroll-behavior: contain;
}
.matrix-table {
  width: 100%;
  min-width: calc(var(--matrix-label-width) + var(--matrix-role-count) * var(--matrix-role-width));
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
}
.matrix-label-col {
  width: var(--matrix-label-width);
}
.matrix-table thead th {
  position: sticky;
  top: 0;
  z-index: 3;
  height: 64px;
  border-bottom: 1px solid var(--surface-edge);
  background: var(--surface-solid);
}
.matrix-role {
  padding: 14px 8px 12px;
  text-align: center;
  vertical-align: bottom;
  transition: background 0.2s ease;
}
.matrix-role > span {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 12.5px;
  font-weight: 680;
  line-height: 19px;
}
.matrix-role small {
  display: block;
  margin-top: 2px;
  font-size: 10px;
  line-height: 15px;
  font-weight: 500;
  color: var(--ink-faint);
}
.matrix-role.is-builtin > span {
  color: var(--ink-muted);
}
.matrix-role.is-off {
  opacity: 0.5;
}
.matrix-table .matrix-label {
  position: sticky;
  left: 0;
  z-index: 2;
  border-right: 1px solid var(--surface-edge);
  background: var(--surface-solid);
  text-align: left;
}
.matrix-table thead .matrix-corner {
  z-index: 4;
  padding: 14px 16px;
  vertical-align: bottom;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-faint);
}
.matrix-table tbody th,
.matrix-table tbody td {
  height: 40px;
  padding: 0;
  border-bottom: 1px solid var(--surface-edge);
}
.matrix-table tbody tr:last-child > * {
  border-bottom: 0;
}
.matrix-table tbody th {
  font-weight: 500;
}
.matrix-label-inner {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  min-height: 40px;
  padding-right: 12px;
}
.matrix-toggle,
.matrix-spacer {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  flex: none;
  color: var(--ink-soft);
}
.matrix-toggle:hover {
  background: var(--hover-surface);
  color: var(--ink);
}
.matrix-resource-icon {
  color: var(--ink-soft);
  flex: none;
}
.matrix-resource-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.is-directory .matrix-resource-name {
  font-weight: 680;
}
.is-button .matrix-resource-name,
.is-api .matrix-resource-name {
  color: var(--ink-muted);
  font-size: 12.5px;
}
.matrix-resource-type {
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--ink-faint);
  flex: none;
}
.matrix-table tbody td {
  text-align: center;
  transition: background 0.15s ease;
}
.matrix-table tbody tr.is-directory > td {
  background: var(--hover-surface);
}
.matrix-table tbody tr.is-hover > td,
.matrix-table tbody td.is-hover {
  background: rgba(35, 109, 233, 0.045);
}
.matrix-table thead th.is-hover,
.matrix-table tbody tr.is-hover > td.is-hover {
  background: var(--brand-soft);
}
.matrix-table tbody tr.is-hover > .matrix-label {
  background: color-mix(in srgb, var(--brand) 5%, var(--surface-solid));
}
.matrix-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 40px;
  cursor: pointer;
}
.matrix-cell:disabled {
  cursor: default;
}
.matrix-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 7px;
  color: #1c2a0c;
  border: 1.5px solid rgba(46, 58, 62, 0.2);
  background: var(--input-surface);
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.2s ease;
}
.is-button .matrix-mark,
.is-api .matrix-mark {
  width: 16px;
  height: 16px;
  border-radius: 6px;
}
.matrix-cell.is-on .matrix-mark {
  border-color: transparent;
  background: var(--brand-bright);
  box-shadow: 0 2px 8px rgba(130, 221, 45, 0.35);
}
.matrix-cell.is-partial .matrix-mark {
  border-color: color-mix(in srgb, var(--brand-bright) 70%, transparent);
  background: linear-gradient(135deg, var(--brand-bright) 0 50%, transparent 50%);
}
.matrix-cell:not(:disabled):hover .matrix-mark {
  transform: scale(1.12);
  border-color: var(--brand-bright);
}
.matrix-cell.is-builtin .matrix-mark {
  opacity: 0.55;
}
.matrix-cell:focus-visible,
.matrix-toggle:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: -3px;
  border-radius: 8px;
}
:global(.dark) .matrix-mark {
  border-color: rgba(255, 255, 255, 0.18);
}
.matrix-empty {
  padding: 64px 20px;
  text-align: center;
  color: var(--ink-soft);
}
.matrix-save-bar {
  position: fixed;
  z-index: 80;
  left: 50%;
  bottom: calc(28px + env(safe-area-inset-bottom, 0px));
  translate: -50% 0;
  width: max-content;
  max-width: calc(100vw - 32px);
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
  padding: 8px 8px 8px 18px;
  color: #f2f5f4;
  background: rgba(21, 24, 26, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  box-shadow:
    var(--shadow-e3),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px) saturate(1.1);
}
.matrix-save-bar > span {
  margin-right: auto;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 9px;
}
.matrix-save-bar i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--brand-bright);
}
:global(body:has(.matrix-save-bar) .toast-host) {
  bottom: calc(102px + env(safe-area-inset-bottom, 0px));
}
.matrix-bar-enter-active,
.matrix-bar-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.matrix-bar-enter-from,
.matrix-bar-leave-to {
  opacity: 0;
  transform: translateY(12px);
}
@media (prefers-reduced-motion: reduce) {
  .matrix-bar-enter-active,
  .matrix-bar-leave-active {
    transition: none;
  }
}

@media (max-width: 720px) {
  .permission-matrix {
    --matrix-label-width: 180px;
    --matrix-role-width: 88px;
  }
  .matrix-resource-type {
    display: none;
  }
  .matrix-save-bar {
    padding-left: 14px;
  }
  .matrix-save-bar > span {
    font-size: 11px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .matrix-cell .matrix-mark,
  .matrix-role,
  .matrix-table td {
    transition: none;
  }
}
</style>
