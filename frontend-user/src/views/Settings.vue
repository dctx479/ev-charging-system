<template>
  <div class="settings">
    <van-nav-bar
      title="设置"
      left-arrow
      @click-left="onClickLeft"
    />

    <div class="content">
      <!-- 账号设置 -->
      <van-cell-group inset title="账号设置">
        <van-cell title="个人信息" icon="user-o" is-link to="/user/info" />
        <van-cell title="修改密码" icon="shield-o" is-link to="/user/password" />
      </van-cell-group>

      <!-- 通知设置 -->
      <van-cell-group inset title="通知设置">
        <van-cell title="订单通知" center>
          <template #right-icon>
            <van-switch v-model="settings.orderNotification" size="20" />
          </template>
        </van-cell>
        <van-cell title="充电完成提醒" center>
          <template #right-icon>
            <van-switch v-model="settings.chargeCompleteNotification" size="20" />
          </template>
        </van-cell>
        <van-cell title="排队到号提醒" center>
          <template #right-icon>
            <van-switch v-model="settings.queueNotification" size="20" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 隐私设置 -->
      <van-cell-group inset title="隐私设置">
        <van-cell title="允许他人查看我的充电记录" center>
          <template #right-icon>
            <van-switch v-model="settings.shareChargeRecord" size="20" />
          </template>
        </van-cell>
        <van-cell title="在充电搭子中显示我的位置" center>
          <template #right-icon>
            <van-switch v-model="settings.showLocation" size="20" />
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 其他设置 -->
      <van-cell-group inset title="其他">
        <van-cell title="清除缓存" is-link @click="handleClearCache">
          <template #value>
            <span class="cache-size">{{ cacheSize }}</span>
          </template>
        </van-cell>
        <van-cell title="关于我们" icon="info-o" is-link @click="handleAbout" />
        <van-cell title="版本号" :value="version" />
      </van-cell-group>

      <!-- 退出登录 -->
      <div class="logout-button">
        <van-button type="danger" size="large" round @click="handleLogout">
          退出登录
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog, showDialog } from 'vant'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const settings = ref({
  orderNotification: true,
  chargeCompleteNotification: true,
  queueNotification: true,
  shareChargeRecord: false,
  showLocation: true
})

const cacheSize = ref('12.5 MB')
const version = ref('1.0.0')

const onClickLeft = () => {
  router.back()
}

// 初始化设置（必须在 watch 之前调用，避免触发不必要的 toast）
const initSettings = () => {
  const savedSettings = localStorage.getItem('userSettings')
  if (savedSettings) {
    settings.value = JSON.parse(savedSettings)
  }
}

initSettings()

// 监听设置变更并保存
watch(
  settings,
  (newSettings) => {
    localStorage.setItem('userSettings', JSON.stringify(newSettings))
    showToast('设置已保存')
  },
  { deep: true }
)

// 清除缓存
const handleClearCache = () => {
  showConfirmDialog({
    title: '提示',
    message: '确定要清除缓存吗？'
  })
    .then(() => {
      // 清除缓存逻辑（保留登录信息）
      const keysToKeep = ['token', 'userInfo', 'userSettings']
      const allKeys = Object.keys(localStorage)

      allKeys.forEach(key => {
        if (!keysToKeep.includes(key)) {
          localStorage.removeItem(key)
        }
      })

      cacheSize.value = '0 MB'
      showSuccessToast('缓存已清除')
    })
    .catch(() => {
      // 取消
    })
}

// 关于我们
const handleAbout = () => {
  showDialog({
    title: '关于我们',
    message: `
      新能源汽车充电站点管理系统

      版本：${version.value}

      功能特色：
      • 智能充电桩查询
      • AI充电时长预测
      • 智能排队系统
      • V2G双向充电
      • 碳积分体系
      • 充电搭子社交

      © 2025 EV Charging System
    `,
    confirmButtonText: '我知道了'
  })
}

// 退出登录
const handleLogout = () => {
  showConfirmDialog({
    title: '提示',
    message: '确定要退出登录吗？'
  })
    .then(() => {
      userStore.logout()
      showSuccessToast('退出成功')
      router.push('/login')
    })
    .catch(() => {
      // 取消
    })
}

</script>

<style scoped>
.settings {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.content {
  padding-bottom: 80px;
}

.van-cell-group {
  margin-top: 12px;
}

.cache-size {
  color: #969799;
  font-size: 14px;
}

.logout-button {
  padding: 24px 16px;
}
</style>
