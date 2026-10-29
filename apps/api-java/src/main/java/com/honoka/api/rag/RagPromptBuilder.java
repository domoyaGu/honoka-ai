package com.honoka.api.rag;

/**
 * 把检索结果拼进 Prompt（真模型阶段可直接把 buildPrompt 交给 LLM）
 *
 * Mock 阶段用 buildMockAnswer：不调用模型，但保留「上下文 → 带引用回答」的形状，
 * 方便前端先验证引用 UI。
 */
import java.util.ArrayList;
import java.util.List;

public final class RagPromptBuilder {

  private static final int SNIPPET_MAX = 180;

  private RagPromptBuilder() {}

  public static String buildPrompt(String question, List<KeywordRetriever.ScoredChunk> hits) {
    StringBuilder sb = new StringBuilder();
    sb.append("你是 Honoka 知识库助手。只根据【检索上下文】回答；");
    sb.append("若上下文不足，明确说明「资料不足」。回答时用 [1][2] 标注引用。\n\n");
    sb.append("【检索上下文】\n");
    if (hits == null || hits.isEmpty()) {
      sb.append("（无命中）\n");
    } else {
      int i = 1;
      for (KeywordRetriever.ScoredChunk hit : hits) {
        ChunkRecord c = hit.chunk();
        sb.append('[')
            .append(i++)
            .append("] 来源=")
            .append(c.docName())
            .append(" (")
            .append(c.source())
            .append(")\n")
            .append(c.text())
            .append("\n\n");
      }
    }
    sb.append("【用户问题】\n").append(question == null ? "" : question.trim());
    return sb.toString();
  }

  /**
   * Mock 回答：复述命中摘要 + 引用编号，模拟「模型读了上下文」的输出形态。
   */
  public static String buildMockAnswer(String question, List<KeywordRetriever.ScoredChunk> hits) {
    if (hits == null || hits.isEmpty()) {
      return "资料不足：关键词检索未命中相关切片。可先上传/确认 data/samples 与 data/uploads 中的 .md/.txt，"
          + "或调用 POST /rag/rebuild 重建索引后再问：「"
          + (question == null ? "" : question.trim())
          + "」";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("（RAG Mock）根据检索上下文，关于「")
        .append(question == null ? "" : question.trim())
        .append("」：\n\n");
    int i = 1;
    for (KeywordRetriever.ScoredChunk hit : hits) {
      ChunkRecord c = hit.chunk();
      sb.append('[')
          .append(i++)
          .append("] ")
          .append(c.docName())
          .append("：")
          .append(shorten(c.text(), SNIPPET_MAX))
          .append('\n');
    }
    sb.append("\n以上为关键词命中摘要；接入真模型后将基于同一 Prompt 生成完整答案。");
    return sb.toString();
  }

  public static List<Citation> toCitations(List<KeywordRetriever.ScoredChunk> hits) {
    List<Citation> list = new ArrayList<>();
    if (hits == null) {
      return list;
    }
    for (KeywordRetriever.ScoredChunk hit : hits) {
      ChunkRecord c = hit.chunk();
      list.add(
          new Citation(
              c.id(),
              c.docId(),
              c.docName(),
              c.source(),
              shorten(c.text(), SNIPPET_MAX),
              hit.score()));
    }
    return list;
  }

  private static String shorten(String text, int max) {
    if (text == null) {
      return "";
    }
    String t = text.replaceAll("\\s+", " ").trim();
    if (t.length() <= max) {
      return t;
    }
    return t.substring(0, max) + "…";
  }
}
