/**
 * 消费后端 Mock / RAG-Mock SSE
 *
 * 对接：`/chat/mock-stream` 或 `/chat/rag-mock-stream`
 * 事件：`data: {"type":"meta"|"chunk"|"done", ...}\n\n`
 * meta / done 可带 citations，供对话页展示引用片段
 * Abort：传入 signal，断开后 reader.read 抛 AbortError，后端停推
 */
import { getToken } from "@/composables/auth";

export interface Citation {
  chunkId: string;
  docId: string;
  docName: string;
  source: string;
  snippet: string;
  score: number;
}

export interface StreamHandlers {
  /** 每个 chunk 的增量文本 */
  onChunk: (text: string) => void;
  /** 收到引用元数据（通常在首包 meta，或 done 兜底） */
  onCitations?: (citations: Citation[]) => void;
  /** 收到 done 时的全文（可选） */
  onDone?: (full: string) => void;
  /** 点「停止」时传入 AbortController.signal */
  signal?: AbortSignal;
}

interface SsePayload {
  type: "meta" | "chunk" | "done";
  text?: string;
  citations?: Citation[];
  promptChars?: number;
}

export type StreamMode = "mock" | "rag";

export async function streamMockReply(
  prompt: string,
  handlers: StreamHandlers,
  mode: StreamMode = "rag",
): Promise<string> {
  const path =
    mode === "rag" ? "/api/chat/rag-mock-stream" : "/api/chat/mock-stream";
  const res = await fetch(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
    },
    body: JSON.stringify({ prompt }),
    signal: handlers.signal,
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(errText || `HTTP ${res.status}`);
  }
  if (!res.body) {
    throw new Error("响应无 body，无法流式读取");
  }

  // 空 prompt 等业务失败时，后端可能返回 JSON 包装而非 SSE
  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const json = (await res.json()) as { code?: number; message?: string };
    throw new Error(json.message || "请求失败");
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  // 半包缓冲：一次 read 可能拆开一条 SSE，或粘多条
  let buffer = "";
  let full = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const parts = buffer.split("\n\n");
    // 最后一段可能不完整，留在 buffer 等下次
    buffer = parts.pop() ?? "";

    for (const part of parts) {
      const line = part.trim();
      if (!line.startsWith("data:")) continue;

      // 兼容 `data:{...}` 与 `data: {...}`
      const raw = line.slice(5).trim();
      if (!raw || raw === "[DONE]") continue;

      let payload: SsePayload;
      try {
        payload = JSON.parse(raw) as SsePayload;
      } catch {
        continue;
      }

      if (payload.type === "meta" && payload.citations) {
        handlers.onCitations?.(payload.citations);
      } else if (payload.type === "chunk" && payload.text) {
        full += payload.text;
        handlers.onChunk(payload.text);
      } else if (payload.type === "done") {
        const finalText = payload.text || full;
        full = finalText;
        if (payload.citations) {
          handlers.onCitations?.(payload.citations);
        }
        handlers.onDone?.(finalText);
        return finalText;
      }
    }
  }

  return full;
}
