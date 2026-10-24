package com.honoka.api.chat;

/**
 * Mock SSE 流式 —— 对照 Node POST /chat/mock-stream
 *
 * 使用 Spring MVC {@link SseEmitter}（你熟悉的同步 Spring MVC + 定时推送），
 * 阶段 A 易懂；阶段 B 若要高并发真模型流式可再评估 WebFlux。
 *
 * 事件体：data: {"type":"chunk"|"done","text":"..."}\n\n
 * 前端 Abort / 关连接 → onCompletion，定时任务停止。
 */
import com.honoka.api.common.ApiResponse;
import java.io.IOException;
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

  @PostMapping("/mock-stream")
  public Object mockStream(@RequestBody Map<String, Object> body) {
    String prompt = body.get("prompt") == null ? "" : String.valueOf(body.get("prompt")).trim();
    if (prompt.isEmpty()) {
      // 非 SSE：业务失败包装（与 Node fail 一致）
      return ResponseEntity.ok(ApiResponse.fail("请输入问题"));
    }

    String full = "prompt: " + prompt;
    // 0L = 不超时；前端 Abort 会触发 completion 回调
    SseEmitter emitter = new SseEmitter(0L);
    AtomicInteger index = new AtomicInteger(0);
    ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];

    futureHolder[0] =
        scheduler.scheduleAtFixedRate(
            () -> {
              try {
                int i = index.getAndIncrement();
                if (i < full.length()) {
                  String piece = String.valueOf(full.charAt(i));
                  // 交给 Jackson 写成 JSON，避免二次转义
                  emitter.send(
                      SseEmitter.event()
                          .data(Map.of("type", "chunk", "text", piece), MediaType.APPLICATION_JSON));
                  return;
                }
                if (futureHolder[0] != null) {
                  futureHolder[0].cancel(false);
                }
                emitter.send(
                    SseEmitter.event()
                        .data(Map.of("type", "done", "text", full), MediaType.APPLICATION_JSON));
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
