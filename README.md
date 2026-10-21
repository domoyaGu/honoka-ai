# Honoka（AI 全栈 → 可扩展示例后台）

> **阶段 A（约 1 个月）**：文档 + 流式问答（带引用）+ 可评测 —— AI 全栈作品。  
> **阶段 B（不限时）**：鉴权权限、前端示例、DB → 定时 → 缓存/队列；并含 **微前端了解 + 可选主子应用 demo**（见 ROADMAP §B4）。

计划详见 [`ROADMAP.md`](./ROADMAP.md)；目录见 [`STRUCTURE.md`](./STRUCTURE.md)。  
协作约定（**关键逻辑自己写**，Agent 搭框架/写重复代码）：[`AGENTS.md`](./AGENTS.md)。  
R1 练习步骤：[`docs/R1练习-自己写.md`](./docs/R1练习-自己写.md)。

## 阶段 A · Must

1. 文档上传/管理  
2. 流式回答 + **引用片段**  
3. 密钥只在服务端  
4. 黄金问 / 单测可回归  
5. 2 分钟演示说明  

## 技术选型（初定）

| 层 | 选型 |
|----|------|
| 前端 | Vue3 + Vite + TypeScript + Element Plus |
| 后端 | Node（Express/Fastify）；Agent/RAG 放在 `apps/api` |
| AI | OpenAI 兼容 API（可本机 Ollama） |

## 本地启动（R0）

```bash
# 终端 1
npm run dev:api

# 终端 2
npm run dev:web
```

- Web：http://localhost:5173（先登录，mock：`admin` / `admin123`）  
- API：http://127.0.0.1:8788/health（前端经 `/api` 代理）  
- 前端工程对照 jiayou：`request` / Pinia / `permission` / Admin 布局（顶栏·侧栏·Tags）

## 安全

- Key 只放 `.env`；演示用 `data/samples/` 自造内容，勿提交敏感原文。  
