package lain.projects.cloudstorage.storageservice.controller;

import lain.projects.cloudstorage.model.security.UserDetailsImpl;
import org.springframework.core.io.Resource;
import lain.projects.cloudstorage.storageservice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping
    public ResponseEntity<Void> upload(@RequestParam("path") String path,
                                       @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        fileService.uploadFile(fullPath, file);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping()
    public ResponseEntity<Resource> download(@RequestParam("path") String path,
                                             @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        InputStream input = fileService.downloadFile(fullPath);
        InputStreamResource resource = new InputStreamResource(input);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + path)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("path") String path,
                                       @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        fileService.deleteFile(fullPath);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/multi")
    public ResponseEntity<Void> uploadMultiple(@RequestParam("path") String path,
                                               @RequestParam("files") MultipartFile[] files,
                                               @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        for (MultipartFile file : files){
            fileService.uploadFile(fullPath + "/" + file.getOriginalFilename(), file);
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/move")
    public ResponseEntity<Void> move(@RequestParam("from") String from,
                                     @RequestParam("to") String to,
    @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fromPath = buildFullPath(user, from);
        String toPath = buildFullPath(user, to);
        fileService.move(fromPath, toPath);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rename")
    public ResponseEntity<Void> rename(@RequestParam("path") String path,
                                       @RequestParam("newName") String newName,
                                       @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        fileService.renameFile(fullPath, newName);
        return ResponseEntity.ok().build();
    }

    private String buildFullPath(UserDetailsImpl user, String path) {
        String fullPath = "user-" + user.getId() + "-files/" + path;
        return fullPath;
    }
}
