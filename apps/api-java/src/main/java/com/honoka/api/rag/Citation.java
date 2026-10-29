package com.honoka.api.rag;

/**
 * 返给前端的引用片段（比 ChunkRecord 更短，适合气泡下方展示）
 */
public record Citation(
    String chunkId,
    String docId,
    String docName,
    String source,
    /** 截断后的片段，便于 UI 预览 */
    String snippet,
    double score) {}
