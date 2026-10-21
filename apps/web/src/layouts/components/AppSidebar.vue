<script setup lang="ts">
/**
 * 侧栏：菜单来自 userStore.menus（登录后 fetchProfile）
 * 若尚未拉到菜单，用本地兜底三项，避免白屏
 */
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import * as Icons from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();
const user = useUserStore();

const active = computed(() => route.path);

const menus = computed(() =>
  user.menus.length
    ? user.menus
    : [
        { path: "/home", name: "首页", icon: "House" },
        { path: "/chat", name: "AI 对话", icon: "ChatDotRound" },
        { path: "/docs", name: "文档库", icon: "Document" },
      ],
);

/** 把字符串图标名映射成 EP 图标组件 */
function iconOf(name?: string) {
  if (!name) return Icons.Menu;
  return (Icons as Record<string, unknown>)[name] || Icons.Menu;
}

/** 不用 el-menu 的 router 模式，统一手动 push，避免双跳（同 jiayou） */
function onSelect(path: string) {
  void router.push(path);
}
</script>

<template>
  <div class="f-menu">
    <el-menu
      :default-active="active"
      :collapse="user.asideCollapsed"
      :collapse-transition="false"
      background-color="#1f2d3d"
      text-color="#bfcbd9"
      active-text-color="#ffffff"
      @select="onSelect"
    >
      <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
        <el-icon><component :is="iconOf(item.icon)" /></el-icon>
        <template #title>{{ item.name }}</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style scoped lang="scss">
.f-menu {
  height: 100%;
  overflow: auto;
  :deep(.el-menu) {
    border-right: none;
  }
}
</style>
