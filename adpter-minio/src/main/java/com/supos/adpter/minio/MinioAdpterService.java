package com.supos.adpter.minio;

import com.supos.adpter.minio.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sunlifang
 * @version 1.0
 * @description: TODO
 * @date 2025/1/8 15:44
 */
@Slf4j
@Component
public class MinioAdpterService {

    @Resource
    private MinioClient minioClient;

    /**
     * 校验指定桶是否存在
     * @param bucket
     * @return
     */
    private boolean bucketExist(String bucket) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
    }

    /**
     * 创建指定桶
     * @param bucket
     * @throws ServerException
     * @throws InsufficientDataException
     * @throws ErrorResponseException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws InvalidResponseException
     * @throws XmlParserException
     * @throws InternalException
     */
    private void createBucket(String bucket) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }

    /**
     * 上传附件
     * @param objectName
     * @param file
     */
    public void upload(String objectName, MultipartFile file) {
        try {
            if (!bucketExist(MinioConfig.DEFAULT_BUCKET)) {
                createBucket(MinioConfig.DEFAULT_BUCKET);
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(MinioConfig.DEFAULT_BUCKET)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1).build());

        } catch (Exception e) {
            log.error("upload to minio error!", e);
            throw new RuntimeException("upload to minio error!");
        }

    }

    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(MinioConfig.DEFAULT_BUCKET)
                    .object(objectName).build());

        } catch (Exception e) {
            log.error("download from minio error!", e);
            throw new RuntimeException("download from minio error!");
        }
    }

    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(MinioConfig.DEFAULT_BUCKET)
                    .object(objectName).build());

        } catch (Exception e) {
            log.error("delete from minio error!", e);
            throw new RuntimeException("delete from minio error!");
        }
    }

    public void delete(List<String> objectNames) {
        try {
            List<DeleteObject> deleteObjects = objectNames.stream().map(objectName -> new DeleteObject(objectName)).collect(Collectors.toList());
            minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(MinioConfig.DEFAULT_BUCKET)
                    .objects(deleteObjects)
                    .build());

        } catch (Exception e) {
            log.error("delete from minio error!", e);
            throw new RuntimeException("delete from minio error!");
        }
    }
}
