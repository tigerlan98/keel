<script setup lang="ts">
import AppButton from '@/components/app/AppButton.vue'
import { useChangePasswordForm } from '@/composables/useChangePasswordForm'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const {
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
} = useChangePasswordForm()
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-muted/40 px-4">
    <div class="w-full max-w-sm rounded-xl border bg-card p-8 shadow-sm">
      <div class="mb-6 space-y-1">
        <h1 class="text-2xl font-semibold tracking-tight">修改密码</h1>
        <p class="text-sm text-muted-foreground">
          {{
            auth.mustChangePassword ? '首次登录必须修改密码后才能继续使用。' : '更新当前账号密码。'
          }}
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

        <AppButton type="submit" :disabled="submitting" variant="primary" class="w-full">
          {{ submitting ? '提交中...' : '确认修改' }}
        </AppButton>
      </form>
    </div>
  </div>
</template>
