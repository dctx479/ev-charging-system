<template>
  <div class="profile-container">
    <!-- 顶部用户信息 -->
    <div class="user-card">
      <div class="user-avatar">
        <van-icon name="user-circle-o" size="60" />
      </div>
      <div class="user-info">
        <div class="user-name">{{ userStore.username || '游客' }}</div>
        <div class="user-phone">{{ userStore.phone || '未绑定手机' }}</div>
      </div>
    </div>

    <!-- 积分卡片 -->
    <div class="credit-card" @click="goToCreditCenter">
      <div class="credit-left">
        <div class="credit-icon">
          <van-icon name="medal-o" size="32" color="#667eea" />
        </div>
        <div class="credit-info">
          <div class="credit-title">我的碳积分</div>
          <div class="credit-amount">{{ creditBalance }} 积分</div>
        </div>
      </div>
      <van-icon name="arrow" color="#999" />
    </div>

    <!-- 功能列表 -->
    <div class="menu-section">
      <van-cell-group>
        <van-cell title="我的订单" icon="orders-o" is-link to="/orders" />
        <van-cell
          title="积分中心"
          icon="medal-o"
          is-link
          @click="goToCreditCenter"
        >
          <template #value>
            <span class="credit-badge">{{ creditBalance }}</span>
          </template>
        </van-cell>
        <van-cell title="账户余额" icon="balance-o" is-link>
          <template #value>
            <span>¥ {{ userStore.balance || 0 }}</span>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <div class="menu-section">
      <van-cell-group>
        <van-cell title="个人信息" icon="contact-o" is-link />
        <van-cell title="设置" icon="setting-o" is-link />
      </van-cell-group>
    </div>

    <!-- 退出登录 -->
    <div class="logout-section">
      <van-button type="danger" block @click="handleLogout">退出登录</van-button>
    </div>

    <!-- 底部导航栏 -->
    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/" icon="shop-o">首页</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/store/user'
import { getCreditBalance } from '@/api/credit'

const router = useRouter()
const userStore = useUserStore()

const active = ref(2)
const creditBalance = ref(0)

// 加载积分余额
const loadCreditBalance = async () => {
  try {
    const userId = userStore.userId || 1
    const res = await getCreditBalance(userId)
    creditBalance.value = res.data
  } catch (error) {
    console.error('加载积分余额失败:', error)
  }
}

// 跳转到积分中心
const goToCreditCenter = () => {
  router.push('/credit/center')
}

// 退出登录
const handleLogout = () => {
  showConfirmDialog({
    title: '提示',
    message: '确定要退出登录吗？'
  })
    .then(() => {
      userStore.logout()
      showToast('退出成功')
      router.push('/login')
    })
    .catch(() => {
      // 取消
    })
}

onMounted(() => {
  loadCreditBalance()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 60px;
}

/* 用户信息卡片 */
.user-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px 20px;
  display: flex;
  align-items: center;
  color: white;
}

.user-avatar {
  margin-right: 16px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 8px;
}

.user-phone {
  font-size: 14px;
  opacity: 0.9;
}

/* 积分卡片 */
.credit-card {
  background: white;
  margin: 16px;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.15);
  cursor: pointer;
  transition: transform 0.2s;
}

.credit-card:active {
  transform: scale(0.98);
}

.credit-left {
  display: flex;
  align-items: center;
}

.credit-icon {
  margin-right: 16px;
}

.credit-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.credit-amount {
  font-size: 20px;
  font-weight: bold;
  color: #667eea;
}

.credit-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: bold;
}

/* 功能菜单 */
.menu-section {
  margin: 12px 16px;
  border-radius: 12px;
  overflow: hidden;
}

/* 退出登录 */
.logout-section {
  margin: 24px 16px;
}
</style>
