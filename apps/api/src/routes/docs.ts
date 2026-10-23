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
    const files = await fs.readdir(UPLOADS_DIR);
    const items = await Promise.all(
      files
        .filter((file) => file !== ".gitkeep")
        .map(async (file) => {
          const stats = await fs.stat(path.join(UPLOADS_DIR, file));
          return {
            id: file,
            name: file.replace(/^\d+-/, ""),
            size: stats.size,
            uploadedAt: stats.mtime.toISOString(),
          };
        }),
    );
    return ok(res, items);
  } catch (e) {
    return fail(res, e instanceof Error ? e.message : "列表失败", 500);
  }
});
docsRouter.post("/upload", (req, res) => {
  upload.single("file")(req, res, async (err) => {
    if (err) {
      return fail(res, err.message || "上传失败");
    }
    // 校验 req.file；name 与列表一致（去掉时间戳前缀）
    if (!req.file) {
      return fail(res, "文件不存在");
    }
    const item: DocItem = {
      id: req.file.filename,
      name: req.file.filename.replace(/^\d+-/, ""),
      size: req.file.size,
      uploadedAt: new Date().toISOString(),
    };
    return ok(res, item, "上传成功");
  });
});

docsRouter.delete("/:id", async (req, res) => {
  try {
    // basename 防 ../ 穿越；禁止删仓库占位文件 .gitkeep
    const id = path.basename(req.params.id).trim();
    if (!id || id === ".gitkeep") {
      return fail(res, "禁止删除该文件");
    }
    await fs.unlink(path.join(UPLOADS_DIR, id));
    return ok(res, true, "删除成功");
  } catch (e) {
    return fail(res, e instanceof Error ? e.message : "删除失败", 404);
  }
});

docsRouter.get("/:id/raw", async (req, res) => {
  try {
    // 读 utf8，供预览 / 后续 RAG；同样防穿越，不读 .gitkeep
    const id = path.basename(req.params.id).trim();
    if (!id || id === ".gitkeep") {
      return fail(res, "文件不存在", 404);
    }
    const text = await fs.readFile(path.join(UPLOADS_DIR, id), "utf8");
    return ok(res, { id, text });
  } catch (e) {
    return fail(res, e instanceof Error ? e.message : "读取失败", 404);
  }
});
