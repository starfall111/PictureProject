package org.example.shared.util;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.PictureConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.UUID;

/**
 * @author Zou
 */
@Getter
@Slf4j
@Component
public class AliOssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.region}")
    private String region;

    @Value("${aliyun.oss.cdn}")
    private String cdn;

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
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) throws Exception {
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String newFileName = UUID.randomUUID() + objectName.substring(objectName.lastIndexOf("."));
        String FileName = dir + "/" + newFileName;

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, FileName, new ByteArrayInputStream(bytes));
        ossClient.putObject(putObjectRequest);

        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + FileName;
    }

    public String processPicture(String styleType, String sourceImage) throws com.aliyuncs.exceptions.ClientException {
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String newFileName = UUID.randomUUID().toString();
        newFileName = newFileName + ".webp";
        String targetImage = dir + "/" + newFileName;

        // 从 URL 中提取 object key: https://bucket.endpoint/path → path
        String domain = bucketName + "." + endpoint.split("//")[1];
        int domainIndex = sourceImage.indexOf(domain);
        if (domainIndex < 0) {
            log.warn("无法从 URL 提取 object key: {}", sourceImage);
            return "";
        }
        String objectKey = sourceImage.substring(domainIndex + domain.length() + 1);

        StringBuilder sbStyle = new StringBuilder();
        Formatter styleFormatter = new Formatter(sbStyle);
        styleFormatter.format("%s|sys/saveas,o_%s,b_%s", styleType,
                BinaryUtil.toBase64String(targetImage.getBytes()),
                BinaryUtil.toBase64String(bucketName.getBytes()));
        ProcessObjectRequest request = new ProcessObjectRequest(bucketName, objectKey, sbStyle.toString());
        GenericResult processResult = ossClient.processObject(request);
        try {
            String json = IOUtils.readStreamAsString(processResult.getResponse().getContent(), "UTF-8");
            processResult.getResponse().getContent().close();
        } catch (IOException e) {
            log.error("处理图片响应读取异常", e);
        }

        return endpoint.split("//")[0] + "//" + cdn + "/" + targetImage;
    }

    /**
     * 根据完整 URL 删除 OSS 文件
     *
     * @param fileUrl OSS 文件完整 URL
     */
    public void deleteByUrl(String fileUrl) throws Exception {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // 从 URL 中提取 object key: https://zoustarfall.xin/path → path
        String domain = cdn;
        int domainIndex = fileUrl.indexOf(domain);
        if (domainIndex < 0) {
            log.warn("无法从 URL 提取 object key: {}", fileUrl);
            return;
        }
        String objectKey = fileUrl.substring(domainIndex + domain.length() + 1);

        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.info("OSS 文件删除成功: {}", objectKey);
        } catch (OSSException oe) {
            log.error("OSS 删除失败: Code={}, Message={}", oe.getErrorCode(), oe.getErrorMessage());
            throw oe;
        } catch (ClientException ce) {
            log.error("OSS 客户端异常: {}", ce.getMessage());
            throw ce;
        }
    }

    /*
     *  下载文件
     *
     * @parm
     * */
    public void download(String imageUrl, String name, long id) throws com.aliyuncs.exceptions.ClientException {
        String domain = cdn;
        int domainIndex = imageUrl.indexOf(domain);
        if (domainIndex < 0) {
            log.warn("无法从 URL 提取 object key: {}", imageUrl);
            return;
        }
        String objectKey = imageUrl.substring(domainIndex + domain.length() + 1);
        String pathName = PictureConstant.TEMP_FILE_URL + id + "_" + name;

        ossClient.getObject(new GetObjectRequest(bucketName, objectKey), new File(pathName));
    }
}
