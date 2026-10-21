/**
 * 路由全局守卫（对照 jiayou `src/permission.js`）
 *
 * 阶段 A：
 * - 无 token → 强制 /login（白名单除外）
 * - 有 token → 首次进入拉一次用户信息 / 菜单（mock 或真接口）
 * - 动态 addRoutes 留到阶段 B（现在业务路由写死在 router/index.ts）
 *
 * 注意：本文件须在 `app.use(createPinia())` 之后再加载，见 main.ts 动态 import。
 */
import nProgress from "nprogress";
import "nprogress/nprogress.css";
import { getToken } from "@/composables/auth";
import { router } from "@/router";
import { useUserStore } from "@/stores/user";

nProgress.configure({ showSpinner: false });

/** 是否已拉过 profile；避免每次点菜单都请求。退出登录时要 reset。 */
let profileLoaded = false;

router.beforeEach(async (to, _from, next) => {
  nProgress.start();
  const token = getToken();
  const whiteList = ["/login"];

  // 未登录：去登录，并带上 redirect 方便登录后回跳
  if (!token && !whiteList.includes(to.path)) {
    next({ path: "/login", query: { redirect: to.fullPath } });
    return;
  }

  // 已登录还访登录页：直接进首页，防重复登录
  if (token && to.path === "/login") {
    next("/home");
    return;
  }

  // 首次带 token 进入系统：拉用户 + 侧栏菜单数据
  if (token && !profileLoaded) {
    const user = useUserStore();
    try {
      await user.fetchProfile();
      profileLoaded = true;
    } catch {
      // token 无效或接口失败：清态回登录
      user.logout();
      profileLoaded = false;
      next("/login");
      nProgress.done();
      return;
    }
  }

  document.title = `${String(to.meta.title || "Honoka")} - Honoka`;
  next();
});

router.afterEach(() => {
  nProgress.done();
});

/** 退出登录后调用，否则下次进系统不会重新 fetchProfile */
export function resetPermissionFlag() {
  profileLoaded = false;
}
