package com.honoka.api.rag;

/**
 * 关键词 Top-K 检索（阶段 A，向量可选后续再加）
 *
 * 打分：查询词在切片中出现次数之和；中文连续字串拆成 bigram，
 * 英文/数字按空白与标点分词。空查询或零命中时返回空列表（由上层降级话术）。
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KeywordRetriever {

  private static final Pattern LATIN_TOKEN = Pattern.compile("[A-Za-z0-9_./-]{2,}");
  private static final Pattern CJK_RUN = Pattern.compile("[\\u4e00-\\u9fff]{2,}");

  public List<ScoredChunk> retrieve(List<ChunkRecord> corpus, String query, int topK) {
    Set<String> tokens = tokenize(query);
    if (tokens.isEmpty() || corpus == null || corpus.isEmpty()) {
      return List.of();
    }
    int k = Math.max(1, topK);
    List<ScoredChunk> scored = new ArrayList<>();
    for (ChunkRecord chunk : corpus) {
      double score = score(chunk.text(), tokens);
      if (score > 0) {
        scored.add(new ScoredChunk(chunk, score));
      }
    }
    scored.sort(
        Comparator.comparingDouble(ScoredChunk::score)
            .reversed()
            .thenComparing(s -> s.chunk().id()));
    if (scored.size() <= k) {
      return scored;
    }
    return scored.subList(0, k);
  }

  static Set<String> tokenize(String raw) {
    Set<String> tokens = new HashSet<>();
    if (raw == null || raw.isBlank()) {
      return tokens;
    }
    String q = raw.toLowerCase(Locale.ROOT);
    Matcher latin = LATIN_TOKEN.matcher(q);
    while (latin.find()) {
      tokens.add(latin.group());
    }
    Matcher cjk = CJK_RUN.matcher(q);
    while (cjk.find()) {
      String run = cjk.group();
      for (int i = 0; i < run.length() - 1; i++) {
        tokens.add(run.substring(i, i + 2));
      }
      if (run.length() >= 3) {
        tokens.add(run);
      }
    }
    return tokens;
  }

  private static double score(String text, Set<String> tokens) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    String hay = text.toLowerCase(Locale.ROOT);
    double s = 0;
    for (String t : tokens) {
      int from = 0;
      while (true) {
        int idx = hay.indexOf(t, from);
        if (idx < 0) {
          break;
        }
        s += 1;
        from = idx + t.length();
      }
    }
    return s;
  }

  /** 检索命中：切片 + 分数 */
  public record ScoredChunk(ChunkRecord chunk, double score) {}
}
