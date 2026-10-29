# 目录说明

本仓服务三条主线（见 `ROADMAP.md`）：**全栈底座**、**大模型应用开发**、**Agent 使用与微调**。  
不设周次计划；目录按「可运行产品 + 可沉淀知识」划分。

```
honoka/
├── apps/
│   ├── web/              # Vue3 主应用：布局壳、文档库、AI 对话、（可选）后台页
│   ├── api-java/         # Spring Boot 3 后端（推荐）：REST + SSE + 后续 RAG/工具
│   ├── api/              # Node Express 对照实现（勿与 Java 同占 API_PORT）
│   └── sub-demo/         # 可选：微前端子应用实验（与 AI 主路径解耦，默认不建）
├── data/
│   ├── samples/          # 可提交的演示/知识库样例（自造内容）
│   ├── uploads/          # 运行时上传（gitignore；两后端共用）
│   └── index/            # 检索索引缓存（gitignore）
├── docs/                 # 知识沉淀（与代码同等重要）
│   ├── 后端Java选型.md   # 已有
│   ├── llm-basics.md     # 建议：模型 API / 流式 / 参数
│   ├── rag.md            # 建议：切片检索引用评测
│   ├── agent-playbook.md # 建议：Agent 场景决策与用法
│   ├── finetune-notes.md # 建议：微调实验结论
│   └── 微前端了解.md     # 可选
├── fixtures/             # 黄金问 / 评测集
├── AGENTS.md             # 与 Cursor Agent 协作约定
├── ROADMAP.md            # 能力地图与主线
├── STRUCTURE.md
├── README.md
├── .env.example          # 仅服务端密钥；禁止 VITE_ 暴露模型 Key
└── .gitignore
```

## 模块怎么用

| 路径 | 主线对应 |
|------|----------|
| `apps/web` + `apps/api-java` | 全栈底座；Agent/RAG 的人机界面与 API |
| `data/samples`、`data/uploads`、`data/index` | RAG 语料与索引 |
| `fixtures` | 大模型/Agent 评测 |
| `docs/*` | 大模型开发、Agent 决策清单、微调笔记 |
| 仓外 GPU/云实验 | 微调实操；结论写回 `docs/finetune-notes.md` |

## 后端选择

- 日常推荐：`npm run dev:api-java`（需 JDK 17+、Maven）。  
- 对照/回滚：`npm run dev:api`。  
- 详情：`docs/后端Java选型.md`。
