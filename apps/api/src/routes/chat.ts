/**
 * 对话路由（阶段 A · R1：Mock 流式）——【流式循环请自己写】
 *
 * 练习说明见：docs/R1练习-自己写.md
 *
 * POST /chat/mock-stream  body: { prompt }
 * SSE：data: {"type":"chunk"|"done","text":"..."}\n\n
 */
import { Router } from "express";
import { fail } from "../lib/http.js";

export const chatRouter = Router();

chatRouter.post("/mock-stream", async (req, res) => {
  const prompt = String(req.body?.prompt || "").trim();
  if (!prompt) {
    return fail(res, "请输入问题");
  }

  // ---------- 下面整段【你来写】，写完可删掉这行 fail ----------
  // 1) 拼 Mock 全文 full（包含 prompt）
  // 2) 设置 SSE 头：
  //    Content-Type: text/event-stream; charset=utf-8
  //    Cache-Control: no-cache, no-transform
  //    Connection: keep-alive
  //    X-Accel-Buffering: no
  //    然后 res.flushHeaders?.()
  // 3) setInterval：每次 res.write(`data: ${JSON.stringify({ type:"chunk", text: piece })}\n\n`)
  // 4) 结束：write type:"done"，res.end()，clearInterval
  // 5) req.on("close", () => clearInterval(...))
  const full = `prompt: ${prompt}`;
  res.setHeader("Content-Type", "text/event-stream; charset=utf-8")
  res.setHeader("Cache-Control", "no-cache, no-transform")
  res.setHeader("Connection", "keep-alive")
  res.setHeader("X-Accel-Buffering", "no")
  res.flushHeaders?.()
  req.on("close", () => clearInterval(interval))
  let i = 0;
  let interval: ReturnType<typeof setInterval>
  interval = setInterval(() => {
    if (i < full.length) {
      const piece = full[i++];
      res.write(`data: ${JSON.stringify({ type: "chunk", text: piece })}\n\n`)
      return
    }
    clearInterval(interval)
    res.write(`data: ${JSON.stringify({ type: "done", text: full })}\n\n`)
    res.end()
  }, 50)
});
