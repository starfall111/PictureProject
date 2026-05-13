<script setup lang="ts">
import BasicLayout from './layouts/BasicLayout.vue';
import type router from './router';
import { userLoginUserStore } from './stores/user';

const loginUserStore = userLoginUserStore();
loginUserStore.getLoginUser();

</script>

<template>
  <div id="app">
    <router-view></router-view>
    <!-- <BasicLayout/> -->
  </div>
</template>

<style scoped>

</style>
