<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

import http, { ApiError } from '@/api/http'

const router = useRouter()
const submitting = ref(false)
const submitError = ref('')

const validationSchema = toTypedSchema(
  z.object({
    username: z.string().min(1, '请输入用户名'),
    password: z.string().min(1, '请输入密码'),
  }),
)

const { defineField, errors, handleSubmit } = useForm({
  validationSchema,
  initialValues: { username: '', password: '' },
})

const [username, usernameAttrs] = defineField('username')
const [password, passwordAttrs] = defineField('password')

const onSubmit = handleSubmit(async (values) => {
  submitError.value = ''
  submitting.value = true
  try {
    await http.post('/admin/auth/login', { ...values, device: 'WEB' })
    await router.push({ name: 'home' })
  } catch (error) {
    submitError.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
})
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-muted/40 px-4">
    <div class="w-full max-w-sm rounded-xl border bg-card p-8 shadow-sm">
      <div class="mb-6 space-y-1">
        <h1 class="text-2xl font-semibold tracking-tight">Keel 管理端</h1>
        <p class="text-sm text-muted-foreground">请使用管理员账号登录</p>
      </div>

      <form class="space-y-4" @submit="onSubmit">
        <div class="space-y-2">
          <label class="text-sm font-medium" for="username">用户名</label>
          <input
            id="username"
            v-model="username"
            v-bind="usernameAttrs"
            type="text"
            autocomplete="username"
            placeholder="请输入用户名"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          />
          <p v-if="errors.username" class="text-sm text-destructive">{{ errors.username }}</p>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium" for="password">密码</label>
          <input
            id="password"
            v-model="password"
            v-bind="passwordAttrs"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            class="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
          />
          <p v-if="errors.password" class="text-sm text-destructive">{{ errors.password }}</p>
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
          {{ submitting ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>
