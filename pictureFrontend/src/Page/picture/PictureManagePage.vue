<template>
    <div id="userManagePage">
        <div>
            <a-flex justify="space-between">
                <h2>图片管理</h2>
                <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
            </a-flex>
            <a-form layout="inline" :model="searchParams" @finish="doSearch">
                <a-form-item label="关键词" name="searchText">
                    <a-input v-model:value="searchParams.searchText" placeholder="从名称和简介搜索" allow-clear />
                </a-form-item>
                <a-form-item label="类型" name="categoryId">
                    <a-select v-model:value="searchParams.categoryId" :options="categoryOptions" placeholder="请选择分类"
                        allowClear /> </a-form-item>
                <a-form-item label="标签" name="tags">
                    <a-select v-model:value="searchParams.tags" :options="tagOptions" mode="tags" placeholder="请输入标签"
                        allowClear style="min-width: 150px;" />
                </a-form-item>
                <a-form-item>
                    <a-button type="primary" html-type="submit">搜索</a-button>
                </a-form-item>
            </a-form>
        </div>

        <div class="table">
            <a-table :columns="columns" :data-source="dataList" :pagination="pagination" @change="doTableChange">
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'url'">
                        <a-image :src="record.url" :width="120" />
                    </template>
                    <!-- 标签 -->
                    <template v-if="column.dataIndex === 'tags'">
                        <a-space wrap>
                            <a-tag v-for="tag in JSON.parse(record.tags || '[]')" :key="tag">{{ tag }}</a-tag>
                        </a-space>
                    </template>
                    <!-- 图片信息 -->
                    <template v-if="column.dataIndex === 'picInfo'">
                        <div>格式：{{ record.picFormat }}</div>
                        <div>宽度：{{ record.picWidth }}</div>
                        <div>高度：{{ record.picHeight }}</div>
                        <div>宽高比：{{ record.picScale }}</div>
                        <div>大小：{{ (record.picSize / 1024).toFixed(2) }}KB</div>
                    </template>
                    <template v-else-if="column.dataIndex === 'createTime'">
                        {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </template>
                    <template v-else-if="column.dataIndex === 'editTime'">
                        {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </template>
                    <template v-else-if="column.key === 'action'">
                        <a-space>
                            <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank">编辑</a-button>
                            <a-button type="link" danger @click="doDelete(record.id)">删除</a-button>
                        </a-space>
                    </template>
                </template>

            </a-table>
        </div>

    </div>

</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { deletePictureUsingDelete, queryPictureAdminUsingPost } from '@/api/pictureController';
import { listTagUsingGet } from '@/api/tagController';
import { listCategoryUsingGet } from '@/api/categoryController';

// ==================== 列表相关 ====================

// 数据  
const dataList = ref([])
const total = ref(0)

// 搜索条件  
const searchParams = reactive<API.queryPictureAdminUsingPOSTParams>({
    current: 1,
    pageSize: 10,
    sortField: 'createTime',
    sortOrder: 'descend',
})

// 分页参数  
const pagination = computed(() => {
    return {
        current: searchParams.current ?? 1,
        pageSize: searchParams.pageSize ?? 10,
        total: total.value,
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条`,
    }
})

// 获取数据  
const fetchData = async () => {
    const res = await queryPictureAdminUsingPost({
        ...searchParams,
    })
    if (res.data.data) {
        dataList.value = res.data.data.records ?? []
        total.value = res.data.data.total ?? 0
    } else {
        message.error('获取数据失败，' + res.data.message)
    }
}

// 页面加载时请求一次  
onMounted(() => {
    fetchData()
})

// 获取数据  
const doSearch = () => {
    // 重置搜索条件  
    searchParams.current = 1
    fetchData()
}

// 表格变化处理  
const doTableChange = (page: any) => {
    searchParams.current = page.current
    searchParams.pageSize = page.pageSize
    fetchData()
}

const doDelete = async (id: any) => {
    const res = await deletePictureUsingDelete(id)
    if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
    } else {
        message.error('删除失败，' + res.data.message)
    }
}

const categoryOptions = ref<{ value: number; label: string }[]>([])
const tagOptions = ref<string[]>([])

// 获取标签和分类选项  
const getTagCategoryOptions = async () => {
    const res_tag = await listTagUsingGet()
    const res_category = await listCategoryUsingGet()
    if (res_category.data.code === 0 && res_category.data.data) {
        // 转换成下拉选项组件接受的格式  
        categoryOptions.value = (res_category.data.data ?? []).map((data: any) => {
            return {
                value: data.id,
                label: data.name,
            }
        })
    } else {
        message.error('加载选项失败，' + res_category.data.message)
    }
    if (res_tag.data.code === 0 && res_tag.data.data) {
        // 转换成下拉选项组件接受的格式  
        tagOptions.value = (res_tag.data.data ?? []).map((data: any) => {
            return {
                value: data.name,
                label: data.name,
            }
        })
    } else {
        message.error('加载选项失败，' + res_tag.data.message)
    }
}

const columns = [
    {
        title: 'id',
        dataIndex: 'id',
        width: 80,
    },
    {
        title: '图片',
        dataIndex: 'url',
    },
    {
        title: '名称',
        dataIndex: 'name',
    },
    {
        title: '简介',
        dataIndex: 'introduction',
        ellipsis: true,
    },
    {
        title: '类型',
        dataIndex: 'categoryName',
    },
    {
        title: '标签',
        dataIndex: 'tags',
    },
    {
        title: '图片信息',
        dataIndex: 'picInfo',
    },
    {
        title: '用户 id',
        dataIndex: 'userId',
        width: 80,
    },
    {
        title: '创建时间',
        dataIndex: 'createTime',
    },
    {
        title: '编辑时间',
        dataIndex: 'editTime',
    },
    {
        title: '操作',
        key: 'action',
    },
]

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

// ==================== 初始化 ====================

onMounted(() => {
    fetchData();
    getTagCategoryOptions();
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
