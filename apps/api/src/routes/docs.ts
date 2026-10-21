/**
 * 文档库路由（阶段 A · R1）——框架已搭，【关键逻辑请自己写】
 *
 * 练习说明见：docs/R1练习-自己写.md
 *
 * GET    /docs
 * POST   /docs/upload   字段名 file；仅 .md / .txt
 * DELETE /docs/:id
 * GET    /docs/:id/raw  （可选，预览/后续 RAG）
 */
import { Router } from "express";
import fs from "node:fs/promises";
import path from "node:path";
import multer from "multer";
import { fail, ok } from "../lib/http.js";
import { UPLOADS_DIR, ensureDir } from "../lib/paths.js";

ensureDir(UPLOADS_DIR);

const ALLOWED_EXT = new Set([".md", ".txt"]);

/** 以下 multer 配置属样板基建，可先读懂再改；业务在下方各个 handler */
const storage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    ensureDir(UPLOADS_DIR);
    cb(null, UPLOADS_DIR);
  },
  filename: (_req, file, cb) => {
    const base = path
      .basename(file.originalname)
      .replace(/[\\/]/g, "_")
      .replace(/\s+/g, "_");
    cb(null, `${Date.now()}-${base}`);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 2 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if (!ALLOWED_EXT.has(ext)) {
      cb(new Error("仅支持 .md / .txt"));
      return;
    }
    cb(null, true);
  },
});

export const docsRouter = Router();

export interface DocItem {
  id: string;
  name: string;
  size: number;
  uploadedAt: string;
}

docsRouter.get("/", async (_req, res) => {
  // try {
  //   // 【你来写】读 UPLOADS_DIR，拼 DocItem[]，ok(res, items)
  //   // 提示：fs.readdir + fs.stat；跳过 .gitkeep
  //   fs.readdir(UPLOADS_DIR).then(files => {
  //   void fs;
  //   void path;
  //   void UPLOADS_DIR;
  //   return fail(res, "TODO: 实现文档列表 GET /docs", 501);
  // } catch (e) {
  //   return fail(res, e instanceof Error ? e.message : "列表失败", 500);
  // }
  try {
    fs.readdir(UPLOADS_DIR).then(files => {
      const items = files.map(async file => {
        if (file === ".gitkeep") return null;
        const stats = await fs.stat(path.join(UPLOADS_DIR, file));
        return {
          id: file,
          name: file,
          size: stats.size,
          uploadedAt: stats.mtime.toISOString(),
        };
      });
      return ok(res, items);
    })
  } catch (e) {
    return fail(res, e instanceof Error ? e.message : "列表失败", 500);
  }
});
docsRouter.post("/upload", (req, res) => {
  upload.single("file")(req, res, async (err) => {
    if (err) {
      return fail(res, err.message || "上传失败");
    }
    // 【你来写】校验 req.file，组装 DocItem，ok(res, item, "上传成功")
    void req.file;
    void ok;
    return fail(res, "TODO: 实现上传成功响应", 501);
  });
});

docsRouter.delete("/:id", async (req, res) => {
  try {
    // 【你来写】basename 防穿越 → unlink → ok(res, true)
    void req.params.id;
    return fail(res, "TODO: 实现删除 DELETE /docs/:id", 501);
  } catch (e) {
    return fail(res, e instanceof Error ? e.message : "删除失败", 404);
  }
});

docsRouter.get("/:id/raw", async (req, res) => {
  try {
    // 【你来写·可选】读文件 utf8，ok(res, { id, text })
    void req.params.id;
    return fail(res, "TODO: 可选实现 GET /docs/:id/raw", 501);
  } catch (e) {
    return fail(res, e instanceof Error ? e.message : "读取失败", 404);
  }
});
