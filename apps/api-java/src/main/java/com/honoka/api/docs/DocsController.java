package com.honoka.api.docs;

/**
 * 文档库路由 —— 对照 Node apps/api/src/routes/docs.ts
 *
 * GET    /docs
 * POST   /docs/upload   字段名 file
 * DELETE /docs/{id}
 * GET    /docs/{id}/raw
 */
import com.honoka.api.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/docs")
public class DocsController {

  private final DocsService docsService;

  public DocsController(DocsService docsService) {
    this.docsService = docsService;
  }

  @GetMapping
  public ApiResponse<List<DocItem>> list() throws Exception {
    return ApiResponse.ok(docsService.list());
  }

  @PostMapping("/upload")
  public ApiResponse<DocItem> upload(@RequestParam("file") MultipartFile file) throws Exception {
    return ApiResponse.ok(docsService.upload(file), "上传成功");
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Boolean> delete(@PathVariable("id") String id) throws Exception {
    docsService.delete(id);
    return ApiResponse.ok(true, "删除成功");
  }

  @GetMapping("/{id}/raw")
  public ApiResponse<Map<String, String>> raw(@PathVariable("id") String id) throws Exception {
    String text = docsService.readRaw(id);
    return ApiResponse.ok(Map.of("id", id, "text", text));
  }
}
