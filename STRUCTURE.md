# 目录说明

本仓按现行计划 **`ROADMAP.md`** 推进：**第 1 跳**业务全栈+AI → **第 2 跳**大模型应用（主攻）→ **第 3 跳** Agent 加厚。

```
honoka/
├── apps/
│   ├── web/              # Vue3：文档库、AI 对话（Mock/RAG/Agent）
│   ├── api-java/         # Spring Boot：REST + SSE + RAG + Agent（唯一后端）
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
│   ├── job-gap-checklist.md   # JD 缺口对照（服从 ROADMAP 三跳）
│   └── career-plan.md         # 已归档 → ROADMAP
├── fixtures/
│   └── golden-questions.json
├── blog/                 # 阶段复盘写稿 + 掘金/博客园本机发布（.env 勿提交）
│   ├── SKILL.md
│   ├── scripts/publish.py
│   ├── cnblogs/
│   ├── templates/
│   ├── references/
│   └── out/
├── AGENTS.md
├── ROADMAP.md            # 【现行唯一计划：第 1/2/3 跳】
├── STRUCTURE.md
├── README.md
├── .env.example
└── .gitignore
```


## 启动

| 命令 | 说明 |
|------|------|
| `npm run dev:api-java` | 后端（JDK 17+、Maven） |
| `npm run dev:web` | 前端 |
| `npm run lab:py` | Python Agent lab（需先起 Java） |

后端仅 `apps/api-java`（端口 `API_PORT`，默认 8788）。
