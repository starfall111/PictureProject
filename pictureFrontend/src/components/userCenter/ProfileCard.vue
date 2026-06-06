<template>
  <a-card :bordered="false" class="profile-card">
    <!-- 头像上传 -->
    <div class="avatar-wrapper">
      <a-upload
        name="avatar"
        :show-upload-list="false"
        :before-upload="beforeAvatarUpload"
        accept="image/*"
      >
        <div class="avatar-upload-wrapper">
          <a-avatar :size="120" :src="userInfo.userAvatar" class="profile-avatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div class="avatar-overlay">
            <CameraOutlined style="font-size: 24px; color: #fff" />
            <span class="avatar-overlay-text">更换头像</span>
          </div>
        </div>
      </a-upload>
    </div>

    <!-- 用户名 -->
    <h2 class="user-name">{{ userInfo.userName || '未设置用户名' }}</h2>

    <!-- 账号 -->
    <span class="user-account">@{{ userInfo.userAccount || '--' }}</span>

    <!-- 角色标签 -->
    <div class="role-tag-wrapper">
      <a-tag v-if="userInfo.userRole === 'admin'" color="green">管理员</a-tag>
      <a-tag v-else color="blue">普通用户</a-tag>
    </div>

    <!-- 注册时间 -->
    <div class="register-time">
      <CalendarOutlined style="margin-right: 6px" />
      <span>{{ formatDate(userInfo.createTime) }}</span>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue';
import { UserOutlined, CameraOutlined, CalendarOutlined } from '@ant-design/icons-vue';
import { userControllerUpdateUser, userControllerUploadAvatar } from '@/api/userController';
import { userLoginUserStore } from '@/stores/user';

interface Props {
  userInfo: API.LoginUserVO;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'avatar-updated', avatarUrl: string): void;
}>();
const loginUserStore = userLoginUserStore();

/** 格式化日期 */
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '--';
  const d = new Date(dateStr);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

/** 头像上传前校验与处理 */
const beforeAvatarUpload = async (file: File) => {
  const isImage = file.type.startsWith('image/');
  if (!isImage) {
    message.error('只能上传图片文件');
    return false;
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB');
    return false;
  }

  const reader = new FileReader();
  reader.onload = async (e) => {
    const avatarUrl = e.target?.result as string;
    try {
      const res = await userControllerUploadAvatar({
        file,
      });
      if (res.data.code === 0) {
        message.success('头像更新成功');
        emit('avatar-updated', avatarUrl);
      } else {
        message.error(res.data.message || '头像更新失败');
      }
    } catch {
      message.error('头像更新失败');
    }
  };
  reader.readAsDataURL(file);
  return false;
};
</script>

<style scoped>
.profile-card {
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  transition: box-shadow 0.3s ease;
}

.profile-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.avatar-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.avatar-upload-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.profile-avatar {
  border: 3px solid #e6f4ff;
  transition: border-color 0.3s ease;
}

.avatar-upload-wrapper:hover .profile-avatar {
  border-color: #1677ff;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-upload-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay-text {
  color: #fff;
  font-size: 13px;
  font-weight: 500;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0 0 4px;
  line-height: 1.35;
}

.user-account {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  display: block;
  margin-bottom: 12px;
}

.role-tag-wrapper {
  margin-bottom: 16px;
}

.register-time {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 767px) {
  .profile-avatar {
    width: 80px !important;
    height: 80px !important;
  }
}
</style>
