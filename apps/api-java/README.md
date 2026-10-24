# @honoka/api-java

阶段 A 的 **Spring Boot 3** 后端，接口与 `apps/api`（Node）对齐，共用 `data/uploads`。

选型对照：[docs/后端Java选型.md](../../docs/后端Java选型.md)

## 环境

- **JDK 17+**（必装；本机若未装，先装 Temurin 17 再开终端）
- **Maven 3.9+**（或 IDE 自带 Maven）

```bash
java -version   # 应显示 17+
mvn -version
```

## 启动

在仓库根目录：

```bash
npm run dev:api-java
```

或：

```bash
cd apps/api-java
mvn spring-boot:run
```

- Health：http://127.0.0.1:8788/health  
- 端口：`API_PORT`（默认 8788），与根目录 `.env` / 前端 `VITE_API_PORT` 一致  
- 上传目录：`honoka.uploads-dir`（默认相对本模块的 `../../data/uploads`）

**不要**与 `npm run dev:api`（Node）同时占用 8788。

## 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查（无 code 包装） |
| GET | `/docs` | 列表 |
| POST | `/docs/upload` | 字段名 `file`，仅 md/txt |
| DELETE | `/docs/{id}` | 删除 |
| GET | `/docs/{id}/raw` | 原文 |
| POST | `/chat/mock-stream` | Mock SSE |

业务包装：`{ "code": 200, "data": ..., "message": "ok" }`。
