<script setup lang="ts">
import { watch } from 'vue'
import AppButton from './AppButton.vue'
import AppDialog from './AppDialog.vue'
import AppField from './AppField.vue'
import AppInput from './AppInput.vue'
import { useChangePasswordForm } from '@/composables/useChangePasswordForm'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()
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
  resetForm,
} = useChangePasswordForm(() => emit('close'))
watch(
  () => props.open,
  () => {
    resetForm()
    submitError.value = ''
  },
)
function close(): void {
  if (!submitting.value) emit('close')
}
</script>

<template>
  <AppDialog
    :open="open"
    title="修改密码"
    size="confirm"
    initial-focus-id="change-password-current"
    @close="close"
  >
    <form id="change-password-form" class="space-y-4" @submit="onSubmit">
      <AppField
        label="当前密码"
        for="change-password-current"
        required
        :error="errors.oldPassword"
        error-id="change-password-current-error"
      >
        <AppInput
          id="change-password-current"
          v-model="oldPassword"
          v-bind="oldPasswordAttrs"
          type="password"
          autocomplete="current-password"
          :disabled="submitting"
          :invalid="!!errors.oldPassword"
          :aria-describedby="errors.oldPassword ? 'change-password-current-error' : undefined"
        />
      </AppField>
      <AppField
        label="新密码"
        for="change-password-new"
        required
        :error="errors.newPassword"
        error-id="change-password-new-error"
      >
        <AppInput
          id="change-password-new"
          v-model="newPassword"
          v-bind="newPasswordAttrs"
          type="password"
          autocomplete="new-password"
          :disabled="submitting"
          :invalid="!!errors.newPassword"
          :aria-describedby="
            errors.newPassword ? 'change-password-new-error' : 'change-password-hint'
          "
        />
        <p id="change-password-hint" class="mt-1.5 text-[11.5px] leading-5 text-ink-soft">
          8–72 位，必须同时包含字母和数字
        </p>
      </AppField>
      <AppField
        label="确认新密码"
        for="change-password-confirm"
        required
        :error="errors.confirmPassword"
        error-id="change-password-confirm-error"
      >
        <AppInput
          id="change-password-confirm"
          v-model="confirmPassword"
          v-bind="confirmPasswordAttrs"
          type="password"
          autocomplete="new-password"
          :disabled="submitting"
          :invalid="!!errors.confirmPassword"
          :aria-describedby="errors.confirmPassword ? 'change-password-confirm-error' : undefined"
        />
      </AppField>
      <p
        v-if="submitError"
        role="alert"
        class="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-[12px] leading-5 text-destructive"
      >
        {{ submitError }}
      </p>
    </form>
    <template #footer>
      <AppButton variant="ghost" :disabled="submitting" @click="close">取消</AppButton>
      <AppButton type="submit" form="change-password-form" :loading="submitting"
        >确认修改</AppButton
      >
    </template>
  </AppDialog>
</template>
