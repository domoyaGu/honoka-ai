package com.honoka.api.docs;

/**
 * 文档业务异常：带业务 code，由 DocsControllerAdvice 转成 ApiResponse
 */
public class DocsException extends RuntimeException {

  private final int code;

  public DocsException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
