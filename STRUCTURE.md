# 目录说明

本仓服务三条主线（见 `ROADMAP.md`）：**全栈底座 + 后端工程加深**、**大模型应用开发**、**Agent 使用与微调**。

```
honoka/
├── apps/
│   ├── web/              # Vue3：文档库、AI 对话（Mock/RAG/Agent）
│   ├── api-java/         # Spring Boot：REST + SSE + RAG 混合检索 + Agent 工具
│   ├── api/              # Node 对照
│   ├── py-labs/          # Python Agent lab（对接 Java /rag）
│   └── sub-demo/         # 可选微前端（默认不建）
├── data/
│   ├── samples/
│   ├── uploads/
│   └── index/
├── docs/
│   ├── 后端Java选型.md
│   ├── backend-ladder.md
│   ├── llm-basics.md
│   ├── rag.md
│   ├── agent-playbook.md
│   ├── finetune-notes.md
│   └── job-gap-checklist.md   # 岗位缺口对照
├── fixtures/
│   └── golden-questions.json
├── blog/                 # 阶段复盘写稿 + 掘金/博客园本机发布脚本（.env 勿提交）
│   ├── SKILL.md
│   ├── scripts/publish.py
│   ├── cnblogs/          # 博客园页首/页脚/侧栏公告 + 定制 CSS
│   ├── templates/
│   ├── references/
│   └── out/
├── AGENTS.md
├── ROADMAP.md
├── STRUCTURE.md
├── README.md
├── .env.example
└── .gitignore
```


## 启动

| 命令 | 说明 |
|------|------|
| `npm run dev:api-java` | 推荐后端（JDK 17+、Maven） |
| `npm run dev:web` | 前端 |
| `npm run lab:py` | Python Agent lab（需先起 Java） |

勿同时起 Node 与 Java 占同一 `API_PORT`。
