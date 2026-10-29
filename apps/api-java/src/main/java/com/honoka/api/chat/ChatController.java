package com.honoka.api.chat;

/**
 * Mock / RAG-Mock SSE 流式 —— 对照 Node POST /chat/mock-stream、/chat/rag-mock-stream
 *
 * 使用 Spring MVC {@link SseEmitter}；RAG 路径先检索再推流，首包 type=meta 带 citations。
 * 事件体：data: {"type":"meta"|"chunk"|"done", ...}\n\n
 * 前端 Abort / 关连接 → onCompletion，定时任务停止。
 */
import com.honoka.api.common.ApiResponse;
import com.honoka.api.rag.Citation;
import com.honoka.api.rag.KeywordRetriever;
import com.honoka.api.rag.RagIndexService;
import com.honoka.api.rag.RagPromptBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
public class ChatController {

  /** 轻量调度：Mock 推字用；真模型阶段可换成专用线程池 */
  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(
          2,
          r -> {
            Thread t = new Thread(r, "honoka-sse");
            t.setDaemon(true);
            return t;
          });

  private final RagIndexService ragIndexService;

  public ChatController(RagIndexService ragIndexService) {
    this.ragIndexService = ragIndexService;
  }

  @PostMapping("/mock-stream")
  public Object mockStream(@RequestBody Map<String, Object> body) {
    String prompt = body.get("prompt") == null ? "" : String.valueOf(body.get("prompt")).trim();
    if (prompt.isEmpty()) {
      return ResponseEntity.ok(ApiResponse.fail("请输入问题"));
    }
    return streamText("prompt: " + prompt, null);
  }

  /**
   * 关键词 RAG Mock 流式：检索 → 拼 Prompt（服务端日志可用）→ 流式 Mock 答案 + 引用元数据
   *
   * Body: { "prompt": "...", "topK": 4 }（topK 可选）
   */
  @PostMapping("/rag-mock-stream")
  public Object ragMockStream(@RequestBody Map<String, Object> body) {
    String prompt = body.get("prompt") == null ? "" : String.valueOf(body.get("prompt")).trim();
    if (prompt.isEmpty()) {
      return ResponseEntity.ok(ApiResponse.fail("请输入问题"));
    }
    Integer topK = null;
    if (body.get("topK") instanceof Number n) {
      topK = n.intValue();
    }
    // 索引为空时尝试重建，避免刚起服务但缓存损坏时无料可搜
    if (ragIndexService.size() == 0) {
      try {
        ragIndexService.rebuild();
      } catch (IOException ignored) {
        // 降级话术由 buildMockAnswer 处理
      }
    }
    List<KeywordRetriever.ScoredChunk> hits = ragIndexService.retrieve(prompt, topK);
    // Prompt 已拼好，真模型阶段把该字符串交给 LLM；Mock 只用来验证闭环
    String assembled = RagPromptBuilder.buildPrompt(prompt, hits);
    String answer = RagPromptBuilder.buildMockAnswer(prompt, hits);
    List<Citation> citations = RagPromptBuilder.toCitations(hits);
    return streamText(answer, citations, assembled.length());
  }

  private SseEmitter streamText(String full, List<Citation> citations) {
    return streamText(full, citations, -1);
  }

  private SseEmitter streamText(String full, List<Citation> citations, int promptChars) {
    SseEmitter emitter = new SseEmitter(0L);
    AtomicInteger index = new AtomicInteger(0);
    ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];
    AtomicInteger phase = new AtomicInteger(citations != null ? 0 : 1);

    futureHolder[0] =
        scheduler.scheduleAtFixedRate(
            () -> {
              try {
                // phase 0：先推 meta（引用），再进入逐字 chunk
                if (phase.get() == 0) {
                  Map<String, Object> meta = new HashMap<>();
                  meta.put("type", "meta");
                  meta.put("citations", citations);
                  if (promptChars >= 0) {
                    meta.put("promptChars", promptChars);
                  }
                  emitter.send(
                      SseEmitter.event().data(meta, MediaType.APPLICATION_JSON));
                  phase.set(1);
                  return;
                }
                int i = index.getAndIncrement();
                if (i < full.length()) {
                  String piece = String.valueOf(full.charAt(i));
                  emitter.send(
                      SseEmitter.event()
                          .data(Map.of("type", "chunk", "text", piece), MediaType.APPLICATION_JSON));
                  return;
                }
                if (futureHolder[0] != null) {
                  futureHolder[0].cancel(false);
                }
                Map<String, Object> done = new HashMap<>();
                done.put("type", "done");
                done.put("text", full);
                if (citations != null) {
                  done.put("citations", citations);
                }
                emitter.send(SseEmitter.event().data(done, MediaType.APPLICATION_JSON));
                emitter.complete();
              } catch (IOException ex) {
                if (futureHolder[0] != null) {
                  futureHolder[0].cancel(false);
                }
                emitter.completeWithError(ex);
              }
            },
            0,
            50,
            TimeUnit.MILLISECONDS);

    Runnable stop =
        () -> {
          if (futureHolder[0] != null) {
            futureHolder[0].cancel(false);
          }
        };
    emitter.onCompletion(stop);
    emitter.onTimeout(stop);
    emitter.onError(e -> stop.run());

    return emitter;
  }
}
