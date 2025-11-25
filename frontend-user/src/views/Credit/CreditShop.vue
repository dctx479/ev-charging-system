<template>
  <div class="credit-shop">
    <van-nav-bar
      title="积分商城"
      left-arrow
      @click-left="onClickLeft"
    />

    <!-- 积分余额 -->
    <div class="credit-balance">
      <div class="balance-left">
        <van-icon name="medal-o" size="32" color="#fff" />
        <div class="balance-info">
          <div class="balance-label">我的积分</div>
          <div class="balance-amount">{{ creditBalance }}</div>
        </div>
      </div>
      <van-button size="small" plain type="primary" round @click="goToCreditHistory">
        积分明细
      </van-button>
    </div>

    <!-- 分类标签 -->
    <van-tabs v-model:active="activeCategory" @change="handleCategoryChange">
      <van-tab title="全部" name="all" />
      <van-tab title="优惠券" name="coupon" />
      <van-tab title="充电折扣" name="discount" />
      <van-tab title="实物礼品" name="gift" />
    </van-tabs>

    <!-- 商品列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div class="product-list">
          <div
            v-for="product in products"
            :key="product.id"
            class="product-item"
            @click="showProductDetail(product)"
          >
            <van-image
              :src="product.imageUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
              fit="cover"
              class="product-image"
            />
            <div class="product-info">
              <div class="product-name">{{ product.name }}</div>
              <div class="product-desc">{{ product.description }}</div>
              <div class="product-footer">
                <div class="product-credit">
                  <van-icon name="medal-o" size="14" color="#667eea" />
                  <span>{{ product.pointsRequired }} 积分</span>
                </div>
                <van-button
                  size="small"
                  type="primary"
                  round
                  :disabled="creditBalance < product.pointsRequired"
                  @click.stop="handleExchange(product)"
                >
                  {{ creditBalance < product.pointsRequired ? '积分不足' : '兑换' }}
                </van-button>
              </div>
            </div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>

    <!-- 商品详情弹窗 -->
    <van-popup
      v-model:show="showDetail"
      position="bottom"
      round
      :style="{ height: '70%' }"
    >
      <div v-if="selectedProduct" class="product-detail">
        <div class="detail-header">
          <van-image
            :src="selectedProduct.imageUrl || 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'"
            fit="cover"
            class="detail-image"
          />
        </div>
        <div class="detail-content">
          <div class="detail-title">{{ selectedProduct.name }}</div>
          <div class="detail-credit">
            <van-icon name="medal-o" size="18" color="#667eea" />
            <span>{{ selectedProduct.pointsRequired }} 积分</span>
          </div>
          <div class="detail-desc">{{ selectedProduct.description }}</div>
          <div class="detail-info">
            <div class="info-item">
              <span class="info-label">有效期：</span>
              <span>{{ selectedProduct.validDays || 30 }}天</span>
            </div>
            <div class="info-item">
              <span class="info-label">库存：</span>
              <span>{{ selectedProduct.stock || '充足' }}</span>
            </div>
          </div>
        </div>
        <div class="detail-footer">
          <van-button
            type="primary"
            size="large"
            round
            :disabled="creditBalance < selectedProduct.pointsRequired"
            @click="confirmExchange"
          >
            {{ creditBalance < selectedProduct.pointsRequired ? '积分不足' : '立即兑换' }}
          </van-button>
        </div>
      </div>
    </van-popup>

    <!-- 兑换记录入口 -->
    <van-floating-bubble
      icon="orders-o"
      @click="goToExchangeRecords"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import { getCreditBalance, getCreditProducts, exchangeProduct } from '@/api/credit'

const router = useRouter()

const creditBalance = ref(0)
const activeCategory = ref('all')
const products = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)
const pageSize = 20

const showDetail = ref(false)
const selectedProduct = ref(null)

const onClickLeft = () => {
  router.back()
}

