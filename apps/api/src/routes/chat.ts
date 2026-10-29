/**
 * 对话路由：Mock SSE + RAG Mock SSE
 *
 * POST /chat/mock-stream      body: { prompt }
 * POST /chat/rag-mock-stream  body: { prompt, topK? }
 * SSE：data: {"type":"meta"|"chunk"|"done", ...}\n\n
 */
import { Router } from "express";
import { fail } from "../lib/http.js";
import {
  buildMockAnswer,
  buildPrompt,
  chunkCount,
  rebuild,
  retrieve,
  toCitations,
  type Citation,
} from "../lib/rag.js";

export const chatRouter = Router();

function startSse(
  res: import("express").Response,
  req: import("express").Request,
  full: string,
  citations: Citation[] | null,
  promptChars?: number,
) {
  res.setHeader("Content-Type", "text/event-stream; charset=utf-8");
  res.setHeader("Cache-Control", "no-cache, no-transform");
  res.setHeader("Connection", "keep-alive");
  res.setHeader("X-Accel-Buffering", "no");
  res.flushHeaders?.();

  let i = 0;
  let phase = citations ? 0 : 1;
  const interval = setInterval(() => {
    if (phase === 0 && citations) {
      const meta: Record<string, unknown> = { type: "meta", citations };
      if (promptChars != null) meta.promptChars = promptChars;
      res.write(`data: ${JSON.stringify(meta)}\n\n`);
      phase = 1;
      return;
    }
    if (i < full.length) {
      const piece = full[i++];
      res.write(`data: ${JSON.stringify({ type: "chunk", text: piece })}\n\n`);
      return;
    }
    clearInterval(interval);
    const done: Record<string, unknown> = { type: "done", text: full };
    if (citations) done.citations = citations;
    res.write(`data: ${JSON.stringify(done)}\n\n`);
    res.end();
  }, 50);

  req.on("close", () => clearInterval(interval));
}

chatRouter.post("/mock-stream", (req, res) => {
  const prompt = String(req.body?.prompt || "").trim();
  if (!prompt) {
    return fail(res, "请输入问题");
  }
  startSse(res, req, `prompt: ${prompt}`, null);
});

chatRouter.post("/rag-mock-stream", (req, res) => {
  const prompt = String(req.body?.prompt || "").trim();
  if (!prompt) {
    return fail(res, "请输入问题");
  }
  const topKRaw = req.body?.topK;
  const topK =
    typeof topKRaw === "number"
      ? topKRaw
      : topKRaw != null
        ? Number(topKRaw)
        : undefined;

  if (chunkCount() === 0) {
    try {
      rebuild();
    } catch {
      /* Mock 降级话术 */
    }
  }
  const hits = retrieve(prompt, Number.isFinite(topK) ? topK : undefined);
  const assembled = buildPrompt(prompt, hits);
  const answer = buildMockAnswer(prompt, hits);
  const citations = toCitations(hits);
  startSse(res, req, answer, citations, assembled.length);
});
