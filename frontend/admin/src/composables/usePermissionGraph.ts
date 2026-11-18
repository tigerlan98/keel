import { computed, ref } from 'vue'
import { fetchRoles, fetchRoleResourceIds } from '@/api/roles'
import { fetchResourceTree } from '@/api/resources'
import { fetchUsers } from '@/api/users'
import type { PageResult, ResourceNode, RoleSummary, UserSummary } from '@/api/types'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

export interface PermissionGraph {
  roles: RoleSummary[]
  tree: ResourceNode[]
  resources: ResourceNode[]
  users: UserSummary[]
  grants: Record<number, number[]>
  usersVisible: boolean
}

export function flattenResources(tree: ResourceNode[]): ResourceNode[] {
  return tree.flatMap((node) => [node, ...flattenResources(node.children ?? [])])
}

export function roleResourceIds(graph: PermissionGraph, role: RoleSummary): number[] {
  return role.code === 'SUPER_ADMIN'
    ? graph.resources.map((node) => node.id)
    : (graph.grants[role.id] ?? [])
}

export function activeResource(resource: ResourceNode): boolean {
  return resource.status === 'ENABLED'
}

export function resourceHolders(graph: PermissionGraph, resourceId: number) {
  const resource = graph.resources.find((node) => node.id === resourceId)
  const active = resource && activeResource(resource)
  const roles = graph.roles.filter(
    (role) =>
      role.status === 'ENABLED' &&
      (role.code === 'SUPER_ADMIN' ||
        (active && roleResourceIds(graph, role).includes(resourceId))),
  )
  const ids = new Set(roles.map((role) => role.id))
  const users = graph.users.filter(
    (user) => user.status === 'ENABLED' && (user.roles ?? []).some((role) => ids.has(role.id)),
  )
  return { roles, users }
}

async function allPages<T extends { id: number }>(
  fetchPage: (page: number) => Promise<PageResult<T>>,
): Promise<T[]> {
  const first = await fetchPage(1)
  const rows = [...first.items]
  for (let page = 2; rows.length < first.total; page += 1) {
    const next = await fetchPage(page)
    if (!next.items.length) throw new Error('数据在加载期间发生变化，请刷新重试')
    rows.push(...next.items)
  }
  if (new Set(rows.map((row) => row.id)).size !== rows.length)
    throw new Error('数据在加载期间发生变化，请刷新重试')
  return rows
}

export function usePermissionGraph() {
  const auth = useAuthStore()
  const graph = ref<PermissionGraph | null>(null)
  const loading = ref(false)
  const error = ref('')
  const canReadResources = computed(() => auth.hasPermission('iam:resource:list'))
  const canReadUsers = computed(() => auth.hasPermission('iam:user:list'))
  let pending: Promise<void> | undefined
  let stale = true
  let revision = 0

  function invalidate(): void {
    stale = true
    revision += 1
  }
  async function load(force = false): Promise<void> {
    if (pending) return pending
    if (!force && graph.value && !stale) return
    if (!canReadResources.value) {
      graph.value = null
      error.value = '查看矩阵和溯源需要资源列表权限'
      return
    }
    pending = (async () => {
      const currentRevision = revision
      loading.value = true
      error.value = ''
      try {
        const [roles, tree, users] = await Promise.all([
          allPages((page) => fetchRoles({ page, size: 100, sort: 'sort,asc' })),
          fetchResourceTree(),
          canReadUsers.value
            ? allPages((page) => fetchUsers({ page, size: 100, sort: 'createdAt,asc' }))
            : Promise.resolve([] as UserSummary[]),
        ])
        const grants: Record<number, number[]> = {}
        for (let offset = 0; offset < roles.length; offset += 6) {
          await Promise.all(
            roles.slice(offset, offset + 6).map(async (role) => {
              grants[role.id] =
                role.code === 'SUPER_ADMIN' ? [] : await fetchRoleResourceIds(role.id)
            }),
          )
        }
        graph.value = {
          roles,
          tree,
          users,
          resources: flattenResources(tree),
          grants,
          usersVisible: canReadUsers.value,
        }
        stale = currentRevision !== revision
      } catch (caught) {
        graph.value = null
        error.value =
          caught instanceof ApiError || caught instanceof Error
            ? caught.message
            : '权限关系加载失败，请重试'
      } finally {
        loading.value = false
      }
    })()
    try {
      await pending
    } finally {
      pending = undefined
    }
  }
  return { graph, loading, error, canReadResources, canReadUsers, load, invalidate }
}
