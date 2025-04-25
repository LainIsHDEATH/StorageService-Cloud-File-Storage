package lain.projects.cloudstorage.storageservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import lain.projects.cloudstorage.storageservice.dto.FileInfo;
import lain.projects.cloudstorage.storageservice.service.FolderService;
import lain.projects.cloudstorage.model.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/directory")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping()
    public ResponseEntity<Void> createDirectory(@RequestParam("path") String path,
                                                @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        folderService.createFolder(fullPath);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping()
    public ResponseEntity<List<FileInfo>> getDirectory(@RequestParam("path") String path,
                                                       @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        List<FileInfo> contents = folderService.listFolder(fullPath);
        return ResponseEntity.ok(contents);
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteDirectory(@RequestParam("path") String path,
                                                @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        folderService.deleteRecursiveFolder(fullPath);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/move-folder")
    public ResponseEntity<Void> move(@RequestParam("from") String from,
                                     @RequestParam("to") String to,
                                     @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fromPath = buildFullPath(user, from);
        String toPath = buildFullPath(user, to);
        folderService.moveFolder(from, to);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download-folder")
    public void downloadDirectory(@RequestParam("path") String path, HttpServletResponse response,
                                  @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + path.replace("/", "_") + ".zip");
        folderService.streamFolderAsZip(fullPath, response.getOutputStream());
    }

    @GetMapping("/rename-folder")
    public ResponseEntity<Void> renameFolder(@RequestParam("path") String path,
                                             @RequestParam("newName") String newName,
                                             @AuthenticationPrincipal UserDetailsImpl user) throws Exception {
        String fullPath = buildFullPath(user, path);
        folderService.renameFolder(fullPath, newName);
        return ResponseEntity.ok().build();
    }

    private String buildFullPath(UserDetailsImpl user, String path) {
        String fullPath = "user-" + user.getId() + "-files/" + path;
        return fullPath;
    }
}
