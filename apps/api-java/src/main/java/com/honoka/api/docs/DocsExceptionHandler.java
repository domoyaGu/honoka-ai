package com.honoka.api.docs;

/**
 * 把 DocsException 收成统一 ApiResponse，避免堆栈直接甩给前端
 */
import com.honoka.api.common.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 只作用于 docs 包，避免把 chat SSE 的异常也包成 JSON */
@RestControllerAdvice(basePackageClasses = DocsController.class)
public class DocsExceptionHandler {

  @ExceptionHandler(DocsException.class)
  public ApiResponse<Void> handleDocs(DocsException e) {
    return ApiResponse.fail(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleOther(Exception e) {
    return ApiResponse.fail(500, e.getMessage() != null ? e.getMessage() : "服务器错误");
  }
}
