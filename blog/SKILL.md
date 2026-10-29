---
name: stage-blog-publish
description: >-
  Generates a stage retrospective blog post from Honoka (or similar) project
  docs/code, then attempts to publish drafts to Juejin and CNBlogs via local
  scripts and cookies. Use when the user asks to 发博客、发掘金、同步博客园、
  阶段复盘发文、生成并发布、一键分发文章.
---

# Stage Blog Publish

把某一阶段成果写成技术博客，并**尝试**发布到掘金 / 博客园（默认草稿）。

本目录在仓库：`blog/`（勿再放到 C 盘 Cursor skills）。

## 硬规则

1. **先写后发**：先把 Markdown 落到磁盘，再调发布脚本。  
2. **默认草稿**：除非用户明确说「正式发布 / 公开」，一律只建草稿。  
3. **密钥本机**：Cookie / 密码只读 `blog/.env`（或用户指定路径）；**禁止**提交 `.env`、禁止打印完整 Cookie。  
4. **只写做过的事**：对照仓库 `docs/` 与代码；禁止编造未实现功能。  
5. **失败不中断**：某一平台失败继续下一个，最后汇总链接/错误。  
6. **人工确认**：发布前用一两句话列出标题、平台、草稿/正式，用户未反对再执行脚本。

## 工作流

### 1. 收集素材

向用户确认或从上下文推断：

- 阶段名（如：RAG 混合检索、Agent 工具调用、Mock SSE）
- 相关路径（`docs/rag.md`、关键源码）
- 目标平台（默认：`juejin,cnblogs`）
- 草稿还是正式（默认草稿）

### 2. 生成正文

按 `templates/stage-post.md` 结构写掘金主版（Markdown）：

- 标题、一句话价值、背景、做了什么、数据流、踩坑、对照岗位/下一步、仓库链接  
- 代码用路径引用，不大段粘贴  
- 无密钥、无真实客户数据  

写入：`blog/out/<slug>-YYYYMMDD.md`

### 3. 尝试发布

检查 `blog/.env` 是否存在；没有则复制 `.env.example` 并指导用户填 Cookie（见 `references/cookies.md`），**不要瞎发**。

```powershell
cd E:\git\honoka-lite\blog
python scripts/publish.py `
  --env .env `
  --title "标题" `
  --file "out\xxx.md" `
  --platforms juejin,cnblogs `
  --tags "AI,RAG,Vue" `
  --brief "摘要不超过 100 字"
```

默认草稿。正式公开须用户明确要求后再加 `--publish`。

### 4. 回报用户

表格：`平台 | 状态 | 链接或错误摘要`（错误可截断，勿泄 Cookie）。

## 平台范围

| 平台 | 脚本 | 备注 |
|------|------|------|
| 掘金 | Cookie → 建草稿（可选再 publish） | 主目标 |
| 博客园 | MetaWeblog（用户名+密码/授权码） | 主目标 |
| 知乎/小红书/公众号 | 不做自动发 | 只生成短版文案供粘贴 |

## 触发例句

- 「把 RAG 阶段生成并发掘金博客园草稿」  
- 「阶段复盘发博客，尝试发布」  
- 「一键分发这篇到掘金」
