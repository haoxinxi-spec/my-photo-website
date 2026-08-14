<template>
  <div class="detail-page">
    <header class="site-header">
      <div class="brand" @click="$router.push('/gallery')">Haoxin Xia</div>
      <nav class="site-nav">
        <a class="nav-item" @click="$router.push('/gallery')">← BACK TO GALLERY</a>
      </nav>
    </header>

    <main class="detail-main">
      <div v-if="loading" class="state">Loading...</div>
      <div v-else-if="!item" class="state">Article not found.</div>
      <article v-else class="article">
        <div class="meta-top">
          <span v-if="item.location">{{ item.location }}</span>
          <span v-if="item.publishedAt" class="dot">·</span>
          <span v-if="item.publishedAt">{{ formatDate(item.publishedAt) }}</span>
          <span v-if="item.isHot" class="hot-tag">HOT</span>
        </div>
        <h1 class="article-title">{{ item.title }}</h1>
        <div v-if="item.subtitle" class="article-sub">{{ item.subtitle }}</div>

        <div v-if="item.coverUrl" class="article-cover">
          <img :src="item.coverUrl" :alt="item.title" />
        </div>

        <div class="article-body">
          <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
        </div>

        <div class="article-footer">
          <button class="back-btn" @click="$router.push('/gallery')">
            ← Back to News
          </button>
        </div>
      </article>
    </main>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'NewsDetail',
  data() {
    return {
      item: null,
      loading: false
    }
  },
  computed: {
    id() {
      return this.$route.params.id
    },
    paragraphs() {
      if (!this.item || !this.item.content) return []
      return this.item.content.split(/\n+/).map(s => s.trim()).filter(Boolean)
    }
  },
  watch: {
    id: {
      immediate: true,
      handler() { this.fetch() }
    }
  },
  methods: {
    async fetch() {
      this.loading = true
      this.item = null
      try {
        const res = await axios.get(`/api/news/${this.id}`)
        if (res.data.success) this.item = res.data.item
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    formatDate(ts) {
      if (!ts) return ''
      return new Date(ts).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })
    }
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #fff;
  color: #1a1a1a;
}

.site-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 48px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  z-index: 20;
}

.brand {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 28px;
  font-weight: 500;
  letter-spacing: 1px;
  font-style: italic;
  cursor: pointer;
}

.site-nav {
  display: flex;
  gap: 32px;
}

.nav-item {
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  cursor: pointer;
  color: #1a1a1a;
}

.nav-item:hover {
  opacity: 0.6;
}

.detail-main {
  padding: 60px 24px 100px;
}

.state {
  text-align: center;
  color: #909399;
  padding: 80px 20px;
  font-size: 14px;
  letter-spacing: 1px;
}

.article {
  max-width: 780px;
  margin: 0 auto;
}

.meta-top {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 2px;
  color: #909399;
  text-transform: uppercase;
  margin-bottom: 16px;
}

.dot { color: #dcdfe6; }

.hot-tag {
  margin-left: 8px;
  background: #f56c6c;
  color: #fff;
  padding: 2px 8px;
  border-radius: 2px;
  font-weight: 600;
}

.article-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 42px;
  line-height: 1.25;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.article-sub {
  font-size: 16px;
  color: #909399;
  margin-bottom: 32px;
  font-style: italic;
}

.article-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  margin-bottom: 32px;
  background: #f5f7fa;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.article-body p {
  font-size: 17px;
  line-height: 1.9;
  color: #2c3e50;
  margin-bottom: 20px;
}

.article-footer {
  margin-top: 60px;
  padding-top: 30px;
  border-top: 1px solid #ebeef5;
}

.back-btn {
  background: transparent;
  color: #1a1a1a;
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  padding: 8px 0;
  border-radius: 0;
}

.back-btn:hover { opacity: 0.6; }
</style>
