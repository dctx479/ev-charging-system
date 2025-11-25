<template>
  <div class="change-password">
    <van-nav-bar
      title="修改密码"
      left-arrow
      @click-left="onClickLeft"
    />

    <div class="content">
      <van-cell-group inset>
        <van-field
          v-model="formData.oldPassword"
          type="password"
          label="当前密码"
          placeholder="请输入当前密码"
        />
        <van-field
          v-model="formData.newPassword"
          type="password"
          label="新密码"
          placeholder="请输入新密码（6-20位）"
        />
        <van-field
          v-model="formData.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入新密码"
        />
      </van-cell-group>

      <div class="tips">
        <van-notice-bar color="#1989fa" background="#ecf9ff" left-icon="info-o">
          密码长度为6-20位，建议包含字母和数字
        </van-notice-bar>
      </div>

      <div class="submit-button">
        <van-button
          type="primary"
          size="large"
          round
          :loading="submitting"
          @click="handleSubmit"
        >
          确认修改
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import { changePassword } from '@/api/user'
import { sha256 } from '@/utils/crypto'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formData = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const submitting = ref(false)

const onClickLeft = () => {
  router.back()
}

const handleSubmit = async () => {
  // 验证
  if (!formData.value.oldPassword) {
    showToast('请输入当前密码')
    return
  }

  if (!formData.value.newPassword) {
    showToast('请输入新密码')
    return
  }

  if (formData.value.newPassword.length < 6 || formData.value.newPassword.length > 20) {
    showToast('密码长度为6-20位')
    return
  }

  if (formData.value.newPassword !== formData.value.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }

  if (formData.value.oldPassword === formData.value.newPassword) {
    showToast('新密码不能与当前密码相同')
    return
  }

  submitting.value = true
  try {
    await changePassword({
      oldPassword: sha256(formData.value.oldPassword),
      newPassword: sha256(formData.value.newPassword)
    })
    showSuccessToast('密码修改成功，请重新登录')
    // 清除登录状态，密码已变更，旧token应失效
    userStore.logout()
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (error) {
    console.error('修改密码失败:', error)
    showToast(error.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.change-password {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.content {
  padding-top: 12px;
}

.van-cell-group {
  margin-bottom: 12px;
}

.tips {
  padding: 12px 16px;
}

.submit-button {
  padding: 24px 16px;
}
</style>
