import {
  Circle,
  FileClock,
  KeyRound,
  LogIn,
  Menu,
  ScrollText,
  Settings,
  Shield,
  SlidersHorizontal,
  Users,
  type LucideIcon,
} from '@lucide/vue'

import type { MenuNode } from '@/api/types'

const ICONS: Record<string, LucideIcon> = {
  shield: Shield,
  users: Users,
  'key-round': KeyRound,
  menu: Menu,
  settings: Settings,
  'sliders-horizontal': SlidersHorizontal,
  'scroll-text': ScrollText,
  'log-in': LogIn,
  'file-clock': FileClock,
}

export function menuIcon(name?: string | null): LucideIcon {
  return (name && ICONS[name]) || Circle
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
