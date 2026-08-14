package com.photowebsite.controller;

import com.photowebsite.service.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/about")
public class AboutController {

    @Autowired
    private DataStore dataStore;

    @Value("${upload.path}")
    private String uploadPath;

    private File uploadDir;

    @PostConstruct
    public void init() {
        uploadDir = new File(uploadPath).getAbsoluteFile();
        if (!uploadDir.exists()) uploadDir.mkdirs();
    }

    private boolean isAdmin(String token) {
        Map<String, String> u = dataStore.findUserByToken(token);
        return u != null && "admin".equals(u.get("role"));
    }

    @GetMapping
    public Map<String, Object> get() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> about = dataStore.getAbout();
        response.put("success", true);
        response.put("text", about.get("text"));
        String img = dataStore.getAboutImageFilename();
        response.put("imageUrl", img != null ? "/uploads/" + img : null);
        return response;
    }

    @PutMapping("/text")
    public Map<String, Object> updateText(@RequestBody Map<String, String> body,
                                           @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        dataStore.updateAboutText(body.get("text"));
        response.put("success", true);
        return response;
    }

    @PostMapping("/image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file,
                                            @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        if (file == null || file.isEmpty()) {
            response.put("success", false);
            response.put("message", "File is empty");
            return response;
        }

        // 删除旧图片文件（如果存在）
        String oldFilename = dataStore.getAboutImageFilename();
        if (oldFilename != null) {
            File oldFile = new File(uploadDir, oldFilename);
            if (oldFile.exists()) oldFile.delete();
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = "about_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        File dest = new File(uploadDir, filename);
        file.transferTo(dest);

        dataStore.setAboutImage(filename);
        response.put("success", true);
        response.put("imageUrl", "/uploads/" + filename);
        return response;
    }

    @DeleteMapping("/image")
    public Map<String, Object> deleteImage(@RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        String filename = dataStore.getAboutImageFilename();
        if (filename != null) {
            File f = new File(uploadDir, filename);
            if (f.exists()) f.delete();
        }
        dataStore.setAboutImage(null);
        response.put("success", true);
        return response;
    }
}
