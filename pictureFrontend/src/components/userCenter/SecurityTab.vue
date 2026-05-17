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
              <template #icon><LockOutlined style="color: #1677ff" /></template>
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
              <template #icon><PhoneOutlined style="color: #52c41a" /></template>
            </a-avatar>
          </template>
        </a-list-item-meta>
        <template #actions>
          <a-button type="link">{{ userInfo.userPhone ? '修改' : '绑定' }}</a-button>
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
              <template #icon><MailOutlined style="color: #faad14" /></template>
            </a-avatar>
          </template>
        </a-list-item-meta>
        <template #actions>
          <a-button type="link">{{ userInfo.userEmail ? '修改' : '绑定' }}</a-button>
        </template>
      </a-list-item>
    </a-list>

    <!-- 修改密码弹窗（预留） -->
    <a-modal
      v-model:open="passwordModalVisible"
      title="修改密码"
      :footer="null"
      :destroy-on-close="true"
      width="440px"
    >
      <a-result
        status="info"
        title="功能开发中"
        sub-title="密码修改功能即将上线，敬请期待"
      >
        <template #extra>
          <a-button type="primary" @click="passwordModalVisible = false">知道了</a-button>
        </template>
      </a-result>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { LockOutlined, PhoneOutlined, MailOutlined } from '@ant-design/icons-vue';

interface Props {
  userInfo: API.LoginUserVO;
}

const props = defineProps<Props>();

const passwordModalVisible = ref(false);

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
</style>
