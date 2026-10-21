/**
 * 用户 / 侧栏状态（对照 jiayou Vuex user 模块；本仓用 Pinia 更利 TypeScript）
 */
import { defineStore } from "pinia";
import { getToken, removeToken, setToken } from "@/composables/auth";
import {
  loginApi,
  getUserInfoApi,
  type MenuItem,
  type UserInfo,
} from "@/api/auth";

interface UserState {
  token: string;
  user: UserInfo | null;
  menus: MenuItem[];
  perms: string[];
  /** true 时侧栏收成图标栏 */
  asideCollapsed: boolean;
}

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    token: getToken(),
    user: null,
    menus: [],
    perms: [],
    asideCollapsed: false,
  }),
  getters: {
    /** 给 AdminLayout 的 el-aside :width 用 */
    asideWidth: (s) => (s.asideCollapsed ? "64px" : "220px"),
    displayName: (s) => s.user?.userName || "未登录",
  },
  actions: {
    toggleAside() {
      this.asideCollapsed = !this.asideCollapsed;
    },
    async login(username: string, password: string) {
      const data = await loginApi({ username, password });
      this.token = data.token;
      setToken(data.token);
      return data;
    },
    async fetchProfile() {
      const data = await getUserInfoApi();
      this.user = data.user;
      this.menus = data.menus;
      this.perms = data.perms;
      return data;
    },
    logout() {
      this.token = "";
      this.user = null;
      this.menus = [];
      this.perms = [];
      removeToken();
    },
  },
});
