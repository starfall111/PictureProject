<template>
    <div id="addPicturePage">
        <h2 style="margin-bottom: 16px">
            批量创建图片
        </h2>

        <a-form layout="vertical" :model="formData" @finish="handleSubmit">
            <a-form-item label="关键字" name="searchText">
                <a-input v-model:value="formData.searchText" placeholder="请输入想要抓取的图片关键字" />
            </a-form-item>
            <a-form-item label="数量" name="count">
                <a-input-number v-model:value="formData.count" placeholder="请输入需要抓取的数量" :min="1"
                    :max="30"></a-input-number>
            </a-form-item>
            <a-form-item label="分类" name="categoryId">
                <a-select v-model:value="formData.categoryId" :options="categoryOptions"
                    placeholder="请在选择分类之前，确定当前分类满足获取的图片类型" allowClear />
                <a-button type="link" size="large" href="/admin/categoryManage" target="_blank">去添加</a-button>
            </a-form-item>
            <a-form-item label="标签" name="tags">
                <a-select v-model:value="formData.tags" :options="tagOptions" mode="tags"
                    placeholder="请在选择标签之前，确定当前标签满足获取的图片类型" allowClear />
                <a-button type="link" size="large" href="/admin/tagManage" target="_blank">去添加</a-button>
            </a-form-item>
            <a-form-item label="名称前缀" name="profile">
                <a-input v-model:value="formData.profile" placeholder="请输入名称前缀，会自动补充序号" />
            </a-form-item>

            <a-form-item>
                <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
                    抓取
                </a-button>
            </a-form-item>
        </a-form>
    </div>

</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { pictureControllerPictureUploadByBatch } from '@/api/pictureController';
import { tagControllerListTag } from '@/api/tagController';
import { categoryControllerListCategory } from '@/api/categoryController';
import { message, } from 'ant-design-vue';

const formData = reactive<API.PictureUploadByBatchDTO>({
    count: 10
})
const loading = ref(false)


/**  
 * 提交表单  
 * @param values  
 */
const handleSubmit = async (values: any) => {
    loading.value = true
    
    const res = await pictureControllerPictureUploadByBatch({
        ...values,
    })
    if (res.data.code === 0 && res.data.data) {
        message.success(`创建成功,共创建 ${res.data.data}条`)
        loading.value = false
    } else {
        message.error('创建失败，' + res.data.message)
    }
}

const categoryOptions = ref<{ value: number; label: string }[]>([])
const tagOptions = ref<string[]>([])

// 获取标签和分类选项  
const getTagCategoryOptions = async () => {
    const res_tag = await tagControllerListTag()
    const res_category = await categoryControllerListCategory()
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

const route = useRoute()

onMounted(async () => {
    await getTagCategoryOptions()
})


</script>

<style>
#addPicturePage {
    max-width: 720px;
    margin: 0 auto;
}
</style>
