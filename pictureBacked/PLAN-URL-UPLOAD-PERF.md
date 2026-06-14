# URL 上传流程全面重构方案

## Context

`UrlUploadPicture.getTempFile` 使用 `HttpUtil.downloadFile` 将远程图片下载到本地临时文件，耗时瓶颈在于：

```
远程 URL ──网络下载──→ 本地磁盘临时文件 ──readBytes──→ byte[] ──网络上传──→ OSS
                                          └──ImageIO.read──→ 获取宽高（二次磁盘读取）
```

数据流经 4 次 I/O（网络下载、磁盘写、磁盘读×2、网络上传），且 OSS 客户端每次请求都新建/销毁，两次 `processPicture` 串行执行。

**目标**：消除不必要的磁盘 I/O，复用 OSS 连接，并行化图片处理。

## 改动文件

| 文件 | 改动内容 |
|------|---------|
| `common/.../util/AliOssUtil.java` | OSS 客户端单例化 + 新增 `InputStream` 上传重载 |
| `server/.../template/upload/PictureUploadTemplate.java` | 模板方法改为内存模式，消除临时文件 |
| `server/.../template/upload/UrlUploadPicture.java` | 内存下载 + HTTP 超时配置 |
| `server/.../template/upload/FileUploadPicture.java` | 适配新模板签名 |

## 步骤

### Step 1: AliOssUtil — OSS 客户端单例化 + InputStream 上传

**文件**: `common/src/main/java/org/example/common/util/AliOssUtil.java`

当前 `upload`、`processPicture`、`deleteByUrl`、`download` 每次调用都 `OSSClientBuilder.create()` 并 `shutdown()`，改为 Spring 管理 OSS 客户端生命周期。

改动：
1. 将 `OSS ossClient` 提升为成员变量，在 `@PostConstruct` 中初始化，`@PreDestroy` 中关闭
2. 各方法复用同一个 `ossClient`，移除方法内的 `create()/shutdown()`
3. 新增 `upload(InputStream inputStream, long contentLength, String objectName)` 重载，支持流式上传

```java
// 新增成员
private OSS ossClient;

@PostConstruct
public void init() throws Exception {
    ClientBuilderConfiguration cfg = new ClientBuilderConfiguration();
    cfg.setSignatureVersion(SignVersion.V4);
    ossClient = OSSClientBuilder.create()
            .endpoint(endpoint)
            .credentialsProvider(CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider())
            .clientConfiguration(cfg)
            .region(region)
            .build();
}

@PreDestroy
public void destroy() {
    if (ossClient != null) ossClient.shutdown();
}

// 新增 InputStream 重载
public String upload(InputStream inputStream, long contentLength, String objectName) throws Exception {
    String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
    String newFileName = UUID.randomUUID() + objectName.substring(objectName.lastIndexOf("."));
    String fileName = dir + "/" + newFileName;
    ObjectMetadata meta = new ObjectMetadata();
    meta.setContentLength(contentLength);
    ossClient.putObject(new PutObjectRequest(bucketName, fileName, inputStream, meta));
    return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + fileName;
}
```

### Step 2: PictureUploadTemplate — 消除临时文件

**文件**: `server/src/main/java/org/example/server/template/upload/PictureUploadTemplate.java`

将核心模板方法从"写临时文件"改为"返回 byte[]"，整个流程在内存中完成：

1. 将抽象方法 `getTempFile(Object, File)` 改为 `getImageBytes(Object)` 返回 `byte[]`
2. `upload` 方法中不再创建临时文件，直接使用 byte[] 进行 OSS 上传和 ImageIO 读取
3. 两次 `processPicture` 改为 `CompletableFuture` 并行执行

