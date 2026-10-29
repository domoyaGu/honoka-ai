package com.honoka.api.chat;

/**
 * Mock / RAG-Mock SSE 流式 —— 对照 Node POST /chat/mock-stream、/chat/rag-mock-stream
 *
 * 使用 Spring MVC {@link SseEmitter}；RAG 路径先检索再推流，首包 type=meta 带 citations。
 * 事件体：data: {"type":"meta"|"chunk"|"done", ...}\n\n
 * 前端 Abort / 关连接 → onCompletion，定时任务停止。
 */
import com.honoka.api.agent.AgentOrchestrator;
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
import org.springframework.web.bind.annotation.GetMapping;
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
  private final AgentOrchestrator agentOrchestrator;

  public ChatController(RagIndexService ragIndexService, AgentOrchestrator agentOrchestrator) {
    this.ragIndexService = ragIndexService;
    this.agentOrchestrator = agentOrchestrator;
  }

  /** 工具白名单目录（Agent 面试口述 + 前端调试） */
  @GetMapping("/agent/tools")
  public ApiResponse<List<Map<String, Object>>> agentTools() {
    return ApiResponse.ok(agentOrchestrator.toolCatalog());
  }

  // JSON：空 prompt 业务失败；SSE：正常推流（两种 produces 并存）
  @PostMapping(
      value = "/mock-stream",
      produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
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
  @PostMapping(
      value = "/rag-mock-stream",
      produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
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
    return streamText(answer, citations, assembled.length(), null);
  }

  /**
   * Agent Mock 流式：先推 tool 步骤，再 meta 引用，再逐字答案
   *
   * Body: { "prompt": "...", "topK": 4 }
   * SSE：tool → meta → chunk → done
   */
  @PostMapping(
      value = "/agent-mock-stream",
      produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public Object agentMockStream(@RequestBody Map<String, Object> body) {
    String prompt = body.get("prompt") == null ? "" : String.valueOf(body.get("prompt")).trim();
    if (prompt.isEmpty()) {
      return ResponseEntity.ok(ApiResponse.fail("请输入问题"));
    }
    Integer topK = null;
    if (body.get("topK") instanceof Number n) {
      topK = n.intValue();
    }
    if (ragIndexService.size() == 0) {
      try {
        ragIndexService.rebuild();
      } catch (IOException ignored) {
        // AgentOrchestrator 会给出空库话术
      }
    }
    AgentOrchestrator.AgentRun run = agentOrchestrator.planAndRun(prompt, topK);
    return streamText(run.answer(), run.citations(), -1, run.steps());
  }

  private SseEmitter streamText(String full, List<Citation> citations) {
    return streamText(full, citations, -1, null);
  }

  private SseEmitter streamText(
      String full, List<Citation> citations, int promptChars, List<Map<String, Object>> toolSteps) {
    SseEmitter emitter = new SseEmitter(0L);
    AtomicInteger index = new AtomicInteger(0);
    ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];
    // phase: 0=tools, 1=meta, 2=chunks；无 tools 时从 meta 或 chunk 开始
    int startPhase = (toolSteps != null && !toolSteps.isEmpty()) ? 0 : (citations != null ? 1 : 2);
    AtomicInteger phase = new AtomicInteger(startPhase);
    AtomicInteger toolIndex = new AtomicInteger(0);

    futureHolder[0] =
        scheduler.scheduleAtFixedRate(
            () -> {
              try {
                if (phase.get() == 0) {
                  int ti = toolIndex.getAndIncrement();
                  if (ti < toolSteps.size()) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "tool");
                    payload.put("step", toolSteps.get(ti));
                    emitter.send(
                        SseEmitter.event().data(payload, MediaType.APPLICATION_JSON));
                    return;
                  }
                  phase.set(citations != null ? 1 : 2);
                  return;
                }
                // phase 1：先推 meta（引用），再进入逐字 chunk
                if (phase.get() == 1) {
                  Map<String, Object> meta = new HashMap<>();
                  meta.put("type", "meta");
                  meta.put("citations", citations);
                  if (promptChars >= 0) {
                    meta.put("promptChars", promptChars);
                  }
                  emitter.send(
                      SseEmitter.event().data(meta, MediaType.APPLICATION_JSON));
                  phase.set(2);
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
                if (toolSteps != null) {
                  done.put("steps", toolSteps);
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
