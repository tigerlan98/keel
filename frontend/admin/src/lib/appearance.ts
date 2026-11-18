import { ref } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'auto'
const STORAGE_KEY = 'keel.appearance.theme'
export const themeMode = ref<ThemeMode>('light')
const systemTheme = globalThis.matchMedia('(prefers-color-scheme: dark)')

function applyTheme(): void {
  const dark = themeMode.value === 'dark' || (themeMode.value === 'auto' && systemTheme.matches)
  const root = globalThis.document.documentElement
  root.classList.toggle('dark', dark)
  root.dataset.theme = dark ? 'dark' : 'light'
  root.style.colorScheme = dark ? 'dark' : 'light'
}

export function setTheme(mode: ThemeMode): void {
  themeMode.value = mode
  applyTheme()
  try {
    globalThis.localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    /* 浏览器禁止存储时仍可切换主题。 */
  }
}

export function initializeAppearance(): void {
  try {
    const saved = globalThis.localStorage.getItem(STORAGE_KEY)
    if (saved === 'light' || saved === 'dark' || saved === 'auto') themeMode.value = saved
  } catch {
    /* 使用默认浅色主题。 */
  }
  applyTheme()
  systemTheme.addEventListener('change', () => {
    if (themeMode.value === 'auto') applyTheme()
  })
  globalThis.addEventListener('storage', (event) => {
    if (event.key !== STORAGE_KEY) return
    themeMode.value =
      event.newValue === 'dark' || event.newValue === 'auto' ? event.newValue : 'light'
    applyTheme()
  })
}
