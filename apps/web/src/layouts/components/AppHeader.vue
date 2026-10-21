<script setup lang="ts">
/** 顶栏：折叠侧栏 / 刷新 / 用户下拉退出（对照 jiayou FHeader 精简） */
import { Fold, Expand, Refresh } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { resetPermissionFlag } from "@/permission";
import { router } from "@/router";

const user = useUserStore();

function onRefresh() {
  window.location.reload();
}

function onLogout() {
  user.logout();
  // 否则下次登录进系统不会再拉 profile
  resetPermissionFlag();
  void router.push("/login");
}
</script>

<template>
  <div class="f-header">
    <div class="brand">
      <span class="logo">H</span>
      <span class="logo-txt">Honoka</span>
    </div>
    <el-icon class="icon-btn" @click="user.toggleAside()">
      <Fold v-if="!user.asideCollapsed" />
      <Expand v-else />
    </el-icon>
    <el-tooltip content="强制刷新" placement="bottom">
      <el-icon class="icon-btn" @click="onRefresh"><Refresh /></el-icon>
    </el-tooltip>
    <div class="spacer" />
    <el-dropdown @command="(c: string) => c === 'logout' && onLogout()">
      <span class="user-entry">
        <span class="name">{{ user.displayName }}</span>
        <el-avatar :size="32" shape="square">{{
          user.displayName.slice(0, 1)
        }}</el-avatar>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped lang="scss">
.f-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 8px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 8px;
}
.logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
}
.logo-txt {
  font-size: 16px;
  font-weight: 700;
}
.icon-btn {
  font-size: 20px;
  cursor: pointer;
  color: #4b5563;
  padding: 6px;
  border-radius: 6px;
  &:hover {
    background: #f3f4f6;
  }
}
.spacer {
  flex: 1;
}
.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.name {
  font-size: 14px;
  color: #333;
}
</style>
