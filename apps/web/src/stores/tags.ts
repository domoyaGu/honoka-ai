/**
 * 多页签（对照 jiayou FTaglist 的「访问过的路由」记忆）
 * affix 页（如首页）不可关闭。
 */
import { defineStore } from "pinia";

export interface TagItem {
  path: string;
  title: string;
  affix?: boolean;
}

export const useTagsStore = defineStore("tags", {
  state: () => ({
    visited: [{ path: "/home", title: "首页", affix: true }] as TagItem[],
  }),
  actions: {
    add(tag: TagItem) {
      if (this.visited.some((t) => t.path === tag.path)) return;
      this.visited.push(tag);
    },
    remove(path: string) {
      const i = this.visited.findIndex((t) => t.path === path && !t.affix);
      if (i >= 0) this.visited.splice(i, 1);
    },
  },
});
