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
          <h1>News</h1>
          <p class="subtitle">Publish updates, announcements and stories.</p>
        </div>
        <button class="btn-primary" @click="startCreate">+ New Article</button>
      </header>

      <section v-if="editing" class="section editor">
        <div class="editor-header">
          <div class="section-title">{{ editing.id ? 'Edit Article' : 'New Article' }}</div>
          <button class="link-btn" @click="editing = null">Cancel</button>
        </div>

        <div class="grid-2">
          <div class="form-row">
            <label>Title</label>
            <input v-model="editing.title" type="text" placeholder="Main headline" />
          </div>
          <div class="form-row">
            <label>Subtitle (date · location)</label>
            <input v-model="editing.subtitle" type="text" placeholder="e.g. Mar 12, 2026 · Shanghai" />
          </div>
        </div>
        <div class="form-row">
          <label>Location tag</label>
          <input v-model="editing.location" type="text" placeholder="e.g. Shanghai" />
        </div>
        <div class="form-row">
          <label>Summary</label>
          <textarea v-model="editing.summary" rows="2" placeholder="Short lead paragraph shown on the news list..."></textarea>
        </div>
        <div class="form-row">
          <label>Content</label>
          <textarea v-model="editing.content" rows="10" placeholder="Full article. Blank lines separate paragraphs."></textarea>
        </div>
        <div class="form-row">
          <label class="inline">
            <input v-model="editing.isHot" type="checkbox" />
            <span>Mark as hot news (appears in the right sidebar)</span>
          </label>
        </div>
        <div class="form-row" v-if="editing.id">
          <label>Cover Image</label>
          <div class="cover-area">
            <div class="cover-preview">
              <img v-if="editing.coverUrl" :src="editing.coverUrl" />
              <div v-else class="cover-empty">No cover</div>
            </div>
            <label class="btn-ghost" for="cover-input">Upload Cover</label>
            <input id="cover-input" type="file" accept="image/*" @change="handleCoverUpload" style="display:none" />
          </div>
        </div>

        <div class="row-actions">
          <span v-if="status" class="status" :class="status.type">{{ status.msg }}</span>
          <button class="btn-primary" :disabled="saving" @click="saveArticle">
            {{ saving ? 'Saving...' : (editing.id ? 'Save Changes' : 'Create') }}
          </button>
        </div>
      </section>

      <section class="section">
        <div class="section-title">All Articles ({{ items.length }})</div>
        <div v-if="loading" class="state">Loading...</div>
        <div v-else-if="items.length === 0" class="state">No articles yet.</div>
        <div v-else class="news-list">
          <div v-for="n in items" :key="n.id" class="news-row">
            <div class="nr-cover">
              <img v-if="n.coverUrl" :src="n.coverUrl" />
              <div v-else class="cover-empty">—</div>
            </div>
            <div class="nr-body">
              <div class="nr-title">
                {{ n.title }}
                <span v-if="n.isHot" class="hot-badge">HOT</span>
              </div>
              <div class="nr-sub">{{ n.subtitle }}</div>
              <div class="nr-meta">Updated {{ formatDate(n.updatedAt) }}</div>
            </div>
            <div class="nr-actions">
              <button class="link-btn" @click="startEdit(n)">Edit</button>
              <button class="link-btn danger" @click="deleteArticle(n)">Delete</button>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminNews',
  data() {
    return {
      items: [],
      editing: null,
      loading: false,
      saving: false,
      status: null,
      displayName: localStorage.getItem('displayName') || 'Admin',
      role: localStorage.getItem('role') || 'admin'
    }
  },
  mounted() {
    this.fetch()
  },
  methods: {
    async fetch() {
      this.loading = true
      try {
        const res = await axios.get('/api/news/list')
        if (res.data.success) this.items = res.data.items
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    startCreate() {
      this.editing = {
        id: null,
        title: '',
        subtitle: '',
        location: '',
        summary: '',
        content: '',
        isHot: false,
        coverUrl: null
      }
    },
    startEdit(n) {
      this.editing = {
        id: n.id,
        title: n.title || '',
        subtitle: n.subtitle || '',
        location: n.location || '',
        summary: n.summary || '',
        content: n.content || '',
        isHot: !!n.isHot,
        coverUrl: n.coverUrl || null
      }
    },
    async saveArticle() {
      if (!this.editing.title.trim()) {
        this.status = { type: 'error', msg: 'Title is required' }
        return
      }
      const token = localStorage.getItem('token')
      this.saving = true
      this.status = null
      try {
        const body = {
          title: this.editing.title,
          subtitle: this.editing.subtitle,
          location: this.editing.location,
          summary: this.editing.summary,
          content: this.editing.content,
          isHot: this.editing.isHot
        }
        let res
        if (this.editing.id) {
          res = await axios.put(`/api/news/${this.editing.id}`, body, {
            headers: { Authorization: token }
          })
        } else {
          res = await axios.post('/api/news', body, {
            headers: { Authorization: token }
          })
          if (res.data.success && res.data.item) {
            this.editing.id = res.data.item.id
            this.editing.coverUrl = res.data.item.coverUrl
          }
        }
        if (res.data.success) {
          this.status = { type: 'ok', msg: 'Saved' }
          this.fetch()
        } else {
          this.status = { type: 'error', msg: res.data.message || 'Save failed' }
        }
      } catch (e) {
        this.status = { type: 'error', msg: 'Network error' }
      } finally {
        this.saving = false
        setTimeout(() => { this.status = null }, 3000)
      }
    },
    async handleCoverUpload(event) {
      const file = event.target.files[0]
      if (!file || !this.editing.id) return
      const token = localStorage.getItem('token')
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await axios.post(`/api/news/${this.editing.id}/cover`, formData, {
          headers: { Authorization: token }
        })
        if (res.data.success) {
          this.editing.coverUrl = res.data.imageUrl
          this.fetch()
        }
      } catch (e) {
        alert('Upload failed')
      }
      event.target.value = ''
    },
    async deleteArticle(n) {
      if (!confirm(`Delete article "${n.title}"?`)) return
      const token = localStorage.getItem('token')
      try {
        const res = await axios.delete(`/api/news/${n.id}`, {
          headers: { Authorization: token }
        })
        if (res.data.success) {
          if (this.editing && this.editing.id === n.id) this.editing = null
          this.fetch()
        }
      } catch (e) {
        alert('Delete failed')
      }
    },
    formatDate(ts) {
      if (!ts) return '—'
      const d = new Date(ts)
      return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
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

.user-name { font-size: 13px; font-weight: 500; }
.user-role { font-size: 11px; color: #909399; text-transform: uppercase; margin-top: 2px; }

.logout {
  margin-top: 12px;
  width: 100%;
  padding: 8px;
  background: #f5f7fa;
  color: #606266;
  border-radius: 4px;
  font-size: 12px;
}
.logout:hover { background: #ebeef5; }

.main {
  flex: 1;
  padding: 32px 40px;
  overflow: auto;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 24px;
}

.topbar h1 { font-size: 22px; font-weight: 500; color: #1a1a1a; }
.subtitle { font-size: 13px; color: #909399; margin-top: 4px; }

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
.btn-primary:hover:not(:disabled) { background: #333; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

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
.btn-ghost:hover { background: #ebeef5; }

.link-btn {
  background: none;
  color: #909399;
  font-size: 12px;
  padding: 4px 6px;
}
.link-btn:hover { color: #2c3e50; }
.link-btn.danger:hover { color: #f56c6c; }

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

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.editor-header .section-title { margin-bottom: 0; }

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
@media (max-width: 700px) {
  .grid-2 { grid-template-columns: 1fr; }
}

.form-row { margin-bottom: 14px; }

.form-row label {
  display: block;
  font-size: 11px;
  color: #606266;
  margin-bottom: 6px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.form-row label.inline {
  display: flex;
  align-items: center;
  gap: 8px;
  text-transform: none;
  letter-spacing: 0;
  font-size: 13px;
  color: #2c3e50;
}

.form-row input[type=text],
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

.row-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.status { font-size: 12px; }
.status.ok { color: #67c23a; }
.status.error { color: #f56c6c; }

.state {
  padding: 40px 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.news-row {
  display: flex;
  gap: 14px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  align-items: center;
}

.nr-cover {
  width: 110px;
  height: 70px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 4px;
  background: #f5f7fa;
}

.nr-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.nr-body { flex: 1; min-width: 0; }

.nr-title {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hot-badge {
  font-size: 10px;
  letter-spacing: 1px;
  padding: 2px 6px;
  background: #f56c6c;
  color: #fff;
  border-radius: 2px;
}

.nr-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #909399;
}

.nr-meta {
  margin-top: 2px;
  font-size: 11px;
  color: #c0c4cc;
}

.nr-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
</style>
