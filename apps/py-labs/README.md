# Python labs（岗位缺口：Python + Agent 形状）

很多苏州 JD 写 Python / LangChain。本目录用 **最少依赖** 演示：

1. 调本仓 Java API 的 `/rag/search`（工具 = HTTP）  
2. 多步「观察 → 调工具 → 综合」循环（Mock，无需 Key）  
3. 可选：设置 `OPENAI_API_KEY` 后走真 function calling（需自行扩展）

## 环境

```bash
cd apps/py-labs
python -m venv .venv
# Windows
.venv\Scripts\activate
pip install -r requirements.txt
```

先起 Java：`npm run dev:api-java`（默认 8788）。

## 运行

```bash
python agent_rag_lab.py "Honoka 的后端推荐用哪个？"
```

环境变量：

- `HONOKA_API_BASE` 默认 `http://127.0.0.1:8788`

## 和 LangChain 的关系

本脚本刻意不锁死 LangChain 大版本；结构对齐：

- Tool = `search_docs`  
- Agent loop = 调工具 → 读结果 → 生成最终答复  

上正式项目时可换成 `langchain` / `langgraph`，工具仍指向同一 REST。
