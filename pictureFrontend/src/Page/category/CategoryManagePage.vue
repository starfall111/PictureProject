<template>
  <div id="categoryManagePage">
    <div class="search">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="分类名称">
          <a-input v-model:value="searchParams.name" placeholder="输入分类名称" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="doClear">重置</a-button>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="openAddModal">新增分类</a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="table">
      <a-table :columns="columns" :data-source="dataList" :pagination="pagination" @change="doTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'createTime'">
            <div>{{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'editTime'">
            <div>{{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="primary" @click="openEditModal(record.id)">编辑</a-button>
              <a-button type="primary" danger @click="handleDelete(record.id)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="分类名称" :rules="[{ required: true, message: '请输入分类名称' }]">
          <a-input v-model:value="formData.name" placeholder="请输入分类名称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  queryCategoryPageUsingPost,
  addCategoryUsingPost,
  updateCategoryUsingPost,
  deleteCategoryUsingDelete,
  getCategoryByIdUsingGet,
} from '@/api/categoryController';

// ==================== 列表相关 ====================

const dataList = ref<API.Category[]>([]);
const total = ref(0);
const searchParams = reactive<API.CategoryQueryDTO>({
  current: 1,
  pageSize: 10,
});

const columns = [
  { title: 'id', dataIndex: 'id', key: 'id', align: 'center' },
  { title: '分类名称', dataIndex: 'name', key: 'name', align: 'center' },
  { title: '图片数量', dataIndex: 'count', key: 'count', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', align: 'center' },
  { title: '编辑时间', dataIndex: 'editTime', key: 'editTime', align: 'center' },
  { title: '操作', key: 'action', align: 'center' },
];

const fetchData = async () => {
  const res = await queryCategoryPageUsingPost({ ...searchParams });
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? [];
    total.value = res.data.data.total ?? 0;
  } else {
    message.error('获取分类数据失败');
    dataList.value = [];
    total.value = 0;
  }
};

const doSearch = () => {
  searchParams.current = 1;
  fetchData();
};

const doClear = () => {
  searchParams.current = 1;
  searchParams.pageSize = 10;
  searchParams.name = undefined;
  searchParams.sortField = undefined;
  searchParams.sortOrder = undefined;
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

// ==================== 新增/编辑弹窗 ====================

const modalVisible = ref(false);
const isEdit = ref(false);
const formData = reactive<{ id?: number; name?: string }>({});

const openAddModal = () => {
  isEdit.value = false;
  formData.id = undefined;
  formData.name = undefined;
  modalVisible.value = true;
};

const openEditModal = async (id: number) => {
  try {
    const res = await getCategoryByIdUsingGet({ id });
    if (res.data.code === 0 && res.data.data) {
      isEdit.value = true;
      formData.id = res.data.data.id;
      formData.name = res.data.data.name;
      modalVisible.value = true;
    } else {
      message.error(res.data.message || '获取分类信息失败');
    }
  } catch {
    message.error('获取分类信息失败');
  }
};

const handleModalOk = async () => {
  if (!formData.name?.trim()) {
    message.warning('请输入分类名称');
    return;
  }
  try {
    if (isEdit.value) {
      const res = await updateCategoryUsingPost({ id: formData.id, name: formData.name });
      if (res.data.code === 0) {
        message.success('编辑成功');
        modalVisible.value = false;
        fetchData();
      } else {
        message.error(res.data.message || '编辑失败');
      }
    } else {
      const res = await addCategoryUsingPost({ name: formData.name });
      if (res.data.code === 0) {
        message.success('新增成功');
        modalVisible.value = false;
        fetchData();
      } else {
        message.error(res.data.message || '新增失败');
      }
    }
  } catch {
    message.error(isEdit.value ? '编辑失败' : '新增失败');
  }
};

const handleModalCancel = () => {
  modalVisible.value = false;
};

// ==================== 删除 ====================

const handleDelete = async (id: number) => {
  const res = await deleteCategoryUsingDelete({ id });
  if (res.data.code === 0) {
    message.success('删除成功');
    fetchData();
  } else {
    message.error(res.data.message || '删除失败');
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