```java
// 抽象方法变更
protected abstract byte[] getImageBytes(Object inputResource) throws IOException;

public final UploadPictureDTO upload(Object inputResource) throws Exception {
    String fileSuffix = validPicture(inputResource);
    ThrowUtils.throwIf(StrUtil.isBlank(fileSuffix), ErrorCode.PARAMS_ERROR, "图片格式错误");

    String fileName = getOriginFileName(inputResource);
    ThrowUtils.throwIf(StrUtil.isBlank(fileName), ErrorCode.PARAMS_ERROR, "文件名不能为空");
    if (!fileName.endsWith("." + fileSuffix)) {
        fileName = fileName + "." + fileSuffix;
    }

    // 一次性获取图片字节（内存中，无磁盘 I/O）
    byte[] imageBytes = getImageBytes(inputResource);
    long fileSize = imageBytes.length;

    String url = null;
    try {
        // 直接上传 byte[] 到 OSS（无中间文件）
        url = aliOssUtil.upload(imageBytes, fileName);
        String originImageUrl = url;

        // 并行执行 OSS 图片处理
        String format = "image/format,webp";
        String resize = "image/resize,s_180";

        CompletableFuture<String> compressedFuture = CompletableFuture.supplyAsync(() -> {
            try { return aliOssUtil.processPicture(format, originImageUrl); }
            catch (Exception e) { throw new CompletionException(e); }
        });
        CompletableFuture<String> thumbnailFuture = CompletableFuture.supplyAsync(() -> {
            try { return aliOssUtil.processPicture(resize, originImageUrl); }
            catch (Exception e) { throw new CompletionException(e); }
        });

        // 获取图片尺寸（从内存 byte[]，无磁盘读取）
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        ThrowUtils.throwIf(ObjUtil.isEmpty(bufferedImage), ErrorCode.SYSTEM_ERROR);
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        double scale = (double) width / height;

        // 等待两个处理完成
        String compressedUrl = compressedFuture.join();
        String thumbnailUrl = thumbnailFuture.join();

        UploadPictureDTO dto = new UploadPictureDTO();
        dto.setName(fileName);
        dto.setUrl(compressedUrl);
        dto.setThumbnailUrl(thumbnailUrl);
        dto.setOriginUrl(originImageUrl);
        dto.setPicFormat(fileSuffix);
        dto.setPicSize(fileSize);
        dto.setPicHeight(height);
        dto.setPicWidth(width);
        dto.setPicScale(scale);
        return dto;
    } catch (Exception e) {
        if (url != null) aliOssUtil.deleteByUrl(url);
        log.error("图片上传失败", e);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
    }
    // 不再需要 finally 清理临时文件
}
```

### Step 3: UrlUploadPicture — 内存下载 + 超时配置

**文件**: `server/src/main/java/org/example/server/template/upload/UrlUploadPicture.java`

```java
@Override
protected byte[] getImageBytes(Object inputResource) throws IOException {
    String filePath = (String) inputResource;
    // 带超时的内存下载，替代 HttpUtil.downloadFile 写磁盘
    HttpResponse response = HttpRequest.get(filePath)
            .timeout(10_000)        // 总超时 10s
            .setReadTimeout(15_000) // 读超时 15s
            .executeAsync();
    try {
        return response.bodyStream().readAllBytes();
    } finally {
        response.close();
    }
}
```

同时移除不再需要的 `import cn.hutool.core.io.FileUtil` 等文件相关导入。

### Step 4: FileUploadPicture — 适配新签名

**文件**: `server/src/main/java/org/example/server/template/upload/FileUploadPicture.java`

```java
@Override
protected byte[] getImageBytes(Object inputResource) throws IOException {
    MultipartFile multipartFile = (MultipartFile) inputResource;
    return multipartFile.getBytes();
}
```

## 优化效果对比

| 环节 | 优化前 | 优化后 |
|------|--------|--------|
| getTempFile | 下载到磁盘文件 | 下载到 byte[]（内存） |
| OSS 上传 | readBytes 从磁盘读 → byte[] → 上传 | 直接用内存 byte[] 上传 |
| 图片尺寸 | ImageIO.read(磁盘文件) | ImageIO.read(ByteArrayInputStream) |
| 临时文件 | 创建 + 写入 + 读取 + 删除 | 完全消除 |
| OSS 客户端 | 每次新建/销毁 | 单例复用 |
| 图片处理 | 两次 processPicture 串行 | CompletableFuture 并行 |

**净效果**：消除 3 次磁盘 I/O + 2 次 OSS 连接建立 + 1 次串行等待。

## 验证

1. **编译检查**: `mvn compile -pl common,server` 确保无编译错误
2. **功能测试**:
   - URL 上传: POST `/api/picture/upload/url` 传入图片 URL，验证返回数据完整（url, thumbnailUrl, originUrl, 宽高）
   - 文件上传: POST `/api/picture/upload` 上传 MultipartFile，验证同样完整
3. **OSS 验证**: 在 OSS 控制台确认三份文件（原始、压缩 webp、缩略图）都存在
4. **超时测试**: 传入一个不响应的 URL，确认在 10-15s 内返回错误而非无限等待
