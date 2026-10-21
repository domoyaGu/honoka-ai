/**
 * 路由表
 *
 * - constantRoutes：登录页 + 布局壳 + 阶段 A 业务页（写死）
 * - 阶段 B：可按后端 menus 动态 addRoute（同 jiayou addRoutes）
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "login",
    component: () => import("@/views/login/index.vue"),
    meta: { title: "登录", hideLayout: true },
  },
  {
    path: "/",
    name: "layout",
    component: () => import("@/layouts/AdminLayout.vue"),
    redirect: "/home",
    children: [
      {
        path: "home",
        name: "home",
        component: () => import("@/views/home/index.vue"),
        meta: { title: "首页", affix: true }, // affix：Tags 不可关
      },
      {
        path: "chat",
        name: "chat",
        component: () => import("@/views/chat/index.vue"),
        meta: { title: "AI 对话" },
      },
      {
        path: "docs",
        name: "docs",
        component: () => import("@/views/docs/index.vue"),
        meta: { title: "文档库" },
      },
    ],
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
});