// 加载积分余额
const loadCreditBalance = async () => {
  try {
    const res = await getCreditBalance()
    creditBalance.value = res.data
  } catch (error) {
    console.error('加载积分余额失败:', error)
  }
}

// 加载商品列表
const loadProducts = async () => {
  loading.value = true
  try {
    const res = await getCreditProducts({
      category: activeCategory.value === 'all' ? null : activeCategory.value,
      page: page.value,
      size: pageSize
    })

    // 后端直接返回数组，不是分页对象
    const newProducts = res.data || []

    if (page.value === 1) {
      products.value = newProducts
    } else {
      products.value.push(...newProducts)
    }

    loading.value = false

    if (newProducts.length < pageSize) {
      finished.value = true
    }
  } catch (error) {
    console.error('加载商品列表失败:', error)
    loading.value = false
    finished.value = true
  }
}

// 下拉刷新
const onRefresh = () => {
  page.value = 1
  finished.value = false
  loadCreditBalance()
  loadProducts()
  refreshing.value = false
}

// 加载更多
const onLoad = () => {
  if (!finished.value) {
    page.value++
    loadProducts()
  }
}

// 切换分类
const handleCategoryChange = () => {
  page.value = 1
  finished.value = false
  products.value = []
  loadProducts()
}

// 显示商品详情
const showProductDetail = (product) => {
  selectedProduct.value = product
  showDetail.value = true
}

// 兑换商品
const handleExchange = (product) => {
  if (creditBalance.value < product.pointsRequired) {
    showToast('积分不足')
    return
  }

  selectedProduct.value = product
  confirmExchange()
}

// 确认兑换
const confirmExchange = () => {
  showConfirmDialog({
    title: '确认兑换',
    message: `确定要用 ${selectedProduct.value.pointsRequired} 积分兑换 ${selectedProduct.value.name} 吗？`
  })
    .then(async () => {
      try {
        await exchangeProduct({
          productId: selectedProduct.value.id,
          quantity: 1,
          address: '' // 如果是实物，需要填写地址
        })

        showSuccessToast('兑换成功')
        showDetail.value = false

        // 刷新积分余额和商品列表
        loadCreditBalance()
        onRefresh()
      } catch (error) {
        console.error('兑换失败:', error)
        showToast(error.response?.data?.message || '兑换失败')
      }
    })
    .catch(() => {
      // 取消
    })
}

// 跳转到积分明细
const goToCreditHistory = () => {
  router.push('/credit/history')
}

// 跳转到兑换记录
const goToExchangeRecords = () => {
  router.push('/credit/exchange/records')
}

onMounted(() => {
  loadCreditBalance()
  loadProducts()
})
</script>

<style scoped>
.credit-shop {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 积分余额 */
.credit-balance {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.balance-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.balance-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.balance-label {
  font-size: 13px;
  opacity: 0.9;
}

.balance-amount {
  font-size: 24px;
  font-weight: bold;
}

/* 商品列表 */
.product-list {
  padding: 12px;
}

.product-item {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 12px;
  display: flex;
  cursor: pointer;
  transition: transform 0.2s;
}

.product-item:active {
  transform: scale(0.98);
}

.product-image {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 15px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 4px;
}

.product-desc {
  font-size: 12px;
  color: #969799;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-credit {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667eea;
  font-weight: bold;
  font-size: 14px;
}

/* 商品详情 */
.product-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  position: relative;
}

.detail-image {
  width: 100%;
  height: 250px;
}

.detail-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.detail-title {
  font-size: 18px;
  font-weight: bold;
  color: #323233;
  margin-bottom: 12px;
}

.detail-credit {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #667eea;
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 16px;
}

.detail-desc {
  font-size: 14px;
  color: #646566;
  line-height: 1.6;
  margin-bottom: 20px;
}

.detail-info {
  background: #f7f8fa;
  padding: 12px;
  border-radius: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #969799;
  margin-right: 8px;
}

.detail-footer {
  padding: 16px;
  border-top: 1px solid #ebedf0;
}
</style>
