<script setup lang="ts">
/**
 * 后台主布局（对照 jiayou AdminContainer，结构更薄）
 * 顶栏 + 可折叠侧栏 + Tags + keep-alive 主区
 */
import AppHeader from "./components/AppHeader.vue";
import AppSidebar from "./components/AppSidebar.vue";
import AppTags from "./components/AppTags.vue";
import { useUserStore } from "@/stores/user";

const user = useUserStore();
</script>

<template>
  <el-container class="admin-layout">
    <el-header class="admin-header" height="56px">
      <AppHeader />
    </el-header>
    <el-container class="admin-body">
      <el-aside :width="user.asideWidth" class="admin-aside">
        <AppSidebar />
      </el-aside>
      <el-container class="admin-main-wrap">
        <AppTags />
        <el-main class="admin-main">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <keep-alive :max="10">
                <component :is="Component" :key="$route.fullPath" />
              </keep-alive>
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.admin-layout {
  height: 100%;
}
.admin-header {
  padding: 0;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
.admin-body {
  height: calc(100% - 56px);
}
.admin-aside {
  background: #1f2d3d;
  transition: width 0.2s ease;
  overflow: hidden;
}
.admin-main-wrap {
  flex-direction: column;
  background: #f0f2f5;
}
.admin-main {
  padding: 16px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
