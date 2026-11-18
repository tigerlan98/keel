import {
  Circle,
  FileClock,
  KeyRound,
  LayoutDashboard,
  LogIn,
  Menu,
  Network,
  ScrollText,
  Settings,
  Shield,
  SlidersHorizontal,
  Users,
  type LucideIcon,
} from '@lucide/vue'

import type { MenuNode } from '@/api/types'

const ICONS: Record<string, LucideIcon> = {
  'layout-dashboard': LayoutDashboard,
  shield: Shield,
  users: Users,
  'key-round': KeyRound,
  menu: Menu,
  network: Network,
  settings: Settings,
  'sliders-horizontal': SlidersHorizontal,
  'scroll-text': ScrollText,
  'log-in': LogIn,
  'file-clock': FileClock,
}

export function menuIcon(name?: string | null): LucideIcon {
  return (name && ICONS[name]) || Circle
}

/**
 * 返回从顶层到当前路径的菜单链，用于顶栏面包屑与侧栏激活父级。
 */
export function findMenuTrail(menus: MenuNode[], path: string): MenuNode[] {
  for (const menu of menus) {
    if (menu.path === path) {
      return [menu]
    }
    if (menu.children?.length) {
      const childTrail = findMenuTrail(menu.children, path)
      if (childTrail.length > 0) {
        return [menu, ...childTrail]
      }
    }
  }
  return []
}

export function menuContainsPath(menu: MenuNode, path: string): boolean {
  if (menu.path === path) {
    return true
  }
  return (menu.children ?? []).some((child) => menuContainsPath(child, path))
}

export function firstMenuPath(
  menus: MenuNode[],
  matches: (path: string) => boolean = () => true,
): string | null {
  for (const menu of menus) {
    if (menu.type === 'MENU' && menu.path && matches(menu.path)) {
      return menu.path
    }
    const childPath = firstMenuPath(menu.children ?? [], matches)
    if (childPath) {
      return childPath
    }
  }
  return null
}

export function flattenMenuPaths(menus: MenuNode[]): string[] {
  const paths: string[] = []
  for (const menu of menus) {
    if (menu.type === 'MENU' && menu.path) {
      paths.push(menu.path)
    }
    paths.push(...flattenMenuPaths(menu.children ?? []))
  }
  return paths
}
