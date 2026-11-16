import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

import { changePassword } from '@/api/auth'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

export function useChangePasswordForm(onSuccess?: () => void) {
  const router = useRouter()
  const auth = useAuthStore()
  const toast = useToastStore()
  const submitting = ref(false)
  const submitError = ref('')
  const passwordRule = z
    .string()
    .min(8, '密码至少 8 位')
    .max(72, '密码最多 72 位')
    .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, '密码必须同时包含字母和数字')
  const { defineField, errors, handleSubmit, resetForm } = useForm({
    validationSchema: toTypedSchema(
      z
        .object({
          oldPassword: z.string().min(1, '请输入当前密码'),
          newPassword: passwordRule,
          confirmPassword: z.string().min(1, '请再次输入新密码'),
        })
        .refine((values) => values.newPassword === values.confirmPassword, {
          path: ['confirmPassword'],
          message: '两次输入的密码不一致',
        }),
    ),
    initialValues: { oldPassword: '', newPassword: '', confirmPassword: '' },
  })
  const [oldPassword, oldPasswordAttrs] = defineField('oldPassword')
  const [newPassword, newPasswordAttrs] = defineField('newPassword')
  const [confirmPassword, confirmPasswordAttrs] = defineField('confirmPassword')
  const onSubmit = handleSubmit(async (values) => {
    if (submitting.value) return
    submitError.value = ''
    submitting.value = true
    try {
      await changePassword(values.oldPassword, values.newPassword)
      onSuccess?.()
      auth.reset()
      toast.show('密码已修改，请重新登录', 'success')
      await router.push({ name: 'login' })
    } catch (error) {
      submitError.value = error instanceof ApiError ? error.message : '修改失败，请稍后重试'
    } finally {
      submitting.value = false
    }
  })
  return {
    oldPassword,
    oldPasswordAttrs,
    newPassword,
    newPasswordAttrs,
    confirmPassword,
    confirmPasswordAttrs,
    errors,
    onSubmit,
    submitting,
    submitError,
    resetForm,
  }
}
