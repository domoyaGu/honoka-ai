package com.honoka.api.rag;

/**
 * 索引中的一块切片 —— 主线 1 RAG 闭环
 *
 * 持久化到 data/index/chunks.json；检索与引用都围绕本结构。
 */
public record ChunkRecord(
    /** 稳定 id：docId + 序号 */
    String id,
    /** 来源文件名（uploads 存盘名或 samples 文件名） */
    String docId,
    /** 展示名（去掉时间戳前缀） */
    String docName,
    /** 语料来源：samples | uploads */
    String source,
    /** 切片正文 */
    String text,
    /** 在原文中的大致起始偏移（字符） */
    int startOffset) {}
