# Honoka 全栈知识库（演示样例）

> 用途：文档库上传 / 列表自测；对话页 Mock 流式提问；后续 R2 切片检索可用。  
> 内容为自造演示文稿，非真实客户资料。

---

## 1. 项目是什么

Honoka 是一个边学边做的 **AI 全栈** 练习仓：

- 前端：`apps/web`（Vue 3 + TypeScript + Element Plus）
- 后端（推荐）：`apps/api-java`（Spring Boot 3，JDK 17）
- 后端（对照）：`apps/api`（Node Express）
- 数据：`data/samples` 可提交样例；`data/uploads` 运行时上传（gitignore）

阶段 A 目标：文档入库 → 提问 → **流式回答 + 引用片段**。

---

## 2. 本地怎么跑

1. 只起一个后端（端口默认 **8788**，Java 与 Node 勿同时占）：
   - Java：`npm run dev:api-java`
   - Node：`npm run dev:api`
2. 前端：`npm run dev:web`（Vite 把 `/api` 代理到 8788）
3. 浏览器登录后打开「文档库」「AI 对话」

健康检查：`GET http://127.0.0.1:8788/health`

---

## 3. 文档库接口（阶段 A）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/docs` | 列表：`id / name / size / uploadedAt` |
| POST | `/docs/upload` | 表单字段名必须是 `file`；仅 `.md` / `.txt` |
| DELETE | `/docs/{id}` | `id` 用 basename，禁止删 `.gitkeep` |
| GET | `/docs/{id}/raw` | 读原文 utf-8 |

统一业务包装：`{ "code": 200, "data": ..., "message": "ok" }`。  
`code !== 200` 时前端按失败处理（即使 HTTP 仍可能是 200）。

---

## 4. Mock 流式对话

- 路径：`POST /chat/mock-stream`
- Body：`{ "prompt": "你的问题" }`
- 响应：SSE，`Content-Type: text/event-stream`
- 事件行格式：`data: {"type":"chunk"|"done","text":"..."}\n\n`
- 前端用 `fetch` + `ReadableStream` 读；点「停止」用 `AbortController.abort()`，后端应停止推送

当前 Mock 行为（Java）：把全文设为 `prompt: ` + 用户问题，再按小段定时 `chunk`，最后发 `done`。  
用来验证：**逐字出现、停止中断、错误提示**，还不做真实 RAG。

---

## 5. 前后端数据流（口述用）

```
用户输入
  → chat/index.vue onSend
  → useMockChatStream（fetch + signal）
  → Vite 代理 /api → 8788
  → ChatController.mockStream（SseEmitter）
  → 多次 data: chunk
  → data: done
  → 前端 onChunk 追加到 assistant 气泡
```

密钥以后只放服务端 `.env`，禁止 `VITE_` 暴露模型 Key。

---

## 6. 阶段节奏（压缩）

| 周 | 必须看见 |
|----|----------|
| 1 | web + api；文档页；Mock 流式 |
| 2～3 | 检索 + 真流式 + 引用 |
| 4 | 评测可跑；演示能讲清 Abort |

关键业务自己写；Agent 只搭框架。一天一刀。

---

## 7. Mock 流式测试用提问（直接复制）

把下面任一句话粘到「AI 对话」发送，观察是否逐段出现；中途点「停止」应立刻停。

1. `Honoka 的后端推荐用哪个？端口是多少？`
2. `文档上传的表单字段名是什么？支持哪些扩展名？`
3. `Mock SSE 的 chunk 和 done 分别表示什么？`
4. `前端点停止时，AbortController 和后端 SseEmitter 各自做什么？`
5. `请用三句话说明：从输入框到流式气泡的完整数据流。`
6. `阶段 A 成功标准有哪三条？`
7. （压测长度）`请重复讲解文档库四个接口，每个接口两句话，尽量写长一点方便观察流式速度。`

---

## 8. 自检清单

- [ ] 本文件上传到文档库后，列表能看到名称（无时间戳前缀更好）
- [ ] `data/uploads/` 出现带时间戳的文件名
- [ ] 对话页发送短问题能逐字出字
- [ ] 长问题发送中点「停止」，输出中断且不再继续涨字
- [ ] 空问题有友好错误提示（不进入假流式）

全部勾上后，再进入 R2：切片 / 检索 / 拼 Prompt。
