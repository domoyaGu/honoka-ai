/**
 * 文档库 API（对接 apps/api `/docs`）
 */
import { request } from "@/utils/request";

export interface DocItem {
  id: string;
  name: string;
  size: number;
  uploadedAt: string;
}

export function listDocs() {
  return request<DocItem[]>({ url: "/docs", method: "get" });
}

/** multipart 字段名必须是 file，与 multer.single('file') 一致 */
export function uploadDoc(file: File) {
  const form = new FormData();
  form.append("file", file);
  return request<DocItem>({
    url: "/docs/upload",
    method: "post",
    data: form,
    // 不要设 Content-Type，交给浏览器生成带 boundary 的 multipart
  });
}

export function deleteDoc(id: string) {
  return request<boolean>({
    url: `/docs/${encodeURIComponent(id)}`,
    method: "delete",
  });
}
