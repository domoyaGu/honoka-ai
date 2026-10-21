import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

/**
 * Vite 配置（对照 jiayou `vite.config.js` 精简）
 *
 * - alias `@` → src
 * - AutoImport：少写 vue/router/pinia 的 import
 * - Components deep:false：只自动注册 src/components 一级 *.vue
 * - proxy `/api` → 本机 API（端口来自 VITE_API_PORT）
 *
 * 阶段 A 故意不加：legacy、windi、visualizer、imagemin（需要时再开一刀）
 */
export default defineConfig(({ mode }) => {
  // 第三参 "" = 读全部前缀；浏览器里仍然只有 VITE_ 会进 import.meta.env
  const env = loadEnv(mode, process.cwd(), "");
  const apiPort = env.VITE_API_PORT || "8788";

  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ["vue", "vue-router", "pinia"],
        resolvers: [ElementPlusResolver()],
        dts: "src/auto-imports.d.ts",
      }),
      Components({
        dirs: ["src/components"],
        deep: false,
        resolvers: [ElementPlusResolver()],
        dts: "src/components.d.ts",
      }),
    ],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    server: {
      host: "0.0.0.0", // 局域网也可访问
      port: 5173,
      open: true,
      proxy: {
        // 浏览器只打同源 /api/*，由此转发到 Express，避免 CORS
        "/api": {
          target: `http://127.0.0.1:${apiPort}`,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ""),
        },
      },
    },
  };
});
