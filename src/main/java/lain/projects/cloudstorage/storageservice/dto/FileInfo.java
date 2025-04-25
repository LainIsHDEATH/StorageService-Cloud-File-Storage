package lain.projects.cloudstorage.storageservice.dto;

public record FileInfo(
        String path,
        String name,
        Long size, // null для папки
        String type // FILE или DIRECTORY
) {}
