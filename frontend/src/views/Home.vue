<template>
  <div class="home">
    <!-- 左上角头像 + 下拉菜单 -->
    <div class="avatar-wrapper" v-click-outside="closeDropdown">
      <div class="avatar" @click="toggleDropdown">
        <span>{{ avatarText }}</span>
      </div>
      <transition name="fade">
        <div v-if="showDropdown" class="dropdown">
          <div class="dropdown-header">
            <div class="avatar-large">
              <span>{{ avatarText }}</span>
            </div>
            <div class="user-name">{{ username }}</div>
          </div>

          <div class="dropdown-section">
            <label class="section-label">个人简介</label>
            <textarea
              v-model="bio"
              @blur="saveBio"
              placeholder="写一段介绍你自己的话..."
              rows="4"
            ></textarea>
          </div>

          <div class="dropdown-section">
            <label class="section-label upload-label" for="upload-input">
              📷 上传新照片
            </label>
            <input
              id="upload-input"
              type="file"
              accept="image/*"
              @change="handleUpload"
              multiple
              style="display:none"
            />
            <div v-if="uploading" class="upload-status">上传中...</div>
            <div v-if="uploadMsg" class="upload-msg" :class="{ error: uploadError }">
              {{ uploadMsg }}
            </div>
          </div>

          <div class="dropdown-footer">
            <button class="logout-btn" @click="logout">退出登录</button>
          </div>
        </div>
      </transition>
    </div>

    <!-- 页面标题 -->
    <header class="page-header">
      <h1>我的照片墙</h1>
      <p v-if="bio" class="header-bio">{{ bio }}</p>
    </header>

    <!-- 照片展示（不规则排列） -->
    <main class="photo-gallery">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="photos.length === 0" class="empty">
        <p>还没有照片，点击左上角头像上传你的第一张照片吧 ✨</p>
      </div>
      <div v-else class="masonry">
        <div
          v-for="(photo, idx) in photos"
          :key="photo.name"
          class="photo-item"
          :class="'variant-' + (idx % 5)"
          @click="openPreview(photo)"
        >
          <img :src="photo.url" :alt="photo.name" loading="lazy" />
        </div>
      </div>
    </main>

    <!-- 大图预览 -->
    <transition name="fade">
      <div v-if="preview" class="preview-mask" @click="closePreview">
        <img :src="preview.url" :alt="preview.name" />
      </div>
    </transition>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'HomeView',
  data() {
    return {
      username: '',
      bio: '',
      showDropdown: false,
      photos: [],
      loading: false,
      uploading: false,
      uploadMsg: '',
      uploadError: false,
      preview: null
    }
  },
  computed: {
    avatarText() {
      return this.username ? this.username.charAt(0).toUpperCase() : '👤'
    }
  },
  mounted() {
    this.username = localStorage.getItem('username') || 'Admin'
    this.bio = localStorage.getItem('bio') || ''
    this.fetchPhotos()
  },
  methods: {
    toggleDropdown() {
      this.showDropdown = !this.showDropdown
    },
    closeDropdown() {
      this.showDropdown = false
    },
    saveBio() {
      localStorage.setItem('bio', this.bio)
    },
    async fetchPhotos() {
      this.loading = true
      try {
        const res = await axios.get('/api/photos/list')
        if (res.data.success) {
          this.photos = res.data.photos
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async handleUpload(event) {
      const files = event.target.files
      if (!files || files.length === 0) return
      const token = localStorage.getItem('token')
      this.uploading = true
      this.uploadMsg = ''
      this.uploadError = false

      let successCount = 0
      let failCount = 0

      for (const file of files) {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const res = await axios.post('/api/photos/upload', formData, {
            headers: {
              'Content-Type': 'multipart/form-data',
              Authorization: token
            }
          })
          if (res.data.success) successCount++
          else failCount++
        } catch (e) {
          failCount++
        }
      }

      this.uploading = false
      if (failCount === 0) {
        this.uploadMsg = `成功上传 ${successCount} 张图片`
      } else {
        this.uploadError = true
        this.uploadMsg = `成功 ${successCount} 张，失败 ${failCount} 张`
      }
      event.target.value = ''
      this.fetchPhotos()

      setTimeout(() => {
        this.uploadMsg = ''
      }, 3000)
    },
    openPreview(photo) {
      this.preview = photo
    },
    closePreview() {
      this.preview = null
    },
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      this.$router.push('/login')
    }
  },
  directives: {
    'click-outside': {
      mounted(el, binding) {
        el.__clickOutside = (event) => {
          if (!(el === event.target || el.contains(event.target))) {
            binding.value(event)
          }
        }
        document.addEventListener('click', el.__clickOutside)
      },
      unmounted(el) {
        document.removeEventListener('click', el.__clickOutside)
      }
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding: 24px;
}

/* 左上角头像 */
.avatar-wrapper {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 100;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
}

.avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 18px rgba(102, 126, 234, 0.5);
}

.dropdown {
  position: absolute;
  top: 68px;
  left: 0;
  width: 320px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  padding: 20px;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.avatar-large {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.dropdown-section {
  margin-bottom: 16px;
}

.section-label {
  display: block;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.dropdown-section textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-family: inherit;
  font-size: 13px;
  resize: vertical;
  color: #2c3e50;
}

.dropdown-section textarea:focus {
  outline: none;
  border-color: #667eea;
}

.upload-label {
  display: inline-block;
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: opacity 0.2s;
}

.upload-label:hover {
  opacity: 0.9;
}

.upload-status {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.upload-msg {
  margin-top: 8px;
  font-size: 12px;
  color: #67c23a;
}

.upload-msg.error {
  color: #f56c6c;
}

.dropdown-footer {
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.logout-btn {
  width: 100%;
  padding: 8px;
  background: #f5f7fa;
  color: #606266;
  border-radius: 8px;
  font-size: 13px;
  transition: background 0.2s;
}

.logout-btn:hover {
  background: #ebeef5;
}

/* 标题 */
.page-header {
  padding: 60px 20px 30px;
  text-align: center;
}

.page-header h1 {
  font-size: 36px;
  color: #2c3e50;
  margin-bottom: 12px;
  font-weight: 300;
  letter-spacing: 2px;
}

.header-bio {
  color: #606266;
  font-size: 14px;
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.6;
}

/* 照片墙 - 不规则布局 */
.photo-gallery {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .empty {
  text-align: center;
  padding: 80px 20px;
  color: #909399;
  font-size: 15px;
}

.masonry {
  column-count: 4;
  column-gap: 16px;
}

@media (max-width: 1200px) {
  .masonry { column-count: 3; }
}
@media (max-width: 768px) {
  .masonry { column-count: 2; }
}
@media (max-width: 480px) {
  .masonry { column-count: 1; }
}

.photo-item {
  break-inside: avoid;
  margin-bottom: 16px;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
  position: relative;
}

.photo-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.photo-item img {
  width: 100%;
  display: block;
  transition: transform 0.4s;
}

.photo-item:hover img {
  transform: scale(1.03);
}

/* 不规则变体 —— 略微不同的圆角、边距、旋转 */
.photo-item.variant-0 {
  border-radius: 14px;
}
.photo-item.variant-1 {
  border-radius: 20px 8px 20px 8px;
  transform: rotate(-0.5deg);
}
.photo-item.variant-1:hover {
  transform: rotate(0) translateY(-4px);
}
.photo-item.variant-2 {
  border-radius: 8px 20px 8px 20px;
  transform: rotate(0.5deg);
}
.photo-item.variant-2:hover {
  transform: rotate(0) translateY(-4px);
}
.photo-item.variant-3 {
  border-radius: 24px;
  margin-top: 8px;
}
.photo-item.variant-4 {
  border-radius: 10px;
  transform: rotate(-0.3deg);
}
.photo-item.variant-4:hover {
  transform: rotate(0) translateY(-4px);
}

/* 大图预览 */
.preview-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: 40px;
  cursor: zoom-out;
}

.preview-mask img {
  max-width: 90%;
  max-height: 90vh;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

/* 动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
