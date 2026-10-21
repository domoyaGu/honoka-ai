/**
 * 鉴权相关 API
 *
 * VITE_USE_MOCK=true（默认）：前端本地假登录，不打后端，方便没 API 时跑通壳子。
 * 关掉 mock 后：走 /auth/login、/auth/me，需 apps/api 实现对应路由。
 */
import { request } from "@/utils/request";

export interface UserInfo {
  userId: string;
  userName: string;
}

/** 侧栏菜单项；阶段 B 可由后端按权限下发，前端再动态 addRoute */
export interface MenuItem {
  path: string;
  name: string;
  /** Element Plus Icons 组件名，如 House / Document */
  icon?: string;
  children?: MenuItem[];
}

export interface LoginResult {
  token: string;
}

export interface ProfileResult {
  user: UserInfo;
  menus: MenuItem[];
  /** 按钮级权限码，阶段 B 给 v-permission 用 */
  perms: string[];
}

const useMock = import.meta.env.VITE_USE_MOCK === "true";

/** 登录：mock 任意非空用户名即可；真接口时校验账号密码 */
export async function loginApi(payload: {
  username: string;
  password: string;
}): Promise<LoginResult> {
  if (useMock) {
    await delay(200);
    if (!payload.username) throw { message: "请输入用户名" };
    return { token: `mock-token-${payload.username}` };
  }
  return request<LoginResult>({
    url: "/auth/login",
    method: "post",
    data: payload,
  });
}

/** 当前用户 + 菜单 + 权限；刷新页面后靠它恢复顶栏/侧栏 */
export async function getUserInfoApi(): Promise<ProfileResult> {
  if (useMock) {
    await delay(100);
    return {
      user: { userId: "1", userName: "演示用户" },
      perms: ["docs:read", "chat:use"],
      menus: [
        { path: "/home", name: "首页", icon: "House" },
        { path: "/chat", name: "AI 对话", icon: "ChatDotRound" },
        { path: "/docs", name: "文档库", icon: "Document" },
      ],
    };
  }
  return request<ProfileResult>({ url: "/auth/me", method: "get" });
}

function delay(ms: number) {
  return new Promise((r) => setTimeout(r, ms));
}
