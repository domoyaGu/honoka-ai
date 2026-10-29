package com.honoka.api.rag;

/**
 * 纯文本 / Markdown 切片（阶段 A：规则切分，不做语义切分）
 *
 * 策略：先按 Markdown 标题或空行拆段，再把过长段按 maxChars 滑窗切，
 * 带 overlap 避免跨窗丢掉关键词。向量切分可后续替换本类实现。
 */
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class TextChunker {

  private static final Pattern HEADING_OR_BLANK = Pattern.compile("(?m)(?=^#{1,6}\\s)|\\n\\s*\\n");

  private final int maxChars;
  private final int overlap;

  public TextChunker(int maxChars, int overlap) {
    this.maxChars = Math.max(64, maxChars);
    this.overlap = Math.max(0, Math.min(overlap, this.maxChars / 2));
  }

  public List<String> chunk(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    String normalized = raw.replace("\r\n", "\n").trim();
    String[] paragraphs = HEADING_OR_BLANK.split(normalized);
    List<String> out = new ArrayList<>();
    for (String para : paragraphs) {
      String p = para.trim();
      if (p.isEmpty()) {
        continue;
      }
      if (p.length() <= maxChars) {
        out.add(p);
      } else {
        out.addAll(slidingWindow(p));
      }
    }
    return out;
  }

  private List<String> slidingWindow(String text) {
    List<String> parts = new ArrayList<>();
    int step = Math.max(1, maxChars - overlap);
    for (int i = 0; i < text.length(); i += step) {
      int end = Math.min(text.length(), i + maxChars);
      parts.add(text.substring(i, end).trim());
      if (end >= text.length()) {
        break;
      }
    }
    return parts.stream().filter(s -> !s.isEmpty()).toList();
  }
}
