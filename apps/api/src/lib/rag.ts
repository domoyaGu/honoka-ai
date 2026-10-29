/**
 * 关键词 RAG（Node 对照 Java com.honoka.api.rag）
 *
 * 切片 → data/index/chunks.json → Top-K 检索 → Prompt / Mock 答案
 */
import fs from "node:fs";
import path from "node:path";
import { ensureDir, INDEX_DIR, SAMPLES_DIR, UPLOADS_DIR } from "./paths.js";

const ALLOWED_EXT = new Set([".md", ".txt"]);
const GITKEEP = ".gitkeep";
const CACHE_FILE = "chunks.json";
const MAX_CHARS = Number(process.env.HONOKA_RAG_CHUNK_SIZE || 500);
const OVERLAP = Number(process.env.HONOKA_RAG_CHUNK_OVERLAP || 60);
const DEFAULT_TOP_K = Number(process.env.HONOKA_RAG_TOP_K || 4);
const SNIPPET_MAX = 180;

export interface ChunkRecord {
  id: string;
  docId: string;
  docName: string;
  source: string;
  text: string;
  startOffset: number;
}

export interface Citation {
  chunkId: string;
  docId: string;
  docName: string;
  source: string;
  snippet: string;
  score: number;
}

export interface ScoredChunk {
  chunk: ChunkRecord;
  score: number;
}

let memory: ChunkRecord[] = [];

function extensionOf(name: string) {
  const i = name.lastIndexOf(".");
  return i >= 0 ? name.slice(i).toLowerCase() : "";
}

