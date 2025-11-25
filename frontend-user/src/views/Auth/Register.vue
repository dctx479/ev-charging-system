<template>
  <div class="register-page">
    <van-nav-bar
      title="用户注册"
      left-arrow
      @click-left="$router.back()"
      fixed
      placeholder
    />

    <div class="content">
      <!-- 用户类型选择 -->
      <van-cell-group inset title="选择用户类型">
        <van-radio-group v-model="formData.userType" direction="horizontal">
          <van-radio name="USER">个人用户</van-radio>
          <van-radio name="OPERATOR">企业运营商</van-radio>
          <van-radio name="VALET_CHARGER">代充师傅</van-radio>
        </van-radio-group>
      </van-cell-group>

      <!-- 基本信息 -->
      <van-cell-group inset title="基本信息">
        <van-field
          v-model="formData.username"
          label="用户名"
          placeholder="3-20个字符，字母数字下划线"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="formData.password"
          type="password"
          label="密码"
          placeholder="6-20个字符"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
        <van-field
          v-model="formData.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="再次输入密码"
          :rules="[{ required: true, message: '请再次输入密码' }]"
        />
        <van-field
          v-model="formData.nickname"
          label="昵称"
          placeholder="选填，默认为用户名"
        />
        <van-field
          v-model="formData.phone"
          type="tel"
          label="手机号"
          placeholder="请输入手机号"
          :rules="[{ required: true, message: '请输入手机号' }]"
        />
        <van-field
          v-model="formData.email"
          type="email"
          label="邮箱"
          placeholder="选填"
        />
      </van-cell-group>

      <!-- 个人用户专属字段 -->
      <van-cell-group v-if="formData.userType === 'USER'" inset title="车辆信息（选填）">
        <van-field
          v-model="formData.carModel"
          label="车型"
          placeholder="如：特斯拉 Model 3"
        />
        <van-field
          v-model="formData.batteryCapacity"
          type="number"
          label="电池容量"
          placeholder="请输入电池容量"
          right-text="kWh"
        />
      </van-cell-group>

      <!-- 企业运营商专属字段 -->
      <van-cell-group v-if="formData.userType === 'OPERATOR'" inset title="企业信息">
        <van-field
          v-model="formData.companyName"
          label="企业名称"
          placeholder="请输入企业名称"
          :rules="[{ required: true, message: '请输入企业名称' }]"
        />
        <van-field
          v-model="formData.businessLicense"
          label="信用代码"
          placeholder="统一社会信用代码"
          :rules="[{ required: true, message: '请输入统一社会信用代码' }]"
        />
      </van-cell-group>

      <!-- 代充师傅专属字段 -->
      <van-cell-group v-if="formData.userType === 'VALET_CHARGER'" inset title="师傅信息">
        <van-field
          v-model="formData.serviceArea"
          label="服务区域"
          placeholder="如：北京市朝阳区"
          :rules="[{ required: true, message: '请输入服务区域' }]"
        />
        <van-field
          v-model="formData.idCard"
          label="身份证号"
          placeholder="请输入身份证号"
          maxlength="18"
          :rules="[{ required: true, message: '请输入身份证号' }]"
        />
      </van-cell-group>

      <!-- 用户协议 -->
      <div class="agreement">
        <van-checkbox v-model="agreed">
          我已阅读并同意
          <span class="link" @click.stop="showAgreement">《用户协议》</span>
          和
          <span class="link" @click.stop="showPrivacy">《隐私政策》</span>
        </van-checkbox>
      </div>

      <!-- 注册按钮 -->
      <div class="register-button">
        <van-button
          type="primary"
          size="large"
          round
          :loading="loading"
          @click="handleRegister"
        >
          注册
        </van-button>
      </div>

      <!-- 已有账号 -->
      <div class="login-link">
        已有账号？
        <span class="link" @click="$router.push('/login')">立即登录</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showDialog } from 'vant'
import { register } from '@/api/auth'
import { sha256, checkPasswordStrength } from '@/utils/crypto'

const router = useRouter()

const formData = reactive({
  userType: 'USER',
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: '',
  // 个人用户字段
  carModel: '',
  batteryCapacity: '',
  // 企业运营商字段
  companyName: '',
  businessLicense: '',
  // 代充师傅字段
  serviceArea: '',
  idCard: ''
})

const agreed = ref(false)
const loading = ref(false)

