package com.photowebsite.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class DataStore {

    @Value("${data.path}")
    private String dataPath;

    private File dataDir;
    private File usersFile;
    private File collectionsFile;

    private final ObjectMapper mapper = new ObjectMapper();

    // users: username -> { password, role, displayName, token }
    private Map<String, Map<String, String>> users = new LinkedHashMap<>();
    // collections: id -> { id, title, description, coverFilename, createdAt, photos:[{filename,description,uploadedAt}] }
    private List<Map<String, Object>> collections = new ArrayList<>();

    @PostConstruct
    public synchronized void init() throws IOException {
        dataDir = new File(dataPath).getAbsoluteFile();
        if (!dataDir.exists()) dataDir.mkdirs();
        usersFile = new File(dataDir, "users.json");
        collectionsFile = new File(dataDir, "collections.json");

        if (usersFile.exists()) {
            users = mapper.readValue(usersFile, new TypeReference<Map<String, Map<String, String>>>() {});
        }
        if (collectionsFile.exists()) {
            collections = mapper.readValue(collectionsFile, new TypeReference<List<Map<String, Object>>>() {});
        }

        // 保证默认账号存在
        if (!users.containsKey("admin")) {
            Map<String, String> admin = new LinkedHashMap<>();
            admin.put("password", "Bblvd77121");
            admin.put("role", "admin");
            admin.put("displayName", "Haoxin Xia");
            admin.put("token", "admin-token-" + UUID.randomUUID());
            users.put("admin", admin);
        }
        if (!users.containsKey("guest")) {
            Map<String, String> guest = new LinkedHashMap<>();
            guest.put("password", "123456");
            guest.put("role", "guest");
            guest.put("displayName", "Guest");
            guest.put("token", "guest-token-" + UUID.randomUUID());
            users.put("guest", guest);
        }
        saveUsers();
        System.out.println("[DataStore] data dir: " + dataDir.getAbsolutePath());
    }

    // ---------------- Users ----------------

    public synchronized Map<String, String> findUser(String username) {
        return users.get(username);
    }

    public synchronized Map<String, String> findUserByToken(String token) {
        if (token == null) return null;
        for (Map<String, String> u : users.values()) {
            if (token.equals(u.get("token"))) return u;
        }
        return null;
    }

    public synchronized boolean registerUser(String username, String password, String displayName) throws IOException {
        if (users.containsKey(username)) return false;
        Map<String, String> u = new LinkedHashMap<>();
        u.put("password", password);
        u.put("role", "guest");
        u.put("displayName", displayName == null || displayName.isEmpty() ? username : displayName);
        u.put("token", "user-token-" + UUID.randomUUID());
        users.put(username, u);
        saveUsers();
        return true;
    }

    public synchronized void saveUsers() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(usersFile, users);
    }

    // ---------------- Collections ----------------

    public synchronized List<Map<String, Object>> listCollections() {
        return collections;
    }

    public synchronized Map<String, Object> findCollection(String id) {
        for (Map<String, Object> c : collections) {
            if (id.equals(String.valueOf(c.get("id")))) return c;
        }
        return null;
    }

    public synchronized Map<String, Object> createCollection(String title, String description) throws IOException {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", UUID.randomUUID().toString().substring(0, 8));
        c.put("title", title);
        c.put("description", description == null ? "" : description);
        c.put("coverFilename", null);
        c.put("createdAt", System.currentTimeMillis());
        c.put("photos", new ArrayList<Map<String, Object>>());
        collections.add(0, c);
        saveCollections();
        return c;
    }

    public synchronized boolean updateCollection(String id, String title, String description) throws IOException {
        Map<String, Object> c = findCollection(id);
        if (c == null) return false;
        if (title != null) c.put("title", title);
        if (description != null) c.put("description", description);
        saveCollections();
        return true;
    }

    public synchronized boolean setCollectionCover(String id, String filename) throws IOException {
        Map<String, Object> c = findCollection(id);
        if (c == null) return false;
        c.put("coverFilename", filename);
        saveCollections();
        return true;
    }

    public synchronized boolean deleteCollection(String id) throws IOException {
        Iterator<Map<String, Object>> it = collections.iterator();
        while (it.hasNext()) {
            if (id.equals(String.valueOf(it.next().get("id")))) {
                it.remove();
                saveCollections();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public synchronized void addPhoto(String collectionId, String filename, String description) throws IOException {
        Map<String, Object> c = findCollection(collectionId);
        if (c == null) return;
        List<Map<String, Object>> photos = (List<Map<String, Object>>) c.get("photos");
        Map<String, Object> photo = new LinkedHashMap<>();
        photo.put("filename", filename);
        photo.put("description", description == null ? "" : description);
        photo.put("uploadedAt", System.currentTimeMillis());
        photos.add(photo);
        saveCollections();
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean updatePhotoDescription(String collectionId, String filename, String description) throws IOException {
        Map<String, Object> c = findCollection(collectionId);
        if (c == null) return false;
        List<Map<String, Object>> photos = (List<Map<String, Object>>) c.get("photos");
        for (Map<String, Object> p : photos) {
            if (filename.equals(p.get("filename"))) {
                p.put("description", description == null ? "" : description);
                saveCollections();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean deletePhoto(String collectionId, String filename) throws IOException {
        Map<String, Object> c = findCollection(collectionId);
        if (c == null) return false;
        List<Map<String, Object>> photos = (List<Map<String, Object>>) c.get("photos");
        boolean removed = photos.removeIf(p -> filename.equals(p.get("filename")));
        if (removed) saveCollections();
        return removed;
    }

    public synchronized void saveCollections() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(collectionsFile, collections);
    }
}
