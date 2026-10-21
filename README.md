# Rome Lite（AI 全栈）

> 源自简历项目 **Rome 保险营销平台** 的竖切复刻：**保险产品知识 / 营销话术助手**。  
> **不是**完整 Rome（不做租户权限全量、批改、对账、uni-app 全套）。

## 产品范围（Must）

1. 极简后台：产品列表（mock）+ 文档上传/管理  
2. 问答：流式回答 + **引用文档片段**  
3. 服务端：会话、检索、密钥不进前端  
4. 5～10 条黄金问可回归（`npm test` / `npm run ai:eval`）  
5. README + 2 分钟演示稿  

## 技术选型（初定）

| 层 | 选型 |
|----|------|
| 前端 | Vue3 + Vite + TypeScript + Element Plus |
| 后端 | Node（Express/Fastify）或后续可换 |
| AI | OpenAI 兼容 API（可本地 Ollama）；RAG 先关键词/向量二选一做透 |

## 计划

见根目录 [`ROADMAP.md`](./ROADMAP.md)（对照原 `jiayou` 岗位计划节奏：**一天一刀**）。

## 本地启动（脚手架就绪后）

```bash
# 待 Week R0 脚手架落地后补充
```

## 安全

- 模型 Key 只放 `.env`（已 gitignore），示例见 `.env.example`  
- 勿提交客户真实保单/条款原文；演示用自造样例文档  
