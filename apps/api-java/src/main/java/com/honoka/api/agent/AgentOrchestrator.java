package com.honoka.api.agent;

/**
 * 最小 Agent 编排（Mock）：规划 → 工具 search_docs → 综合回答
 *
 * 对应岗位常考的 Tool / Function Calling 形状；真模型阶段把「选工具」交给 LLM，
 * 本类先用规则演示「多步 + 护栏（最多 1 次检索）」。
 */
import com.honoka.api.rag.Citation;
import com.honoka.api.rag.KeywordRetriever;
import com.honoka.api.rag.RagIndexService;
import com.honoka.api.rag.RagPromptBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AgentOrchestrator {

  public static final String TOOL_SEARCH = "search_docs";
  public static final String TOOL_STATS = "rag_stats";

  private final RagIndexService ragIndexService;

  public AgentOrchestrator(RagIndexService ragIndexService) {
    this.ragIndexService = ragIndexService;
  }

  /** 白名单工具说明（给前端 / 文档 / 未来函数调用 schema） */
  public List<Map<String, Object>> toolCatalog() {
    List<Map<String, Object>> tools = new ArrayList<>();
    tools.add(
        Map.of(
            "name", TOOL_SEARCH,
            "description", "在知识库切片中检索与问题相关的文档片段",
            "parameters", Map.of("query", "string", "topK", "number?")));
    tools.add(
        Map.of(
            "name", TOOL_STATS,
            "description", "查看当前 RAG 索引切片数量与默认检索模式",
            "parameters", Map.of()));
    return tools;
  }

  public AgentRun planAndRun(String userGoal, Integer topK) {
    String goal = userGoal == null ? "" : userGoal.trim();
    List<Map<String, Object>> steps = new ArrayList<>();

    // 步 1：总是先看索引是否为空（护栏：避免空库瞎答）
    Map<String, Object> statsArgs = Map.of();
    Map<String, Object> statsResult =
        Map.of(
            "chunkCount", ragIndexService.size(),
            "retrieveMode", ragIndexService.defaultRetrieveMode());
    steps.add(step(TOOL_STATS, statsArgs, statsResult));

    List<Citation> citations = List.of();
    String answer;
    if (ragIndexService.size() == 0) {
      answer = "资料不足：索引为空。请先 POST /rag/rebuild 或上传文档后再试。";
    } else {
      // 步 2：检索（本 Mock 固定调用 search_docs；真 Agent 由模型决定是否调用）
      int k = topK == null || topK < 1 ? 4 : topK;
      List<KeywordRetriever.ScoredChunk> hits = ragIndexService.retrieve(goal, k);
      citations = RagPromptBuilder.toCitations(hits);
      Map<String, Object> searchResult = new LinkedHashMap<>();
      searchResult.put("hitCount", hits.size());
      searchResult.put("citations", citations);
      steps.add(step(TOOL_SEARCH, Map.of("query", goal, "topK", k), searchResult));

      answer =
          "（Agent Mock）已调用工具 "
              + TOOL_SEARCH
              + "，再综合回答：\n\n"
              + RagPromptBuilder.buildMockAnswer(goal, hits);
    }

    return new AgentRun(goal, steps, citations, answer, toolCatalog());
  }

  private static Map<String, Object> step(String tool, Object args, Object result) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("tool", tool);
    m.put("args", args);
    m.put("result", result);
    return m;
  }

  public record AgentRun(
      String goal,
      List<Map<String, Object>> steps,
      List<Citation> citations,
      String answer,
      List<Map<String, Object>> tools) {}
}
