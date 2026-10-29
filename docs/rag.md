# RAG：切片 · 关键词检索 · 引用（本仓实现）

> 对应 `ROADMAP` 主线 1（全栈闭环）与主线 2 §3.3。  
> 阶段目标：先跑通 **chunk → retrieve → prompt → citations**；向量库可后接，不挡演示。

## 1. 数据流（口述用）

```
samples/ + uploads/（.md / .txt）
  → TextChunker 按标题/空行切段，过长再滑窗
  → 写入内存 + data/index/chunks.json
  → KeywordRetriever Top-K
  → RagPromptBuilder 拼 Prompt / Mock 答案
  → POST /chat/rag-mock-stream（SSE：meta 引用 → chunk → done）
  → 对话页气泡下方展示引用片段
```

真模型阶段：把 `buildPrompt` 的结果交给 LLM，仍用同一套 citations 结构。

## 2. 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat/rag-mock-stream` | Body `{ prompt, topK? }`；SSE，首包 `type=meta` 带 `citations` |
| POST | `/chat/mock-stream` | 原纯 Mock，不含检索 |
| POST | `/rag/rebuild` | 重建索引（上传新文档后建议调一次） |
| GET | `/rag/stats` | `{ chunkCount }` |
| GET | `/rag/search?q=&k=` | 调试命中与 `promptPreview`（非流式） |

统一业务包装（非 SSE）：`{ code, data, message }`。

SSE 事件示例：

```text
data: {"type":"meta","citations":[{"chunkId":"...","docName":"...","snippet":"...","score":3}],"promptChars":1200}

data: {"type":"chunk","text":"（"}

data: {"type":"done","text":"...全文...","citations":[...]}
```

## 3. 切片与检索（阶段 A 取舍）

- **切片**：优先 Markdown 标题 / 空行；单段超过 `honoka.rag-chunk-size`（默认 500）再滑窗，重叠默认 60。  
- **检索**：拉丁词按正则；中文连续字串用 bigram + 整段；按出现次数打分取 Top-K。  
- **空命中**：Mock 明确返回「资料不足」，避免假装知道。  
- **未做**：向量嵌入、rerank、异步队列建索引、鉴权 RBAC。

配置（Java `application.yml` / 环境变量）：

- `HONOKA_SAMPLES_DIR` / `HONOKA_UPLOADS_DIR` / `HONOKA_INDEX_DIR`
- `HONOKA_RAG_CHUNK_SIZE` / `HONOKA_RAG_CHUNK_OVERLAP` / `HONOKA_RAG_TOP_K`

密钥仍只放服务端 `.env`，禁止 `VITE_` 暴露模型 Key。

## 4. 手测清单

1. 起 Java：`npm run dev:api-java`（勿与 Node 同占端口）。  
2. `POST /rag/rebuild` → `chunkCount > 0`。  
3. `GET /rag/search?q=后端推荐` → 能看到 fullstack-kb / welcome 相关 snippet。  
4. 前端「AI 对话」保持 RAG 模式，发送：`Honoka 的后端推荐用哪个？端口是多少？`  
5. 应逐字出字，气泡下出现「引用片段」；点停止可中断。  
6. 上传新 `.md` 后执行 rebuild，再问新内容相关问题。

## 5. 代码位置

| 端 | 路径 |
|----|------|
| Java（推荐） | `apps/api-java/.../rag/*`，`ChatController#ragMockStream` |
| Node 对照 | `apps/api/src/lib/rag.ts`，`routes/chat.ts` / `routes/rag.ts` |
| 前端 | `useMockChatStream.ts`，`views/chat/index.vue` |
| 索引缓存 | `data/index/chunks.json`（gitignore，保留 `.gitkeep`） |

## 6. 下一步（能力加深，非本文件范围）

- 真模型流式：同一 Prompt + citations 结构。  
- 可选向量 / 混合检索；`fixtures/` 黄金问回归。  
- Agent：把「检索」收成工具，白名单 + 步数护栏（见 `docs/agent-playbook.md`，待写）。
