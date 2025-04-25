package lain.projects.cloudstorage.storageservice.service;

import io.minio.*;
import io.minio.messages.Item;
import lain.projects.cloudstorage.storageservice.dto.FileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public void createFolder(String path) throws Exception {
        if (!path.endsWith("/")) {
            path += "/";
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .contentType("application/x-directory")
                        .build()
        );
    }

    public List<FileInfo> listFolder(String path) throws Exception {
        if (!path.endsWith("/")) {
            path += "/";
        }

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(path)
                        .delimiter("/")
                        .build()
        );

        List<FileInfo> items = new ArrayList<>();

        for (Result<Item> result : results) {
            Item item = result.get();

            String fullPath = item.objectName();

            String name = fullPath.substring(path.length());
            if (name.isEmpty()) continue;

            String relativePath = fullPath.substring(fullPath.indexOf("/") + 1);
            boolean isDir = item.isDir();
            Long size = isDir ? null : item.size();

            items.add(new FileInfo(relativePath, name, size, isDir ? "DIRECTORY" : "FILE"));
        }

        return items;
    }

    public void deleteRecursiveFolder(String path) throws Exception {
        if (!path.endsWith("/")) {
            path += "/";
        }

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(path)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.objectName())
                            .build());
        }
    }

    public void moveFolder(String from, String to) throws Exception {
        if (!from.endsWith("/")) from += "/";
        if (!to.endsWith("/")) to += "/";

        // Получаем имя последней папки (например, "folder2" из "folder2/")
        String folderName = from.substring(0, from.length() - 1);
        folderName = folderName.contains("/")
                ? folderName.substring(folderName.lastIndexOf("/") + 1)
                : folderName;

        String targetBasePath = to + folderName + "/";

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(from)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String source = item.objectName();
            String target = targetBasePath + source.substring(from.length());

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucket)
                            .object(target)
                            .source(CopySource.builder()
                                    .bucket(bucket)
                                    .object(source)
                                    .build())
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(source)
                            .build()
            );
        }
    }

    public void streamFolderAsZip(String path, OutputStream outputStream) throws Exception {
        if (!path.endsWith("/")) path += "/";

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(path)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();

                if (item.isDir()) continue;

                String objectName = item.objectName();
                try (InputStream objectStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .build()
                )) {
                    zipOut.putNextEntry(new ZipEntry(objectName.substring(path.length())));
                    objectStream.transferTo(zipOut);
                    zipOut.closeEntry();
                }
            }
            zipOut.finish();
        }
    }

    public void renameFolder(String from, String newName) throws Exception {
        if (!from.endsWith("/")) from += "/";
        String parent = from.substring(0, from.lastIndexOf("/", from.length() - 2) + 1);
        String to = parent + newName + "/";

        // folder1/folder2 -> folder1/NewFolder

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(from)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String source = item.objectName();
            String target = to + source.substring(from.length());

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucket)
                            .object(target)
                            .source(CopySource.builder()
                                    .bucket(bucket)
                                    .object(source)
                                    .build())
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(source)
                            .build()
            );
        }
    }
}
