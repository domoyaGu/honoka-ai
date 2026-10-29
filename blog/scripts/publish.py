#!/usr/bin/env python3
"""
本机多平台尝试发布（默认草稿）。

支持：
  - juejin：Cookie → 创建草稿；无 --draft 时再 publish
  - cnblogs：MetaWeblog newPost（publish=False 为草稿）

仅标准库。Cookie/密码从 --env 读取，勿打印完整密钥。
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

TAG_NAME_TO_ID = {
    "Python": "6809640408797167623",
    "JavaScript": "6809640407484334093",
    "TypeScript": "6809640543006490638",
    "Java": "6809640445233070094",
    "Vue.js": "6809640407484334100",
    "前端": "6809640407484334093",
    "后端": "6809640408797167623",
    "Spring Boot": "6809641037787561992",
    "AI": "6809640445233070096",
    "LLM": "7257794499869573175",
    "Agent": "7516396389476401162",
    "RAG": "7257794499869573175",
    "架构": "6809640501776482317",
}


def load_env(path: Path) -> Dict[str, str]:
    if not path.exists():
        raise FileNotFoundError(f"找不到 {path}，请先复制 .env.example 为 .env 并填写凭证")
    out: Dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, _, v = line.partition("=")
        out[k.strip()] = v.strip().strip("\"'")
    return out


def mask(s: str) -> str:
    if not s:
        return "(空)"
    if len(s) <= 8:
        return "***"
    return s[:4] + "…" + s[-4:]


# ── 掘金 ─────────────────────────────────────────────


def juejin_request(cookie: str, path: str, body: dict) -> dict:
    url = f"https://api.juejin.cn/content_api/v1{path}"
    data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method="POST",
        headers={
            "accept": "*/*",
            "content-type": "application/json",
            "origin": "https://juejin.cn",
            "referer": "https://juejin.cn/",
            "user-agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            ),
            "cookie": cookie,
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", errors="replace")
        return {"err_no": e.code, "err_msg": text[:500]}
    except Exception as e:  # noqa: BLE001
        return {"err_no": -1, "err_msg": str(e)}


def resolve_tags(tags: List[str]) -> List[str]:
    ids: List[str] = []
    for t in tags:
        if t.isdigit():
            ids.append(t)
        elif t in TAG_NAME_TO_ID:
            ids.append(TAG_NAME_TO_ID[t])
    return ids


def publish_juejin(
    env: Dict[str, str], title: str, content: str, brief: str, tags: List[str], draft: bool
) -> Dict[str, Any]:
    cookie = env.get("JUEJIN_COOKIE") or env.get("JUEJIN_COOKIES") or ""
    if not cookie:
        return {"ok": False, "platform": "juejin", "error": "未配置 JUEJIN_COOKIE"}

    tag_ids = resolve_tags(tags)
    body = {
        "category_id": "0",
        "tag_ids": tag_ids,
        "link_url": "",
        "cover_image": "",
        "title": title,
        "brief_content": brief[:100],
        "edit_type": 10,
        "html_content": "deprecated",
        "mark_content": content,
        "theme_ids": [],
        "pics": [],
    }
    created = juejin_request(cookie, "/article_draft/create", body)
    if created.get("err_no") != 0:
        return {
            "ok": False,
            "platform": "juejin",
            "error": created.get("err_msg") or str(created)[:300],
        }

    draft_id = (created.get("data") or {}).get("id")
    result: Dict[str, Any] = {
        "ok": True,
        "platform": "juejin",
        "draft_id": draft_id,
        "mode": "draft",
    }
    if draft or not draft_id:
        result["hint"] = "已存掘金草稿，请到创作者中心确认"
        return result

    pub = juejin_request(
        cookie,
        "/article/publish",
        {"draft_id": draft_id, "sync_to_org": False, "column_ids": [], "theme_ids": []},
    )
    if pub.get("err_no") != 0:
        return {
            "ok": False,
            "platform": "juejin",
            "draft_id": draft_id,
            "error": pub.get("err_msg") or "publish 失败（草稿可能仍在）",
        }
    article_id = (pub.get("data") or {}).get("article_id")
    result["mode"] = "published"
    if article_id:
        result["url"] = f"https://juejin.cn/post/{article_id}"
    return result


# ── 博客园 MetaWeblog ─────────────────────────────────


def _xml_value(val: Any) -> str:
    if isinstance(val, bool):
        return f"<boolean>{1 if val else 0}</boolean>"
    if isinstance(val, int):
        return f"<int>{val}</int>"
    if isinstance(val, dict):
        members = []
        for k, v in val.items():
            members.append(f"<member><name>{k}</name><value>{_xml_value(v)}</value></member>")
        return "<struct>" + "".join(members) + "</struct>"
    text = str(val)
    # CDATA for markdown body
    if "<" in text or "&" in text or "\n" in text:
        safe = text.replace("]]>", "]]]]><![CDATA[>")
        return f"<string><![CDATA[{safe}]]></string>"
    return f"<string>{text}</string>"


def metaweblog_new_post(
    username: str,
    password: str,
    blog_id: str,
    title: str,
    description: str,
    categories: List[str],
    publish: bool,
) -> Dict[str, Any]:
    url = f"https://rpc.cnblogs.com/metaweblog/{username}"
    post = {
        "title": title,
        "description": description,
        "categories": categories,
    }
    # Build params: blogid, user, pass, struct, publish
    params_xml = []
    for p in (blog_id, username, password, post, publish):
        params_xml.append(f"<param><value>{_xml_value(p)}</value></param>")
    # categories as array in struct — MetaWeblog expects categories as array of string
    # Fix: description is markdown HTML/md; categories should be array
    # Rebuild struct properly for categories
    cat_items = "".join(f"<value><string>{c}</string></value>" for c in categories)
    struct = f"""<struct>
      <member><name>title</name><value><string><![CDATA[{title}]]></string></value></member>
      <member><name>description</name><value><string><![CDATA[{description.replace(']]>', '')}]]></string></value></member>
      <member><name>categories</name><value><array><data>{cat_items}</data></array></value></member>
    </struct>"""
    body = f"""<?xml version="1.0"?>
