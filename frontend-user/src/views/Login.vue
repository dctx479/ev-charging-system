<template>
  <div class="login-container">
    <div class="login-header">
      <h1>充电站管理系统</h1>
      <p>欢迎使用</p>
    </div>

    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field
          v-model="formData.username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="formData.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>

      <div class="button-group">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
        >
          登录
        </van-button>
      </div>
    </van-form>

    <div v-if="isDev" class="tips">
      <p>测试账号：admin / 123456</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { sha256 } from '@/utils/crypto'
import { resetLoginRedirectTime } from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formData = ref({
  username: '',
  password: ''
})

const loading = ref(false)

// 仅在开发环境显示测试账号
const isDev = import.meta.env.DEV

const handleLogin = async () => {
  loading.value = true
  try {
    // 直接发送明文密码，后端使用BCrypt验证
    const res = await login({
      username: formData.value.username,
      password: formData.value.password
    })

    // 从JWT提取exp时间戳，计算expiresIn（秒）
    const token = res.data.token
    const payload = JSON.parse(atob(token.split('.')[1]))
    const expiresIn = payload.exp ? payload.exp - Math.floor(Date.now() / 1000) : 24 * 60 * 60

    // 保存 token 和用户信息
    userStore.login(res.data.token, res.data.userInfo, expiresIn)

    // 重置 401 重定向时间戳，允许下次 401 可以正常重定向
    resetLoginRedirectTime()

    showToast({
      message: '登录成功',
      type: 'success'
    })

    // 跳转到首页或重定向页面（防止开放重定向）
    const redirect = route.query.redirect || '/'
    const safeRedirect = (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) ? redirect : '/'
    router.push(safeRedirect)
  } catch (error) {
    console.error('登录失败:', error)
    showToast(error.response?.data?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-header {
  text-align: center;
  color: white;
  margin-bottom: 60px;
  padding-top: 60px;
}

.login-header h1 {
  font-size: 28px;
  margin-bottom: 10px;
  font-weight: bold;
}

.login-header p {
  font-size: 14px;
  opacity: 0.9;
}

.button-group {
  margin: 30px 16px;
}

.tips {
  text-align: center;
  margin-top: 20px;
  color: white;
  font-size: 12px;
  opacity: 0.8;
}
</style>
