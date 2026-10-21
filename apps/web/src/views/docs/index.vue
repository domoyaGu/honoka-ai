<script setup lang="ts">
/**
 * 文档库页 —— UI 样板已搭；【refresh / 上传 / 删除 请自己接线】
 * 说明：docs/R1练习-自己写.md §4
 */
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { UploadFilled } from "@element-plus/icons-vue";
import {
  deleteDoc,
  listDocs,
  uploadDoc,
  type DocItem,
} from "@/api/docs";

const loading = ref(false);
const uploading = ref(false);
const list = ref<DocItem[]>([]);

async function refresh() {
  loading.value = true;
  try {
    // 【你来写】list.value = await listDocs()
    list.value = await listDocs();
    // void listDocs;
    ElMessage.info("TODO: 实现 refresh → listDocs()");
  } catch {
    /* request 已 toast */
  } finally {
    loading.value = false;
  }
}

async function onFileChange(file: { raw?: File }) {
  const raw = file.raw;
  if (!raw) return;
  // 【你来写】校验 .md/.txt → uploadDoc(raw) → refresh()
  void uploadDoc;
  void uploading;
  ElMessage.info("TODO: 实现上传 onFileChange");
}

async function onRemove(row: DocItem) {
  // 【你来写】ElMessageBox.confirm → deleteDoc(row.id) → refresh()
  void deleteDoc;
  void ElMessageBox;
  void row;
  ElMessage.info("TODO: 实现删除 onRemove");
}

function formatSize(n: number) {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / 1024 / 1024).toFixed(1)} MB`;
}

onMounted(() => {
  void refresh();
});
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head">
        <span>文档库（自己接线）</span>
        <el-upload
          :show-file-list="false"
          :auto-upload="false"
          accept=".md,.txt,text/plain,text/markdown"
          :disabled="uploading"
          :on-change="onFileChange"
        >
          <el-button type="primary" :icon="UploadFilled" :loading="uploading">
            上传 .md / .txt
          </el-button>
        </el-upload>
      </div>
    </template>

    <el-table :data="list" empty-text="实现 listDocs 后这里会有数据">
      <el-table-column prop="name" label="文件名" min-width="200" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize((row as DocItem).size) }}</template>
      </el-table-column>
      <el-table-column prop="uploadedAt" label="更新时间" min-width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="onRemove(row as DocItem)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped lang="scss">
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
