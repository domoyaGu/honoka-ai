# 后端 Java 选型（Honoka）

> 目录：`apps/api-java`（Spring Boot 3）。Node 版 `apps/api` 仍保留，便于对照与回滚。

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

### 5. 继续 Node（Express / Fastify）vs 换 Java

| | Node `apps/api` | Java `apps/api-java` |
|--|-----------------|----------------------|
| 上手（你当前） | 与前端同语言 | 复用 Spring Boot 记忆 |
| AI 脚本/原型 | 改得快 | 略繁琐，类型更严 |
| 阶段 B 中台感 | 要自己堆 | Security/JPA/定时更顺 |
| 部署 | 轻 | 需 JDK 17+ |
| 本仓策略 | **保留对照** | **新主线后端（推荐）** |

---

## 目录与扩展点（面向阶段 B）

```
apps/api-java/
  pom.xml
  src/main/java/com/honoka/api/
    HonokaApiApplication.java
    common/          # ApiResponse
    config/          # CORS、路径配置
    health/
    docs/            # 文档库（后续可抽 storage 接口 → OSS/DB）
    chat/            # SSE（后续接 OpenAI 兼容客户端）
  src/main/resources/application.yml
```

后续可加模块而不推翻：

1. `security` — Spring Security + JWT（B1）  
2. `persistence` — Spring Data JPA（用户/会话）  
3. `rag` — 切片/检索服务，被 `chat` 调用  
4. `job` — `@Scheduled` 或队列消费者（B4/B5）  

---

## 与 Node 版并存时怎么跑

同一端口 **8788 只能起一个**。前端 Vite 代理仍指向该端口：

```bash
# 终端 A：Java（需 JDK 17+ 与 Maven）
npm run dev:api-java

# 或
cd apps/api-java && mvn spring-boot:run

# 终端 B：前端
npm run dev:web
```

Node 版：`npm run dev:api`（练习对照时用，勿与 Java 同时占端口）。
