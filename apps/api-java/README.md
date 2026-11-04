# @honoka/api-java

Honoka **唯一后端**：Spring Boot 3，共用仓根 `data/uploads`。

选型：[docs/后端Java选型.md](../../docs/后端Java选型.md)

## 环境

- **JDK 17+**
- **Maven 3.9+**（或 IDE 自带）

```bash
java -version   # 应显示 17+
mvn -version
```

## 启动

仓库根目录：

```bash
npm run dev:api-java
```

或：

```bash
cd apps/api-java
mvn spring-boot:run
```

- Health：http://127.0.0.1:8788/health  
- 端口：`API_PORT`（默认 8788）  
- 上传目录：`honoka.uploads-dir`（默认 `../../data/uploads`）

## 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查（无 code 包装） |
| GET | `/docs` | 列表 |
| POST | `/docs/upload` | 字段名 `file`，仅 md/txt |
| DELETE | `/docs/{id}` | 删除 |
| GET | `/docs/{id}/raw` | 原文 |
| POST | `/rag/rebuild` | 重建索引 |
| GET | `/rag/stats` | 索引统计 |
| GET | `/rag/search` | 检索调试 |
| POST | `/chat/mock-stream` | Mock SSE |
| POST | `/chat/rag-mock-stream` | RAG Mock SSE |
| POST | `/chat/agent-mock-stream` | Agent Mock SSE |

业务包装：`{ "code": 200, "data": ..., "message": "ok" }`。
