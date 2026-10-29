package com.honoka.api.rag;

/**
 * 混合检索：关键词打分 + 字符 n-gram「伪向量」余弦相似度
 *
 * 岗位常要求向量库（Milvus 等）。本仓先用零依赖的本地向量近似，
 * 讲清 Embedding → 向量库 → 混合检索形状；真向量见 docs/rag.md / docs/job-gap-checklist.md。
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HybridRetriever {

  private final KeywordRetriever keyword = new KeywordRetriever();

  /**
   * @param mode keyword | vector | hybrid
   * @param vectorWeight hybrid 时向量权重 0～1（默认 0.45）
   */
  public List<KeywordRetriever.ScoredChunk> retrieve(
      List<ChunkRecord> corpus, String query, int topK, String mode, double vectorWeight) {
    if (corpus == null || corpus.isEmpty() || query == null || query.isBlank()) {
      return List.of();
    }
    int k = Math.max(1, topK);
    String m = mode == null ? "hybrid" : mode.trim().toLowerCase(Locale.ROOT);

    if ("keyword".equals(m)) {
      return keyword.retrieve(corpus, query, k);
    }

    Map<String, Double> qVec = bagOfNgrams(query, 2);
    if (qVec.isEmpty()) {
      return keyword.retrieve(corpus, query, k);
    }

    List<KeywordRetriever.ScoredChunk> keywordHits = keyword.retrieve(corpus, query, Math.max(k * 3, 12));
    Map<String, Double> kwScores = new HashMap<>();
    double maxKw = 1e-9;
    for (KeywordRetriever.ScoredChunk h : keywordHits) {
      kwScores.put(h.chunk().id(), h.score());
      maxKw = Math.max(maxKw, h.score());
    }

    double w = Math.min(1.0, Math.max(0.0, vectorWeight));
    if ("vector".equals(m)) {
      w = 1.0;
    }

    List<KeywordRetriever.ScoredChunk> scored = new ArrayList<>();
    for (ChunkRecord chunk : corpus) {
      Map<String, Double> cVec = bagOfNgrams(chunk.text(), 2);
      double cos = cosine(qVec, cVec);
      double kw = kwScores.getOrDefault(chunk.id(), 0.0) / maxKw;
      double hybrid = (1.0 - w) * kw + w * cos;
      if (hybrid > 0.02 || cos > 0.08) {
        scored.add(new KeywordRetriever.ScoredChunk(chunk, hybrid));
      }
    }
    scored.sort(
        Comparator.comparingDouble(KeywordRetriever.ScoredChunk::score)
            .reversed()
            .thenComparing(s -> s.chunk().id()));
    if (scored.size() <= k) {
      return scored;
    }
    return scored.subList(0, k);
  }

  /** 字符 n-gram TF 袋（中英混排都可用，演示「向量检索」概念） */
  static Map<String, Double> bagOfNgrams(String text, int n) {
    Map<String, Double> bag = new HashMap<>();
    if (text == null || text.isBlank() || n < 1) {
      return bag;
    }
    String s = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    if (s.length() < n) {
      bag.put(s, 1.0);
      return bag;
    }
    for (int i = 0; i <= s.length() - n; i++) {
      String gram = s.substring(i, i + n);
      bag.merge(gram, 1.0, Double::sum);
    }
    return bag;
  }

  static double cosine(Map<String, Double> a, Map<String, Double> b) {
    if (a.isEmpty() || b.isEmpty()) {
      return 0;
    }
    double dot = 0;
    double na = 0;
    double nb = 0;
    for (Map.Entry<String, Double> e : a.entrySet()) {
      double av = e.getValue();
      na += av * av;
      Double bv = b.get(e.getKey());
      if (bv != null) {
        dot += av * bv;
      }
    }
    for (double bv : b.values()) {
      nb += bv * bv;
    }
    if (na <= 0 || nb <= 0) {
      return 0;
    }
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
  }

  /** 暴露给调试：当前查询的 token 集合 */
  public Set<String> debugTokens(String query) {
    return KeywordRetriever.tokenize(query);
  }
}
