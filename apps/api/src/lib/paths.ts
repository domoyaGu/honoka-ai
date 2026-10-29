/**
 * 仓库内数据目录路径
 * apps/api/src/lib → 上四级到仓根
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

/** Honoka 仓库根目录 */
export const ROOT_DIR = path.resolve(__dirname, "../../../../");

/** 用户上传文档（gitignore 内容，保留 .gitkeep） */
export const UPLOADS_DIR = path.join(ROOT_DIR, "data", "uploads");

/** 可提交的演示样例（RAG 与 uploads 一起入库） */
export const SAMPLES_DIR = path.join(ROOT_DIR, "data", "samples");

/** 检索索引缓存（gitignore；与 Java 共用 chunks.json） */
export const INDEX_DIR = path.join(ROOT_DIR, "data", "index");

/** 确保目录存在，避免首次上传报错 */
export function ensureDir(dir: string) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}
