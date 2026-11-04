# Honoka

知识库 + 对话实验场（Vue3 + Spring Boot），按 **第 1 → 2 → 3 跳** 练到可投递。

**现行计划（唯一）**：[`ROADMAP.md`](./ROADMAP.md)  
目录：[`STRUCTURE.md`](./STRUCTURE.md) · 协作：[`AGENTS.md`](./AGENTS.md)

## 本地启动

```bash
# 终端 1
npm run dev:api-java   # JDK 17+、Maven

# 终端 2
npm run dev:web
```

- Web：http://localhost:5173  
- API：http://127.0.0.1:8788/health  
- 密钥只放根目录 `.env`，禁止 `VITE_` 暴露模型 Key  

当前指针：第 2 跳 · 真模型流式（见 ROADMAP）。
