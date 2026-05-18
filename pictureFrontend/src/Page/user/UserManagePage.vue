<template>
  <div id="userManagePage">
    <div class="search">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="账号">
          <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="searchParams.userPhone" placeholder="输入手机号" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="searchParams.userEmail" placeholder="输入邮箱" />
        </a-form-item>
        <a-form-item>
          <a-radio-group v-model:value="searchParams.userRole" size="large">
            <a-radio-button value="admin">管理员</a-radio-button>
            <a-radio-button value="user">普通用户</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="doClear">重置</a-button>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="openAddModal">新增用户</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="table">
      <a-table :columns="columns" :data-source="dataList" :pagination="pagination" @change="doTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-image :src="record.userAvatar" />
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <div v-if="record.userRole === 'admin'">
              <a-tag color="green">管理员</a-tag>
            </div>
            <div v-else>
              <a-tag color="blue">用户</a-tag>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <div>{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="primary" @click="openEditModal(record.id)">编辑</a-button>
            <a-button type="danger" @click="deleteUser(record.id)">删除</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 修改用户信息弹窗 -->
    <UserEditModal v-model:open="editModalVisible" :record="currentRecord" @success="fetchData" />

    <!-- 新增用户弹窗 -->
    <a-modal
      v-model:open="addModalVisible"
      title="新增用户"
      @ok="handleAddOk"
      @cancel="addModalVisible = false"
    >
      <a-form :model="addForm" layout="vertical">
        <a-form-item label="账号" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="addForm.userAccount" placeholder="请输入账号" />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="addForm.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像链接">
          <a-input v-model:value="addForm.userAvatar" placeholder="请输入头像链接" />
        </a-form-item>
        <a-form-item label="简介">
          <a-input v-model:value="addForm.userProfile" placeholder="请输入简介" />
        </a-form-item>
        <a-form-item label="角色" :rules="[{ required: true, message: '请选择角色' }]">
          <a-select v-model:value="addForm.userRole" placeholder="请选择角色">
            <a-select-option value="admin">管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { deleteUserUsingDelete, getUserInfoUsingGet, listUserVoByQueryUsingPost, addUserUsingPost } from '@/api/userController';
import UserEditModal from '@/components/UserEditModal.vue';

// ==================== 列表相关 ====================

const dataList = ref<API.UserVO[]>([]);
const total = ref(0);
const searchParams = reactive<API.UserQueryDTO>({
  current: 1,
  pageSize: 10,
});

const columns = [
  { title: 'id', dataIndex: 'id', key: 'id', align: 'center' },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount', align: 'center' },
  { title: '手机号', dataIndex: 'userPhone', key: 'userPhone', align: 'center' },
  { title: '邮箱', key: 'userEmail', dataIndex: 'userEmail', align: 'center' },
  { title: '昵称', key: 'userNickname', dataIndex: 'userNickname', align: 'center' },
  { title: '头像', key: 'userAvatar', dataIndex: 'userAvatar', align: 'center' },
  { title: '角色', key: 'userRole', dataIndex: 'userRole', align: 'center' },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', align: 'center' },
  { title: '操作', key: 'action' },
];

const fetchData = async () => {
  const res = await listUserVoByQueryUsingPost({ ...searchParams });
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? [];
    total.value = res.data.data.total ?? 0;
  } else {
    message.error('获取用户数据失败');
    dataList.value = [];
    total.value = 0;
  }
};

const deleteUser = async (id: number) => {
  const res = await deleteUserUsingDelete({ id });
  if (res.data.code === 0) {
    message.success('删除成功');
    fetchData();
  } else {
    message.error('删除失败');
  }
};

const doSearch = () => {
  fetchData();
};

const doClear = () => {
  const keys = Object.keys(searchParams) as Array<keyof API.UserQueryDTO>;
  keys.forEach((key) => {
    if (key === 'current') {
      searchParams[key] = 1;
    } else if (key === 'pageSize') {
      searchParams[key] = 10;
    } else {
      (searchParams as any)[key] = undefined;
    }
  });
  fetchData();
};

const pagination = computed(() => ({
  current: searchParams.current ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: total.value,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
}));

const doTableChange = (page: any) => {
  searchParams.current = page.current;
  searchParams.pageSize = page.pageSize;
  fetchData();
};

// ==================== 编辑弹窗相关 ====================

const editModalVisible = ref(false);
const currentRecord = ref<API.UserVO | null>(null);

const openEditModal = async (id: number) => {
  try {
    const res = await getUserInfoUsingGet({ id });
    if (res.data.code === 0 && res.data.data) {
      currentRecord.value = res.data.data;
      editModalVisible.value = true;
    } else {
      message.error(res.data.message || '获取用户信息失败');
    }
  } catch {
    message.error('获取用户信息失败');
  }
};

// ==================== 新增用户弹窗 ====================

const addModalVisible = ref(false);
const addForm = reactive<{ userAccount?: string; userName?: string; userAvatar?: string; userProfile?: string; userRole?: string }>({});

const openAddModal = () => {
  addForm.userAccount = undefined;
  addForm.userName = undefined;
  addForm.userAvatar = undefined;
  addForm.userProfile = undefined;
  addForm.userRole = undefined;
  addModalVisible.value = true;
};

const handleAddOk = async () => {
  if (!addForm.userAccount?.trim()) {
    message.warning('请输入账号');
    return;
  }
  try {
    const res = await addUserUsingPost({ ...addForm });
    if (res.data.code === 0) {
      message.success('新增成功');
      addModalVisible.value = false;
      fetchData();
    } else {
      message.error(res.data.message || '新增失败');
    }
  } catch {
    message.error('新增失败');
  }
};

// ==================== 初始化 ====================

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.search {
  margin: 20px;
}

.table {
  margin: 20px;
}
</style>
