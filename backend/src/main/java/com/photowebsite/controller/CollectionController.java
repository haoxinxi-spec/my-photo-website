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
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired
    private DataStore dataStore;

    @Value("${upload.path}")
    private String uploadPath;

    private File uploadDir;

    @PostConstruct
    public void init() {
        uploadDir = new File(uploadPath).getAbsoluteFile();
        if (!uploadDir.exists()) uploadDir.mkdirs();
        System.out.println("[CollectionController] Upload dir: " + uploadDir.getAbsolutePath());
    }

    private boolean isAdmin(String token) {
        Map<String, String> u = dataStore.findUserByToken(token);
        return u != null && "admin".equals(u.get("role"));
    }

    private boolean isAuthenticated(String token) {
        return dataStore.findUserByToken(token) != null;
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> collections = new ArrayList<>();
        for (Map<String, Object> c : dataStore.listCollections()) {
            Map<String, Object> item = new LinkedHashMap<>(c);
            String cover = (String) c.get("coverFilename");
            item.put("coverUrl", cover != null ? "/uploads/" + cover : null);
            List<?> photos = (List<?>) c.get("photos");
            item.put("photoCount", photos == null ? 0 : photos.size());
            item.remove("photos");
            collections.add(item);
        }
        response.put("success", true);
        response.put("collections", collections);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> c = dataStore.findCollection(id);
        if (c == null) {
            response.put("success", false);
            response.put("message", "Collection not found");
            return response;
        }
        Map<String, Object> item = new LinkedHashMap<>(c);
        String cover = (String) c.get("coverFilename");
        item.put("coverUrl", cover != null ? "/uploads/" + cover : null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> photos = (List<Map<String, Object>>) c.get("photos");
        List<Map<String, Object>> photoList = new ArrayList<>();
        if (photos != null) {
            for (Map<String, Object> p : photos) {
                Map<String, Object> photo = new LinkedHashMap<>(p);
                photo.put("url", "/uploads/" + p.get("filename"));
                photoList.add(photo);
            }
        }
        item.put("photos", photoList);
        response.put("success", true);
        response.put("collection", item);
        return response;
    }

    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, String> body,
                                       @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        String title = body.get("title");
        if (title == null || title.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Title required");
            return response;
        }
        Map<String, Object> c = dataStore.createCollection(title.trim(), body.get("description"));
        response.put("success", true);
        response.put("collection", c);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id,
                                       @RequestBody Map<String, String> body,
                                       @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        boolean ok = dataStore.updateCollection(id, body.get("title"), body.get("description"));
        response.put("success", ok);
        if (!ok) response.put("message", "Collection not found");
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
        String filename = saveFile(file);
        if (filename == null) {
            response.put("success", false);
            response.put("message", "Upload failed");
            return response;
        }
        dataStore.setCollectionCover(id, filename);
        response.put("success", true);
        response.put("filename", filename);
        response.put("url", "/uploads/" + filename);
        return response;
    }

    @PostMapping("/{id}/photos")
    public Map<String, Object> uploadPhoto(@PathVariable String id,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        if (dataStore.findCollection(id) == null) {
            response.put("success", false);
            response.put("message", "Collection not found");
            return response;
        }
        String filename = saveFile(file);
        if (filename == null) {
            response.put("success", false);
            response.put("message", "Upload failed");
            return response;
        }
        dataStore.addPhoto(id, filename, description);
        response.put("success", true);
        response.put("filename", filename);
        response.put("url", "/uploads/" + filename);
        return response;
    }

    @PutMapping("/{id}/photos/{filename}")
    public Map<String, Object> updatePhotoDesc(@PathVariable String id,
                                                @PathVariable String filename,
                                                @RequestBody Map<String, String> body,
                                                @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        boolean ok = dataStore.updatePhotoDescription(id, filename, body.get("description"));
        response.put("success", ok);
        return response;
    }

    @DeleteMapping("/{id}/photos/{filename}")
    public Map<String, Object> deletePhoto(@PathVariable String id,
                                            @PathVariable String filename,
                                            @RequestHeader(value = "Authorization", required = false) String token) throws IOException {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(token)) {
            response.put("success", false);
            response.put("message", "Admin required");
            return response;
        }
        boolean removed = dataStore.deletePhoto(id, filename);
        if (removed) {
            File f = new File(uploadDir, filename);
            if (f.exists()) f.delete();
        }
        response.put("success", removed);
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
        response.put("success", dataStore.deleteCollection(id));
        return response;
    }

    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        File dest = new File(uploadDir, filename);
        file.transferTo(dest);
        return filename;
    }
}