// 表单验证
const validateForm = () => {
  // 用户名验证
  if (!formData.username) {
    showToast('请输入用户名')
    return false
  }
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(formData.username)) {
    showToast('用户名为3-20个字符，只能包含字母、数字和下划线')
    return false
  }

  // 密码验证
  if (!formData.password) {
    showToast('请输入密码')
    return false
  }
  if (formData.password.length < 6 || formData.password.length > 20) {
    showToast('密码长度为6-20个字符')
    return false
  }

  // 密码强度检测（仅警告，不阻止）
  const passwordStrength = checkPasswordStrength(formData.password)
  if (passwordStrength.level === 'weak') {
    showToast('提示：' + passwordStrength.tips[0])
  }

  // 确认密码验证
  if (formData.password !== formData.confirmPassword) {
    showToast('两次输入的密码不一致')
    return false
  }

  // 手机号验证
  if (!formData.phone) {
    showToast('请输入手机号')
    return false
  }
  if (!/^1[3-9]\d{9}$/.test(formData.phone)) {
    showToast('手机号格式不正确')
    return false
  }

  // 邮箱验证（选填）
  if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
    showToast('邮箱格式不正确')
    return false
  }

  // 企业运营商额外验证
  if (formData.userType === 'OPERATOR') {
    if (!formData.companyName) {
      showToast('请输入企业名称')
      return false
    }
    if (!formData.businessLicense) {
      showToast('请输入统一社会信用代码')
      return false
    }
  }

  // 代充师傅额外验证
  if (formData.userType === 'VALET_CHARGER') {
    if (!formData.serviceArea) {
      showToast('请输入服务区域')
      return false
    }
    if (!formData.idCard) {
      showToast('请输入身份证号')
      return false
    }
    if (!/^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(formData.idCard)) {
      showToast('身份证号格式不正确')
      return false
    }
  }

  // 用户协议验证
  if (!agreed.value) {
    showToast('请阅读并同意用户协议和隐私政策')
    return false
  }

  return true
}

// 注册
const handleRegister = async () => {
  if (!validateForm()) {
    return
  }

  // 密码强度较弱时，异步询问用户是否继续
  const passwordStrength = checkPasswordStrength(formData.password)
  if (passwordStrength.level === 'weak') {
    try {
      await showDialog({
        title: '密码强度较弱',
        message: passwordStrength.tips.join('\n'),
        confirmButtonText: '继续注册',
        cancelButtonText: '修改密码',
        showCancelButton: true
      })
    } catch {
      // 用户选择修改密码，取消注册
      return
    }
  }

  loading.value = true
  try {
    // 对密码进行SHA256加密
    const encryptedPassword = sha256(formData.password)
    const encryptedConfirmPassword = sha256(formData.confirmPassword)

    // 构建请求数据
    const requestData = {
      username: formData.username,
      password: encryptedPassword,
      confirmPassword: encryptedConfirmPassword,
      nickname: formData.nickname || formData.username,
      phone: formData.phone,
      email: formData.email,
      userType: formData.userType
    }

    // 根据用户类型添加额外字段
    if (formData.userType === 'USER') {
      if (formData.carModel) requestData.carModel = formData.carModel
      if (formData.batteryCapacity) requestData.batteryCapacity = parseInt(formData.batteryCapacity)
    } else if (formData.userType === 'OPERATOR') {
      requestData.companyName = formData.companyName
      requestData.businessLicense = formData.businessLicense
    } else if (formData.userType === 'VALET_CHARGER') {
      requestData.serviceArea = formData.serviceArea
      requestData.idCard = formData.idCard
    }

    const res = await register(requestData)

    if (res.code === 200) {
      showSuccessToast('注册成功')

      // 根据用户类型显示不同提示
      let message = '注册成功！'
      if (formData.userType === 'OPERATOR') {
        message += '\n企业账号需要管理员审核后才能使用完整功能。'
      } else if (formData.userType === 'VALET_CHARGER') {
        message += '\n代充师傅账号需要管理员审核后才能接单。'
      }

      showDialog({
        title: '注册成功',
        message: message
      }).then(() => {
        router.push('/login')
      })
    }
  } catch (error) {
    console.error('注册失败:', error)
    showToast(error.response?.data?.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 显示用户协议
const showAgreement = () => {
  showDialog({
    title: '用户协议',
    message: '这里是用户协议内容...',
    confirmButtonText: '我知道了'
  })
}

// 显示隐私政策
const showPrivacy = () => {
  showDialog({
    title: '隐私政策',
    message: '这里是隐私政策内容...',
    confirmButtonText: '我知道了'
  })
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.content {
  padding-bottom: 80px;
}

.van-cell-group {
  margin-top: 12px;
}

.van-radio-group {
  display: flex;
  justify-content: space-around;
  padding: 16px;
}

.agreement {
  padding: 16px;
  text-align: center;
  font-size: 14px;
  color: #666;
}

.link {
  color: #1989fa;
  cursor: pointer;
}

.register-button {
  padding: 24px 16px 12px;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #666;
  padding: 12px;
}
</style>
