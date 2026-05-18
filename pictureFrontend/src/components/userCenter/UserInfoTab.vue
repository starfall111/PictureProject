<template>
  <div class="user-info-tab">
    <!-- 顶部操作栏 -->
    <div class="tab-header">
      <span class="tab-title">基本资料</span>
      <a-button
        v-if="!isEditing"
        type="primary"
        @click="enterEditing"
      >
        <template #icon><EditOutlined /></template>
        编辑资料
      </a-button>
      <a-space v-else>
        <a-button @click="cancelEditing">取消</a-button>
        <a-button type="primary" :loading="saveLoading" @click="handleSave">
          保存修改
        </a-button>
      </a-space>
    </div>

    <a-divider style="margin: 16px 0 24px" />

    <!-- 展示模式 -->
    <div v-if="!isEditing" class="display-mode">
      <a-descriptions :column="{ xs: 1, sm: 1, md: 2 }" bordered>
        <a-descriptions-item label="账号">
          {{ userInfo.userAccount || '--' }}
        </a-descriptions-item>
        <a-descriptions-item label="角色">
          <a-tag v-if="userInfo.userRole === 'admin'" color="green">管理员</a-tag>
          <a-tag v-else color="blue">普通用户</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="用户名">
          {{ userInfo.userName || '--' }}
        </a-descriptions-item>
        <a-descriptions-item label="手机号">
          {{ userInfo.userPhone || '未绑定' }}
        </a-descriptions-item>
        <a-descriptions-item label="邮箱">
          {{ userInfo.userEmail || '未绑定' }}
        </a-descriptions-item>
        <a-descriptions-item label="简介">
          {{ userInfo.userProfile || '暂无简介' }}
        </a-descriptions-item>
      </a-descriptions>
    </div>

    <!-- 编辑模式 -->
    <div v-else class="edit-mode">
      <a-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        layout="vertical"
        class="edit-form"
      >
        <a-row :gutter="24">
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item label="账号">
              <a-input :value="userInfo.userAccount" disabled size="large" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item label="用户名" name="userName">
              <a-input v-model:value="editForm.userName" placeholder="请输入用户名" size="large" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="24">
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item label="手机号">
              <div class="readonly-field-row">
                <a-input :value="editForm.userPhone || '未绑定'" disabled size="large" />
                <a-button type="link" size="large" @click="goToSecurity">
                  {{ editForm.userPhone ? '去修改' : '去绑定' }}
                </a-button>
              </div>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item label="邮箱">
              <div class="readonly-field-row">
                <a-input :value="editForm.userEmail || '未绑定'" disabled size="large" />
                <a-button type="link" size="large" @click="goToSecurity">
                  {{ editForm.userEmail ? '去修改' : '去绑定' }}
                </a-button>
              </div>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="个人简介" name="userProfile">
          <a-textarea
            v-model:value="editForm.userProfile"
            placeholder="介绍一下自己吧"
            :auto-size="{ minRows: 3, maxRows: 5 }"
            size="large"
            :maxlength="256"
            show-count
          />
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { EditOutlined } from '@ant-design/icons-vue';
import { updateUserUsingPost1 } from '@/api/userController';
import { userLoginUserStore } from '@/stores/user';

interface Props {
  userInfo: API.LoginUserVO;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update-success'): void;
  (e: 'switch-security'): void;
}>();

const loginUserStore = userLoginUserStore();
const isEditing = ref(false);
const saveLoading = ref(false);
const editFormRef = ref();

/** 编辑表单数据 */
const editForm = reactive({
  userName: '',
  userPhone: '',
  userEmail: '',
  userProfile: '',
});

/** 校验规则 */
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
  userProfile: [{ max: 256, message: '简介最长 256 个字符' }],
};

/** 进入编辑模式 */
const enterEditing = () => {
  editForm.userName = props.userInfo.userName ?? '';
  editForm.userPhone = props.userInfo.userPhone ?? '';
  editForm.userEmail = props.userInfo.userEmail ?? '';
  editForm.userProfile = props.userInfo.userProfile ?? '';
  isEditing.value = true;
};

/** 取消编辑 */
const cancelEditing = () => {
  isEditing.value = false;
};

/** 跳转到安全设置 */
const goToSecurity = () => {
  emit('switch-security');
};

/** 保存修改 */
const handleSave = async () => {
  try {
    await editFormRef.value?.validateFields();
  } catch {
    return;
  }

  saveLoading.value = true;
  try {
    const res = await updateUserUsingPost1({
      // id: loginUserStore.loginUser.id,
      userName: editForm.userName,
      userPhone: editForm.userPhone,
      userEmail: editForm.userEmail,
      userProfile: editForm.userProfile,
    });
    if (res.data.code === 0) {
      message.success('修改成功');
      isEditing.value = false;
      emit('update-success');
    } else {
      message.error(res.data.message || '修改失败');
    }
  } catch {
    message.error('修改失败');
  } finally {
    saveLoading.value = false;
  }
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

.edit-form {
  max-width: 720px;
}

.readonly-field-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.readonly-field-row .ant-input {
  flex: 1;
}
</style>
