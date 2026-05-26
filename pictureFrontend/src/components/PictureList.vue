<template>
  <component :is="PictureListComponent" v-bind="$attrs" />
</template>

<script setup lang="ts">
import PictureListComponent from './PictureList/index.vue'
</script>
