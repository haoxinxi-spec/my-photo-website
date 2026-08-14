<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-name">Haoxin Xia</div>
        <div class="brand-sub">Admin Panel</div>
      </div>
      <nav class="nav">
        <router-link to="/admin" class="nav-link" exact-active-class="active">Collections</router-link>
        <router-link to="/admin/about" class="nav-link" exact-active-class="active">About</router-link>
        <router-link to="/admin/news" class="nav-link" exact-active-class="active">News</router-link>
        <router-link to="/admin/appearance" class="nav-link" exact-active-class="active">Appearance</router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-name">{{ displayName }}</div>
          <div class="user-role">{{ role }}</div>
        </div>
        <button class="logout" @click="logout">Logout</button>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <h1>About</h1>
          <p class="subtitle">Edit the introduction and portrait shown on the public site.</p>
        </div>
      </header>

      <section class="section">
        <div class="section-title">Introduction Text</div>
        <textarea
          v-model="text"
          rows="10"
          placeholder="Write your introduction here. Use blank lines to separate paragraphs."
        ></textarea>
        <div class="row-actions">
          <span v-if="textStatus" class="status" :class="textStatus.type">{{ textStatus.msg }}</span>
          <button class="btn-primary" :disabled="savingText" @click="saveText">
            {{ savingText ? 'Saving...' : 'Save Text' }}
          </button>
        </div>
      </section>

      <section class="section">
        <div class="section-title-row">
          <div class="section-title">Portrait Image</div>
          <div class="section-actions">
            <label class="btn-ghost" for="about-image-input">
              {{ imageUrl ? 'Replace Image' : 'Upload Image' }}
            </label>
            <input id="about-image-input" type="file" accept="image/*" @change="handleImageUpload" style="display:none" />
            <button v-if="imageUrl" class="btn-ghost danger" @click="deleteImage">Delete Image</button>
          </div>
        </div>
        <div v-if="uploading" class="status info">Uploading...</div>
        <div class="image-preview">
          <img v-if="imageUrl" :src="imageUrl" alt="About portrait" />
          <div v-else class="image-empty">No image uploaded</div>
        </div>
        <p class="hint">Portrait shown on the right side of the About section.</p>

        <div class="caption-row">
          <label>Image Caption</label>
          <input
            v-model="imageCaption"
            type="text"
            placeholder="e.g. Lugu Lake, Yunnan, 2024"
          />
          <div class="row-actions">
            <span v-if="captionStatus" class="status" :class="captionStatus.type">{{ captionStatus.msg }}</span>
            <button class="btn-primary" :disabled="savingCaption" @click="saveCaption">
              {{ savingCaption ? 'Saving...' : 'Save Caption' }}
            </button>
          </div>
          <p class="hint">The caption appears in italics below the image on the guest gallery.</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminAbout',
  data() {
    return {
      text: '',
      imageUrl: null,
      imageCaption: '',
      savingText: false,
      savingCaption: false,
      uploading: false,
      textStatus: null,
      captionStatus: null,
      displayName: localStorage.getItem('displayName') || 'Admin',
      role: localStorage.getItem('role') || 'admin'
    }
  },
  mounted() {
    this.fetch()
  },
  methods: {
    async fetch() {
      try {
        const res = await axios.get('/api/about')
        if (res.data.success) {
          this.text = res.data.text || ''
          this.imageUrl = res.data.imageUrl || null
          this.imageCaption = res.data.imageCaption || ''
        }
      } catch (e) {
        console.error(e)
      }
    },
    async saveText() {
      const token = localStorage.getItem('token')
      this.savingText = true
      this.textStatus = null
      try {
        const res = await axios.put('/api/about/text', { text: this.text }, {
          headers: { Authorization: token }
        })
        if (res.data.success) {
          this.textStatus = { type: 'ok', msg: 'Saved' }
        } else {
          this.textStatus = { type: 'error', msg: res.data.message || 'Save failed' }
        }
      } catch (e) {
        this.textStatus = { type: 'error', msg: 'Network error' }
      } finally {
        this.savingText = false
        setTimeout(() => { this.textStatus = null }, 3000)
      }
    },
    async saveCaption() {
      const token = localStorage.getItem('token')
      this.savingCaption = true
      this.captionStatus = null
      try {
        const res = await axios.put('/api/about/caption', { caption: this.imageCaption }, {
          headers: { Authorization: token }
        })
        if (res.data.success) {
          this.captionStatus = { type: 'ok', msg: 'Saved' }
        } else {
          this.captionStatus = { type: 'error', msg: res.data.message || 'Save failed' }
        }
      } catch (e) {
        this.captionStatus = { type: 'error', msg: 'Network error' }
      } finally {
        this.savingCaption = false
        setTimeout(() => { this.captionStatus = null }, 3000)
      }
    },
    async handleImageUpload(event) {
      const file = event.target.files[0]
      if (!file) return
      const token = localStorage.getItem('token')
      this.uploading = true
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await axios.post('/api/about/image', formData, {
          headers: { Authorization: token }
        })
        if (res.data.success) {
          this.imageUrl = res.data.imageUrl
        } else {
          alert(res.data.message || 'Upload failed')
        }
      } catch (e) {
        alert('Upload failed')
      } finally {
        this.uploading = false
        event.target.value = ''
      }
    },
    async deleteImage() {
      if (!confirm('Delete the current portrait image?')) return
      const token = localStorage.getItem('token')
      try {
        const res = await axios.delete('/api/about/image', {
          headers: { Authorization: token }
        })
        if (res.data.success) {
          this.imageUrl = null
        }
      } catch (e) {
        alert('Delete failed')
      }
    },
    logout() {
      localStorage.clear()
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
  background: #fafafa;
  color: #2c3e50;
}

