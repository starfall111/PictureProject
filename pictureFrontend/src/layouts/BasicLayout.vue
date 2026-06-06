<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <global-header />
      </a-layout-header>
      <a-layout>
        <GlobalSider class="sider" />
        <a-layout-content class="content">
          <slot />
        </a-layout-content>
      </a-layout>

      <a-layout-footer>
        <div class="footer">
          全局底部版权信息栏
        </div>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { mainControllerHealth } from '@/api/mainController';
import GlobalHeader from '@/components/GlobalHeader.vue';
import GlobalSider from '@/components/GlobalSider.vue';

mainControllerHealth().then(res => {
  console.log('后端健康检查结果:', res.data.data);
}).catch(err => {
  console.error('后端健康检查失败:', err);
});
</script>

<style>
#basicLayout .footer {
  background-color: #efefef;
  padding: 16px;
  position: fixed;
  bottom: 0;
  right: 0;
  left: 0;
  text-align: center;
}

#basicLayout .content {
  background: linear-gradient(to right, #fefefe, #fff);
  margin-bottom: 28px;
  padding: 28px;
}

#basicLayout .header {
  padding-inline: 20px;
  margin-bottom: 1px;
  color: unset;
  background: white;
}

#basicLayout .sider {
  background: #fff;
  padding-top: 20px;
  border-right: 0.5px solid #eee;
}

#basicLayout :deep(.ant-menu-root) {
  border-bottom: none !important;
  border-inline-end: none !important;
}
</style>
