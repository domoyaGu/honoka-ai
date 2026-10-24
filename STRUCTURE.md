# 目录说明

两阶段共用同一仓（见 `ROADMAP.md`）：

- **阶段 A**：先把 `apps/web` + 后端 + `data/*` + `fixtures` 跑通 AI 闭环  
- **后端**：`apps/api-java`（Spring Boot，推荐）与 `apps/api`（Node，对照）二选一占 8788  
- **阶段 B**：同目录扩展权限/示例/任务/队列；可选 `apps/sub-demo` 做微前端子应用（见 ROADMAP §B4）  

```
honoka/
├── apps/
│   ├── web/          # 主应用 / 可选微前端基座（A：壳+AI；B：鉴权与示例）
│   ├── api-java/     # Java 后端（Spring Boot 3；阶段 A 推荐主路径）
│   ├── api/          # Node 后端（Express；对照 / 回滚）
│   └── sub-demo/     # （B·MF3 可选）子应用；阶段 A 不建
├── data/
│   ├── samples/      # 可提交的演示样例文档
│   ├── uploads/      # 运行时上传（内容 gitignore；两后端共用）
│   └── index/        # 检索索引缓存（内容 gitignore）
├── docs/             # 含 后端Java选型.md
├── fixtures/         # 评测黄金集等
├── AGENTS.md
├── ROADMAP.md
├── README.md
├── STRUCTURE.md
├── .env.example
└── .gitignore
```

Java 选型说明：`docs/后端Java选型.md`。
