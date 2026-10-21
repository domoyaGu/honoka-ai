/**
 * Honoka API 入口（阶段 A）
 *
 * - /health
 * - /docs/*   文档上传列表删除（R1）
 * - /chat/*   Mock SSE 流式（R1）；真模型后续仍挂在本进程
 */
import cors from "cors";
import dotenv from "dotenv";
import express from "express";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chatRouter } from "./routes/chat.js";
import { docsRouter } from "./routes/docs.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

dotenv.config({ path: path.resolve(__dirname, "../../../.env") });
dotenv.config();

const app = express();
const port = Number(process.env.API_PORT || 8788);

app.use(cors());
app.use(express.json({ limit: "2mb" }));

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    service: "honoka-api",
    time: new Date().toISOString(),
  });
});

app.use("/docs", docsRouter);
app.use("/chat", chatRouter);

app.listen(port, () => {
  console.log(`[honoka-api] http://127.0.0.1:${port}`);
});
