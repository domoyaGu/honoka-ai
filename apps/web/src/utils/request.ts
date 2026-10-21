/**
 * Axios 封装（对照 jiayou `src/axios.js`，阶段 A 精简版）
 *
 * - baseURL 固定 `/api`，开发时由 Vite proxy 转到本机 API（见 vite.config.ts）
 * - 请求头自动带 Bearer Token
 * - 响应：优先按 { code, data, message } 解包；否则原样返回（如 /health）
 * - 401：清 token、重置用户态、跳转登录
 */
import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";
import { ElMessage } from "element-plus";
import { getToken, removeToken } from "@/composables/auth";
import { router } from "@/router";
import { useUserStore } from "@/stores/user";

/** 后端约定的统一业务包装；阶段 B 真实接口尽量都用这套 */
export interface ApiResult<T = unknown> {
  code: number;
  data: T;
  message?: string;
}

const service: AxiosInstance = axios.create({
  baseURL: "/api",
  timeout: 30000,
});

/** 发出前：注入登录态 */
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) {
    // axios v1 推荐用 headers.set，避免普通对象赋值类型问题
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  // FormData 上传时若残留 application/json，会破坏 multipart
  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    config.headers.delete("Content-Type");
  }
  return config;
});

service.interceptors.response.use(
  (response) => {
    const payload = response.data;

    // 标准包装：业务码在 body.code（不是 HTTP status）
    if (
      payload &&
      typeof payload === "object" &&
      "code" in payload &&
      "data" in payload
    ) {
      const body = payload as ApiResult;
      if (body.code === 401) {
        handleUnauthorized(body.message || "请重新登录");
        return Promise.reject(body);
      }
      // 兼容 200 与 0 两种成功码
      if (body.code !== 200 && body.code !== 0) {
        ElMessage.error(body.message || "请求失败");
        return Promise.reject(body);
      }
      // 调用方直接拿到 data，少一层解包
      return body.data;
    }

    // 裸 JSON（健康检查等）原样交给调用方
    return payload;
  },
  (error) => {
    // HTTP 层错误（网络断开、5xx、真 401 状态码等）
    const status = error.response?.status;
    if (status === 401) {
      handleUnauthorized("登录已过期");
    } else {
      ElMessage.error(error.message || "网络错误");
    }
    return Promise.reject(error);
  },
);

/** 统一掉登录：清本地态并带回跳地址 */
function handleUnauthorized(msg: string) {
  ElMessage.warning(msg);
  removeToken();
  try {
    useUserStore().$reset();
  } catch {
    // 极早时机 pinia 可能尚未 active，忽略即可
  }
  if (router.currentRoute.value.path !== "/login") {
    void router.push({
      path: "/login",
      query: { redirect: router.currentRoute.value.fullPath },
    });
  }
}

/**
 * 带泛型的请求入口：业务 api 模块优先用这个，而不是直接 service.get
 * 注意：成功时返回的是「已解包的 data」，类型 T 对应 data 而非整个 AxiosResponse
 */
export function request<T = unknown>(config: AxiosRequestConfig) {
  return service.request<any, T>(config);
}

export default service;
