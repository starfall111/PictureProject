<template>
  <div class="coupon-page">
    <div class="coupon-header">
      <h2>我的券包</h2>
      <router-link to="/seckill">
        <a-button type="link">去抢券 <RightOutlined /></a-button>
      </router-link>
    </div>

    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane v-for="tab in COUPON_TAB_LIST" :key="tab.key" :tab="tab.label" />
    </a-tabs>

    <a-spin :spinning="loading">
      <div class="coupon-list">
        <CouponCard
          v-for="coupon in couponList"
          :key="coupon.id"
          :coupon="coupon"
          @activate="handleActivate"
        />
        <a-empty v-if="!loading && couponList.length === 0" description="暂无券" />
      </div>
    </a-spin>

    <div class="coupon-pagination" v-if="pagination.total > pagination.pageSize">
      <a-pagination
        v-model:current="pagination.current"
        :total="pagination.total"
        :pageSize="pagination.pageSize"
        @change="fetchCoupons"
      />
    </div>

    <ActivateConfirmModal
      :visible="activateModalVisible"
      :coupon="selectedCoupon"
      :loading="activating"
      @confirm="doActivate"
      @cancel="activateModalVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { RightOutlined } from '@ant-design/icons-vue'
import CouponCard from '@/components/coupon/CouponCard.vue'
import ActivateConfirmModal from '@/components/coupon/ActivateConfirmModal.vue'
import { couponControllerListMyCoupons, couponControllerActivateCoupon } from '@/api/couponController'
import { COUPON_TAB_LIST } from '@/constants/coupon'

const activeTab = ref<number>(1)
const couponList = ref<API.CouponVO[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 10, total: 0 })

const activateModalVisible = ref(false)
const selectedCoupon = ref<API.CouponVO | null>(null)
const activating = ref(false)

async function fetchCoupons() {
  loading.value = true
  try {
    const res = await couponControllerListMyCoupons({
      page: pagination.value.current,
      size: pagination.value.pageSize,
      status: activeTab.value,
    })
    if (res.data.code === 0) {
      couponList.value = res.data.data?.records ?? []
      pagination.value.total = res.data.data?.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  pagination.value.current = 1
  fetchCoupons()
}

function handleActivate(couponId: number) {
  selectedCoupon.value = couponList.value.find(c => c.id === couponId) ?? null
  activateModalVisible.value = true
}

async function doActivate(couponId: number) {
  if (activating.value) return
  activating.value = true
  try {
    const res = await couponControllerActivateCoupon({ couponId })
    if (res.data.code === 0 && res.data.data?.activated) {
      message.success(`激活成功，已获得 ${res.data.data.couponTypeName ?? 'VIP会员'}`)
      activateModalVisible.value = false
      selectedCoupon.value = null
      fetchCoupons()
    } else {
      message.error(res.data.message || '激活失败')
    }
  } catch (e: any) {
    message.error(e?.message || '激活失败')
  } finally {
    activating.value = false
  }
}

onMounted(() => {
  fetchCoupons()
})
</script>

<style scoped>
.coupon-page {
  max-width: 900px;
  margin: 0 auto;
}

.coupon-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.coupon-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.coupon-pagination {
  margin-top: 24px;
  text-align: center;
}
</style>
