import type { DepartmentOption } from '@/api/departments'
export type FlatDepartment = DepartmentOption & { depth: number; path: string }
export function flattenDepartments(
  nodes: DepartmentOption[],
  depth = 0,
  ancestors: string[] = [],
): FlatDepartment[] {
  return nodes.flatMap((node) => [
    { ...node, depth, path: [...ancestors, node.name].join(' / ') },
    ...flattenDepartments(node.children, depth + 1, [...ancestors, node.name]),
  ])
}
export function departmentSubtreeIds(nodes: DepartmentOption[], id: number): number[] {
  for (const node of nodes) {
    if (node.id === id) return flattenDepartments([node]).map((item) => item.id)
    const found = departmentSubtreeIds(node.children, id)
    if (found.length) return found
  }
  return []
}
