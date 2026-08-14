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
    private File aboutFile;
    private File settingsFile;
    private File newsFile;

    private final ObjectMapper mapper = new ObjectMapper();

    // users: username -> { password, role, displayName, token }
    private Map<String, Map<String, String>> users = new LinkedHashMap<>();
    // collections
    private List<Map<String, Object>> collections = new ArrayList<>();
    // about: { text, imageFilename, imageCaption }
    private Map<String, Object> about = new LinkedHashMap<>();
    // settings
    private Map<String, Object> settings = new LinkedHashMap<>();
    // news: [{ id, title, subtitle, location, publishedAt, updatedAt, summary, content, coverFilename, isHot }]
    private List<Map<String, Object>> news = new ArrayList<>();

    @PostConstruct
    public synchronized void init() throws IOException {
        dataDir = new File(dataPath).getAbsoluteFile();
        if (!dataDir.exists()) dataDir.mkdirs();
        usersFile = new File(dataDir, "users.json");
        collectionsFile = new File(dataDir, "collections.json");
        aboutFile = new File(dataDir, "about.json");
        settingsFile = new File(dataDir, "settings.json");
        newsFile = new File(dataDir, "news.json");

        if (usersFile.exists()) {
            users = mapper.readValue(usersFile, new TypeReference<Map<String, Map<String, String>>>() {});
        }
        if (collectionsFile.exists()) {
            collections = mapper.readValue(collectionsFile, new TypeReference<List<Map<String, Object>>>() {});
        }
        if (aboutFile.exists()) {
            about = mapper.readValue(aboutFile, new TypeReference<Map<String, Object>>() {});
        }
        if (settingsFile.exists()) {
            settings = mapper.readValue(settingsFile, new TypeReference<Map<String, Object>>() {});
        }
        if (newsFile.exists()) {
            news = mapper.readValue(newsFile, new TypeReference<List<Map<String, Object>>>() {});
        }
        if (!about.containsKey("text")) {
            about.put("text", "I'm Haoxin Xia — a photographer capturing quiet moments between light and shadow. This site is a personal archive of the images I keep returning to.");
        }
        if (!about.containsKey("imageFilename")) about.put("imageFilename", null);
        if (!about.containsKey("imageCaption")) about.put("imageCaption", "");
        if (!settings.containsKey("loginBgFilename")) settings.put("loginBgFilename", null);
        if (!settings.containsKey("newsBannerFilename")) settings.put("newsBannerFilename", null);
        if (news.isEmpty()) seedSampleNews();
        saveAbout();
        saveSettings();
        saveNews();

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

    // ---------------- About ----------------

    public synchronized Map<String, Object> getAbout() {
        return about;
    }

    public synchronized void updateAboutText(String text) throws IOException {
        about.put("text", text == null ? "" : text);
        saveAbout();
    }

    public synchronized void setAboutImage(String filename) throws IOException {
        about.put("imageFilename", filename);
        saveAbout();
    }

    public synchronized void updateAboutImageCaption(String caption) throws IOException {
        about.put("imageCaption", caption == null ? "" : caption);
        saveAbout();
    }

    public synchronized String getAboutImageCaption() {
        Object v = about.get("imageCaption");
        return v == null ? "" : String.valueOf(v);
    }

    public synchronized String getAboutImageFilename() {
        Object v = about.get("imageFilename");
        return v == null ? null : String.valueOf(v);
    }

    public synchronized void saveAbout() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(aboutFile, about);
    }

    // ---------------- Settings ----------------

    public synchronized String getLoginBgFilename() {
        Object v = settings.get("loginBgFilename");
        return v == null ? null : String.valueOf(v);
    }

    public synchronized void setLoginBgFilename(String filename) throws IOException {
        settings.put("loginBgFilename", filename);
        saveSettings();
    }

    public synchronized String getNewsBannerFilename() {
        Object v = settings.get("newsBannerFilename");
        return v == null ? null : String.valueOf(v);
    }

    public synchronized void setNewsBannerFilename(String filename) throws IOException {
        settings.put("newsBannerFilename", filename);
        saveSettings();
    }

    public synchronized void saveSettings() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile, settings);
    }

    // ---------------- News ----------------

    public synchronized List<Map<String, Object>> listNews() {
        List<Map<String, Object>> sorted = new ArrayList<>(news);
        sorted.sort((a, b) -> Long.compare(getLong(b, "updatedAt"), getLong(a, "updatedAt")));
        return sorted;
    }

    private long getLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0L;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v));
    }

    public synchronized Map<String, Object> findNews(String id) {
        for (Map<String, Object> n : news) {
            if (id.equals(String.valueOf(n.get("id")))) return n;
        }
        return null;
    }

    public synchronized Map<String, Object> createNews(Map<String, Object> body) throws IOException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", UUID.randomUUID().toString().substring(0, 8));
        long now = System.currentTimeMillis();
        item.put("title", str(body.get("title")));
        item.put("subtitle", str(body.get("subtitle")));
        item.put("location", str(body.get("location")));
        item.put("summary", str(body.get("summary")));
        item.put("content", str(body.get("content")));
        item.put("coverFilename", null);
        item.put("isHot", Boolean.TRUE.equals(body.get("isHot")));
        item.put("publishedAt", now);
        item.put("updatedAt", now);
        news.add(item);
        saveNews();
        return item;
    }

    public synchronized boolean updateNews(String id, Map<String, Object> body) throws IOException {
        Map<String, Object> item = findNews(id);
        if (item == null) return false;
        if (body.containsKey("title")) item.put("title", str(body.get("title")));
        if (body.containsKey("subtitle")) item.put("subtitle", str(body.get("subtitle")));
        if (body.containsKey("location")) item.put("location", str(body.get("location")));
        if (body.containsKey("summary")) item.put("summary", str(body.get("summary")));
        if (body.containsKey("content")) item.put("content", str(body.get("content")));
        if (body.containsKey("isHot")) item.put("isHot", Boolean.TRUE.equals(body.get("isHot")));
        item.put("updatedAt", System.currentTimeMillis());
        saveNews();
        return true;
    }

    public synchronized boolean setNewsCover(String id, String filename) throws IOException {
        Map<String, Object> item = findNews(id);
        if (item == null) return false;
        item.put("coverFilename", filename);
        item.put("updatedAt", System.currentTimeMillis());
        saveNews();
        return true;
    }

    public synchronized boolean deleteNews(String id) throws IOException {
        boolean removed = news.removeIf(n -> id.equals(String.valueOf(n.get("id"))));
        if (removed) saveNews();
        return removed;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private void seedSampleNews() {
        long now = System.currentTimeMillis();
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", "sample01");
        a.put("title", "New Series: Light and Shadow in Lugu Lake");
        a.put("subtitle", "A month-long journey through Yunnan's sacred waters");
        a.put("location", "Yunnan, China");
        a.put("summary", "This spring I spent thirty days by Lugu Lake, following the light from dawn to dusk. The resulting series captures the quiet rhythm of the Mosuo people and the water that shapes their world.");
        a.put("content", "This spring I spent thirty days by Lugu Lake, following the light from dawn to dusk. The resulting series captures the quiet rhythm of the Mosuo people and the water that shapes their world.\n\nEvery morning began at four. I would hike to the eastern ridge before the first fishing boats set out, and wait for the mountains to catch the first blue light. There is a stillness there that feels less like silence than like a held breath.\n\nBy afternoon I would drift with the fishermen. They spoke little but pointed often — a bird, a shadow on the water, a passing storm. Their vocabulary of light was richer than any I have read in books. When the sun softened over the Lion Mountain, they would signal me to lift the camera, and I learned to trust their timing more than my own.\n\nThe series is now being sequenced for an exhibition later this year. I'll share more details soon.");
        a.put("coverFilename", null);
        a.put("isHot", true);
        a.put("publishedAt", now - 3L * 24 * 3600 * 1000);
        a.put("updatedAt", now - 3L * 24 * 3600 * 1000);
        news.add(a);

        Map<String, Object> b = new LinkedHashMap<>();
        b.put("id", "sample02");
        b.put("title", "Print Sale — Autumn Edition Now Open");
        b.put("subtitle", "Limited archival prints, signed and numbered");
        b.put("location", "Online");
        b.put("summary", "A small batch of archival prints from the past three years is now available. Each print is signed, numbered, and produced on Hahnemühle photo rag.");
        b.put("content", "A small batch of archival prints from the past three years is now available. Each print is signed, numbered, and produced on Hahnemühle photo rag.\n\nThis edition includes twelve images selected from Yunnan, Sichuan, and the Tibetan plateau. Sizes range from 12×18 to 24×36 inches. Orders will begin shipping in the first week of the next month.\n\nPlease reach out via the Contact page for any custom sizing or framing questions.");
        b.put("coverFilename", null);
        b.put("isHot", true);
        b.put("publishedAt", now - 10L * 24 * 3600 * 1000);
        b.put("updatedAt", now - 10L * 24 * 3600 * 1000);
        news.add(b);

        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", "sample03");
        c.put("title", "Workshop Announcement: Portraits in Natural Light");
        c.put("subtitle", "Small-group workshop, Shanghai");
        c.put("location", "Shanghai");
        c.put("summary", "A two-day workshop for photographers who want to sharpen their eye for natural light in portrait work. Enrollment is limited to eight participants.");
        c.put("content", "A two-day workshop for photographers who want to sharpen their eye for natural light in portrait work. Enrollment is limited to eight participants.\n\nWe will spend the first day walking the old lanes of the former French Concession, learning to read how buildings and trees shape afternoon light. The second day is studio-based: we will study how a single north-facing window can carry an entire session, and how minimal gear can produce maximal restraint.\n\nA modest lunch is included on both days. Full details and application form will be posted on this site next week.");
        c.put("coverFilename", null);
        c.put("isHot", false);
        c.put("publishedAt", now - 30L * 24 * 3600 * 1000);
        c.put("updatedAt", now - 30L * 24 * 3600 * 1000);
        news.add(c);
    }

    public synchronized void saveNews() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(newsFile, news);
    }
}