function chunkText(raw: string): string[] {
  const normalized = raw.replace(/\r\n/g, "\n").trim();
  if (!normalized) return [];
  const paragraphs = normalized.split(/(?=^#{1,6}\s)|\n\s*\n/m);
  const out: string[] = [];
  const max = Math.max(64, MAX_CHARS);
  const overlap = Math.max(0, Math.min(OVERLAP, Math.floor(max / 2)));
  const step = Math.max(1, max - overlap);
  for (const para of paragraphs) {
    const p = para.trim();
    if (!p) continue;
    if (p.length <= max) {
      out.push(p);
      continue;
    }
    for (let i = 0; i < p.length; i += step) {
      const piece = p.slice(i, Math.min(p.length, i + max)).trim();
      if (piece) out.push(piece);
      if (i + max >= p.length) break;
    }
  }
  return out;
}

function ingestDir(dir: string, source: string, out: ChunkRecord[]) {
  if (!fs.existsSync(dir)) return;
  for (const name of fs.readdirSync(dir).sort()) {
    if (name === GITKEEP) continue;
    const file = path.join(dir, name);
    if (!fs.statSync(file).isFile()) continue;
    if (!ALLOWED_EXT.has(extensionOf(name))) continue;
    const text = fs.readFileSync(file, "utf8");
    const pieces = chunkText(text);
    const display = name.replace(/^\d+-/, "");
    let offset = 0;
    pieces.forEach((piece, i) => {
      out.push({
        id: `${source}:${name}#${i}`,
        docId: name,
        docName: display,
        source,
        text: piece,
        startOffset: offset,
      });
      offset += piece.length;
    });
  }
}

function cachePath() {
  return path.join(INDEX_DIR, CACHE_FILE);
}

export function loadOrRebuild(): number {
  ensureDir(INDEX_DIR);
  const cache = cachePath();
  if (fs.existsSync(cache)) {
    try {
      memory = JSON.parse(fs.readFileSync(cache, "utf8")) as ChunkRecord[];
      return memory.length;
    } catch {
      /* fall through */
    }
  }
  return rebuild().chunkCount;
}

export function rebuild() {
  ensureDir(SAMPLES_DIR);
  ensureDir(UPLOADS_DIR);
  ensureDir(INDEX_DIR);
  const built: ChunkRecord[] = [];
  ingestDir(SAMPLES_DIR, "samples", built);
  ingestDir(UPLOADS_DIR, "uploads", built);
  memory = built;
  fs.writeFileSync(cachePath(), JSON.stringify(built, null, 2), "utf8");
  return {
    chunkCount: built.length,
    samplesDir: SAMPLES_DIR,
    uploadsDir: UPLOADS_DIR,
    cacheFile: cachePath(),
  };
}

export function chunkCount() {
  return memory.length;
}

function tokenize(raw: string): Set<string> {
  const tokens = new Set<string>();
  const q = (raw || "").toLowerCase();
  for (const m of q.matchAll(/[A-Za-z0-9_./-]{2,}/g)) {
    tokens.add(m[0]);
  }
  for (const m of q.matchAll(/[\u4e00-\u9fff]{2,}/g)) {
    const run = m[0];
    for (let i = 0; i < run.length - 1; i++) {
      tokens.add(run.slice(i, i + 2));
    }
    if (run.length >= 3) tokens.add(run);
  }
  return tokens;
}

function scoreText(text: string, tokens: Set<string>): number {
  const hay = text.toLowerCase();
  let s = 0;
  for (const t of tokens) {
    let from = 0;
    while (true) {
      const idx = hay.indexOf(t, from);
      if (idx < 0) break;
      s += 1;
      from = idx + t.length;
    }
  }
  return s;
}

export function retrieve(query: string, topK?: number): ScoredChunk[] {
  if (!memory.length) loadOrRebuild();
  const tokens = tokenize(query);
  if (!tokens.size) return [];
  const k = Math.max(1, topK || DEFAULT_TOP_K);
  const scored: ScoredChunk[] = [];
  for (const chunk of memory) {
    const score = scoreText(chunk.text, tokens);
    if (score > 0) scored.push({ chunk, score });
  }
  scored.sort((a, b) => b.score - a.score || a.chunk.id.localeCompare(b.chunk.id));
  return scored.slice(0, k);
}

function shorten(text: string, max = SNIPPET_MAX) {
  const t = (text || "").replace(/\s+/g, " ").trim();
  return t.length <= max ? t : `${t.slice(0, max)}…`;
}

export function toCitations(hits: ScoredChunk[]): Citation[] {
  return hits.map((h) => ({
    chunkId: h.chunk.id,
    docId: h.chunk.docId,
    docName: h.chunk.docName,
    source: h.chunk.source,
    snippet: shorten(h.chunk.text),
    score: h.score,
  }));
}

export function buildPrompt(question: string, hits: ScoredChunk[]) {
  const lines = [
    "你是 Honoka 知识库助手。只根据【检索上下文】回答；若上下文不足，明确说明「资料不足」。回答时用 [1][2] 标注引用。",
    "",
    "【检索上下文】",
  ];
  if (!hits.length) {
    lines.push("（无命中）");
  } else {
    hits.forEach((h, i) => {
      lines.push(`[${i + 1}] 来源=${h.chunk.docName} (${h.chunk.source})`);
      lines.push(h.chunk.text);
      lines.push("");
    });
  }
  lines.push("【用户问题】", question.trim());
  return lines.join("\n");
}

export function buildMockAnswer(question: string, hits: ScoredChunk[]) {
  if (!hits.length) {
    return `资料不足：关键词检索未命中相关切片。可先上传/确认 data/samples 与 data/uploads 中的 .md/.txt，或调用 POST /rag/rebuild 重建索引后再问：「${question.trim()}」`;
  }
  const parts = [
    `（RAG Mock）根据检索上下文，关于「${question.trim()}」：`,
    "",
  ];
  hits.forEach((h, i) => {
    parts.push(`[${i + 1}] ${h.chunk.docName}：${shorten(h.chunk.text)}`);
  });
  parts.push("", "以上为关键词命中摘要；接入真模型后将基于同一 Prompt 生成完整答案。");
  return parts.join("\n");
}

// 启动时尝试加载缓存
loadOrRebuild();
