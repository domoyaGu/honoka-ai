package com.honoka.api.config;

/**
 * 绑定 application.yml 里 honoka.* 配置。
 *
 * 密钥仍只走环境变量 / 仓库根 .env，不在此暴露模型 Key。
 */
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "honoka")
public class HonokaProperties {

  /** 上传目录（相对或绝对路径） */
  private String uploadsDir = "../../data/uploads";

  /** 可提交样例语料 */
  private String samplesDir = "../../data/samples";

  /** 检索索引缓存目录 */
  private String indexDir = "../../data/index";

  /** 切片最大字符数 */
  private int ragChunkSize = 500;

  /** 滑窗重叠字符数 */
  private int ragChunkOverlap = 60;

  /** 默认 Top-K */
  private int ragTopK = 4;

  /** 检索模式：keyword | vector | hybrid */
  private String ragMode = "hybrid";

  /** hybrid 时向量权重 0～1 */
  private double ragVectorWeight = 0.45;

  public String getUploadsDir() {
    return uploadsDir;
  }

  public void setUploadsDir(String uploadsDir) {
    this.uploadsDir = uploadsDir;
  }

  public String getSamplesDir() {
    return samplesDir;
  }

  public void setSamplesDir(String samplesDir) {
    this.samplesDir = samplesDir;
  }

  public String getIndexDir() {
    return indexDir;
  }

  public void setIndexDir(String indexDir) {
    this.indexDir = indexDir;
  }

  public int getRagChunkSize() {
    return ragChunkSize;
  }

  public void setRagChunkSize(int ragChunkSize) {
    this.ragChunkSize = ragChunkSize;
  }

  public int getRagChunkOverlap() {
    return ragChunkOverlap;
  }

  public void setRagChunkOverlap(int ragChunkOverlap) {
    this.ragChunkOverlap = ragChunkOverlap;
  }

  public int getRagTopK() {
    return ragTopK;
  }

  public void setRagTopK(int ragTopK) {
    this.ragTopK = ragTopK;
  }

  public String getRagMode() {
    return ragMode;
  }

  public void setRagMode(String ragMode) {
    this.ragMode = ragMode;
  }

  public double getRagVectorWeight() {
    return ragVectorWeight;
  }

  public void setRagVectorWeight(double ragVectorWeight) {
    this.ragVectorWeight = ragVectorWeight;
  }
}
