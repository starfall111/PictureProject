<template>
    <div id="addPicturePage" :class="{ 'immersive-layout': isImmersive }">
        <!-- 沉浸式双栏布局 -->
        <template v-if="isImmersive">
            <a-typography-paragraph v-if="spaceId" type="secondary" style="margin-bottom: 12px">
                保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
            </a-typography-paragraph>
            <a-tabs v-model:activeKey="uploadType" style="margin-bottom: 16px">
                <a-tab-pane key="file" tab="文件上传" />
                <a-tab-pane key="url" tab="URL 上传" />
            </a-tabs>
            <div class="immersive-upload-container">
                <!-- 左侧：上传区 -->
                <div class="immersive-upload-left">
                    <PictureUpload v-if="uploadType === 'file'" :picture="picture" :onSuccess="onSuccess" :spaceId="spaceId" />
                    <UrlPictureUpload v-else :picture="picture" :onSuccess="onSuccess" :spaceId="spaceId" />
                </div>
                <!-- 右侧：表单卡片 -->
                <div class="immersive-upload-right">
                    <PictureFormCard
                        v-if="picture"
                        :picture="picture"
                        :categoryOptions="categoryOptions"
                        :tagOptions="tagOptions"
                        :loading="loading"
                        :isEdit="!!route.query?.id"
                        @submit="handleSubmit"
                    />
                    <div v-else class="upload-placeholder">
                        <p>请先上传图片</p>
                    </div>
                </div>
            </div>
        </template>

        <!-- 默认布局 -->
        <template v-else>
            <h2 style="margin-bottom: 16px">
                {{ route.query?.id ? '修改图片' : '创建图片' }}
            </h2>
            <a-typography-paragraph v-if="spaceId" type="secondary">
                保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
            </a-typography-paragraph>

            <!-- 选择上传方式 -->
            <a-tabs v-model:activeKey="uploadType">
                <a-tab-pane key="file" tab="文件上传">
                    <PictureUpload :picture="picture" :onSuccess="onSuccess" :spaceId="spaceId" />
                </a-tab-pane>
                <a-tab-pane key="url" tab="URL 上传" force-render>
                    <UrlPictureUpload :picture="picture" :onSuccess="onSuccess" :spaceId="spaceId" />
                </a-tab-pane>
            </a-tabs>

            <a-form layout="vertical" :model="pictureForm" @finish="handleSubmit" v-if="picture">
                <a-form-item label="名称" name="name">
                    <a-input v-model:value="pictureForm.name" placeholder="请输入名称" />
                </a-form-item>
                <a-form-item label="简介" name="introduction">
                    <a-textarea v-model:value="pictureForm.introduction" placeholder="请输入简介" :rows="2" autoSize
                        allowClear />
                </a-form-item>
                <a-form-item label="分类" name="categoryId">
                    <a-select v-model:value="pictureForm.categoryId" :options="categoryOptions" placeholder="请选择分类"
                        allowClear />
                </a-form-item>
                <a-form-item label="标签" name="tags">
                    <a-select v-model:value="pictureForm.tags" :options="tagOptions" mode="tags" placeholder="请输入标签"
                        allowClear />
                </a-form-item>

                <a-form-item>
                    <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
                        {{ route.query?.id ? '修改' : '创建' }}</a-button>
                </a-form-item>
            </a-form>
        </template>
    </div>

</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { useRoute, useRouter } from 'vue-router';
import { pictureControllerEditPicture, pictureControllerGetPictureByIdUser } from '@/api/pictureController';
import { tagControllerListTag } from '@/api/tagController';
import { categoryControllerListCategory } from '@/api/categoryController';
import { message, Modal } from 'ant-design-vue';
import { userLoginUserStore } from '@/stores/user';
import { useLayoutScheme } from '@/composables/useLayoutScheme';
import PictureFormCard from '@/layouts/scheme-1-immersive/components/PictureFormCard.vue';

const { activeSchemeId } = useLayoutScheme();
const isImmersive = computed(() => activeSchemeId.value === 'scheme-1-immersive');

const pictureForm = reactive<API.PictureEditDTO>({})
const picture = ref<API.PictureVO>()
const onSuccess = (newPicture: API.PictureVO) => {
    picture.value = newPicture
    pictureForm.name = newPicture.name
}

const router = useRouter()
const uploadType = ref<'file' | 'url'>('file')
const loading = ref(false)

// 空间 id
const spaceId = computed(() => {
    return route.query?.spaceId
})

/**  
 * 提交表单  
 * @param values  
 */
const handleSubmit = async (values: any) => {
    loading.value = true
    const pictureId = picture.value.id
    console.log(values)
    if (!pictureId) {
        return
    }
    const res = await pictureControllerEditPicture({
        id: pictureId,
        spaceId: spaceId.value,
        ...values,
    })
    if (res.data.code === 0 && res.data.data) {
        message.success('创建成功')
        loading.value = false
        // 跳转到图片详情页  
        router.push({
            path: `/picture/${pictureId}`,
        })
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

// 获取老数据
const getOldPicture = async () => {
    const id = route.query?.id
    if (id) {
        const res = await pictureControllerGetPictureByIdUser({
            id: id,
        })
        if (res.data.code === 0 && res.data.data) {
            const data = res.data.data
            picture.value = data
            pictureForm.name = data.name
            pictureForm.introduction = data.introduction
            pictureForm.categoryId = data.categoryId
            pictureForm.tags = data.tags
        }
    }
}

onMounted(async () => {
    const loginUserStore = userLoginUserStore()
    await loginUserStore.getLoginUser(true)
    await getTagCategoryOptions()
    await getOldPicture()
})


</script>

<style>
#addPicturePage {
    max-width: 720px;
    margin: 0 32px;
}

/* 沉浸式双栏布局 */
#addPicturePage.immersive-layout {
    max-width: 100%;
}

.immersive-upload-container {
    display: grid;
    grid-template-columns: 3fr 2fr;
    gap: 24px;
    align-items: start;
}

.immersive-upload-left {
    min-width: 0;
}

.immersive-upload-right {
    position: sticky;
    top: 80px;
}

.upload-placeholder {
    text-align: center;
    padding: 48px 24px;
    color: var(--fg-muted, #9CA3AF);
    background: var(--surface-primary, #FFFFFF);
    border-radius: var(--radius-card, 12px);
    box-shadow: var(--shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.04));
}

@media (max-width: 640px) {
    .immersive-upload-container {
        grid-template-columns: 1fr;
    }
}
</style>
