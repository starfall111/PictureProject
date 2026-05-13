import { ref } from 'vue'
import { message } from 'ant-design-vue'

// ===== 类型定义 =====

interface CaptchaVerifyResult {
  captchaResult: boolean
  bizResult: boolean
}

interface AliyunCaptchaOptions {
  onCaptchaVerify?: (captchaVerifyParam: string) => Promise<boolean>
  sceneId?: string
}

interface AliyunCaptchaConfig {
  SceneId: string
  mode: 'popup'
  element: string
  button: string
  captchaVerifyCallback: (captchaVerifyParam: string) => Promise<CaptchaVerifyResult>
  onBizResultCallback: (bizResult: boolean) => void
  getInstance: (instance: unknown) => void
  slideStyle: { width: number; height: number }
  language: 'cn'
}

// 扩展 Window 接口以支持阿里云验证码 SDK
declare global {
  interface Window {
    initAliyunCaptcha?: (config: AliyunCaptchaConfig) => unknown
  }
}

// ===== 全局单例状态 =====
let captchaInstance: unknown = null
let sdkInitialized = false

let currentVerifyCallback: ((captchaVerifyParam: string) => Promise<boolean>) | null = null

let hiddenContainer: HTMLDivElement | null = null
let hiddenButton: HTMLButtonElement | null = null

type VerifyCallback = (captchaVerifyParam: string) => Promise<boolean>

/**
 * 阿里云图形验证码 2.0 composable
 */
export function useAliyunCaptcha(options: AliyunCaptchaOptions = {}) {
  const { onCaptchaVerify, sceneId } = options

  const captchaVerifying = ref(false)
  const sdkReady = ref(sdkInitialized)

  function waitForSDK(timeout = 5000): Promise<void> {
    return new Promise((resolve, reject) => {
      if (window.initAliyunCaptcha) {
        resolve()
        return
      }

      const startTime = Date.now()
      const timer = setInterval(() => {
        if (window.initAliyunCaptcha) {
          clearInterval(timer)
          resolve()
        } else if (Date.now() - startTime > timeout) {
          clearInterval(timer)
          reject(new Error('SDK 加载超时'))
        }
      }, 100)
    })
  }

  function ensureHiddenDOM() {
    if (hiddenContainer && hiddenButton) return

    hiddenContainer = document.createElement('div')
    hiddenContainer.id = 'aliyun-captcha-element'
    hiddenContainer.style.cssText = 'position:fixed;top:-9999px;left:-9999px;opacity:0;'
    document.body.appendChild(hiddenContainer)

    hiddenButton = document.createElement('button')
    hiddenButton.id = 'aliyun-captcha-button'
    hiddenButton.style.cssText = 'width:1px;height:1px;border:none;padding:0;opacity:0;'
    hiddenContainer.appendChild(hiddenButton)
  }

  async function init() {
    if (onCaptchaVerify) {
      currentVerifyCallback = onCaptchaVerify
    }

    if (sdkInitialized) {
      sdkReady.value = true
      return
    }

    try {
      await waitForSDK()
      ensureHiddenDOM()

      captchaInstance = window.initAliyunCaptcha!({
        SceneId: sceneId || import.meta.env.VITE_ALIYUN_CAPTCHA_SCENE_ID,
        mode: 'popup',
        element: '#aliyun-captcha-element',
        button: '#aliyun-captcha-button',
        captchaVerifyCallback: async (captchaVerifyParam: string): Promise<CaptchaVerifyResult> => {
          if (!currentVerifyCallback) {
            return { captchaResult: false, bizResult: false }
          }

          captchaVerifying.value = true
          try {
            const bizResult = await currentVerifyCallback(captchaVerifyParam)
            return { captchaResult: true, bizResult: !!bizResult }
          } catch {
            return { captchaResult: true, bizResult: false }
          } finally {
            captchaVerifying.value = false
          }
        },
        onBizResultCallback: () => {},
        getInstance: (instance: unknown) => {
          captchaInstance = instance
        },
        slideStyle: { width: 360, height: 40 },
        language: 'cn',
      })

      sdkInitialized = true
      sdkReady.value = true
    } catch (e) {
      console.warn('[AliyunCaptcha] 初始化失败:', e)
      sdkReady.value = false
    }
  }

  function triggerCaptcha() {
    if (!sdkReady.value || !captchaInstance) {
      message.error('验证码加载失败，请刷新页面重试')
      return
    }

    if (onCaptchaVerify) {
      currentVerifyCallback = onCaptchaVerify
    }

    hiddenButton?.click()
  }

  return { init, triggerCaptcha, captchaVerifying, sdkReady }
}
