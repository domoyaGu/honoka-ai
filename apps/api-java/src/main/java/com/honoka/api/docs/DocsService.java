package com.honoka.api.docs;

/**
 * 文档库业务：读写 data/uploads（与 Node apps/api 共用目录）
 *
 * 安全：所有 id 经 Paths.get(...).getFileName()，防 ../ 穿越；禁止动 .gitkeep
 */
import com.honoka.api.config.HonokaProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocsService {

  private static final Set<String> ALLOWED_EXT = Set.of(".md", ".txt");
  private static final String GITKEEP = ".gitkeep";

  private final Path uploadsDir;

  public DocsService(HonokaProperties props) {
    this.uploadsDir = Paths.get(props.getUploadsDir()).toAbsolutePath().normalize();
  }

  @PostConstruct
  void ensureUploadsDir() throws IOException {
    Files.createDirectories(uploadsDir);
  }

  public List<DocItem> list() throws IOException {
    if (!Files.isDirectory(uploadsDir)) {
      return List.of();
    }
    List<DocItem> items = new ArrayList<>();
    try (Stream<Path> stream = Files.list(uploadsDir)) {
      for (Path file : stream.toList()) {
        String name = file.getFileName().toString();
        if (GITKEEP.equals(name) || !Files.isRegularFile(file)) {
          continue;
        }
        items.add(toItem(file, name));
      }
    }
    return items;
  }

  public DocItem upload(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new DocsException(400, "文件不存在");
    }
    String original = file.getOriginalFilename();
    if (original == null || original.isBlank()) {
      throw new DocsException(400, "文件名无效");
    }
    String base =
        Paths.get(original).getFileName().toString().replaceAll("[\\\\/]", "_").replaceAll("\\s+", "_");
    String ext = extensionOf(base);
    if (!ALLOWED_EXT.contains(ext)) {
      throw new DocsException(400, "仅支持 .md / .txt");
    }
    String stored = System.currentTimeMillis() + "-" + base;
    Path target = uploadsDir.resolve(stored).normalize();
    if (!target.startsWith(uploadsDir)) {
      throw new DocsException(400, "非法路径");
    }
    file.transferTo(target);
    return toItem(target, stored);
  }

  public void delete(String id) throws IOException {
    Path target = resolveSafe(id);
    Files.delete(target);
  }

  public String readRaw(String id) throws IOException {
    Path target = resolveSafe(id);
    return Files.readString(target, StandardCharsets.UTF_8);
  }

  private Path resolveSafe(String rawId) {
    if (rawId == null || rawId.isBlank()) {
      throw new DocsException(400, "非法文件 id");
    }
    String id = Paths.get(rawId).getFileName().toString().trim();
    if (id.isEmpty() || GITKEEP.equals(id)) {
      throw new DocsException(400, "禁止访问该文件");
    }
    Path target = uploadsDir.resolve(id).normalize();
    if (!target.startsWith(uploadsDir)) {
      throw new DocsException(400, "非法路径");
    }
    if (!Files.isRegularFile(target)) {
      throw new DocsException(404, "文件不存在");
    }
    return target;
  }

  private static DocItem toItem(Path file, String storedName) throws IOException {
    long size = Files.size(file);
    String uploadedAt =
        Files.getLastModifiedTime(file).toInstant().atOffset(ZoneOffset.UTC).toString();
    // 展示名去掉时间戳前缀，与 Node 版一致
    String display = storedName.replaceFirst("^\\d+-", "");
    return new DocItem(storedName, display, size, uploadedAt.isEmpty() ? Instant.now().toString() : uploadedAt);
  }

  private static String extensionOf(String name) {
    int i = name.lastIndexOf('.');
    return i >= 0 ? name.substring(i).toLowerCase() : "";
  }
}
