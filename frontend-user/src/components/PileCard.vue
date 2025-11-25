<template>
  <van-card
    class="pile-card"
    :title="pile.pileName || `充电桩 ${pile.pileNo}`"
    :desc="`编号: ${pile.pileNo}`"
    @click="$emit('click')"
  >
    <template #tags>
      <van-tag
        v-if="pile.status === 1"
        type="success"
        size="medium"
      >
        可用
      </van-tag>
      <van-tag
        v-else-if="pile.status === 2"
        type="primary"
        size="medium"
      >
        充电中
      </van-tag>
      <van-tag
        v-else-if="pile.status === 3"
        type="warning"
        size="medium"
      >
        预约中
      </van-tag>
      <van-tag
        v-else-if="pile.status === 4"
        type="danger"
        size="medium"
      >
        故障
      </van-tag>
      <van-tag v-else type="default" size="medium">离线</van-tag>
    </template>

    <template #footer>
      <div class="pile-info">
        <van-cell-group inset>
          <van-cell title="类型" :value="getPileType(pile.pileType)" />
          <van-cell title="功率" :value="`${pile.power || 0} kW`" />
          <van-cell title="价格" :value="getPriceText(pile)" />
        </van-cell-group>
        <!-- 开始充电按钮 - 仅在空闲状态显示 -->
        <van-button
          v-if="pile.status === 1"
          type="primary"
          size="small"
          round
          block
          class="charge-button"
          @click.stop="handleStartCharging"
        >
          开始充电
        </van-button>
      </div>
    </template>
  </van-card>
</template>

<script setup>
defineProps({
  pile: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click', 'startCharging'])

const getPileType = (type) => {
  // 1快充 2慢充 3超充
  switch (type) {
    case 1:
      return '快充'
    case 2:
      return '慢充'
    case 3:
      return '超充'
    default:
      return '交流充电'
  }
}

const getPriceText = (pile) => {
  // 使用 ?? (nullish coalescing) 避免将 0 视为假值（免费充电桩场景）
  const price = pile.priceFlat ?? pile.pricePeak ?? pile.priceValley ?? 0
  return `${price} 元/kWh`
}

const handleStartCharging = () => {
  emit('startCharging')
}
</script>

<style scoped>
.pile-card {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.pile-info {
  margin-top: 8px;
}

.van-cell-group {
  margin: 0;
}

.charge-button {
  margin-top: 12px;
}
</style>
