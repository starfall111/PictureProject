<template>
  <div id="userCenterPage">
    <div class="user-center-container">
      <!-- 左侧区域 -->
      <div class="left-panel">
        <!-- 个人资料卡片 -->
        <ProfileCard
          :userInfo="loginUserStore.loginUser"
          @avatar-updated="handleAvatarUpdated"
        />

        <!-- 统计卡片 -->
        <StatsCard
          :uploadCount="userStats.uploadCount ?? 0"
          :likeCount="userStats.totalLikes ?? 0"
          :favoriteCount="userStats.totalFavorites ?? 0"
          :viewCount="userStats.totalViews ?? 0"
          :shareCount="userStats.totalShares ?? 0"
          :downloadCount="userStats.totalDownloads ?? 0"
        />

        <!-- 退出登录 -->
        <a-popconfirm
          title="确定要退出登录吗？"
          ok-text="确定"
          cancel-text="取消"
          @confirm="handleLogout"
        >
          <a-button type="primary" danger block size="large">
            <template #icon><LogoutOutlined /></template>
            退出登录
          </a-button>
        </a-popconfirm>
      </div>

      <!-- 右侧区域 -->
      <div class="right-panel">
        <a-card :bordered="false" class="main-card">
          <a-tabs v-model:activeKey="activeTab" size="large">
            <a-tab-pane key="info" tab="基本信息">
              <UserInfoTab
                :userInfo="loginUserStore.loginUser"
                @update-success="handleUpdateSuccess"
                @switch-security="activeTab = 'security'"
              />
            </a-tab-pane>
            <a-tab-pane key="security" tab="安全设置">
              <SecurityTab :userInfo="loginUserStore.loginUser" @bind-success="handleUpdateSuccess" />
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import { LogoutOutlined } from '@ant-design/icons-vue';
import { useRouter } from 'vue-router';
import { userLoginUserStore } from '@/stores/user';
import { userControllerLogOut, userControllerGetUserProfile } from '@/api/userController';
import ProfileCard from '@/components/userCenter/ProfileCard.vue';
import StatsCard from '@/components/userCenter/StatsCard.vue';
import UserInfoTab from '@/components/userCenter/UserInfoTab.vue';
import SecurityTab from '@/components/userCenter/SecurityTab.vue';

const router = useRouter();
const loginUserStore = userLoginUserStore();
const activeTab = ref('info');

// 用户统计数据
const userStats = ref<API.UserProfileVO>({
  uploadCount: 0,
  totalLikes: 0,
  totalFavorites: 0,
  totalViews: 0,
  totalShares: 0,
  totalDownloads: 0,
});

// 获取用户统计数据
const fetchUserStats = async () => {
  const userId = loginUserStore.loginUser.id;
  if (!userId) return;

  try {
    const res = await userControllerGetUserProfile({ id: userId });
    if (res.data.code === 0 && res.data.data) {
      userStats.value = res.data.data;
    }
  } catch {
    // 静默失败
  }
};

/** 页面加载时刷新用户数据 */
onMounted(async () => {
  if (!loginUserStore.loginUser.id) {
    router.replace('/user/login');
    return;
  }
  await loginUserStore.getLoginUser();
  await fetchUserStats();
});

/** 头像更新回调 */
const handleAvatarUpdated = (avatarUrl: string) => {
  loginUserStore.loginUser.userAvatar = avatarUrl;
};

/** 信息更新成功回调 */
const handleUpdateSuccess = async () => {
  await loginUserStore.getLoginUser();
  await fetchUserStats();
};

/** 退出登录 */
const handleLogout = async () => {
  try {
    const res = await userControllerLogOut();
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser({});
      message.success('退出登录成功');
      router.push('/user/login');
    } else {
      message.error('退出登录失败');
    }
  } catch {
    message.error('退出登录失败');
  }
};
</script>

<style scoped>
#userCenterPage {
  min-height: calc(100vh - 120px);
  padding: 24px;
}

.user-center-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧面板 */
.left-panel {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  min-width: 0;
}

.main-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

/* 响应式：平板及以下 */
@media (max-width: 991px) {
  .user-center-container {
    flex-direction: column;
  }

  .left-panel {
    width: 100%;
  }
}

/* 响应式：手机 */
@media (max-width: 767px) {
  #userCenterPage {
    padding: 12px;
  }

  .user-center-container {
    gap: 12px;
  }
}
</style>