.sidebar {
  width: 240px;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  padding: 24px 20px;
}

.brand-name {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 22px;
  font-weight: 500;
  color: #1a1a1a;
}

.brand-sub {
  font-size: 10px;
  letter-spacing: 3px;
  color: #909399;
  text-transform: uppercase;
  margin-top: 2px;
}

.nav {
  margin-top: 32px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-link {
  display: block;
  padding: 10px 12px;
  border-radius: 4px;
  color: #606266;
  text-decoration: none;
  font-size: 13px;
  transition: background 0.15s, color 0.15s;
}

.nav-link:hover {
  background: #f5f7fa;
}

.nav-link.active {
  background: #1a1a1a;
  color: #fff;
}

.sidebar-footer {
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
}

.user-role {
  font-size: 11px;
  color: #909399;
  text-transform: uppercase;
  margin-top: 2px;
}

.logout {
  margin-top: 12px;
  width: 100%;
  padding: 8px;
  background: #f5f7fa;
  color: #606266;
  border-radius: 4px;
  font-size: 12px;
}

.logout:hover {
  background: #ebeef5;
}

.main {
  flex: 1;
  padding: 32px 40px;
  overflow: auto;
}

.topbar {
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 24px;
}

.topbar h1 {
  font-size: 22px;
  font-weight: 500;
  color: #1a1a1a;
}

.subtitle {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #606266;
  margin-bottom: 16px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title-row .section-title {
  margin-bottom: 0;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.section textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  color: #2c3e50;
  resize: vertical;
}

.section textarea:focus {
  outline: none;
  border-color: #1a1a1a;
}

.row-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.status {
  font-size: 12px;
}

.status.ok {
  color: #67c23a;
}

.status.error {
  color: #f56c6c;
}

.status.info {
  color: #909399;
  margin-bottom: 12px;
}

.btn-primary {
  padding: 8px 16px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-primary:hover:not(:disabled) {
  background: #333;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-ghost {
  display: inline-block;
  padding: 8px 14px;
  background: #f5f7fa;
  color: #606266;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.15s;
}

.btn-ghost:hover {
  background: #ebeef5;
}

.btn-ghost.danger {
  color: #f56c6c;
  border-color: #fbc4c4;
}

.btn-ghost.danger:hover {
  background: #fef0f0;
}

.image-preview {
  width: 320px;
  max-width: 100%;
  aspect-ratio: 3 / 4;
  background: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.image-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.caption-row {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.caption-row label {
  display: block;
  font-size: 11px;
  color: #606266;
  margin-bottom: 6px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.caption-row input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-family: inherit;
  font-size: 13px;
  color: #2c3e50;
}

.caption-row input:focus {
  outline: none;
  border-color: #1a1a1a;
}
</style>
