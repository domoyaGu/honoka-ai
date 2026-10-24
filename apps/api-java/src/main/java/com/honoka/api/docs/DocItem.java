package com.honoka.api.docs;

/**
 * 文档列表项 —— 字段与 Node DocItem / 前端 api/docs.ts 一致
 */
public record DocItem(String id, String name, long size, String uploadedAt) {}
