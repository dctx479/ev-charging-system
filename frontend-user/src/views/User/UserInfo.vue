<template>
  <div class="user-info">
    <van-nav-bar
      title="个人信息"
      left-arrow
      @click-left="onClickLeft"
    />

    <div class="content">
      <!-- 头像 -->
      <van-cell-group inset>
        <van-cell title="头像" center>
          <template #value>
            <van-uploader
              v-model="avatarFile"
              :max-count="1"
              :after-read="handleUploadAvatar"
            >
              <div class="avatar-preview">
                <van-image
                  v-if="userInfo.avatar"
                  :src="userInfo.avatar"
                  round
                  width="60"
                  height="60"
                  fit="cover"
                />
                <van-icon v-else name="user-circle-o" size="60" />
              </div>
            </van-uploader>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 基本信息 -->
      <van-cell-group inset title="基本信息">
        <van-field
          v-model="userInfo.username"
          label="用户名"
          placeholder="请输入用户名"
          readonly
          disabled
        />
        <van-field
          v-model="userInfo.nickname"
          label="昵称"
          placeholder="请输入昵称"
        />
        <van-field
          v-model="userInfo.phone"
          label="手机号"
          placeholder="请输入手机号"
          type="tel"
        />
        <van-field
          v-model="userInfo.email"
          label="邮箱"
          placeholder="请输入邮箱"
          type="email"
        />
      </van-cell-group>

      <!-- 车辆信息 -->
      <van-cell-group inset title="车辆信息">
        <van-field
          v-model="userInfo.carModel"
          label="车型"
          placeholder="如：特斯拉 Model 3"
        />
        <van-field
          v-model="userInfo.batteryCapacity"
          label="电池容量"
          placeholder="请输入电池容量"
          type="number"
          right-text="kWh"
        />
      </van-cell-group>

      <!-- 保存按钮 -->
      <div class="save-button">
        <van-button
          type="primary"
          size="large"
          round
          :loading="saving"
          @click="handleSave"
        >
          保存
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import { getUserInfo, updateUserInfo, uploadAvatar } from '@/api/user'

const router = useRouter()

const userInfo = ref({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  avatar: '',
  carModel: '',
  batteryCapacity: ''
})

const avatarFile = ref([])
const saving = ref(false)

const onClickLeft = () => {
  router.back()
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res = await getUserInfo()
    userInfo.value = res.data ?? {
      username: '',
      nickname: '',
      phone: '',
      email: '',
      avatar: '',
      carModel: '',
      batteryCapacity: ''
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    showToast('加载失败')
  }
}

// 上传头像
const handleUploadAvatar = async (file) => {
  try {
    const formData = new FormData()
    formData.append('file', file.file)

    const res = await uploadAvatar(formData)
    const avatarUrl = res.data?.url || res.data?.avatarUrl || (typeof res.data === 'string' ? res.data : null)
    if (avatarUrl) {
      userInfo.value.avatar = avatarUrl
    }
    showSuccessToast('头像上传成功')
  } catch (error) {
    console.error('上传失败:', error)
    showToast('上传失败')
  }
}

// 保存用户信息
const handleSave = async () => {
  // 验证
  if (!userInfo.value.nickname) {
    showToast('请输入昵称')
    return
  }

  if (userInfo.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userInfo.value.email)) {
    showToast('邮箱格式不正确')
    return
  }

  saving.value = true
  try {
    await updateUserInfo({
      nickname: userInfo.value.nickname,
      phone: userInfo.value.phone,
      email: userInfo.value.email,
      carModel: userInfo.value.carModel,
      batteryCapacity: userInfo.value.batteryCapacity
    })
    showSuccessToast('保存成功')
    setTimeout(() => {
      router.back()
    }, 1000)
  } catch (error) {
    console.error('保存失败:', error)
    showToast(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.user-info {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.content {
  padding-bottom: 80px;
}

.avatar-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.van-cell-group {
  margin-top: 12px;
}

.save-button {
  padding: 24px 16px;
}
</style>
