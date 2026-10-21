/**
 * 前端入口（对照 jiayou `main.js` 精简版）
 * 顺序：创建 app → 全局图标 → Pinia → Router → ElementPlus → 守卫 → mount
 */
import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";

import App from "./App.vue";
import { router } from "./router";
import "./styles/main.css";

const app = createApp(App);

// 全局注册 EP 图标，模板里可直接 <House /> 或侧栏按名字动态 component
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(createPinia());
app.use(router);
app.use(ElementPlus, { locale: zhCn });

// 动态 import：保证 pinia 已 use 再执行 permission 里的 useUserStore
void import("./permission");

app.mount("#app");
