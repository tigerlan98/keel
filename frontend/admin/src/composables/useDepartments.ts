import { ref } from 'vue'
import { fetchDepartmentOptions, type DepartmentOption } from '@/api/departments'
import { ApiError } from '@/api/http'
export function useDepartments() {
  const departments = ref<DepartmentOption[]>([])
  const departmentsLoading = ref(false)
  const departmentsError = ref('')
  let generation = 0
  async function loadDepartments(): Promise<void> {
    const version = ++generation
    departmentsLoading.value = true
    departmentsError.value = ''
    try {
      const rows = await fetchDepartmentOptions()
      if (version === generation) departments.value = rows
    } catch (error) {
      if (version === generation)
        departmentsError.value = error instanceof ApiError ? error.message : '部门加载失败，请重试'
    } finally {
      if (version === generation) departmentsLoading.value = false
    }
  }
  return { departments, departmentsLoading, departmentsError, loadDepartments }
}
