<template>
  <a-modal
    :open="open"
    title="修改用户信息"
    :width="560"
    :height="80"
    :footer="null"
    :destroyOnClose="true"
    @cancel="handleCancel"
    style="top: 0;"
  >
    <a-form
      ref="editFormRef"
      :model="editForm"
      :rules="editRules"
      layout="vertical"
      class="edit-form"
    >
      <!-- 头像上传区域 -->
      <div class="avatar-section">
        <a-upload
          name="avatar"
          :show-upload-list="false"
          :before-upload="beforeAvatarUpload"
          accept="image/*"
        >
          <div class="avatar-upload-wrapper">
            <a-avatar :size="96" :src="editForm.userAvatar" class="edit-avatar">
              <template #icon>
                <UserOutlined />
              </template>
            </a-avatar>
            <div class="avatar-upload-overlay">
              <span>更换头像</span>
            </div>
          </div>
        </a-upload>
      </div>

            <!-- 只读字段 -->
      <div class="readonly-section">
        <div class="readonly-field">
          <span class="readonly-label">账号</span>
          <span class="readonly-value">{{ editForm.userAccount }}</span>
        </div>
      </div>

      <a-divider style="margin: 16px 0 24px" />

      <!-- 可编辑字段 -->
      <a-form-item label="用户名" name="userName">
        <a-input v-model:value="editForm.userName" placeholder="请输入用户名" size="large" />
      </a-form-item>

      <a-form-item label="手机号" name="userPhone">
        <a-input v-model:value="editForm.userPhone" placeholder="请输入手机号" size="large" />
      </a-form-item>

      <a-form-item label="邮箱" name="userEmail">
        <a-input v-model:value="editForm.userEmail" placeholder="请输入邮箱" size="large" />
      </a-form-item>

      <a-form-item label="昵称" name="userNickname">
        <a-input v-model:value="editForm.userNickname" placeholder="请输入昵称" size="large" />
      </a-form-item>

      <a-form-item label="角色" name="userRole">
        <a-select v-model:value="editForm.userRole" placeholder="请选择角色" size="large">
          <a-select-option value="admin">管理员</a-select-option>
          <a-select-option value="user">普通用户</a-select-option>
        </a-select>
      </a-form-item>

      <!-- 底部按钮 -->
      <div class="form-footer">
        <a-button @click="handleCancel" size="large">取消</a-button>
        <a-button type="primary" size="large" :loading="editLoading" @click="handleOk">
          保存修改
        </a-button>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { UserOutlined } from '@ant-design/icons-vue';
import { updateUserUsingPost, updateUserUsingPost1 } from '@/api/userController';

interface EditFormState {
  id: number | undefined;
  userName: string | undefined;
  userPhone: string | undefined;
  userEmail: string | undefined;
  userNickname: string | undefined;
  userAvatar: string | undefined;
  userProfile: string | undefined;
  userAccount: string | undefined;
  userRole: string | undefined;
}

const props = defineProps<{
  open: boolean;
  record: API.UserVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'success'): void;
}>();

const editFormRef = ref();
const editLoading = ref(false);

const editForm = reactive<EditFormState>({
  id: undefined,
  userName: '',
  userPhone: '',
  userEmail: '',
  userNickname: '',
  userAvatar: '',
  userProfile: '',
  userAccount: '',
  userRole: '',
});

// 弹窗打开时填充数据
watch(
  () => props.open,
  (val) => {
    if (val && props.record) {
      editForm.id = props.record.id;
      editForm.userName = props.record.userName ?? '';
      editForm.userPhone = props.record.userPhone ?? '';
      editForm.userEmail = props.record.userEmail ?? '';
      editForm.userNickname = props.record.userName ?? '';
      editForm.userAvatar = props.record.userAvatar ?? '';
      editForm.userProfile = props.record.userProfile ?? '';
      editForm.userAccount = props.record.userAccount ?? '';
      editForm.userRole = props.record.userRole ?? '';
    }
  },
);

// 校验规则
const validatePhone = (_rule: any, value: string) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    return Promise.reject('请输入正确的手机号格式');
  }
  return Promise.resolve();
};

const validateEmail = (_rule: any, value: string) => {
  if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    return Promise.reject('请输入正确的邮箱格式');
  }
  return Promise.resolve();
};

const editRules = {
  userName: [
    { required: true, message: '请输入用户名' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符' },
  ],
  userPhone: [{ validator: validatePhone }],
  userEmail: [{ validator: validateEmail }],
  userNickname: [{ max: 30, message: '昵称最长 30 个字符' }],
  userProfile: [{ max: 256, message: '简介最长 256 个字符' }],
  userRole: [{ required: true, message: '请选择角色' }],
};

/** 头像上传前校验 */
const beforeAvatarUpload = (file: File) => {
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
  reader.onload = (e) => {
    editForm.userAvatar = e.target?.result as string;
  };
  reader.readAsDataURL(file);
  return false;
};

/** 保存修改 */
const handleOk = async () => {
  try {
    await editFormRef.value?.validateFields();
  } catch {
    return;
  }
  editLoading.value = true;
  try {
    // TODO: 调用后端更新用户信息接口
    const res = await updateUserUsingPost({
      id: editForm.id,
      userAvatar: editForm.userAvatar,
      userEmail: editForm.userEmail,
      userName: editForm.userName,
      userPhone: editForm.userPhone,
      userProfile: editForm.userProfile,
      userRole: editForm.userRole,
    });
    console.log('提交的用户数据:', { ...editForm });
    message.success('修改成功');
    emit('update:open', false);
    emit('success');
  } catch {
    message.error('修改失败');
  } finally {
    editLoading.value = false;
  }
};

/** 取消 */
const handleCancel = () => {
  emit('update:open', false);
};
</script>

<style scoped>
.avatar-section {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.avatar-upload-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}

.edit-avatar {
  border: 3px solid #2563eb;
  cursor: pointer;
}

.avatar-upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-upload-wrapper:hover .avatar-upload-overlay {
  opacity: 1;
}

.avatar-upload-overlay span {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}

.edit-form {
  padding-top: 8px;
}

.readonly-section {
  display: flex;
  gap: 24px;
  padding: 16px;
  margin-bottom: 24px;
  background-color: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.readonly-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.readonly-label {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}

.readonly-value {
  font-size: 16px;
  color: rgba(0, 0, 0, 0.65);
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
