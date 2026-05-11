<script setup lang="ts">
import BasicLayout from './layouts/BasicLayout.vue';
import { userLoginUserStore } from './stores/user';

const loginUserStore = userLoginUserStore();
loginUserStore.getLoginUser();

</script>

<template>
  <div id="app">
    <BasicLayout/>
  </div>
</template>

<style scoped>

</style>
