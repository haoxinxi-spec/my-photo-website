<template>
  <div class="home" @click="closeContextMenu">
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
      <p class="hint">💡 在照片上点击右键可以编辑简介或删除</p>
    </header>

    <!-- 照片展示 -->
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
          :class="['variant-' + (idx % 5), { flipped: flippedPhoto === photo.name }]"
          @click="onPhotoClick($event, photo)"
          @contextmenu.prevent="onContextMenu($event, photo)"
        >
          <div class="card-inner">
            <div class="card-face card-front">
              <img :src="photo.url" :alt="photo.name" loading="lazy" />
            </div>
            <div class="card-face card-back" @click.stop>
              <div class="back-title">照片简介</div>
              <textarea
                v-model="descriptions[photo.name]"
                @blur="saveDescription(photo.name)"
                placeholder="给这张照片写点介绍..."
              ></textarea>
              <div class="back-actions">
                <button class="back-btn" @click="flipBack">✓ 完成</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 右键菜单 -->
    <transition name="fade">
      <div
        v-if="contextMenu.show"
        class="context-menu"
        :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
        @click.stop
      >
        <div class="context-item" @click="handleFlip">
          <span class="ctx-icon">🔄</span>
          <span>翻面编辑简介</span>
        </div>
        <div class="context-divider"></div>
        <div class="context-item danger" @click="handleDelete">
          <span class="ctx-icon">🗑️</span>
          <span>删除照片</span>
        </div>
      </div>
    </transition>

    <!-- 大图预览 -->
    <transition name="fade">
      <div v-if="preview" class="preview-mask" @click="closePreview">
        <div class="preview-content" @click.stop>
          <img :src="preview.url" :alt="preview.name" />
          <div v-if="descriptions[preview.name]" class="preview-desc">
            {{ descriptions[preview.name] }}
          </div>
          <button class="preview-close" @click="closePreview">×</button>
        </div>
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
      preview: null,
      descriptions: {},
      flippedPhoto: null,
      contextMenu: {
        show: false,
        x: 0,
        y: 0,
        target: null
      }
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
    this.loadDescriptions()
    this.fetchPhotos()
    window.addEventListener('scroll', this.closeContextMenu)
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.closeContextMenu)
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
    loadDescriptions() {
      try {
        const raw = localStorage.getItem('photoDescriptions')
        this.descriptions = raw ? JSON.parse(raw) : {}
      } catch (e) {
        this.descriptions = {}
      }
    },
    saveDescription(name) {
      localStorage.setItem('photoDescriptions', JSON.stringify(this.descriptions))
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
      let lastError = ''

      for (const file of files) {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const res = await axios.post('/api/photos/upload', formData, {
            headers: { Authorization: token }
          })
          if (res.data.success) {
            successCount++
          } else {
            failCount++
            lastError = res.data.message || '未知错误'
          }
        } catch (e) {
          failCount++
          if (e.response) {
            lastError = `HTTP ${e.response.status}: ${e.response.data && e.response.data.message ? e.response.data.message : e.response.statusText}`
          } else if (e.request) {
            lastError = '后端无响应，请检查后端是否启动 (localhost:8080)'
          } else {
            lastError = e.message
          }
        }
      }

      this.uploading = false
      if (failCount === 0) {
        this.uploadMsg = `成功上传 ${successCount} 张图片`
      } else {
        this.uploadError = true
        this.uploadMsg = `成功 ${successCount} 张，失败 ${failCount} 张（${lastError}）`
      }
      event.target.value = ''
      this.fetchPhotos()

      setTimeout(() => {
        this.uploadMsg = ''
      }, this.uploadError ? 8000 : 3000)
    },
    onPhotoClick(event, photo) {
      // 翻面状态下，正面点击不预览（背面 click.stop 已处理）
      if (this.flippedPhoto === photo.name) return
      this.openPreview(photo)
    },
    openPreview(photo) {
      this.preview = photo
    },
    closePreview() {
      this.preview = null
    },
    onContextMenu(event, photo) {
      // 计算菜单位置，避免溢出屏幕
      const menuWidth = 180
      const menuHeight = 100
      let x = event.clientX
      let y = event.clientY
      if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
      if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 10

      this.contextMenu = {
        show: true,
        x,
        y,
        target: photo
      }
    },
    closeContextMenu() {
      this.contextMenu.show = false
    },
    handleFlip() {
      const photo = this.contextMenu.target
      this.flippedPhoto = photo.name
      this.closeContextMenu()
    },
    flipBack() {
      this.flippedPhoto = null
    },
    async handleDelete() {
      const photo = this.contextMenu.target
      this.closeContextMenu()
      if (!confirm(`确定要删除这张照片吗？此操作不可撤销。`)) return

      const token = localStorage.getItem('token')
      try {
        const res = await axios.delete(`/api/photos/delete/${photo.name}`, {
          headers: { Authorization: token }
        })
        if (res.data.success) {
          // 同步清理该照片的简介
          if (this.descriptions[photo.name]) {
            delete this.descriptions[photo.name]
            localStorage.setItem('photoDescriptions', JSON.stringify(this.descriptions))
          }
          if (this.flippedPhoto === photo.name) this.flippedPhoto = null
          this.fetchPhotos()
        } else {
          alert('删除失败：' + (res.data.message || '未知错误'))
        }
      } catch (e) {
        alert('删除失败：' + (e.response && e.response.data && e.response.data.message ? e.response.data.message : e.message))
      }
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
  margin: 0 auto 8px;
  line-height: 1.6;
}

