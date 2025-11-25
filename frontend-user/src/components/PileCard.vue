<template>
  <van-card
    class="pile-card"
    :title="pile.name"
    :desc="`编号: ${pile.pileCode}`"
    @click="$emit('click')"
  >
    <template #tags>
      <van-tag
        v-if="pile.status === 'AVAILABLE'"
        type="success"
        size="medium"
      >
        可用
      </van-tag>
      <van-tag
        v-else-if="pile.status === 'CHARGING'"
        type="primary"
        size="medium"
      >
        充电中
      </van-tag>
      <van-tag
        v-else-if="pile.status === 'FAULT'"
        type="danger"
        size="medium"
      >
        故障
      </van-tag>
      <van-tag
        v-else-if="pile.status === 'MAINTENANCE'"
        type="warning"
        size="medium"
      >
        维护中
      </van-tag>
      <van-tag v-else type="default" size="medium">离线</van-tag>
    </template>

    <template #footer>
      <div class="pile-info">
        <van-cell-group inset>
          <van-cell title="类型" :value="getPileType(pile.pileType)" />
          <van-cell title="功率" :value="`${pile.ratedPower} kW`" />
          <van-cell title="价格" :value="`${pile.price} 元/kWh`" />
          <van-cell
            v-if="pile.status === 'CHARGING'"
            title="当前功率"
            :value="`${pile.currentPower} kW`"
          />
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
  return type === 'DC' ? '直流充电' : '交流充电'
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
