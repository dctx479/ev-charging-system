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

defineEmits(['click'])

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
  // 显示平时电价，如果没有则显示峰时电价
  const price = pile.priceFlat || pile.pricePeak || pile.priceValley || 0
  return `${price} 元/kWh`
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
</style>
