package com.honoka.api.health;

/**
 * 健康检查 —— 对照 Node GET /health（无业务包装，前端原样返回）
 */
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of(
        "ok", true,
        "service", "honoka-api-java",
        "time", Instant.now().toString());
  }
}
