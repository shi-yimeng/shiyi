package cc.shiyi.service.impl;

import cc.shiyi.constant.MessageConstant;
import cc.shiyi.exception.UploadFileErrorException;
import cc.shiyi.properties.ImageProperties;
import cc.shiyi.service.CommonService;
import cc.shiyi.utils.AliOssUtil;
import cc.shiyi.utils.ImageCompressUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class CommonServiceImpl implements CommonService {

    @Autowired
    private AliOssUtil aliOssUtil;
    @Autowired
    private ImageCompressUtil imageCompressUtil;
    @Autowired
    private ImageProperties imageProperties;

    /** 本地文件上传根目录（绝对路径），可通过 yml 的 shiyi.upload.dir 覆盖 */
    @Value("${shiyi.upload.dir:/var/www/uploads}")
    private String localUploadDir;

    /** 本地上传对外 URL 前缀（默认 /static/uploads，Nginx 需 alias 到 localUploadDir） */
    @Value("${shiyi.upload.url-prefix:/static/uploads}")
    private String localUrlPrefix;

    /** 是否启用本地兜底：当 shiyi.alioss.endpoint 为空时自动启用 */
    private volatile boolean useLocalFallback = false;

    @PostConstruct
    public void init() {
        String endpoint = aliOssUtil.getEndpoint() == null ? "" : aliOssUtil.getEndpoint().trim();
        String bucket = aliOssUtil.getBucketName() == null ? "" : aliOssUtil.getBucketName().trim();
        String ak = aliOssUtil.getAccessKeyId() == null ? "" : aliOssUtil.getAccessKeyId().trim();
        this.useLocalFallback = !StringUtils.hasText(endpoint)
                || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(ak);
        if (useLocalFallback) {
            log.warn("[upload] AliOSS 未配置（endpoint/bucket/ak 任一为空），启用本地兜底：dir={}, urlPrefix={}",
                    localUploadDir, localUrlPrefix);
            try {
                Files.createDirectories(Paths.get(localUploadDir));
            } catch (IOException e) {
                throw new RuntimeException("无法创建本地上传目录: " + localUploadDir, e);
            }
        }
    }

    /**
     * 文件上传：优先走 AliOSS；未配置时走本地磁盘，返回可直接访问的 URL 路径
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadFileErrorException(MessageConstant.FILE_EMPTY);
        }
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "file.bin";
            }
            String extension = "bin";
            int dot = fileName.lastIndexOf(".");
            if (dot >= 0 && dot < fileName.length() - 1) {
                extension = fileName.substring(dot + 1).toLowerCase();
            }

            byte[] bytes = file.getBytes();
            // 图片统一压缩并转配置的输出格式
            if ("image".equals(aliOssUtil.getFileCategory(extension))) {
                bytes = imageCompressUtil.compress(file);
                extension = imageProperties.getOutPutFormat();
            }
            String uuidFileName = UUID.randomUUID() + "." + extension;

            if (useLocalFallback) {
                return saveLocal(bytes, extension, uuidFileName);
            }
            return aliOssUtil.upload(bytes, extension, uuidFileName);

        } catch (IOException e) {
            log.error("[upload] 文件上传失败: {}", e.getMessage(), e);
            throw new UploadFileErrorException(MessageConstant.UPLOAD_FAILED);
        }
    }

    private String saveLocal(byte[] bytes, String extension, String uuidFileName) throws IOException {
        String category = aliOssUtil.getFileCategory(extension);
        Path targetDir = Paths.get(localUploadDir, category);
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(uuidFileName);
        try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        String prefix = localUrlPrefix.endsWith("/") ? localUrlPrefix : localUrlPrefix + "/";
        String url = prefix + category + "/" + uuidFileName;
        log.info("[upload] 本地保存成功: {} -> {}", targetFile, url);
        return url;
    }
}
