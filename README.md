# Haoxin Xia — Photography Website

Personal photography site with two experiences:

- **Admin panel** — organize photos into **Collections**, upload covers, edit descriptions.
- **Guest gallery** — Steve McCurry-inspired artistic layout with `PORTFOLIO / SELECTED WORKS / SHOP / NEWS / ABOUT / CONTACT`.

Stack: Vue 3 + vue-router + axios (frontend) · Spring Boot 2 (backend) · JSON files on disk (no DB).

## Default Accounts

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `Bblvd77121` |
| Guest | `guest` | `123456` |

The login screen also lets anyone **register** — new accounts get guest access.

## Project Layout

```
my-photo-website/
├── backend/                                    # Spring Boot
│   ├── src/main/java/com/photowebsite/
│   │   ├── PhotoWebsiteApplication.java
│   │   ├── config/WebConfig.java
│   │   ├── service/DataStore.java              # JSON persistence
│   │   └── controller/
│   │       ├── AuthController.java             # login / register / me
│   │       └── CollectionController.java       # CRUD for collections & photos
│   ├── src/main/resources/application.properties
│   ├── uploads/                                # uploaded images
│   ├── data/                                   # users.json / collections.json
│   └── pom.xml
└── frontend/                                   # Vue 3
    ├── src/
    │   ├── views/
    │   │   ├── Login.vue                       # sign-in + register (EN)
    │   │   ├── AdminCollections.vue            # admin: collection list
    │   │   ├── AdminCollectionDetail.vue       # admin: edit collection + photos
    │   │   └── GuestGallery.vue                # public site
    │   ├── router/index.js                     # role-based guards
    │   ├── App.vue
    │   └── main.js
    ├── public/index.html                       # loads Playfair Display
    ├── vue.config.js
    └── package.json
```

## Run

### Backend
```bash
cd backend
mvn spring-boot:run          # http://localhost:8080
```
Data is persisted to `backend/data/*.json` and image files under `backend/uploads/`.

### Frontend
```bash
cd frontend
npm install
npm run serve                # http://localhost:8081
```
Vite/webpack dev server proxies `/api` and `/uploads` to the backend.

## API Overview

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/auth/login` | Returns token + role |
| POST | `/api/auth/register` | Creates a guest account |
| GET  | `/api/auth/me` | Verify token |
| GET  | `/api/collections/list` | Public — list of collections with cover |
| GET  | `/api/collections/{id}` | Public — collection detail with photos |
| POST | `/api/collections/create` | Admin |
| PUT  | `/api/collections/{id}` | Admin — edit title/description |
| POST | `/api/collections/{id}/cover` | Admin — upload cover image |
| POST | `/api/collections/{id}/photos` | Admin — upload a photo |
| PUT  | `/api/collections/{id}/photos/{filename}` | Admin — edit description |
| DELETE | `/api/collections/{id}/photos/{filename}` | Admin |
| DELETE | `/api/collections/{id}` | Admin |
