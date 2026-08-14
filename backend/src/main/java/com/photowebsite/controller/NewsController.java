package com.photowebsite.controller;

import com.photowebsite.service.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/news")
public class NewsController {

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

    private Map<String, Object> withUrl(Map<String, Object> raw) {
        Map<String, Object> item = new LinkedHashMap<>(raw);
        Object cover = raw.get("coverFilename");
        item.put("coverUrl", cover != null ? "/uploads/" + cover : null);
        return item;
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> n : dataStore.listNews()) {
            items.add(withUrl(n));
        }
        response.put("success", true);
        response.put("items", items);
        return response;
    }

    @GetMapping("/hot")
    public Map<String, Object> hot() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> n : dataStore.listNews()) {
            if (Boolean.TRUE.equals(n.get("isHot"))) items.add(withUrl(n));
        }
        response.put("success", true);
        response.put("items", items);
        return response;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("q") String q) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        if (q != null && !q.trim().isEmpty()) {
            String needle = q.trim().toLowerCase();
            for (Map<String, Object> n : dataStore.listNews()) {
                if (matches(n, needle)) items.add(withUrl(n));
            }
        }
        response.put("success", true);
        response.put("items", items);
        return response;
    }

    private boolean matches(Map<String, Object> n, String needle) {
        return contains(n.get("title"), needle) ||
               contains(n.get("subtitle"), needle) ||
               contains(n.get("summary"), needle) ||
               contains(n.get("content"), needle) ||
               contains(n.get("location"), needle);
    }

    private boolean contains(Object v, String needle) {
        return v != null && String.valueOf(v).toLowerCase().contains(needle);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> item = dataStore.findNews(id);
        if (item == null) {
            response.put("success", false);
            response.put("message", "News not found");
            return response;
        }
        response.put("success", true);
        response.put("item", withUrl(item));
        return response;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        Map<String, Object> item = dataStore.createNews(body);
        response.put("success", true);
        response.put("item", withUrl(item));
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id,
                                       @RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        boolean ok = dataStore.updateNews(id, body);
        response.put("success", ok);
        return response;
    }

    @PostMapping("/{id}/cover")
    public Map<String, Object> uploadCover(@PathVariable String id,
                                            @RequestParam("file") MultipartFile file,
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
        Map<String, Object> item = dataStore.findNews(id);
        if (item == null) {
            response.put("success", false);
            response.put("message", "News not found");
            return response;
        }
        Object oldCover = item.get("coverFilename");
        if (oldCover != null) {
            File oldFile = new File(uploadDir, String.valueOf(oldCover));
            if (oldFile.exists()) oldFile.delete();
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = "news_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        File dest = new File(uploadDir, filename);
        file.transferTo(dest);
        dataStore.setNewsCover(id, filename);

        response.put("success", true);
        response.put("imageUrl", "/uploads/" + filename);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id,
                                       @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        Map<String, Object> item = dataStore.findNews(id);
        if (item != null && item.get("coverFilename") != null) {
            File f = new File(uploadDir, String.valueOf(item.get("coverFilename")));
            if (f.exists()) f.delete();
        }
        response.put("success", dataStore.deleteNews(id));
        return response;
    }
}
