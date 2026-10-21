<script setup lang="ts">
/** 页签条：路由变化时写入 tagsStore；关掉当前页时跳到最后一个标签 */
import { computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useTagsStore } from "@/stores/tags";

const route = useRoute();
const router = useRouter();
const tags = useTagsStore();

watch(
  () => route.fullPath,
  () => {
    if (route.meta.hideLayout) return;
    tags.add({
      path: route.path,
      title: String(route.meta.title || route.name || "未命名"),
      affix: Boolean(route.meta.affix),
    });
  },
  { immediate: true },
);

const list = computed(() => tags.visited);

function go(path: string) {
  void router.push(path);
}

function close(path: string) {
  tags.remove(path);
  if (route.path === path) {
    const last = tags.visited[tags.visited.length - 1];
    void router.push(last?.path || "/home");
  }
}
</script>

<template>
  <div class="f-tags">
    <el-tag
      v-for="t in list"
      :key="t.path"
      :closable="!t.affix"
      :effect="route.path === t.path ? 'dark' : 'plain'"
      class="tag"
      @click="go(t.path)"
      @close="close(t.path)"
    >
      {{ t.title }}
    </el-tag>
  </div>
</template>

<style scoped lang="scss">
.f-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
.tag {
  cursor: pointer;
}
</style>