<methodCall>
  <methodName>metaWeblog.newPost</methodName>
  <params>
    <param><value><string>{blog_id}</string></value></param>
    <param><value><string>{username}</string></value></param>
    <param><value><string><![CDATA[{password}]]></string></value></param>
    <param><value>{struct}</value></param>
    <param><value><boolean>{1 if publish else 0}</boolean></value></param>
  </params>
</methodCall>"""
    req = urllib.request.Request(
        url,
        data=body.encode("utf-8"),
        method="POST",
        headers={"Content-Type": "text/xml; charset=utf-8"},
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return {"ok": False, "platform": "cnblogs", "error": f"HTTP {e.code}"}
    except Exception as e:  # noqa: BLE001
        return {"ok": False, "platform": "cnblogs", "error": str(e)}

    if "<fault>" in raw:
        # extract faultString if any
        m = re.search(r"<string>(.*?)</string>", raw)
        msg = m.group(1) if m else raw[:200]
        return {"ok": False, "platform": "cnblogs", "error": msg}

    m = re.search(r"<string>(\d+)</string>", raw)
    post_id = m.group(1) if m else None
    link = f"https://www.cnblogs.com/{username}/p/{post_id}.html" if post_id else None
    return {
        "ok": True,
        "platform": "cnblogs",
        "post_id": post_id,
        "url": link,
        "mode": "published" if publish else "draft",
    }


def publish_cnblogs(
    env: Dict[str, str], title: str, content: str, tags: List[str], draft: bool
) -> Dict[str, Any]:
    user = env.get("CNBLOGS_USERNAME") or ""
    password = env.get("CNBLOGS_PASSWORD") or ""
    blog_id = env.get("CNBLOGS_BLOG_ID") or user
    if not user or not password:
        return {"ok": False, "platform": "cnblogs", "error": "未配置 CNBLOGS_USERNAME/PASSWORD"}
    return metaweblog_new_post(
        user, password, blog_id, title, content, tags or ["随笔"], publish=not draft
    )


# ── CLI ───────────────────────────────────────────────


def main(argv: Optional[List[str]] = None) -> int:
    p = argparse.ArgumentParser(description="尝试发布到掘金/博客园（默认草稿）")
    p.add_argument("--env", default=".env")
    p.add_argument("--title", required=True)
    p.add_argument("--file", required=True, help="Markdown 文件")
    p.add_argument("--brief", default="")
    p.add_argument("--tags", default="AI,RAG", help="逗号分隔")
    p.add_argument("--platforms", default="juejin,cnblogs")
    p.add_argument(
        "--publish",
        action="store_true",
        help="正式公开（默认只建草稿）",
    )
    args = p.parse_args(argv)

    draft = not args.publish
    env_path = Path(args.env)
    try:
        env = load_env(env_path)
    except FileNotFoundError as e:
        print(json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False))
        return 1

    content = Path(args.file).read_text(encoding="utf-8")
    brief = args.brief or re.sub(r"[#>*`\[\]]", "", content)[:100].strip()
    tags = [t.strip() for t in args.tags.split(",") if t.strip()]
    platforms = [x.strip().lower() for x in args.platforms.split(",") if x.strip()]

    print(
        f"准备发布 | draft={draft} | platforms={platforms} | "
        f"juejin_cookie={mask(env.get('JUEJIN_COOKIE') or env.get('JUEJIN_COOKIES') or '')} | "
        f"cnblogs_user={env.get('CNBLOGS_USERNAME') or '(空)'}"
    )

    results = []
    for plat in platforms:
        if plat == "juejin":
            results.append(publish_juejin(env, args.title, content, brief, tags, draft))
        elif plat in ("cnblogs", "cnblog"):
            results.append(publish_cnblogs(env, args.title, content, tags, draft))
        else:
            results.append({"ok": False, "platform": plat, "error": "不支持的平台"})

    print(json.dumps({"results": results}, ensure_ascii=False, indent=2))
    return 0 if any(r.get("ok") for r in results) else 2


if __name__ == "__main__":
    sys.exit(main())
