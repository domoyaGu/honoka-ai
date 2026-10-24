/**
 * 消费后端 Mock SSE ——【读流循环请自己写】
 *
 * 练习说明见：docs/R1练习-自己写.md
 */
import { getToken } from "@/composables/auth";

export interface StreamHandlers {
  onChunk: (text: string) => void;
  onDone?: (full: string) => void;
  signal?: AbortSignal;
}

export async function streamMockReply(
  prompt: string,
  handlers: StreamHandlers,
): Promise<string> {
  const res = await fetch("/api/chat/mock-stream", {
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

  // 【你来写】
  // const reader = res.body.getReader();
  // 循环 read → TextDecoder → 按 \n\n 拆 SSE → JSON.parse data 行
  // type===chunk → handlers.onChunk；type===done → 结束
  //
  // 返回全文 string
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let full = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const text = decoder.decode(value, { stream: true })
    const lines = text.split('\n\n')
    for (const line of lines) {
      const { type, text } = JSON.parse(line)
      if (type === 'chunk') {
        handlers.onChunk(text)
      } else if (type === 'done') {
        handlers.onDone?.(text)
      }
    }
  }
  return full
}
