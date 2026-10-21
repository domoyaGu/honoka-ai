<script setup lang="ts">
/**
 * 登录页：阶段 A 默认 mock（任意非空用户名即可）
 * 成功后优先跳转 query.redirect（守卫带过来的原地址）
 */
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const route = useRoute();
const user = useUserStore();
const loading = ref(false);

const form = reactive({
  username: "admin",
  password: "admin123",
});

async function onSubmit() {
  loading.value = true;
  try {
    await user.login(form.username, form.password);
    ElMessage.success("登录成功");
    const redirect = (route.query.redirect as string) || "/home";
    await router.replace(redirect);
  } catch (e: unknown) {
    const msg =
      e && typeof e === "object" && "message" in e
        ? String((e as { message: string }).message)
        : "登录失败";
    ElMessage.error(msg);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h1>Honoka</h1>
      <p class="sub">阶段 A · 演示登录（mock）</p>
      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          style="width: 100%"
        >
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100%;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #1f2d3d 0%, #2563eb 120%);
}
.login-card {
  width: 380px;
  h1 {
    margin: 0 0 4px;
    font-size: 28px;
  }
  .sub {
    margin: 0 0 20px;
    color: #6b7280;
    font-size: 13px;
  }
}
</style>
