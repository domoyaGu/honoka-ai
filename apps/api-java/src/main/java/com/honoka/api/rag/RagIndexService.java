package com.honoka.api.rag;

/**
 * RAG 索引：扫描 samples + uploads → 切片 → 内存 + data/index/chunks.json 缓存
 *
 * 主线 1：文档入库后的检索底座。阶段 A 用关键词；向量索引可另存同目录。
 */
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.honoka.api.config.HonokaProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RagIndexService {

  private static final Logger log = LoggerFactory.getLogger(RagIndexService.class);
  private static final Set<String> ALLOWED_EXT = Set.of(".md", ".txt");
  private static final String GITKEEP = ".gitkeep";
  private static final String CACHE_FILE = "chunks.json";

  private final Path samplesDir;
  private final Path uploadsDir;
  private final Path indexDir;
  private final TextChunker chunker;
  private final KeywordRetriever retriever = new KeywordRetriever();
  private final ObjectMapper mapper;
  private final int defaultTopK;

  /** 线程安全可读快照；重建时整体替换 */
  private final CopyOnWriteArrayList<ChunkRecord> chunks = new CopyOnWriteArrayList<>();

  public RagIndexService(HonokaProperties props, ObjectMapper mapper) {
    this.samplesDir = Paths.get(props.getSamplesDir()).toAbsolutePath().normalize();
    this.uploadsDir = Paths.get(props.getUploadsDir()).toAbsolutePath().normalize();
    this.indexDir = Paths.get(props.getIndexDir()).toAbsolutePath().normalize();
    this.chunker = new TextChunker(props.getRagChunkSize(), props.getRagChunkOverlap());
    this.defaultTopK = props.getRagTopK();
    this.mapper = mapper;
  }

  @PostConstruct
  void init() throws IOException {
    Files.createDirectories(samplesDir);
    Files.createDirectories(uploadsDir);
    Files.createDirectories(indexDir);
    if (!loadCache()) {
      rebuild();
    }
  }

  public synchronized Map<String, Object> rebuild() throws IOException {
    List<ChunkRecord> built = new ArrayList<>();
    ingestDir(samplesDir, "samples", built);
    ingestDir(uploadsDir, "uploads", built);
    chunks.clear();
    chunks.addAll(built);
    Path cache = indexDir.resolve(CACHE_FILE);
    mapper.writerWithDefaultPrettyPrinter().writeValue(cache.toFile(), built);
    log.info("RAG 索引重建完成：{} 个切片 → {}", built.size(), cache);
    return Map.of(
        "chunkCount", built.size(),
        "samplesDir", samplesDir.toString(),
        "uploadsDir", uploadsDir.toString(),
        "cacheFile", cache.toString());
  }

  public List<ChunkRecord> allChunks() {
    return List.copyOf(chunks);
  }

  public int size() {
    return chunks.size();
  }

  public List<KeywordRetriever.ScoredChunk> retrieve(String query, Integer topK) {
    int k = topK == null || topK < 1 ? defaultTopK : topK;
    return retriever.retrieve(chunks, query, k);
  }

  private boolean loadCache() {
    Path cache = indexDir.resolve(CACHE_FILE);
    if (!Files.isRegularFile(cache)) {
      return false;
    }
    try {
      List<ChunkRecord> loaded =
          mapper.readValue(cache.toFile(), new TypeReference<List<ChunkRecord>>() {});
      chunks.clear();
      chunks.addAll(loaded);
      log.info("RAG 索引自缓存加载：{} 个切片", loaded.size());
      return true;
    } catch (IOException e) {
      log.warn("读取索引缓存失败，将重建：{}", e.getMessage());
      return false;
    }
  }

  private void ingestDir(Path dir, String source, List<ChunkRecord> out) throws IOException {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (Stream<Path> stream = Files.list(dir)) {
      for (Path file : stream.sorted().toList()) {
        String name = file.getFileName().toString();
        if (GITKEEP.equals(name) || !Files.isRegularFile(file)) {
          continue;
        }
        String ext = extensionOf(name);
        if (!ALLOWED_EXT.contains(ext)) {
          continue;
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        List<String> pieces = chunker.chunk(text);
        String display = name.replaceFirst("^\\d+-", "");
        int offset = 0;
        for (int i = 0; i < pieces.size(); i++) {
          String piece = pieces.get(i);
          String id = source + ":" + name + "#" + i;
          out.add(new ChunkRecord(id, name, display, source, piece, offset));
          offset += piece.length();
        }
      }
    }
  }

  private static String extensionOf(String name) {
    int i = name.lastIndexOf('.');
    return i >= 0 ? name.substring(i).toLowerCase() : "";
  }
}
