<template>
  <div class="tab-content">
    <!-- 引导绑定手机号横幅 -->
    <div v-if="!userPhone" class="bind-phone-banner">
      <div class="banner-content">
        <svg class="banner-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2C10.9 2 10 2.9 10 4V12C10 13.1 10.9 14 12 14C13.1 14 14 13.1 14 12V4C14 2.9 13.1 2 12 2Z" fill="currentColor"/>
          <path d="M17 7C17 6.44772 16.5523 6 16 6C15.4477 6 15 6.44772 15 7V12C15 13.6569 13.6569 15 12 15C10.3431 15 9 13.6569 9 12V7C9 6.44772 8.55228 6 8 6C7.44772 6 7 6.44772 7 7V12C7 14.76 9.24 17 12 17C14.76 17 17 14.76 17 12V7Z" fill="currentColor"/>
          <path d="M12 19C11.4477 19 11 19.4477 11 20C11 20.5523 11.4477 21 12 21C12.5523 21 13 20.5523 13 20C13 19.4477 12.5523 19 12 19Z" fill="currentColor" fill-opacity="0"/>
          <rect x="10" y="19" width="4" height="2" rx="1" fill="currentColor"/>
        </svg>
        <span class="banner-text">绑定手机号后，上传图片即可免审核直接发布</span>
        <a-button type="link" size="small" class="banner-btn" @click="phoneModalVisible = true">
          立即绑定
        </a-button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!isLoading && dataList.length === 0" class="empty-state">
      <PendingIcon class="empty-icon" />
      <p class="empty-text">没有待审核的图片</p>
    </div>

    <!-- 图片列表 -->
    <PictureList
      v-else
      :dataList="convertToPictureVO(dataList)"
      :loading="isLoading && dataList.length === 0"
      layoutMode="waterfall"
      :showSocial="true"
      :hasMore="hasMore"
      :isLoadingMore="isLoading"
      @loadMore="() => fetchData(false)"
    />
    <!-- 哨兵元素 -->
    <div :id="`scroll-sentinel-pending-${userId}`" class="scroll-sentinel" />

    <!-- 手机号绑定弹窗 -->
    <a-modal
      v-model:open="phoneModalVisible"
      title="绑定手机号"
      :footer="null"
      :destroy-on-close="true"
      width="440px"
    >
      <a-form layout="vertical" class="bind-form">
        <a-form-item label="手机号" required>
          <a-input v-model:value="phoneForm.phone" placeholder="请输入手机号" size="large" :maxlength="11" />
        </a-form-item>
        <a-form-item label="验证码" required>
          <div class="code-input-row">
            <a-input v-model:value="phoneForm.code" placeholder="请输入验证码" size="large" :maxlength="6" />
            <a-button size="large" :disabled="phoneCountdown > 0" :loading="captchaVerifying" @click="sendPhoneCode">
              {{ phoneCountdown > 0 ? `${phoneCountdown}s 后重发` : '获取验证码' }}
            </a-button>
          </div>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="large" block :loading="phoneBindLoading" @click="handleBindPhone">
            确认绑定
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { pictureControllerQueryPendingPictures } from '@/api/pictureController'
import { userControllerBindAccount } from '@/api/userController'
import { noticeControllerSendVerificationCode } from '@/api/noticeController'
import { useAliyunCaptcha } from '@/access/useAliyunCaptcha'
import PictureList from '@/components/PictureList/index.vue'
import { useTabContent } from './useTabContent'

interface Props {
  userId: string | number
  isVisible?: boolean
  userPhone?: string
}

const props = withDefaults(defineProps<Props>(), {
  isVisible: false,
  userPhone: '',
})

const emit = defineEmits<{
  (e: 'bindSuccess'): void
}>()

// 将 PictureBriefVO 转换为 PictureVO（保持兼容性）
const convertToPictureVO = (list: API.PictureBriefVO[]): API.PictureVO[] => {
  return list.map(item => ({
    ...item,
    socialInfo: {
      likeCount: item.likeCount,
      favoriteCount: item.favoriteCount,
      viewCount: item.viewCount,
      downloadCount: item.downloadCount,
      isLiked: !!item.likeTime,
      isFavorited: !!item.favoriteTime
    }
  } as API.PictureVO))
}

