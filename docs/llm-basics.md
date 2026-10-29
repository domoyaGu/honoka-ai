# 大模型应用基础（Honoka）

> `ROADMAP` 主线 2。密钥只进服务端 `.env`，禁止 `VITE_` 暴露。

## API 形态

- Chat Completions（messages）vs Completions；多模态另议。  
- OpenAI 兼容：`OPENAI_API_BASE` + `OPENAI_API_KEY` + `OPENAI_MODEL`（见根 `.env.example`）。  
- Ollama 示例：`http://127.0.0.1:11434/v1`。

## 关键参数

| 参数 | 作用 |
|------|------|
| temperature | 随机性；知识库问答宜偏低 |
| top_p | 核采样；常与 temperature 二选一调 |
| max_tokens | 输出上限；控成本与截断 |
| stop | 停止序列 |
| stream | SSE 增量；本仓对话已练 Abort |

## 结构化输出

- JSON mode / schema；失败则校验重试。  
- Honoka 业务包装与模型 JSON 分开：HTTP `{code,data,message}` ≠ 模型正文。

## Prompt

- 系统提示：角色、约束、引用格式 `[1][2]`。  
- 用户提示：问题；RAG 时把检索上下文放系统或独立块。  
- 防注入：工具结果与用户输入分区；不可信内容勿当指令。

## 流式与 Abort

```
fetch → getReader → data: chunk → UI 追加
停止 → AbortController.abort → 后端停推
```

见 `useMockChatStream.ts` 与 Java `SseEmitter`。

## 成本与安全

- Token ≈ 延迟与费用；缓存重复前缀。  
- 日志脱敏；密钥轮换；限流。
