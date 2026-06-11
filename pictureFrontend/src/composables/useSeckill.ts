import { ref, computed } from 'vue'
import {
  seckillControllerGetToken,
  seckillControllerGrab,
  seckillControllerGetResult,
} from '@/api/seckillController'
import { useSeckillStore } from '@/stores/seckill'

export interface GrabResult {
  success: boolean
  message: string
  orderNo?: string
  couponType?: number
}

export function useSeckill() {
  const grabbingBatchId = ref<number | null>(null)
  const grabResult = ref<GrabResult | null>(null)
  const seckillStore = useSeckillStore()

  /** 是否正在抢购中（任意批次） */
  const grabbing = computed(() => grabbingBatchId.value !== null)

  /**
   * 发起抢购
   */
  async function grab(batchId: number): Promise<GrabResult> {
    grabbingBatchId.value = batchId
    grabResult.value = null

    try {
      // 1. 获取 Token（全局有且匹配批次则复用，否则重新获取）
      let token = ''
      if (seckillStore.token && seckillStore.tokenBatchId === batchId) {
        token = seckillStore.token
      } else {
        const tokenRes = await seckillControllerGetToken({ batchId })
        if (tokenRes.data.code !== 0) {
          grabbingBatchId.value = null
        return fail(tokenRes.data.message || '获取令牌失败')
        }
        token = tokenRes.data.data?.token as string
        seckillStore.setToken(token, batchId)
      }

      // 2. 发起抢购
      const grabRes = await seckillControllerGrab({ batchId, token })
      if (grabRes.data.code !== 0) {
        return fail(grabRes.data.message || '抢购失败')
      }
      const orderNo = grabRes.data.data?.orderNo as string

      // 3. 轮询结果（最多 30 次，每次间隔 1.5s）
      const result = await pollResult(orderNo)

      grabbingBatchId.value = null
      grabResult.value = result
      return result
    } catch (e: any) {
      grabbingBatchId.value = null
      const result = { success: false, message: e?.message || '网络异常' }
      grabResult.value = result
      return result
    }
  }

  /**
   * 轮询抢购结果
   */
  async function pollResult(orderNo: string): Promise<GrabResult> {
    const maxAttempts = 30
    const interval = 1500

    for (let i = 0; i < maxAttempts; i++) {
      await sleep(interval)

      const res = await seckillControllerGetResult({ orderNo })
      if (res.data.code !== 0) continue

      const status = res.data.data?.status
      if (status === 1) {
        return {
          success: true,
          message: '抢购成功！',
          orderNo,
          couponType: res.data.data?.couponType,
        }
      }
      if (status === 2) {
        return { success: false, message: '抢购失败，请稍后再试', orderNo }
      }
      // status === 0 → 处理中，继续轮询
    }

    return { success: false, message: '查询超时，请在我的券包中确认结果', orderNo }
  }

  function fail(msg: string): GrabResult {
    grabbingBatchId.value = null
    const result = { success: false, message: msg }
    grabResult.value = result
    return result
  }

  function reset() {
    grabbingBatchId.value = null
    grabResult.value = null
  }

  return { grabbing, grabbingBatchId, grabResult, grab, reset }
}

function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
}
