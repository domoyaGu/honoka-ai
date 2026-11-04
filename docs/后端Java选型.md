# 后端 Java 选型（Honoka）

> 目录：`apps/api-java`（Spring Boot 3）。本仓**仅保留 Java 后端**（原 Node `apps/api` 已移除）。

## 为什么选 Spring Boot 3（Web MVC）

| 理由 | 说明 |
|------|------|
| 你能接手 | 学过 Spring Boot，Controller / Service / 配置 心智可复用 |
| 阶段 B 扩展 | Security、JPA、定时任务、Actuator、校验 生态成熟，和若依式后台同栈 |
| 流式够用 | `SseEmitter` 覆盖阶段 A Mock/真模型 SSE；不必一上来 WebFlux |
| 与前端契约 | 仍返回 `{ code, data, message }`，`apps/web` 代理与 axios 不用改 |
| 工程习惯 | Maven + `application.yml` 是国内面试/协作常见形态 |

**刻意不用 WebFlux 作为默认**：响应式学习曲线陡，调试难；阶段 A 流量小，MVC + 线程池推 SSE 更直观。若以后单机要扛大量长连接，再评估 WebFlux / 虚拟线程。

---

## 框架对比（选型时怎么讲）

### 1. Spring Boot（本仓选择）

- **是什么**：在 Spring 之上的约定式脚手架，内嵌 Tomcat，自动配置。
- **优点**：生态最大；资料多；阶段 B 鉴权/DB/任务几乎「插依赖就有」；企业岗位重合度高。
- **缺点**：比轻框架「重」；自动配置需会查；包体与启动略慢（对本仓无所谓）。
- **适合**：要长期做成示例后台 + AI API 的同一进程或同仓多模块。

### 2. Spring Boot + WebFlux（Reactor）

- **是什么**：响应式非阻塞 Web 栈（Netty）。
- **优点**：高并发长连接、背压模型清晰；流式 API 很「原生」。
- **缺点**：整条链路要反应式（DB 驱动、思维模型）；和经典 `@Service` 阻塞调用混用易踩坑。
- **适合**：明确要极致并发 SSE/WebSocket 时；**Honoka 阶段 A 不默认上**。

### 3. Quarkus / Micronaut

- **是什么**：云原生、偏 GraalVM 原生镜像、启动快、内存省。
- **优点**：冷启动与容器镜像优秀；DI 也是注解风格。
- **缺点**：生态/国内岗位/中文资料不如 Boot；你已有 Boot 基础时迁移收益有限。
- **适合**：Serverless / 强约束内存的场景；本仓自学后台优先 Boot。

### 4. Helidon / Dropwizard

- **是什么**：更轻的 Java HTTP 微框架。
- **优点**：小而清晰。
- **缺点**：周边（安全、数据访问、Admin）要自己攒；和「示例型后台」目标不匹配。
- **适合**：微型工具服务，不是本仓主路径。

### 5. 为何不保留 Node 对照 API

曾用 Express 作对照，易与 Java 抢 `API_PORT`、双实现漂移。现行计划只维护 `apps/api-java`；前端 Vite 仍代理到同一端口。

---

## 目录与扩展点

```
apps/api-java/
  pom.xml
  src/main/java/com/honoka/api/
    HonokaApiApplication.java
    common/          # ApiResponse
    config/          # CORS、路径配置
    health/
    docs/            # 文档库（后续可抽 storage 接口 → OSS/DB）
    chat/            # SSE（Mock / RAG-Mock；后续接 OpenAI 兼容客户端）
    rag/             # 切片 / 关键词检索 / Prompt（见 docs/rag.md）
  src/main/resources/application.yml
```

后续可加模块而不推翻：

1. `security` — Spring Security + JWT  
2. `persistence` — Spring Data JPA（用户/会话）  
3. `rag` — 已有关键词版；可演进向量 / 异步建索引  
4. `job` — `@Scheduled` 或队列消费者  

---

## 怎么跑

```bash
# 终端 A：Java（需 JDK 17+ 与 Maven）
npm run dev:api-java

# 终端 B：前端
npm run dev:web
```

默认端口 **8788**（`API_PORT`）。
