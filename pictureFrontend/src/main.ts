
// 扩展 Window 接口以支持阿里云验证码配置
declare global {
  interface Window {
    AliyunCaptchaConfig: {
      region: string;
      prefix: string;
    };
  }
}

// 阿里云验证码全局配置（必须在 SDK 加载前设置）
window.AliyunCaptchaConfig = {
  region: 'cn',
  prefix: import.meta.env.VITE_ALIYUN_CAPTCHA_PREFIX || ''
}

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import "@/access";

// Layout scheme system
import '@/styles/base.css'
import '@/styles/schemes.css'
import '@/layouts/scheme-1-immersive'
import { useLayoutScheme } from '@/composables/useLayoutScheme'

const app = createApp(App)

// Initialize scheme system (sets data-scheme on <html>)
const { initialize } = useLayoutScheme()
initialize()

app.use(Antd);
app.use(createPinia())
app.use(router)

app.mount('#app')
