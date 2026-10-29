# Agent 用法手册（Honoka）

> `ROADMAP` 主线 3 + 苏州岗位常见问法。本仓可演示：对话切 **Agent** 模式。

## 1. 概念（面试 30 秒）

| 概念 | 一句话 |
|------|--------|
| Chat | 只对话，无行动 |
| RAG | 先检索再生成 |
| Agent | 模型（或规则）+ 规划 + **工具** + 护栏，多步达成目标 |
| Workflow | 人定节点；模型填槽或分支 |
| Multi-Agent | 多角色分工，单 Agent 提示爆炸时再拆 |

## 2. 决策清单（面对一堆 Agent 产品时）

1. 目标能否拆成工具步骤？不能 → Chat/RAG。  
2. 错误代价高？→ 工作流 + 人工确认。  
3. 要不要实时环境（浏览器/终端）？→ 选带沙箱的产品。  
4. 知识来源：文档→RAG 工具；API→Function Calling。  
5. 无评测集不上复杂自治。  
6. 写死最大步数与费用。

## 3. 本仓实现

- 工具白名单：`search_docs`、`rag_stats`（`GET /chat/agent/tools`）。  
- 编排：`AgentOrchestrator`（Mock：固定先 stats 再 search）。  
- SSE：`POST /chat/agent-mock-stream` → `type=tool` → `meta` → `chunk` → `done`。  
- 护栏：最多检索一轮；空索引明确失败话术。

真模型阶段：把「选哪个 tool」交给 LLM function calling，执行层仍用白名单。

## 4. 产品选型（Dify / Cursor / 自研）

| 场景 | 建议 |
|------|------|
| 业务方自建工作流、少代码 | Dify / 同类 |
| 改本仓库、开发辅助 | Cursor Agent（范围+护栏） |
| 要深度定制与面试可讲 | 自研如 Honoka（工具+SSE+引用） |

## 5. Python lab

见 `apps/py-labs/README.md`：用 httpx 调本仓 `/rag/search`，演示 LangChain 风格「工具循环」而不绑死版本。
