# Honoka

可演示的知识库 + 对话实验场：Vue3 + Spring Boot（Node 对照），练 **大模型应用 / AI 全栈**。

**现行计划**（唯一）：[`ROADMAP.md`](./ROADMAP.md)  
目录：[`STRUCTURE.md`](./STRUCTURE.md) · 协作：[`AGENTS.md`](./AGENTS.md)

## 本地启动

```bash
# 终端 1 —— 二选一（不要同时占同一 API_PORT）
npm run dev:api-java   # 需 JDK 17+ 与 Maven（推荐）
# npm run dev:api      # Node 对照

# 终端 2
npm run dev:web
```

- Web：http://localhost:5173  
- API：http://127.0.0.1:8788/health（前端经 `/api` 代理）  
- 密钥只放根目录 `.env`，禁止 `VITE_` 暴露模型 Key  

Java 选型：[`docs/后端Java选型.md`](./docs/后端Java选型.md)。下一优先：见 ROADMAP §2–3（默认真模型流式）。
