/**
 * RAG 辅助路由 —— 对照 Java RagController
 */
import { Router } from "express";
import { fail, ok } from "../lib/http.js";
import {
  buildPrompt,
  chunkCount,
  rebuild,
  retrieve,
  toCitations,
} from "../lib/rag.js";

export const ragRouter = Router();

ragRouter.post("/rebuild", (_req, res) => {
  return ok(res, rebuild(), "索引已重建");
});

ragRouter.get("/stats", (_req, res) => {
  return ok(res, { chunkCount: chunkCount() });
});

ragRouter.get("/search", (req, res) => {
  const q = String(req.query.q || "").trim();
  if (!q) return fail(res, "请提供查询参数 q");
  const kRaw = req.query.k;
  const k = kRaw != null && String(kRaw) !== "" ? Number(kRaw) : undefined;
  const hits = retrieve(q, Number.isFinite(k) ? k : undefined);
  return ok(res, {
    query: q,
    citations: toCitations(hits),
    promptPreview: buildPrompt(q, hits),
  });
});