.hint {
  color: #a0a4ab;
  font-size: 12px;
  margin-top: 8px;
}

/* 照片墙 - 每行 2-3 张 */
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
  column-count: 3;
  column-gap: 24px;
}

@media (max-width: 900px) {
  .masonry { column-count: 2; }
}
@media (max-width: 520px) {
  .masonry { column-count: 1; }
}

.photo-item {
  break-inside: avoid;
  margin-bottom: 24px;
  cursor: pointer;
  position: relative;
  perspective: 1200px;
}

.card-inner {
  position: relative;
  width: 100%;
  transition: transform 0.7s cubic-bezier(0.4, 0.2, 0.2, 1);
  transform-style: preserve-3d;
}

.photo-item.flipped .card-inner {
  transform: rotateY(180deg);
}

.card-face {
  width: 100%;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s, transform 0.3s;
  overflow: hidden;
}

.card-front {
  border-radius: 6px;
}

.card-front img {
  width: 100%;
  display: block;
  transition: transform 0.4s;
}

.photo-item:not(.flipped):hover .card-front {
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
  transform: translateY(-3px);
}

.photo-item:not(.flipped):hover .card-front img {
  transform: scale(1.03);
}

/* 背面 */
.card-back {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  min-height: 260px;
  background: linear-gradient(135deg, #f9fafc 0%, #eef1f7 100%);
  border-radius: 6px;
  transform: rotateY(180deg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  cursor: default;
}

.back-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #dcdfe6;
}

.card-back textarea {
  flex: 1;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 10px 12px;
  font-family: inherit;
  font-size: 13px;
  color: #2c3e50;
  resize: none;
  background: #fff;
  line-height: 1.6;
}

.card-back textarea:focus {
  outline: none;
  border-color: #667eea;
}

.back-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.back-btn {
  padding: 6px 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 6px;
  font-size: 12px;
  transition: opacity 0.2s;
}

.back-btn:hover {
  opacity: 0.9;
}

/* 不规则变体 —— 圆角更小、微旋转差异保留 */
.photo-item.variant-0 .card-face {
  border-radius: 6px;
}
.photo-item.variant-1 {
  transform: rotate(-0.4deg);
}
.photo-item.variant-1 .card-face {
  border-radius: 10px 4px 10px 4px;
}
.photo-item.variant-2 {
  transform: rotate(0.4deg);
}
.photo-item.variant-2 .card-face {
  border-radius: 4px 10px 4px 10px;
}
.photo-item.variant-3 .card-face {
  border-radius: 8px;
}
.photo-item.variant-4 {
  transform: rotate(-0.2deg);
}
.photo-item.variant-4 .card-face {
  border-radius: 5px;
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.18);
  padding: 6px;
  z-index: 300;
  min-width: 180px;
  user-select: none;
}

.context-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  color: #2c3e50;
  cursor: pointer;
  transition: background 0.15s;
}

.context-item:hover {
  background: #f5f7fa;
}

.context-item.danger {
  color: #f56c6c;
}

.context-item.danger:hover {
  background: #fef0f0;
}

.ctx-icon {
  font-size: 14px;
}

.context-divider {
  height: 1px;
  background: #ebeef5;
  margin: 4px 8px;
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

.preview-content {
  position: relative;
  max-width: 90%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: default;
}

.preview-content img {
  max-width: 100%;
  max-height: 80vh;
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.preview-desc {
  margin-top: 16px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  color: #2c3e50;
  border-radius: 8px;
  max-width: 600px;
  font-size: 14px;
  line-height: 1.6;
  text-align: center;
}

.preview-close {
  position: absolute;
  top: -16px;
  right: -16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  color: #2c3e50;
  font-size: 22px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* 动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
