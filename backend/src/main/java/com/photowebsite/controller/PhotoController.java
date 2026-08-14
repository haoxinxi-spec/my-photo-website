package com.photowebsite.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    @Value("${upload.path}")
    private String uploadPath;

    private File uploadDir;

    @PostConstruct
    public void init() {
        uploadDir = new File(uploadPath).getAbsoluteFile();
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        System.out.println("[PhotoController] Upload directory resolved to: " + uploadDir.getAbsolutePath());
    }

    @GetMapping("/list")
    public Map<String, Object> listPhotos() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> photos = new ArrayList<>();

        File[] files = uploadDir.listFiles();
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (File file : files) {
                if (file.isFile() && isImageFile(file.getName())) {
                    Map<String, String> photo = new HashMap<>();
                    photo.put("name", file.getName());
                    photo.put("url", "/uploads/" + file.getName());
                    photos.add(photo);
                }
            }
        }

        response.put("success", true);
        response.put("photos", photos);
        return response;
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadPhoto(@RequestParam("file") MultipartFile file,
                                            @RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();

        if (!"photo-website-token-2026".equals(token)) {
            response.put("success", false);
            response.put("message", "未授权，请先登录");
            return response;
        }

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "文件为空");
            return response;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            File destination = new File(uploadDir, filename);
            file.transferTo(destination);

            response.put("success", true);
            response.put("message", "上传成功");
            response.put("filename", filename);
            response.put("url", "/uploads/" + filename);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "上传失败: " + e.getMessage());
        }

        return response;
    }

    @DeleteMapping("/delete/{filename}")
    public Map<String, Object> deletePhoto(@PathVariable String filename,
                                            @RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();

        if (!"photo-website-token-2026".equals(token)) {
            response.put("success", false);
            response.put("message", "未授权");
            return response;
        }

        File file = new File(uploadDir, filename);
        if (file.exists() && file.delete()) {
            response.put("success", true);
            response.put("message", "删除成功");
        } else {
            response.put("success", false);
            response.put("message", "文件不存在或删除失败");
        }
        return response;
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".bmp") || lower.endsWith(".webp");
    }
}
