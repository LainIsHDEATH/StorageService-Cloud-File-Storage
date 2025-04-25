package lain.projects.cloudstorage.storageservice.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.jni.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public void uploadFile(String path, MultipartFile file) throws Exception {
        InputStream inputStream = file.getInputStream();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    public InputStream downloadFile(String path) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .build()
        );
    }

    public void deleteFile(String path) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .build()
        );
    }

    public void move(String from, String to) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(to)
                        .source(CopySource.builder()
                                .bucket(bucket)
                                .object(from)
                                .build())
                        .build()
        );

        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(from)
                        .build()
        );
    }

    public void renameFile(String from, String newName) throws Exception {
        String dir = from.contains("/") ? from.substring(0, from.lastIndexOf("/") + 1) : "";
        String to = dir + newName;
        move(from, to);
    }
}
