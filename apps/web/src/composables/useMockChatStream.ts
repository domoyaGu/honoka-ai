/**
 * 消费后端 Mock / RAG / Agent SSE
 *
 * 事件：`data: {"type":"tool"|"meta"|"chunk"|"done", ...}\n\n`
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

export interface AgentToolStep {
  tool: string;
  args: unknown;
  result: unknown;
}

export interface StreamHandlers {
  onChunk: (text: string) => void;
  onCitations?: (citations: Citation[]) => void;
  /** Agent：每完成一次工具调用推一条 */
  onToolStep?: (step: AgentToolStep) => void;
  onDone?: (full: string) => void;
  signal?: AbortSignal;
}

interface SsePayload {
  type: "tool" | "meta" | "chunk" | "done";
  text?: string;
  citations?: Citation[];
  promptChars?: number;
  step?: AgentToolStep;
  steps?: AgentToolStep[];
}

export type StreamMode = "mock" | "rag" | "agent";

export async function streamMockReply(
  prompt: string,
  handlers: StreamHandlers,
  mode: StreamMode = "rag",
): Promise<string> {
  const path =
    mode === "agent"
      ? "/api/chat/agent-mock-stream"
      : mode === "rag"
        ? "/api/chat/rag-mock-stream"
        : "/api/chat/mock-stream";
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

  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const json = (await res.json()) as { code?: number; message?: string };
    throw new Error(json.message || "请求失败");
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let full = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const parts = buffer.split("\n\n");
    buffer = parts.pop() ?? "";

    for (const part of parts) {
      const line = part.trim();
      if (!line.startsWith("data:")) continue;

      const raw = line.slice(5).trim();
      if (!raw || raw === "[DONE]") continue;

      let payload: SsePayload;
      try {
        payload = JSON.parse(raw) as SsePayload;
      } catch {
        continue;
      }

      if (payload.type === "tool" && payload.step) {
        handlers.onToolStep?.(payload.step);
      } else if (payload.type === "meta" && payload.citations) {
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
        if (payload.steps) {
          for (const s of payload.steps) {
            handlers.onToolStep?.(s);
          }
        }
        handlers.onDone?.(finalText);
        return finalText;
      }
    }
  }

  return full;
}
