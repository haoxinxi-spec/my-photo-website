# 我的照片站 (My Photo Website)

一个个人照片展示网站：Vue 3 前端 + Spring Boot 后端。

## 功能

- 登录页面（初始用户名 `admin` / 密码 `Bblvd77121`）
- 左上角头像，点击展开下拉栏：填写个人简介、上传新照片、退出登录
- 主页展示所有照片，采用瀑布流不规则布局，圆角处理
- 点击照片可放大预览

## 目录结构

```
my-photo-website/
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/photowebsite/
│   │   ├── PhotoWebsiteApplication.java
│   │   ├── config/WebConfig.java
│   │   └── controller/
│   │       ├── AuthController.java
│   │       └── PhotoController.java
│   ├── src/main/resources/application.properties
│   ├── uploads/              # 上传的照片存放目录
│   └── pom.xml
└── frontend/                 # Vue 3 前端
    ├── src/
    │   ├── views/{Login,Home}.vue
    │   ├── router/index.js
    │   ├── App.vue
    │   └── main.js
    ├── public/index.html
    ├── vue.config.js
    └── package.json
```

## 快速启动

### 后端

需要 JDK 8+ 和 Maven。

```bash
cd backend
mvn spring-boot:run
```

后端将监听 `http://localhost:8080`。

### 前端

需要 Node.js 16+。

```bash
cd frontend
npm install
npm run serve
```

前端将监听 `http://localhost:8081`，已配置代理，会自动转发 `/api` 和 `/uploads` 到后端。

浏览器打开 http://localhost:8081 即可访问。

## 登录信息

- 用户名：`admin`
- 密码：`Bblvd77121`

## API 说明

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录 |
| GET  | `/api/photos/list` | 获取照片列表 |
| POST | `/api/photos/upload` | 上传照片（需 token） |
| DELETE | `/api/photos/delete/{filename}` | 删除照片（需 token） |

## 说明

- 未使用数据库，用户信息硬编码在后端，照片以文件形式存于 `backend/uploads/` 目录。
- 前端登录 token 存于浏览器 `localStorage`。
