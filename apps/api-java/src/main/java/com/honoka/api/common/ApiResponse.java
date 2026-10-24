package com.honoka.api.common;

/**
 * 统一业务响应，与前端 apps/web/src/utils/request.ts 的 { code, data, message } 约定一致。
 * 业务失败时仍可 HTTP 200，靠 code 区分（同许多国内后台习惯）。
 */
public record ApiResponse<T>(int code, T data, String message) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(200, data, "ok");
  }

  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(200, data, message);
  }

  public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>(400, null, message);
  }

  public static <T> ApiResponse<T> fail(int code, String message) {
    return new ApiResponse<>(code, null, message);
  }
}
