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
@RequestMapping("/api/settings")
public class SettingsController {

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

    @GetMapping("/login-bg")
    public Map<String, Object> getLoginBg() {
        Map<String, Object> response = new HashMap<>();
        String filename = dataStore.getLoginBgFilename();
        response.put("success", true);
        response.put("imageUrl", filename != null ? "/uploads/" + filename : null);
        return response;
    }

    @PostMapping("/login-bg")
    public Map<String, Object> uploadLoginBg(@RequestParam("file") MultipartFile file,
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

        String oldFilename = dataStore.getLoginBgFilename();
        if (oldFilename != null) {
            File oldFile = new File(uploadDir, oldFilename);
            if (oldFile.exists()) oldFile.delete();
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = "loginbg_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        File dest = new File(uploadDir, filename);
        file.transferTo(dest);

        dataStore.setLoginBgFilename(filename);
        response.put("success", true);
        response.put("imageUrl", "/uploads/" + filename);
        return response;
    }

    @DeleteMapping("/login-bg")
    public Map<String, Object> deleteLoginBg(@RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        String filename = dataStore.getLoginBgFilename();
        if (filename != null) {
            File f = new File(uploadDir, filename);
            if (f.exists()) f.delete();
        }
        dataStore.setLoginBgFilename(null);
        response.put("success", true);
        return response;
    }

    // ---------------- News banner ----------------

    @GetMapping("/news-banner")
    public Map<String, Object> getNewsBanner() {
        Map<String, Object> response = new HashMap<>();
        String filename = dataStore.getNewsBannerFilename();
        response.put("success", true);
        response.put("imageUrl", filename != null ? "/uploads/" + filename : null);
        return response;
    }

    @PostMapping("/news-banner")
    public Map<String, Object> uploadNewsBanner(@RequestParam("file") MultipartFile file,
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
        String old = dataStore.getNewsBannerFilename();
        if (old != null) {
            File oldFile = new File(uploadDir, old);
            if (oldFile.exists()) oldFile.delete();
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = "newsbanner_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        File dest = new File(uploadDir, filename);
        file.transferTo(dest);
        dataStore.setNewsBannerFilename(filename);
        response.put("success", true);
        response.put("imageUrl", "/uploads/" + filename);
        return response;
    }

    @DeleteMapping("/news-banner")
    public Map<String, Object> deleteNewsBanner(@RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        String filename = dataStore.getNewsBannerFilename();
        if (filename != null) {
            File f = new File(uploadDir, filename);
            if (f.exists()) f.delete();
        }
        dataStore.setNewsBannerFilename(null);
        response.put("success", true);
        return response;
    }
}
