# 岗位缺口 ↔ 本仓补齐清单

> 对照苏州常见 JD（RAG / Agent / 全栈 / 微调）与 Honoka 能力。  
> 「已补」= 本仓可演示或有完整笔记；「需仓外」= 要 GPU/账号/现场经验。

| 缺口（JD 常见） | 状态 | 本仓落点 |
|-----------------|------|----------|
| 关键词 RAG + 引用 + SSE | ✅ | `docs/rag.md`，对话 RAG 模式 |
| 向量 / 混合检索概念 | ✅ 本地 n-gram 伪向量 + hybrid | `HybridRetriever`；`GET /rag/search?mode=` |
| 真 Embedding + Milvus | 📝 笔记 | 下文 §Milvus；接真模型时再换 |
| Agent / Tool Calling | ✅ Mock 编排 | `AgentOrchestrator`，对话 **Agent** 模式 |
| LangChain / LangGraph | ✅ Python lab 形状 | `apps/py-labs/` |
| Dify / 低代码 Agent | 📝 | `docs/agent-playbook.md` §产品选型 |
| Prompt / 流式 / 参数 | ✅ | `docs/llm-basics.md` |
| 微调 SFT/LoRA/DPO | 📝 实验清单 | `docs/finetune-notes.md`（需仓外 GPU） |
| Spring Boot + Vue 全栈 | ✅ | `apps/api-java` + `apps/web` |
| 库 / 鉴权 / 缓存 / MQ | 📝 知识地图 | `docs/backend-ladder.md`（薄落地按需） |
| Python 后端语感 | ✅ lab | `apps/py-labs/agent_rag_lab.py` |
| Cursor / AI 编程工具 | ✅ 用法 | 本仓协作 + playbook |
| 客户现场 FDE | ❌ 非技术 | 简历用「独立交付 Demo」替代 |

## Milvus / 真向量（口述 + 下一步）

1. Embedding：调用 OpenAI `embeddings` 或本地 bge；密钥只放服务端 `.env`。  
2. 写入：chunk 文本 → vector → Milvus collection（或 PGVector）。  
3. 检索：ANN Top-K →（可选）关键词混合 → rerank → 同现有 `Citation` 结构。  
4. 本仓当前用 **字符 n-gram 余弦** 占位同一接口形状（`mode=vector|hybrid`），换真向量时主要改 `HybridRetriever` 实现。

## 建议投递话术（8 年前端 → AI 全栈）

- 自研 Vue3 + Spring Boot 知识库：上传、**混合检索**、流式回答与引用。  
- Agent Mock：工具白名单 `search_docs` / `rag_stats`，可演示多步轨迹。  
- 同步补 Python lab 与微调实验笔记；后端加深按 `backend-ladder` 推进。
