# 目录说明

两阶段共用同一仓（见 `ROADMAP.md`）：

- **阶段 A**：先把 `apps/web` + `apps/api` + `data/*` + `fixtures` 跑通 AI 闭环  
- **阶段 B**：同目录扩展权限/示例/任务/队列；可选 `apps/sub-demo` 做微前端子应用（见 ROADMAP §B4）  

```
honoka/
├── apps/
│   ├── web/          # 主应用 / 可选微前端基座（A：壳+AI；B：鉴权与示例）
│   ├── api/          # 后端（A：RAG/流式；B：定时/缓存/队列）
│   └── sub-demo/     # （B·MF3 可选）子应用；阶段 A 不建
├── data/
│   ├── samples/      # 可提交的演示样例文档
│   ├── uploads/      # 运行时上传（内容 gitignore）
│   └── index/        # 检索索引缓存（内容 gitignore）
├── docs/             # 架构 / 演示说明
├── fixtures/         # 评测黄金集等
├── AGENTS.md
├── ROADMAP.md
├── README.md
├── STRUCTURE.md
├── .env.example
└── .gitignore
```

下一刀：R0.2 脚手架填满 `apps/web`、`apps/api`。
