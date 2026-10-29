# RAG：切片 · 混合检索 · 引用（本仓实现）

> 对应 `ROADMAP` 主线 1 / 2。关键词 + 本地伪向量混合；真 Milvus 见 `docs/job-gap-checklist.md`。

## 1. 数据流

```
samples/ + uploads/
  → TextChunker
  → data/index/chunks.json
  → HybridRetriever（keyword | vector | hybrid）
  → RagPromptBuilder
  → SSE：meta 引用 → chunk → done
```

Agent 路径额外：`tool` 事件（`search_docs` / `rag_stats`）→ 同上。

## 2. 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat/rag-mock-stream` | RAG Mock 流式 |
| POST | `/chat/agent-mock-stream` | Agent：先 tool 再回答 |
| POST | `/chat/mock-stream` | 纯 Mock |
| GET | `/chat/agent/tools` | 工具白名单 |
| POST | `/rag/rebuild` | 重建索引 |
| GET | `/rag/stats` | chunkCount + retrieveMode |
| GET | `/rag/search?q=&k=&mode=` | mode=`keyword`/`vector`/`hybrid` |

## 3. 检索模式

- **keyword**：词/ bigram 出现次数。  
- **vector**：字符 n-gram TF 余弦（零依赖占位「向量检索」）。  
- **hybrid**：归一化关键词分 + 余弦加权（默认 `HONOKA_RAG_MODE=hybrid`）。

换真 Embedding + Milvus 时，保持 `Citation` 与 SSE 形状，替换 `HybridRetriever` 内部即可。

## 4. 手测

1. `npm run dev:api-java`  
2. `POST /rag/rebuild`  
3. `GET /rag/search?q=后端推荐&mode=hybrid`  
4. 前端 RAG / Agent 模式提问；Agent 应先出「工具调用」再出字。  
5. Python：`cd apps/py-labs && python agent_rag_lab.py`

## 5. 代码位置

| 端 | 路径 |
|----|------|
| Java | `rag/*`，`agent/AgentOrchestrator`，`ChatController` |
| 前端 | `useMockChatStream.ts`，`views/chat/index.vue` |
| Python | `apps/py-labs/` |
| 评测 | `fixtures/golden-questions.json` |
