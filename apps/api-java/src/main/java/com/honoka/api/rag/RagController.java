package com.honoka.api.rag;

/**
 * RAG 辅助接口 —— 重建索引 / 调试检索（不经流式）
 *
 * 主问答入口见 ChatController#ragMockStream。
 */
import com.honoka.api.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagController {

  private final RagIndexService indexService;

  public RagController(RagIndexService indexService) {
    this.indexService = indexService;
  }

  /** 上传新文档后可手动重建，使关键词索引立刻包含新文件 */
  @PostMapping("/rebuild")
  public ApiResponse<Map<String, Object>> rebuild() throws Exception {
    return ApiResponse.ok(indexService.rebuild(), "索引已重建");
  }

  @GetMapping("/stats")
  public ApiResponse<Map<String, Object>> stats() {
    return ApiResponse.ok(Map.of("chunkCount", indexService.size()));
  }

  /** 调试：看某查询的 Top-K 命中（不走 SSE） */
  @GetMapping("/search")
  public ApiResponse<Map<String, Object>> search(
      @RequestParam("q") String q, @RequestParam(value = "k", required = false) Integer k) {
    String query = q == null ? "" : q.trim();
    if (query.isEmpty()) {
      return ApiResponse.fail("请提供查询参数 q");
    }
    List<KeywordRetriever.ScoredChunk> hits = indexService.retrieve(query, k);
    List<Citation> citations = RagPromptBuilder.toCitations(hits);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("query", query);
    data.put("citations", citations);
    data.put("promptPreview", RagPromptBuilder.buildPrompt(query, hits));
    return ApiResponse.ok(data);
  }
}
