<template>
  <div id="systemMessageManagePage">
    <div class="search">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="标题">
          <a-input v-model:value="searchParams.title" placeholder="输入标题搜索" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="searchParams.status"
            :options="SYSTEM_MESSAGE_STATUS_OPTIONS"
            placeholder="全部"
            allow-clear
            style="min-width: 120px;"
          />
        </a-form-item>
        <a-form-item label="发送模式">
          <a-select
            v-model:value="searchParams.sendMode"
            :options="SEND_MODE_OPTIONS"
            placeholder="全部"
            allow-clear
            style="min-width: 120px;"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="doClear">重置</a-button>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="openAddModal">新增消息</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="table">
      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'content'">
            <span class="content-ellipsis">{{ record.content }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'targetType'">
            <span>{{ TARGET_TYPE_MAP[record.targetType] || record.targetType }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'sendMode'">
            <span>{{ SEND_MODE_MAP[record.sendMode] || record.sendMode }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="SYSTEM_MESSAGE_STATUS_COLOR[record.status]">
              {{ SYSTEM_MESSAGE_STATUS_MAP[record.status] || record.status }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'filterInfo'">
            <span v-if="record.targetType === 'ROLE'">
              角色：{{ record.filterRole === 'admin' ? '管理员' : '普通用户' }}
            </span>
            <span v-else-if="record.targetType === 'SPACE_LEVEL'">
              空间等级 ≥ {{ record.filterSpaceLevel }}
            </span>
            <span v-else-if="record.targetType === 'REGISTER_TIME'">
              {{ record.filterRegisterStart ? dayjs(record.filterRegisterStart).format('YYYY-MM-DD') : '不限' }}
              ~
              {{ record.filterRegisterEnd ? dayjs(record.filterRegisterEnd).format('YYYY-MM-DD') : '不限' }}
            </span>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-else-if="column.dataIndex === 'publishTime'">
            <span v-if="record.publishTime">
              {{ dayjs(record.publishTime).format('YYYY-MM-DD HH:mm:ss') }}
            </span>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openEditModal(record)">编辑</a-button>
              <a-button
                v-if="record.status === SYSTEM_MESSAGE_STATUS_ENUM.DRAFT || record.status === SYSTEM_MESSAGE_STATUS_ENUM.REVOKED"
                type="link"
                style="color: #52c41a;"
                @click="handlePublish(record.id)"
              >
                发布
              </a-button>
              <a-button
                v-if="record.status === SYSTEM_MESSAGE_STATUS_ENUM.PUBLISHED"
                type="link"
                style="color: #faad14;"
                @click="handleRevoke(record.id)"
              >
                撤回
              </a-button>
              <a-button type="link" danger @click="handleDelete(record.id)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑消息' : '新增消息'"
      :width="640"
      :destroyOnClose="true"
      @ok="handleModalOk"
      @cancel="modalVisible = false"
    >
      <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
        <a-form-item label="标题" name="title">
          <a-input v-model:value="formData.title" placeholder="请输入消息标题" />
        </a-form-item>
        <a-form-item label="内容" name="content">
          <a-textarea
            v-model:value="formData.content"
            placeholder="请输入消息内容"
            :rows="4"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发送模式" name="sendMode">
              <a-select
                v-model:value="formData.sendMode"
                :options="SEND_MODE_OPTIONS"
                placeholder="请选择发送模式"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="目标类型" name="targetType">
              <a-select
                v-model:value="formData.targetType"
                :options="TARGET_TYPE_OPTIONS"
                placeholder="请选择目标类型"
                @change="onTargetTypeChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <!-- 按角色筛选 -->
        <a-form-item v-if="formData.targetType === 'ROLE'" label="目标角色" name="filterRole">
          <a-select
            v-model:value="formData.filterRole"
            :options="FILTER_ROLE_OPTIONS"
            placeholder="请选择角色"
          />
        </a-form-item>
        <!-- 按空间等级筛选 -->
        <a-form-item v-if="formData.targetType === 'SPACE_LEVEL'" label="最低空间等级" name="filterSpaceLevel">
          <a-input-number
            v-model:value="formData.filterSpaceLevel"
            :min="0"
            placeholder="请输入最低空间等级"
            style="width: 100%;"
          />
        </a-form-item>
        <!-- 按注册时间筛选 -->
        <template v-if="formData.targetType === 'REGISTER_TIME'">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="注册起始时间" name="filterRegisterStart">
                <a-date-picker
                  v-model:value="formData.filterRegisterStart"
                  placeholder="请选择起始时间"
                  style="width: 100%;"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="注册截止时间" name="filterRegisterEnd">
                <a-date-picker
                  v-model:value="formData.filterRegisterEnd"
                  placeholder="请选择截止时间"
                  style="width: 100%;"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message, Modal } from 'ant-design-vue';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import {
  adminSystemMessageControllerCreateSystemMessage,
  adminSystemMessageControllerUpdateSystemMessage,
  adminSystemMessageControllerDeleteSystemMessage,
  adminSystemMessageControllerListSystemMessages,
  adminSystemMessageControllerPublishSystemMessage,
  adminSystemMessageControllerRevokeSystemMessage,
} from '@/api/adminSystemMessageController';
import {
  SYSTEM_MESSAGE_STATUS_ENUM,
  SYSTEM_MESSAGE_STATUS_MAP,
  SYSTEM_MESSAGE_STATUS_OPTIONS,
  SYSTEM_MESSAGE_STATUS_COLOR,
  SEND_MODE_MAP,
  SEND_MODE_OPTIONS,
  TARGET_TYPE_MAP,
  TARGET_TYPE_OPTIONS,
  FILTER_ROLE_OPTIONS,
} from '@/constants/systemMessage';

// ==================== 列表相关 ====================

const dataList = ref<API.SystemMessageVO[]>([]);
const total = ref(0);
const searchParams = reactive<API.listSystemMessagesUsingGETParams>({
  current: 1,
  pageSize: 10,
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80, align: 'center' as const },
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '内容', dataIndex: 'content', key: 'content', ellipsis: true, width: 200 },
  { title: '目标类型', dataIndex: 'targetType', key: 'targetType', width: 100 },
  { title: '发送模式', dataIndex: 'sendMode', key: 'sendMode', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' as const },
  { title: '筛选条件', dataIndex: 'filterInfo', key: 'filterInfo', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: 170 },
  { title: '操作', key: 'action', width: 240 },
];

const fetchData = async () => {
  const res = await adminSystemMessageControllerListSystemMessages({ ...searchParams });
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? [];
    total.value = res.data.data.total ?? 0;
  } else {
    message.error('获取系统消息失败');
    dataList.value = [];
    total.value = 0;
  }
};

const doSearch = () => {
  searchParams.current = 1;
  fetchData();
};

const doClear = () => {
  const keys = Object.keys(searchParams) as Array<keyof API.listSystemMessagesUsingGETParams>;
  keys.forEach((key) => {
    if (key === 'current') {
      searchParams[key] = 1;
    } else if (key === 'pageSize') {
      searchParams[key] = 10;
    } else {
      delete searchParams[key];
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

// ==================== 发布 / 撤回 ====================

const handlePublish = (id: number) => {
  Modal.confirm({
    title: '确认发布',
    content: '确定要发布该消息吗？发布后将对目标用户可见。',
    okText: '确定发布',
    cancelText: '取消',
    onOk: async () => {
      const res = await adminSystemMessageControllerPublishSystemMessage({ id });
      if (res.data.code === 0) {
        message.success('发布成功');
        fetchData();
      } else {
        message.error(res.data.message || '发布失败');
      }
    },
  });
};

const handleRevoke = (id: number) => {
  Modal.confirm({
    title: '确认撤回',
    content: '确定要撤回该消息吗？撤回后目标用户将不再可见。',
    okText: '确定撤回',
    okType: 'warning',
    cancelText: '取消',
    onOk: async () => {
      const res = await adminSystemMessageControllerRevokeSystemMessage({ id });
      if (res.data.code === 0) {
        message.success('撤回成功');
        fetchData();
      } else {
        message.error(res.data.message || '撤回失败');
      }
    },
  });
};

// ==================== 删除 ====================

const handleDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该消息吗？此操作不可恢复。',
    okText: '确定删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const res = await adminSystemMessageControllerDeleteSystemMessage({ id });
      if (res.data.code === 0) {
        message.success('删除成功');
        fetchData();
      } else {
        message.error(res.data.message || '删除失败');
      }
    },
  });
};

// ==================== 新增 / 编辑弹窗 ====================

const modalVisible = ref(false);
const isEdit = ref(false);
const formRef = ref();
let editingId: number | undefined;

interface FormState {
  title?: string;
  content?: string;
  sendMode?: string;
  targetType?: string;
  filterRole?: string;
  filterSpaceLevel?: number;
  filterRegisterStart?: Dayjs;
  filterRegisterEnd?: Dayjs;
}

const formData = reactive<FormState>({});

const formRules = {
  title: [{ required: true, message: '请输入消息标题' }],
  content: [{ required: true, message: '请输入消息内容' }],
  sendMode: [{ required: true, message: '请选择发送模式' }],
  targetType: [{ required: true, message: '请选择目标类型' }],
};

const resetForm = () => {
  formData.title = undefined;
  formData.content = undefined;
  formData.sendMode = undefined;
  formData.targetType = undefined;
  formData.filterRole = undefined;
  formData.filterSpaceLevel = undefined;
  formData.filterRegisterStart = undefined;
  formData.filterRegisterEnd = undefined;
  editingId = undefined;
};

const onTargetTypeChange = () => {
  // 切换目标类型时清除之前的筛选条件
  formData.filterRole = undefined;
  formData.filterSpaceLevel = undefined;
  formData.filterRegisterStart = undefined;
  formData.filterRegisterEnd = undefined;
};

const openAddModal = () => {
  resetForm();
  isEdit.value = false;
  modalVisible.value = true;
};

const openEditModal = (record: API.SystemMessageVO) => {
  resetForm();
  isEdit.value = true;
  editingId = record.id;
  formData.title = record.title;
  formData.content = record.content;
  formData.sendMode = record.sendMode;
  formData.targetType = record.targetType;
  formData.filterRole = record.filterRole;
  formData.filterSpaceLevel = record.filterSpaceLevel;
  formData.filterRegisterStart = record.filterRegisterStart ? dayjs(record.filterRegisterStart) : undefined;
  formData.filterRegisterEnd = record.filterRegisterEnd ? dayjs(record.filterRegisterEnd) : undefined;
  modalVisible.value = true;
};

const handleModalOk = async () => {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  // 构建请求体
  const body: API.SystemMessageCreateDTO = {
    id: editingId,
    title: formData.title,
    content: formData.content,
    sendMode: formData.sendMode,
    targetType: formData.targetType,
    filterRole: formData.targetType === 'ROLE' ? formData.filterRole : undefined,
    filterSpaceLevel: formData.targetType === 'SPACE_LEVEL' ? formData.filterSpaceLevel : undefined,
    filterRegisterStart: formData.targetType === 'REGISTER_TIME' && formData.filterRegisterStart
      ? formData.filterRegisterStart.format('YYYY-MM-DD')
      : undefined,
    filterRegisterEnd: formData.targetType === 'REGISTER_TIME' && formData.filterRegisterEnd
      ? formData.filterRegisterEnd.format('YYYY-MM-DD')
      : undefined,
  };

  try {
    if (isEdit.value) {
      const res = await adminSystemMessageControllerUpdateSystemMessage(body);
      if (res.data.code === 0) {
        message.success('更新成功');
      } else {
        message.error(res.data.message || '更新失败');
        return;
      }
    } else {
      const res = await adminSystemMessageControllerCreateSystemMessage(body);
      if (res.data.code === 0) {
        message.success('创建成功');
      } else {
        message.error(res.data.message || '创建失败');
        return;
      }
    }
    modalVisible.value = false;
    fetchData();
  } catch {
    message.error(isEdit.value ? '更新失败' : '创建失败');
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
.content-ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
