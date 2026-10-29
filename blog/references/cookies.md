# 如何配置发布凭证

文件：`blog/.env`（从 `blog/.env.example` 复制）。已被仓库根 `.gitignore` 忽略，**禁止**提交。

## 掘金 `JUEJIN_COOKIE`

1. 浏览器登录 https://juejin.cn  
2. F12 → Network → 筛选 `api.juejin.cn`  
3. 点任意请求 → Request Headers → 复制整段 `Cookie`  
4. 粘贴到 `.env` 的 `JUEJIN_COOKIE=`

Cookie 过期后重新复制即可。发布脚本默认只建**草稿**。

## 博客园 MetaWeblog

1. 登录博客园 → 设置 → 博客设置  
2. 开启 **允许 MetaWeblog 博客客户端访问**  
3. `.env`：
   - `CNBLOGS_USERNAME=` 登录用户名（URL 里的那个）  
   - `CNBLOGS_PASSWORD=` 登录密码或后台生成的**授权码**（推荐授权码）  
   - `CNBLOGS_BLOG_ID=` 一般等于用户名，可不填  

`publish.py --draft` 时 `publish=false`（草稿）；`--publish` 才公开。

## 安全

- 只存在本机 `blog/.env`  

- Agent 回报时只显示掩码，不打印完整 Cookie  
- 云端 Agent / 分享对话前先确认未粘贴密钥  
