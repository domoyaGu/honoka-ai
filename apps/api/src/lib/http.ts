/**
 * 统一业务响应：与前端 `utils/request.ts` 解包约定一致
 * code === 200 时前端拿到 data
 */
import type { Response } from "express";

export function ok<T>(res: Response, data: T, message = "ok") {
  return res.json({ code: 200, data, message });
}

export function fail(
  res: Response,
  message: string,
  code = 400,
  httpStatus = 200,
) {
  // 业务失败仍可 HTTP 200，靠 code 区分（同许多后台约定）
  return res.status(httpStatus).json({ code, data: null, message });
}
