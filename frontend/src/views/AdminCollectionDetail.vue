<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-name">Haoxin Xia</div>
        <div class="brand-sub">Admin Panel</div>
      </div>
      <nav class="nav">
        <router-link to="/admin" class="nav-link">Collections</router-link>
        <router-link to="/admin/about" class="nav-link">About</router-link>
        <router-link to="/admin/appearance" class="nav-link">Appearance</router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <div class="user-name">{{ displayName }}</div>
          <div class="user-role">{{ role }}</div>
        </div>
        <button class="logout" @click="logout">Logout</button>
      </div>
    </aside>

    <main class="main" v-if="collection">
      <header class="topbar">
        <div>
          <button class="back-btn" @click="$router.push('/admin')">← Back</button>
          <h1>{{ collection.title }}</h1>
          <p class="subtitle">{{ collection.description || 'No description' }}</p>
        </div>
      </header>

      <section class="section">
        <div class="section-title">Collection Info</div>
        <div class="info-row">
          <div class="form-row">
            <label>Title</label>
            <input v-model="editTitle" type="text" @blur="saveInfo" />
          </div>
          <div class="form-row">
            <label>Description</label>
            <textarea v-model="editDescription" rows="3" @blur="saveInfo"></textarea>
          </div>
          <div class="form-row">
            <label>Cover</label>
            <div class="cover-area">
              <div class="cover-preview">
                <img v-if="collection.coverUrl" :src="collection.coverUrl" />
                <div v-else class="cover-empty">No cover</div>
              </div>
              <div>
                <label class="upload-label" for="cover-input">Upload Cover</label>
                <input id="cover-input" type="file" accept="image/*" @change="handleCoverUpload" style="display:none" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-title-row">
          <div class="section-title">Photos ({{ collection.photos.length }})</div>
          <label class="btn-primary" for="photo-input">+ Upload Photos</label>
          <input id="photo-input" type="file" accept="image/*" multiple @change="handlePhotosUpload" style="display:none" />
        </div>
        <div v-if="uploading" class="upload-status">Uploading {{ uploadProgress.done }}/{{ uploadProgress.total }}...</div>

        <div v-if="collection.photos.length === 0" class="empty">
          No photos yet.
        </div>
        <div v-else class="photo-list">
          <div v-for="photo in collection.photos" :key="photo.filename" class="photo-row">
            <div class="thumb">
              <img :src="photo.url" :alt="photo.filename" />
            </div>
            <div class="photo-fields">
              <label>Description</label>
              <textarea
                v-model="photoDescriptions[photo.filename]"
                @blur="savePhotoDesc(photo.filename)"
                rows="3"
                placeholder="Describe this photo..."
              ></textarea>
            </div>
            <div class="photo-actions">
              <button class="link-btn danger" @click="deletePhoto(photo)">Delete</button>
            </div>
          </div>
        </div>
      </section>
    </main>

    <main class="main" v-else>
      <div class="state">Loading...</div>
    </main>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminCollectionDetail',
  data() {
    return {
      collection: null,
      editTitle: '',
      editDescription: '',
      photoDescriptions: {},
      uploading: false,
      uploadProgress: { done: 0, total: 0 },
      displayName: localStorage.getItem('displayName') || 'Admin',
      role: localStorage.getItem('role') || 'admin'
    }
  },
  computed: {
    id() {
      return this.$route.params.id
    }
  },
  mounted() {
    this.fetch()
  },
  methods: {
    async fetch() {
      try {
        const res = await axios.get(`/api/collections/${this.id}`)
        if (res.data.success) {
          this.collection = res.data.collection
          this.editTitle = this.collection.title
          this.editDescription = this.collection.description || ''
          this.photoDescriptions = {}
          this.collection.photos.forEach(p => {
            this.photoDescriptions[p.filename] = p.description || ''
          })
        }
      } catch (e) {
        console.error(e)
      }
    },
    async saveInfo() {
      if (this.editTitle === this.collection.title &&
          this.editDescription === (this.collection.description || '')) return
      const token = localStorage.getItem('token')
      try {
        await axios.put(`/api/collections/${this.id}`, {
          title: this.editTitle,
          description: this.editDescription
        }, { headers: { Authorization: token } })
        this.fetch()
      } catch (e) {
        alert('Save failed')
      }
    },
    async handleCoverUpload(event) {
      const file = event.target.files[0]
      if (!file) return
      const token = localStorage.getItem('token')
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await axios.post(`/api/collections/${this.id}/cover`, formData, {
          headers: { Authorization: token }
        })
        if (res.data.success) this.fetch()
        else alert(res.data.message || 'Upload failed')
      } catch (e) {
        alert('Upload failed')
      }
      event.target.value = ''
    },
    async handlePhotosUpload(event) {
      const files = Array.from(event.target.files)
      if (files.length === 0) return
      const token = localStorage.getItem('token')
      this.uploading = true
      this.uploadProgress = { done: 0, total: files.length }

      for (const file of files) {
        const formData = new FormData()
        formData.append('file', file)
        try {
          await axios.post(`/api/collections/${this.id}/photos`, formData, {
            headers: { Authorization: token }
          })
        } catch (e) {
          console.error('upload failed', file.name, e)
        }
        this.uploadProgress.done++
      }

      this.uploading = false
      event.target.value = ''
      this.fetch()
    },
    async savePhotoDesc(filename) {
      const token = localStorage.getItem('token')
      try {
        await axios.put(`/api/collections/${this.id}/photos/${filename}`, {
          description: this.photoDescriptions[filename] || ''
        }, { headers: { Authorization: token } })
      } catch (e) {
        console.error(e)
      }
    },
    async deletePhoto(photo) {
      if (!confirm('Delete this photo?')) return
      const token = localStorage.getItem('token')
      try {
        const res = await axios.delete(`/api/collections/${this.id}/photos/${photo.filename}`, {
          headers: { Authorization: token }
        })
        if (res.data.success) this.fetch()
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
  transition: background 0.15s;
}

.nav-link:hover {
  background: #f5f7fa;
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

.back-btn {
  background: none;
  color: #909399;
  font-size: 12px;
  padding: 4px 0;
  margin-bottom: 8px;
}

.back-btn:hover {
  color: #2c3e50;
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

.form-row {
  margin-bottom: 14px;
}

.form-row label {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-bottom: 6px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.form-row input,
.form-row textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-family: inherit;
  font-size: 13px;
  color: #2c3e50;
  resize: vertical;
}

.form-row input:focus,
.form-row textarea:focus {
  outline: none;
  border-color: #1a1a1a;
}

.cover-area {
  display: flex;
  gap: 16px;
  align-items: center;
}

.cover-preview {
  width: 160px;
  height: 100px;
  background: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
}

.upload-label {
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

.upload-label:hover {
  background: #ebeef5;
}

.btn-primary {
  display: inline-block;
  padding: 8px 16px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  letter-spacing: 1px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-primary:hover {
  background: #333;
}

.upload-status {
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  margin-bottom: 16px;
}

.empty {
  padding: 40px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.photo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.photo-row {
  display: flex;
  gap: 16px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.thumb {
  width: 140px;
  height: 100px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 4px;
  background: #fff;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-fields {
  flex: 1;
}

.photo-fields label {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.photo-fields textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-family: inherit;
  font-size: 13px;
  color: #2c3e50;
  resize: vertical;
  background: #fff;
}

.photo-fields textarea:focus {
  outline: none;
  border-color: #1a1a1a;
}

.photo-actions {
  display: flex;
  align-items: flex-start;
}

.link-btn {
  background: none;
  color: #909399;
  font-size: 12px;
  padding: 4px 6px;
}

.link-btn.danger:hover {
  color: #f56c6c;
}

.state {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
}
</style>
