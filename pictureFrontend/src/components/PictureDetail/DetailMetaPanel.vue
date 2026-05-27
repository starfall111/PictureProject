<template>
  <a-collapse :bordered="false" class="meta-collapse" v-model:activeKey="activeKeys">
    <a-collapse-panel key="meta" header="图片详情">
      <div class="meta-grid">
        <div class="meta-item" v-if="picture.picFormat">
          <FileImageOutlined class="meta-icon" />
          <span class="meta-label">格式</span>
          <span class="meta-value">{{ picture.picFormat }}</span>
        </div>
        <div class="meta-item" v-if="picture.picWidth && picture.picHeight">
          <ExpandOutlined class="meta-icon" />
          <span class="meta-label">分辨率</span>
          <span class="meta-value">{{ picture.picWidth }} x {{ picture.picHeight }}</span>
        </div>
        <div class="meta-item" v-if="picture.picSize">
          <HddOutlined class="meta-icon" />
          <span class="meta-label">大小</span>
          <span class="meta-value">{{ formatSize(picture.picSize) }}</span>
        </div>
        <div class="meta-item" v-if="picture.picScale != null">
          <ColumnWidthOutlined class="meta-icon" />
          <span class="meta-label">宽高比</span>
          <span class="meta-value">{{ picture.picScale.toFixed(2) }}</span>
        </div>
        <div class="meta-item" v-if="picture.categoryName">
          <FolderOutlined class="meta-icon" />
          <span class="meta-label">分类</span>
          <span class="meta-value">{{ picture.categoryName }}</span>
        </div>
      </div>
    </a-collapse-panel>
  </a-collapse>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { formatSize } from '@/util/format'
import {
  FileImageOutlined,
  ExpandOutlined,
  HddOutlined,
  ColumnWidthOutlined,
  FolderOutlined,
} from '@ant-design/icons-vue'

interface Props {
  picture: API.PictureVO
}

defineProps<Props>()

const activeKeys = ref<string[]>([])
</script>

<style scoped>
.meta-collapse {
  background: transparent;
  margin-bottom: 24px;
}

.meta-collapse :deep(.ant-collapse-header) {
  font-size: 15px;
  color: #595959;
  padding: 12px 0;
}

.meta-collapse :deep(.ant-collapse-content-box) {
  padding: 0 0 8px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-icon {
  color: #8c8c8c;
  font-size: 16px;
}

.meta-label {
  color: #8c8c8c;
  font-size: 14px;
  min-width: 48px;
}

.meta-value {
  font-weight: 500;
  color: #262626;
  font-size: 14px;
}

@media (max-width: 767px) {
  .meta-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
