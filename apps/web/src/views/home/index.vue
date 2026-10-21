<script setup lang="ts">
import { onMounted, ref } from "vue";
import { fetchHealth } from "@/api/health";
import { useUserStore } from "@/stores/user";

const user = useUserStore();
const apiOk = ref<boolean | null>(null);
const apiMessage = ref("");

onMounted(async () => {
  try {
    const data = await fetchHealth();
    apiOk.value = Boolean(data.ok);
    apiMessage.value = data.service;
  } catch (e) {
    apiOk.value = false;
    apiMessage.value = e instanceof Error ? e.message : "请求失败";
  }
});
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <span>工作台</span>
    </template>
    <p>
      你好，{{ user.displayName }}。工程结构对照 jiayou：axios 封装、Pinia、permission
      守卫、Admin 布局（顶栏 / 侧栏 / Tags）。
    </p>
    <el-alert
      style="margin-top: 12px"
      :closable="false"
      show-icon
      :type="apiOk === null ? 'info' : apiOk ? 'success' : 'warning'"
      :title="
        apiOk === null
          ? '正在检查 API…'
          : apiOk
            ? `API 正常：${apiMessage}`
            : `API 未连通：${apiMessage}（请 npm run dev:api）`
      "
    />
  </el-card>
</template>
