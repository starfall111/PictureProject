<template>
  <div class="security-tab">
    <div class="tab-header">
      <span class="tab-title">安全设置</span>
    </div>

    <a-divider style="margin: 16px 0 24px" />

    <a-list :bordered="false">
      <!-- 密码安全 -->
      <a-list-item>
        <a-list-item-meta>
          <template #title>
            <div class="security-item-title">
              <span>登录密码</span>
              <a-tag v-if="hasPassword" color="green">已设置</a-tag>
              <a-tag v-else color="orange">未设置</a-tag>
            </div>
          </template>
          <template #description>
            定期更换密码有助于保护账号安全
          </template>
          <template #avatar>
            <a-avatar :size="40" style="background-color: #e6f4ff">
              <template #icon>
                <LockOutlined style="color: #1677ff" />
              </template>
            </a-avatar>
          </template>
        </a-list-item-meta>
        <template #actions>
          <a-button type="link" @click="handleChangePassword">修改</a-button>
        </template>
      </a-list-item>

      <!-- 手机绑定 -->
      <a-list-item>
        <a-list-item-meta>
          <template #title>
            <div class="security-item-title">
              <span>手机绑定</span>
              <a-tag v-if="userInfo.userPhone" color="green">已绑定</a-tag>
              <a-tag v-else color="orange">未绑定</a-tag>
            </div>
          </template>
          <template #description>
            {{ userInfo.userPhone ? `已绑定手机 ${maskPhone(userInfo.userPhone)}` : '绑定手机号后可用于找回密码' }}
          </template>
          <template #avatar>
            <a-avatar :size="40" style="background-color: #f6ffed">
              <template #icon>
                <PhoneOutlined style="color: #52c41a" />
              </template>
            </a-avatar>
          </template>
        </a-list-item-meta>
        <template #actions>
          <a-button type="link" @click="phoneModalVisible = true">{{ userInfo.userPhone ? '修改' : '绑定' }}</a-button>
        </template>
      </a-list-item>

      <!-- 邮箱绑定 -->
      <a-list-item>
        <a-list-item-meta>
          <template #title>
            <div class="security-item-title">
              <span>邮箱绑定</span>
              <a-tag v-if="userInfo.userEmail" color="green">已绑定</a-tag>
              <a-tag v-else color="orange">未绑定</a-tag>
            </div>
          </template>
          <template #description>
            {{ userInfo.userEmail ? `已绑定邮箱 ${maskEmail(userInfo.userEmail)}` : '绑定邮箱后可用于找回密码' }}
          </template>
          <template #avatar>
            <a-avatar :size="40" style="background-color: #fff7e6">
              <template #icon>
                <MailOutlined style="color: #faad14" />
              </template>
            </a-avatar>
          </template>
        </a-list-item-meta>
        <template #actions>
          <a-button type="link" @click="emailModalVisible = true">{{ userInfo.userEmail ? '修改' : '绑定' }}</a-button>
        </template>
      </a-list-item>
    </a-list>

    <!-- 修改密码弹窗（预留） -->
    <a-modal v-model:open="passwordModalVisible" title="修改密码" :footer="null" :destroy-on-close="true" width="440px">
      <a-result status="info" title="功能开发中" sub-title="密码修改功能即将上线，敬请期待">
        <template #extra>
          <a-button type="primary" @click="passwordModalVisible = false">知道了</a-button>
        </template>
      </a-result>
    </a-modal>

    <!-- 手机绑定弹窗 -->
    <a-modal v-model:open="phoneModalVisible" :title="userInfo.userPhone ? '修改手机号' : '绑定手机号'" :footer="null"
      :destroy-on-close="true" width="440px">
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

    <!-- 邮箱绑定弹窗 -->
    <a-modal v-model:open="emailModalVisible" :title="userInfo.userEmail ? '修改邮箱' : '绑定邮箱'" :footer="null"
      :destroy-on-close="true" width="440px">
      <a-form layout="vertical" class="bind-form">
        <a-form-item label="邮箱" required>
          <a-input v-model:value="emailForm.email" placeholder="请输入邮箱地址" size="large" />
        </a-form-item>
        <a-form-item label="验证码" required>
          <div class="code-input-row">
            <a-input v-model:value="emailForm.code" placeholder="请输入验证码" size="large" :maxlength="6" />
            <a-button size="large" :disabled="emailCountdown > 0" :loading="captchaVerifying" @click="sendEmailCode">
              {{ emailCountdown > 0 ? `${emailCountdown}s 后重发` : '获取验证码' }}
            </a-button>
          </div>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="large" block :loading="emailBindLoading" @click="handleBindEmail">
            确认绑定
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { LockOutlined, PhoneOutlined, MailOutlined } from '@ant-design/icons-vue';
import { bindAccountUsingPost } from '@/api/userController';
import { sendVerificationCodeUsingPost } from '@/api/noticeController';
import { useAliyunCaptcha } from '@/access/useAliyunCaptcha';

interface Props {
  userInfo: API.LoginUserVO;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'bind-success'): void;
}>();

const passwordModalVisible = ref(false);
const phoneModalVisible = ref(false);
const emailModalVisible = ref(false);

// ==================== 阿里云验证码 ====================

