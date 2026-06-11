import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSeckillStore = defineStore('seckill', () => {
  /** 当前持有的秒杀令牌 */
  const token = ref<string | null>(null)
  /** 令牌对应的批次 ID */
  const tokenBatchId = ref<number | null>(null)

  function setToken(newToken: string, batchId: number) {
    token.value = newToken
    tokenBatchId.value = batchId
  }

  function clearToken() {
    token.value = null
    tokenBatchId.value = null
  }

  return { token, tokenBatchId, setToken, clearToken }
})
