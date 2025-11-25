<template>
  <div class="balance">
    <van-nav-bar
      title="账户余额"
      left-arrow
      @click-left="onClickLeft"
    />

    <!-- 余额卡片 -->
    <div class="balance-card">
      <div class="balance-label">账户余额（元）</div>
      <div class="balance-amount">¥ {{ balance.toFixed(2) }}</div>
      <div class="balance-actions">
        <van-button type="primary" size="small" round @click="showRechargeDialog = true">
          充值
        </van-button>
        <van-button plain type="primary" size="small" round @click="showWithdrawDialog = true">
          提现
        </van-button>
      </div>
    </div>

    <!-- 交易记录 -->
    <div class="transactions">
      <van-tabs v-model:active="activeTab" @change="handleTabChange">
        <van-tab title="全部" name="all" />
        <van-tab title="收入" name="income" />
        <van-tab title="支出" name="expense" />
      </van-tabs>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          finished-text="没有更多了"
          @load="onLoad"
        >
          <div
            v-for="item in transactions"
            :key="item.id"
            class="transaction-item"
          >
            <div class="transaction-left">
              <div class="transaction-title">{{ item.description }}</div>
              <div class="transaction-time">{{ item.createTime }}</div>
            </div>
            <div
              class="transaction-amount"
              :class="{ income: item.type === 'income', expense: item.type === 'expense' }"
            >
              {{ item.type === 'income' ? '+' : '-' }}¥{{ Math.abs(item.amount).toFixed(2) }}
            </div>
          </div>
        </van-list>
      </van-pull-refresh>
    </div>

    <!-- 充值对话框 -->
    <van-dialog
      v-model:show="showRechargeDialog"
      title="账户充值"
      show-cancel-button
      @confirm="handleRecharge"
    >
      <van-field
        v-model="rechargeAmount"
        type="number"
        label="充值金额"
        placeholder="请输入充值金额"
        left-icon="gold-coin-o"
      />
      <van-radio-group v-model="paymentMethod" class="payment-method">
        <van-cell-group inset>
          <van-cell title="微信支付" clickable @click="paymentMethod = 'wechat'">
            <template #right-icon>
              <van-radio name="wechat" />
            </template>
          </van-cell>
          <van-cell title="支付宝" clickable @click="paymentMethod = 'alipay'">
            <template #right-icon>
              <van-radio name="alipay" />
            </template>
          </van-cell>
        </van-cell-group>
      </van-radio-group>
    </van-dialog>

    <!-- 提现对话框 -->
    <van-dialog
      v-model:show="showWithdrawDialog"
      title="账户提现"
      show-cancel-button
      @confirm="handleWithdraw"
    >
      <van-field
        v-model="withdrawAmount"
        type="number"
        label="提现金额"
        placeholder="请输入提现金额"
        left-icon="gold-coin-o"
      />
      <van-field
        v-model="bankAccount"
        label="银行卡号"
        placeholder="请输入银行卡号"
        left-icon="credit-pay"
      />
      <div class="withdraw-tips">
        <van-notice-bar color="#ed6a0c" background="#fffbe8" left-icon="info-o">
          提现将在1-3个工作日内到账
        </van-notice-bar>
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showLoadingToast } from 'vant'
import { getBalance, recharge, withdraw, getTransactions } from '@/api/wallet'

const router = useRouter()

const balance = ref(0)
const activeTab = ref('all')
const transactions = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)
const pageSize = 20

const showRechargeDialog = ref(false)
const rechargeAmount = ref('')
const paymentMethod = ref('wechat')

const showWithdrawDialog = ref(false)
const withdrawAmount = ref('')
const bankAccount = ref('')

const onClickLeft = () => {
  router.back()
}

// 加载余额
const loadBalance = async () => {
  try {
    const res = await getBalance()
    balance.value = res.data
  } catch (error) {
    console.error('加载余额失败:', error)
  }
}

// 加载交易记录
const loadTransactions = async () => {
  loading.value = true
  try {
    const res = await getTransactions({
      page: page.value,
      size: pageSize,
      type: activeTab.value === 'all' ? null : activeTab.value
    })

    const newTransactions = res.data.records || []

    if (page.value === 1) {
      transactions.value = newTransactions
    } else {
      transactions.value.push(...newTransactions)
    }

    loading.value = false

    if (newTransactions.length < pageSize) {
      finished.value = true
    }
  } catch (error) {
    console.error('加载交易记录失败:', error)
    loading.value = false
    finished.value = true
  }
}

// 下拉刷新
const onRefresh = () => {
  page.value = 1
  finished.value = false
  loadBalance()
  loadTransactions()
  refreshing.value = false
}

// 加载更多
const onLoad = () => {
  if (!finished.value) {
    page.value++
    loadTransactions()
  }
}

// 切换标签
const handleTabChange = () => {
  page.value = 1
  finished.value = false
  transactions.value = []
  loadTransactions()
}

// 充值
const handleRecharge = async () => {
  const amount = parseFloat(rechargeAmount.value)

  if (!amount || amount <= 0) {
    showToast('请输入有效的充值金额')
    return
  }

  if (amount < 10) {
    showToast('最低充值金额为10元')
    return
  }

  showLoadingToast({
    message: '正在处理...',
    forbidClick: true,
    duration: 0
  })

  try {
    await recharge({
      amount,
      paymentMethod: paymentMethod.value
    })

    showSuccessToast('充值成功')
    rechargeAmount.value = ''
    showRechargeDialog.value = false

    // 刷新余额和交易记录
    loadBalance()
    onRefresh()
  } catch (error) {
    console.error('充值失败:', error)
    showToast(error.response?.data?.message || '充值失败')
  }
}

// 提现
const handleWithdraw = async () => {
  const amount = parseFloat(withdrawAmount.value)

  if (!amount || amount <= 0) {
    showToast('请输入有效的提现金额')
    return
  }

  if (amount > balance.value) {
    showToast('提现金额不能超过账户余额')
    return
  }

  if (amount < 10) {
    showToast('最低提现金额为10元')
    return
  }

  if (!bankAccount.value) {
    showToast('请输入银行卡号')
    return
  }

  showLoadingToast({
    message: '正在处理...',
    forbidClick: true,
    duration: 0
  })

  try {
    await withdraw({
      amount,
      bankAccount: bankAccount.value
    })

    showSuccessToast('提现申请已提交')
    withdrawAmount.value = ''
    bankAccount.value = ''
    showWithdrawDialog.value = false

    // 刷新余额和交易记录
    loadBalance()
    onRefresh()
  } catch (error) {
    console.error('提现失败:', error)
    showToast(error.response?.data?.message || '提现失败')
  }
}

onMounted(() => {
  loadBalance()
  loadTransactions()
})
</script>

<style scoped>
.balance {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 余额卡片 */
.balance-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px 20px;
  color: white;
  text-align: center;
}

.balance-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 12px;
}

.balance-amount {
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 20px;
}

.balance-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 交易记录 */
.transactions {
  background-color: white;
  min-height: calc(100vh - 200px);
}

.transaction-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.transaction-left {
  flex: 1;
}

.transaction-title {
  font-size: 15px;
  color: #323233;
  margin-bottom: 4px;
}

.transaction-time {
  font-size: 12px;
  color: #969799;
}

.transaction-amount {
  font-size: 16px;
  font-weight: bold;
}

.transaction-amount.income {
  color: #07c160;
}

.transaction-amount.expense {
  color: #ee0a24;
}

/* 对话框样式 */
.payment-method {
  margin-top: 12px;
}

.withdraw-tips {
  padding: 12px 16px;
}
</style>