const fetchPendingPictures = async (params: {
  current: number
  pageSize: number
}) => {
  return await pictureControllerQueryPendingPictures({
    current: params.current,
    pageSize: params.pageSize,
  })
}

const { dataList, hasMore, isLoading, fetchData } = useTabContent({
  userId: `${props.userId}-pending`,
  fetchFunction: fetchPendingPictures,
  isVisible: computed(() => props.isVisible ?? false),
})

// ==================== 手机号绑定 ====================

const phoneModalVisible = ref(false)
const phoneForm = reactive({ phone: '', code: '' })
const phoneCountdown = ref(0)
const phoneBindLoading = ref(false)
let phoneTimer: ReturnType<typeof setInterval> | null = null

const { init: initCaptcha, triggerCaptcha, captchaVerifying } = useAliyunCaptcha({
  onCaptchaVerify: async (captchaVerifyParam: string) => {
    return await doSendPhoneCode(captchaVerifyParam)
  }
})

onMounted(() => {
  initCaptcha()
})

const startPhoneCountdown = () => {
  phoneCountdown.value = 60
  phoneTimer = setInterval(() => {
    phoneCountdown.value--
    if (phoneCountdown.value <= 0) {
      clearInterval(phoneTimer!)
      phoneTimer = null
    }
  }, 1000)
}

const doSendPhoneCode = async (captchaVerifyParam: string) => {
  try {
    const res = await noticeControllerSendVerificationCode({
      account: phoneForm.phone,
      captchaVerifyParam,
      type: 1,
    })
    if (res.data.code === 0 && res.data.data) {
      startPhoneCountdown()
      message.success('验证码已发送')
      return true
    } else {
      message.error(res.data.message || '验证码发送失败')
      return false
    }
  } catch {
    message.error('验证码发送失败')
    return false
  }
}

const sendPhoneCode = () => {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    message.warning('请输入正确的手机号')
    return
  }
  triggerCaptcha()
}

const handleBindPhone = async () => {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    message.warning('请输入正确的手机号')
    return
  }
  if (!phoneForm.code) {
    message.warning('请输入验证码')
    return
  }
  phoneBindLoading.value = true
  try {
    const res = await userControllerBindAccount({
      type: 1,
      account: phoneForm.phone,
      verificationCode: phoneForm.code,
    })
    if (res.data.code === 0) {
      message.success('手机号绑定成功')
      phoneModalVisible.value = false
      emit('bindSuccess')
    } else {
      message.error(res.data.message || '绑定失败')
    }
  } catch {
    message.error('绑定失败')
  } finally {
    phoneBindLoading.value = false
  }
}

// 待审核图标组件
const PendingIcon = {
  template: `
    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" stroke-opacity="0.3"/>
      <path d="M12 7V12L15 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-opacity="0.3"/>
    </svg>
  `
}
</script>

<style scoped>
.tab-content {
  min-height: 300px;
}

.bind-phone-banner {
  background: linear-gradient(135deg, #E6F7FF 0%, #BAE7FF 100%);
  border: 1px solid #91D5FF;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 20px;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-icon {
  flex-shrink: 0;
  color: #1677ff;
}

.banner-text {
  flex: 1;
  font-size: 14px;
  color: #0958D9;
  line-height: 1.5;
}

.banner-btn {
  flex-shrink: 0;
  font-weight: 600;
  padding: 0;
}

.bind-form {
  padding-top: 8px;
}

.code-input-row {
  display: flex;
  gap: 12px;
}

.code-input-row .ant-input {
  flex: 1;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: #9CA3AF;
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  color: #9CA3AF;
}

.empty-text {
  font-size: 14px;
  color: #9CA3AF;
}

.scroll-sentinel {
  height: 1px;
  width: 100%;
}
</style>
