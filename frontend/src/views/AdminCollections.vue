<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-name">Haoxin Xia</div>
        <div class="brand-sub">Admin Panel</div>
      </div>
      <nav class="nav">
        <router-link to="/admin" class="nav-link" exact-active-class="active">
          Collections
        </router-link>
        <router-link to="/admin/about" class="nav-link" exact-active-class="active">
          About
        </router-link>
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
          <h1>Collections</h1>
          <p class="subtitle">Organize your photos into collections.</p>
        </div>
        <button class="btn-primary" @click="showCreateDialog = true">
          + New Collection
        </button>
      </header>

      <section class="content">
        <div v-if="loading" class="state">Loading...</div>
        <div v-else-if="collections.length === 0" class="state">
          No collections yet. Click "+ New Collection" to create one.
        </div>
        <div v-else class="collection-grid">
          <div
            v-for="c in collections"
            :key="c.id"
            class="collection-card"
            @click="openCollection(c.id)"
          >
            <div class="cover">
              <img v-if="c.coverUrl" :src="c.coverUrl" :alt="c.title" />
              <div v-else class="cover-empty">No cover</div>
            </div>
            <div class="card-body">
              <div class="card-title">{{ c.title }}</div>
              <div class="card-desc">{{ c.description || 'No description' }}</div>
              <div class="card-meta">
                <span>{{ c.photoCount }} photos</span>
                <button class="link-btn danger" @click.stop="deleteCollection(c)">Delete</button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- Create collection dialog -->
    <div v-if="showCreateDialog" class="modal-mask" @click.self="showCreateDialog = false">
      <div class="modal">
        <div class="modal-header">New Collection</div>
        <div class="modal-body">
          <div class="form-row">
            <label>Title</label>
            <input v-model="newTitle" type="text" placeholder="Collection title" />
          </div>
          <div class="form-row">
            <label>Description</label>
            <textarea v-model="newDescription" rows="4" placeholder="Describe this collection..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-ghost" @click="showCreateDialog = false">Cancel</button>
          <button class="btn-primary" :disabled="creating" @click="handleCreate">
            {{ creating ? 'Creating...' : 'Create' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminCollections',
  data() {
    return {
      collections: [],
      loading: false,
      showCreateDialog: false,
      newTitle: '',
      newDescription: '',
      creating: false,
      displayName: localStorage.getItem('displayName') || 'Admin',
      role: localStorage.getItem('role') || 'admin'
    }
  },
  mounted() {
    this.fetchCollections()
  },
  methods: {
    async fetchCollections() {
      this.loading = true
      try {
        const res = await axios.get('/api/collections/list')
        if (res.data.success) this.collections = res.data.collections
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    openCollection(id) {
      this.$router.push(`/admin/collections/${id}`)
    },
    async handleCreate() {
      if (!this.newTitle.trim()) return
      const token = localStorage.getItem('token')
      this.creating = true
      try {
        const res = await axios.post('/api/collections/create', {
          title: this.newTitle.trim(),
          description: this.newDescription
        }, { headers: { Authorization: token } })
        if (res.data.success) {
          this.showCreateDialog = false
          this.newTitle = ''
          this.newDescription = ''
          this.fetchCollections()
        } else {
          alert(res.data.message || 'Failed to create')
        }
      } catch (e) {
        alert('Network error')
      } finally {
        this.creating = false
      }
    },
    async deleteCollection(c) {
      if (!confirm(`Delete collection "${c.title}"? Photos inside will remain on disk.`)) return
      const token = localStorage.getItem('token')
      try {
        const res = await axios.delete(`/api/collections/${c.id}`, {
          headers: { Authorization: token }
        })
        if (res.data.success) this.fetchCollections()
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
  color: #2c3e50;
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
  transition: background 0.15s;
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
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
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

.btn-primary {
  padding: 8px 16px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  letter-spacing: 1px;
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
  padding: 8px 16px;
  background: transparent;
  color: #606266;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 12px;
  transition: background 0.15s;
}

.btn-ghost:hover {
  background: #f5f7fa;
}

.state {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.collection-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.collection-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.collection-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  background: #f5f7fa;
  overflow: hidden;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
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

.card-body {
  padding: 14px 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.card-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  min-height: 34px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.link-btn {
  background: none;
  color: #909399;
  font-size: 12px;
  padding: 4px 6px;
}

.link-btn:hover {
  color: #2c3e50;
}

.link-btn.danger:hover {
  color: #f56c6c;
}

/* Modal */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #fff;
  width: 420px;
  border-radius: 6px;
  overflow: hidden;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.modal-body {
  padding: 20px;
}

.form-row {
  margin-bottom: 14px;
}

.form-row label {
  display: block;
  font-size: 11px;
  color: #606266;
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

.modal-footer {
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
