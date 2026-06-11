<template>
  <a-modal
    :open="visible"
    title="创建批次"
    :width="520"
    centered
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="emit('cancel')"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      :label-col="{ span: 5 }"
      style="margin-top: 16px"
    >
      <a-form-item label="批次名称" name="name">
        <a-input v-model:value="formState.name" placeholder="请输入批次名称" :maxlength="128" />
      </a-form-item>
      <a-form-item label="券类型" name="type">
        <a-select v-model:value="formState.type" placeholder="请选择券类型">
          <a-select-option :value="3">3天VIP体验券</a-select-option>
          <a-select-option :value="7">7天VIP畅享券</a-select-option>
          <a-select-option :value="30">30天VIP至尊券</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="总库存" name="totalStock">
        <a-input-number
          v-model:value="formState.totalStock"
          :min="1"
          :max="100000"
          placeholder="请输入库存数量"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="开始时间" name="startTime">
        <a-date-picker
          v-model:value="formState.startTime"
          show-time
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择开始时间"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="结束时间" name="endTime">
        <a-date-picker
          v-model:value="formState.endTime"
          show-time
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择结束时间（可选）"
          style="width: 100%"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'

defineProps<{
  visible: boolean
  loading: boolean
}>()

const emit = defineEmits<{
  ok: [data: API.BatchCreateDTO]
  cancel: []
}>()

const formRef = ref<FormInstance>()

const formState = reactive({
  name: undefined as string | undefined,
  type: undefined as number | undefined,
  totalStock: undefined as number | undefined,
  startTime: undefined as any,
  endTime: undefined as any,
})

const rules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入批次名称' }],
  type: [{ required: true, message: '请选择券类型' }],
  totalStock: [{ required: true, message: '请输入总库存' }],
  startTime: [{ required: true, message: '请选择开始时间' }],
}

watch(() => arguments, () => {
  // reset form when modal opens
})

function resetForm() {
  formState.name = undefined
  formState.type = undefined
  formState.totalStock = undefined
  formState.startTime = undefined
  formState.endTime = undefined
}

async function handleOk() {
  try {
    await formRef.value?.validate()
    const payload: API.BatchCreateDTO = {
      name: formState.name,
      type: formState.type,
      totalStock: formState.totalStock,
      startTime: formState.startTime?.toDate?.(),
      endTime: formState.endTime?.toDate?.(),
    }
    emit('ok', payload)
    resetForm()
  } catch {
    // validation failed
  }
}
</script>