type BindingType = 'phone' | 'email' | null;
const currentBindingType = ref<BindingType>(null);

const { init: initCaptcha, triggerCaptcha, captchaVerifying } = useAliyunCaptcha({
  onCaptchaVerify: async (captchaVerifyParam: string) => {
    if (currentBindingType.value === 'phone') {
      return await doSendPhoneCode(captchaVerifyParam);
    } else if (currentBindingType.value === 'email') {
      return await doSendEmailCode(captchaVerifyParam);
    }
    return false;
  }
});

onMounted(() => {
  initCaptcha();
});

// ==================== 手机绑定 ====================

const phoneForm = reactive({ phone: '', code: '' });
const phoneCountdown = ref(0);
const phoneBindLoading = ref(false);
let phoneTimer: ReturnType<typeof setInterval> | null = null;

const startPhoneCountdown = () => {
  phoneCountdown.value = 60;
  phoneTimer = setInterval(() => {
    phoneCountdown.value--;
    if (phoneCountdown.value <= 0) {
      clearInterval(phoneTimer!);
      phoneTimer = null;
    }
  }, 1000);
};

const doSendPhoneCode = async (captchaVerifyParam: string) => {
  try {
    const res = await sendVerificationCodeUsingPost({
      account: phoneForm.phone,
      captchaVerifyParam,
      type: 1,
    });
    if (res.data.code === 0 && res.data.data) {
      startPhoneCountdown();
      message.success('验证码已发送');
      return true;
    } else {
      message.error(res.data.message || '验证码发送失败');
      return false;
    }
  } catch {
    message.error('验证码发送失败');
    return false;
  }
};

const sendPhoneCode = () => {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    message.warning('请输入正确的手机号');
    return;
  }
  currentBindingType.value = 'phone';
  triggerCaptcha();
};

const handleBindPhone = async () => {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    message.warning('请输入正确的手机号');
    return;
  }
  if (!phoneForm.code) {
    message.warning('请输入验证码');
    return;
  }
  phoneBindLoading.value = true;
  try {
    const res = await bindAccountUsingPost({
      type: 1,
      account: phoneForm.phone,
      verificationCode: phoneForm.code,
    });
    if (res.data.code === 0) {
      message.success('手机号绑定成功');
      phoneModalVisible.value = false;
      emit('bind-success');
    } else {
      message.error(res.data.message || '绑定失败');
    }
  } catch {
    message.error('绑定失败');
  } finally {
    phoneBindLoading.value = false;
  }
};

// ==================== 邮箱绑定 ====================

const emailForm = reactive({ email: '', code: '' });
const emailCountdown = ref(0);
const emailBindLoading = ref(false);
let emailTimer: ReturnType<typeof setInterval> | null = null;

const startEmailCountdown = () => {
  emailCountdown.value = 60;
  emailTimer = setInterval(() => {
    emailCountdown.value--;
    if (emailCountdown.value <= 0) {
      clearInterval(emailTimer!);
      emailTimer = null;
    }
  }, 1000);
};

const doSendEmailCode = async (captchaVerifyParam: string) => {
  try {
    const res = await sendVerificationCodeUsingPost({
      account: emailForm.email,
      captchaVerifyParam,
      type: 2,
    });
    if (res.data.code === 0 && res.data.data) {
      startEmailCountdown();
      message.success('验证码已发送');
      return true;
    } else {
      message.error(res.data.message || '验证码发送失败');
      return false;
    }
  } catch {
    message.error('验证码发送失败');
    return false;
  }
};

const sendEmailCode = () => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.email)) {
    message.warning('请输入正确的邮箱地址');
    return;
  }
  currentBindingType.value = 'email';
  triggerCaptcha();
};

const handleBindEmail = async () => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.email)) {
    message.warning('请输入正确的邮箱地址');
    return;
  }
  if (!emailForm.code) {
    message.warning('请输入验证码');
    return;
  }
  emailBindLoading.value = true;
  try {
    const res = await bindAccountUsingPost({
      type: 1,
      account: phoneForm.phone,
      verificationCode: phoneForm.code,
    });
    if (res.data.code === 0) {
      message.success('邮箱绑定成功');
      emailModalVisible.value = false;
      emit('bind-success');
    } else {
      message.error(res.data.message || '绑定失败');
    }
  } catch {
    message.error('绑定失败');
  } finally {
    emailBindLoading.value = false;
  }
};

// ==================== 通用 ====================

/** 是否设置了密码（有账号即认为有密码） */
const hasPassword = computed(() => !!props.userInfo.userAccount);

/** 手机号脱敏 */
const maskPhone = (phone: string) => {
  if (phone.length >= 11) {
    return phone.slice(0, 3) + '****' + phone.slice(7);
  }
  return phone;
};

/** 邮箱脱敏 */
const maskEmail = (email: string) => {
  const atIndex = email.indexOf('@');
  if (atIndex > 1) {
    return email.slice(0, 2) + '***' + email.slice(atIndex);
  }
  return email;
};

/** 修改密码 */
const handleChangePassword = () => {
  passwordModalVisible.value = true;
};
</script>

<style scoped>
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tab-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.security-item-title {
  display: flex;
  align-items: center;
  gap: 8px;
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
</style>
