<template>
  <van-card
    class="station-card"
    :title="station.name"
    :desc="station.address"
    @click="$emit('click')"
  >
    <template #thumb>
      <van-image
        v-if="station.imageUrl"
        :src="station.imageUrl"
        width="90"
        height="90"
        fit="cover"
        radius="8"
      />
      <div v-else class="default-image">
        <van-icon name="shop-o" size="40" />
      </div>
    </template>

    <template #tags>
      <van-tag
        v-if="station.status === 'ACTIVE'"
        type="success"
        size="medium"
      >
        营业中
      </van-tag>
      <van-tag
        v-else-if="station.status === 'MAINTENANCE'"
        type="warning"
        size="medium"
      >
        维护中
      </van-tag>
      <van-tag v-else type="danger" size="medium">已关闭</van-tag>
    </template>

    <template #footer>
      <div class="station-info">
        <div class="info-item">
          <van-icon name="charging" />
          <span>{{ station.availablePiles }}/{{ station.totalPiles }} 可用</span>
        </div>
        <div class="info-item">
          <van-icon name="star" />
          <span>{{ station.rating }}</span>
        </div>
        <div v-if="station.distance" class="info-item">
          <van-icon name="location-o" />
          <span>{{ station.distanceText }}</span>
        </div>
      </div>
    </template>
  </van-card>
</template>

<script setup>
defineProps({
  station: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])
</script>

<style scoped>
.station-card {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.default-image {
  width: 90px;
  height: 90px;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #999;
}

.station-info {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
}

.info-item .van-icon {
  font-size: 14px;
}
</style>
