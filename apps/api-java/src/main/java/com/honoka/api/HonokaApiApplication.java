package com.honoka.api;

/**
 * Honoka Java API 入口（阶段 A 并行后端）
 *
 * 对照 Node：apps/api/src/index.ts
 * 选型说明：docs/后端Java选型.md
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HonokaApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(HonokaApiApplication.class, args);
  }
}
