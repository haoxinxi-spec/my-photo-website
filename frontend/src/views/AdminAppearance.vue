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
          <h1>Appearance</h1>
          <p class="subtitle">Customize public-facing visuals.</p>
        </div>
      </header>

      <section class="section">
        <div class="section-title-row">
          <div class="section-title">Login Background</div>
          <div class="section-actions">
            <label class="btn-ghost" for="login-bg-input">
              {{ imageUrl ? 'Replace Image' : 'Upload Image' }}
            </label>
            <input id="login-bg-input" type="file" accept="image/*" @change="handleUpload" style="display:none" />
            <button v-if="imageUrl" class="btn-ghost danger" @click="deleteImage">Reset to Default</button>
          </div>
        </div>
        <div v-if="uploading" class="status info">Uploading...</div>
        <div class="image-preview">
          <img v-if="imageUrl" :src="imageUrl" alt="Login background" />
          <img v-else src="/login-bg.jpg" alt="Default login background" />
        </div>
        <p class="hint">This image is shown as the background of the login page. Recommended: high-resolution landscape image (1920×1080 or larger).</p>
      </section>
    </main>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminAppearance',
  data() {
    return {
      imageUrl: null,
      uploading: false,
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
        const res = await axios.get('/api/settings/login-bg')
        if (res.data.success) this.imageUrl = res.data.imageUrl
      } catch (e) {
        console.error(e)
      }
    },
    async handleUpload(event) {
      const file = event.target.files[0]
      if (!file) return
      const token = localStorage.getItem('token')
      this.uploading = true
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await axios.post('/api/settings/login-bg', formData, {
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
      if (!confirm('Reset login background to default?')) return
      const token = localStorage.getItem('token')
      try {
        const res = await axios.delete('/api/settings/login-bg', {
          headers: { Authorization: token }
        })
        if (res.data.success) this.imageUrl = null
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
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.status.info {
  color: #909399;
  font-size: 12px;
  margin-bottom: 12px;
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
  width: 100%;
  max-width: 640px;
  aspect-ratio: 16 / 9;
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

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
