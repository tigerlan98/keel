<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

import { changePassword } from '@/api/auth'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

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

const validationSchema = toTypedSchema(
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
)

const { defineField, errors, handleSubmit } = useForm({
  validationSchema,
  initialValues: { oldPassword: '', newPassword: '', confirmPassword: '' },
})

const [oldPassword, oldPasswordAttrs] = defineField('oldPassword')
const [newPassword, newPasswordAttrs] = defineField('newPassword')
const [confirmPassword, confirmPasswordAttrs] = defineField('confirmPassword')

const onSubmit = handleSubmit(async (values) => {
  submitError.value = ''
  submitting.value = true
  try {
    await changePassword(values.oldPassword, values.newPassword)
    auth.reset()
    toast.show('密码已修改，请重新登录', 'success')
    await router.push({ name: 'login' })
  } catch (error) {
    submitError.value = error instanceof ApiError ? error.message : '修改失败，请稍后重试'
  } finally {
    submitting.value = false
  }
})
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-muted/40 px-4">
    <div class="w-full max-w-sm rounded-xl border bg-card p-8 shadow-sm">
      <div class="mb-6 space-y-1">
        <h1 class="text-2xl font-semibold tracking-tight">修改密码</h1>
        <p class="text-sm text-muted-foreground">
          {{ auth.mustChangePassword ? '首次登录必须修改密码后才能继续使用。' : '更新当前账号密码。' }}
        </p>
      </div>

      <form class="space-y-4" @submit="onSubmit">
        <div class="space-y-2">
          <label class="text-sm font-medium" for="oldPassword">当前密码</label>
          <input
            id="oldPassword"
            v-model="oldPassword"
            v-bind="oldPasswordAttrs"
            type="password"
            autocomplete="current-password"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          />
          <p v-if="errors.oldPassword" class="text-sm text-destructive">{{ errors.oldPassword }}</p>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium" for="newPassword">新密码</label>
          <input
            id="newPassword"
            v-model="newPassword"
            v-bind="newPasswordAttrs"
            type="password"
            autocomplete="new-password"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          />
          <p v-if="errors.newPassword" class="text-sm text-destructive">{{ errors.newPassword }}</p>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium" for="confirmPassword">确认新密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            v-bind="confirmPasswordAttrs"
            type="password"
            autocomplete="new-password"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          />
          <p v-if="errors.confirmPassword" class="text-sm text-destructive">
            {{ errors.confirmPassword }}
          </p>
        </div>

        <p
          v-if="submitError"
          class="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive"
        >
          {{ submitError }}
        </p>

        <button
          type="submit"
          :disabled="submitting"
          class="inline-flex h-9 w-full items-center justify-center rounded-md bg-primary text-sm font-medium text-primary-foreground shadow-xs transition-colors hover:bg-primary/90 disabled:pointer-events-none disabled:opacity-50"
        >
          {{ submitting ? '提交中...' : '确认修改' }}
        </button>
      </form>
    </div>
  </div>
</template>
