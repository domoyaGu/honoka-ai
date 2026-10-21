/** 探活：验证 Vite 代理与 apps/api 是否通 */
import { request } from "@/utils/request";

export interface HealthInfo {
  ok: boolean;
  service: string;
  time: string;
}

export function fetchHealth() {
  // /health 返回裸 JSON，request 拦截器会原样给出 HealthInfo
  return request<HealthInfo>({ url: "/health", method: "get" });
}
