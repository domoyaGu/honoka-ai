# 后端工程梯子（Honoka）

> 对应 `ROADMAP` §二.B。先懂再薄落地；完整若依式中台刻意不做。

## 1. 数据库

- Schema：用户 / 会话 / 文档元数据；软删字段；文档正文可仍落盘。  
- 事务：元数据写入与「索引版本号」同事务或最终一致。  
- 索引：按 userId、updatedAt 列表；勿给低区分度列滥建。  
- 迁移：Flyway；演示可先 H2/SQLite，生产 MySQL/PG。  
- 访问：Spring Data JPA 或 MyBatis；连接池 HikariCP。

## 2. 鉴权

- 演示：现有 mock 登录 → 演进 JWT（短 Access + 可选 Refresh）。  
- Spring Security Filter 链；401/403 与 `{code,data,message}` 对齐。  
- RBAC：角色 → API 权限；菜单后发。密码 bcrypt，禁止明文。

## 3. 缓存（Redis）

- 场景：热点文档 raw、会话限流计数、分布式锁（重建索引）。  
- 失效：按 docId 删除；重建索引时 bump version。

## 4. 消息队列

- 场景：上传后异步 `rebuild` 索引、通知。  
- 至少一次投递 + 幂等（同一 fileId 只建一次）。  
- 选型直觉：Redis Stream / Rabbit 入门；Kafka 偏日志洪峰。

## 5. 定时与可观测

- `@Scheduled`：清理过期上传、重建统计。  
- Actuator：health / metrics；日志带 requestId。

## 6. API 约定

- REST 清晰；列表分页；删除幂等。  
- 统一 `{ code, data, message }`；SSE 路径不包这层。

## 与本仓交叉

| 能力 | AI 侧 | 后端加深 |
|------|-------|----------|
| 建索引 | `/rag/rebuild` 同步 | 可改 MQ 异步 |
| 对话 | SSE | 限流、鉴权 |
| Agent 工具 | 白名单 HTTP | 工具=受控内部 API |
