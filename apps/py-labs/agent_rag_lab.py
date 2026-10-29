"""
Honoka Python Agent lab —— 工具循环 Mock（对接 Java RAG）

形状对齐岗位常考的 Tool Calling / LangChain Agent：
  1) 观察目标
  2) 调用 search_docs（HTTP GET /rag/search）
  3) 基于命中综合一段中文答复（本地模板，不强制 OpenAI Key）

用法：
  python agent_rag_lab.py "Honoka 的后端推荐用哪个？"
"""

from __future__ import annotations

import json
import os
import sys
from typing import Any

import httpx

API_BASE = os.environ.get("HONOKA_API_BASE", "http://127.0.0.1:8788").rstrip("/")


def tool_rag_stats(client: httpx.Client) -> dict[str, Any]:
    r = client.get(f"{API_BASE}/rag/stats")
    r.raise_for_status()
    body = r.json()
    return body.get("data") or body


def tool_search_docs(client: httpx.Client, query: str, k: int = 4) -> dict[str, Any]:
    r = client.get(f"{API_BASE}/rag/search", params={"q": query, "k": k, "mode": "hybrid"})
    r.raise_for_status()
    body = r.json()
    return body.get("data") or body


def synthesize_answer(goal: str, search: dict[str, Any]) -> str:
    citations = search.get("citations") or []
    if not citations:
        return f"资料不足：未检索到与「{goal}」相关的切片。请确认 Java API 已启动并执行 POST /rag/rebuild。"
    lines = [f"（Python Agent lab）目标：{goal}", "", "工具 search_docs 命中："]
    for i, c in enumerate(citations, 1):
        name = c.get("docName") or c.get("doc_id") or "?"
        snip = (c.get("snippet") or "")[:160]
        lines.append(f"[{i}] {name}: {snip}")
    lines.append("")
    lines.append("以上为工具结果摘要；接入 LLM 后可在此步改为模型生成。")
    return "\n".join(lines)


def run_agent(goal: str) -> None:
    print(f"== goal ==\n{goal}\n")
    with httpx.Client(timeout=30.0) as client:
        print("== tool: rag_stats ==")
        stats = tool_rag_stats(client)
        print(json.dumps(stats, ensure_ascii=False, indent=2))
        print()

        print("== tool: search_docs ==")
        search = tool_search_docs(client, goal)
        # 打印精简 citations，避免刷屏
        slim = {
            "mode": search.get("mode"),
            "citations": [
                {
                    "docName": c.get("docName"),
                    "score": c.get("score"),
                    "snippet": (c.get("snippet") or "")[:120],
                }
                for c in (search.get("citations") or [])
            ],
        }
        print(json.dumps(slim, ensure_ascii=False, indent=2))
        print()

        print("== final ==")
        print(synthesize_answer(goal, search))


def main() -> None:
    goal = " ".join(sys.argv[1:]).strip() or "Honoka 的后端推荐用哪个？端口是多少？"
    try:
        run_agent(goal)
    except httpx.ConnectError:
        print(f"无法连接 {API_BASE}。请先：npm run dev:api-java", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
