package com.honoka.api.config;

/**
 * 绑定 application.yml 里 honoka.* 配置。
 */
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "honoka")
public class HonokaProperties {

  /** 上传目录（相对或绝对路径） */
  private String uploadsDir = "../../data/uploads";

  public String getUploadsDir() {
    return uploadsDir;
  }

  public void setUploadsDir(String uploadsDir) {
    this.uploadsDir = uploadsDir;
  }
}
