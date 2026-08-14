<template>
  <div class="guest">
    <header class="site-header">
      <div class="brand">Haoxin Xia</div>
      <nav class="site-nav">
        <a
          v-for="item in navItems"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeNav === item.key }"
          @click="setActive(item.key)"
        >{{ item.label }}</a>
      </nav>
      <div class="account">
        <span class="user-name">{{ displayName }}</span>
        <button class="logout" @click="logout">Logout</button>
      </div>
    </header>

    <main class="site-main">
      <!-- PORTFOLIO: 所有合集封面 grid -->
      <section v-if="activeNav === 'portfolio'" class="portfolio">
        <div v-if="loading" class="state">Loading...</div>
        <div v-else-if="collections.length === 0" class="state">
          No collections available yet.
        </div>
        <div v-else class="portfolio-grid">
          <div
            v-for="c in collections"
            :key="c.id"
            class="portfolio-card"
            @click="openCollection(c)"
          >
            <div class="pc-cover">
              <img v-if="c.coverUrl" :src="c.coverUrl" :alt="c.title" />
              <div v-else class="pc-empty">{{ c.title }}</div>
            </div>
            <div class="pc-title">{{ c.title }}</div>
            <div class="pc-meta">{{ c.photoCount }} images</div>
          </div>
        </div>
      </section>

      <!-- SELECTED WORKS: 所有照片汇总 -->
      <section v-else-if="activeNav === 'selected'" class="selected">
        <div v-if="loading" class="state">Loading...</div>
        <div v-else-if="allPhotos.length === 0" class="state">
          No works available yet.
        </div>
        <div v-else class="selected-grid">
          <div
            v-for="(photo, idx) in allPhotos"
            :key="photo.filename"
            class="sw-item"
            @click="openPreview(photo)"
          >
            <img :src="photo.url" :alt="photo.filename" loading="lazy" />
          </div>
        </div>
      </section>

      <!-- ABOUT: 左文字 + 右图片 -->
      <section v-else-if="activeNav === 'about'" class="about-section">
        <div v-if="aboutLoading" class="state">Loading...</div>
        <div v-else class="about-inner">
          <div class="about-text">
            <div class="about-title">About</div>
            <div class="about-body" v-if="about.text">
              <p v-for="(line, i) in aboutParagraphs" :key="i">{{ line }}</p>
            </div>
            <div v-else class="about-empty">No introduction yet.</div>
          </div>
          <div class="about-image-column">
            <div class="about-image">
              <img v-if="about.imageUrl" :src="about.imageUrl" alt="Haoxin Xia" />
              <div v-else class="about-image-empty">No image</div>
            </div>
            <div v-if="about.imageCaption" class="about-caption">{{ about.imageCaption }}</div>
          </div>
        </div>
      </section>

      <!-- NEWS: 三栏 -->
      <section v-else-if="activeNav === 'news'" class="news-section">
        <div v-if="newsBannerUrl" class="news-banner">
          <img :src="newsBannerUrl" alt="News banner" />
          <div class="news-banner-overlay">
            <div class="news-banner-title">News</div>
            <div class="news-banner-sub">Updates, announcements and stories</div>
          </div>
        </div>

        <div class="news-top">
          <div class="news-heading" :class="{ collapsed: !!newsBannerUrl }">
            <div v-if="!newsBannerUrl" class="news-title">News</div>
            <div v-if="!newsBannerUrl" class="news-subtitle">Updates, announcements and stories.</div>
          </div>
          <div class="news-search" v-click-outside="closeSuggest">
            <div class="search-wrap">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="Search articles..."
                @input="onSearchInput"
                @focus="onSearchFocus"
              />
              <span class="search-icon">⌕</span>
            </div>
            <transition name="fade">
              <div v-if="showSuggest && (suggestions.length || searchQuery)" class="suggest">
                <div v-if="suggestions.length === 0" class="suggest-empty">
                  No matches for "{{ searchQuery }}"
                </div>
                <div
                  v-for="s in suggestions"
                  :key="s.id"
                  class="suggest-item"
                  @click="goDetail(s.id)"
                >
                  <div class="suggest-title" v-html="highlight(s.title)"></div>
                  <div class="suggest-sub">{{ s.subtitle }}</div>
                </div>
              </div>
            </transition>
          </div>
        </div>

        <div class="news-grid">
          <div class="news-main">
            <div v-if="newsLoading" class="state">Loading...</div>
            <div v-else-if="newsItems.length === 0" class="state">No articles yet.</div>
            <article
              v-for="n in newsItems"
              :key="n.id"
              class="news-card"
            >
              <h2 class="nc-title">{{ n.title }}</h2>
              <div class="nc-sub">{{ n.subtitle || formatSubtitle(n) }}</div>
              <div v-if="n.coverUrl" class="nc-cover">
                <img :src="n.coverUrl" :alt="n.title" />
              </div>
              <p class="nc-summary">{{ n.summary || cleanContentPreview(n.content) }}</p>
              <button class="learn-more" @click="goDetail(n.id)">Learn more →</button>
            </article>
          </div>

          <aside class="news-aside">
            <div class="aside-title">Hot News</div>
            <div v-if="hotItems.length === 0" class="aside-empty">No hot articles.</div>
            <div v-else class="aside-list">
              <div
                v-for="h in hotItems"
                :key="h.id"
                class="aside-item"
                @click="goDetail(h.id)"
              >
                <div class="aside-item-title">{{ h.title }}</div>
                <div class="aside-item-sub">{{ h.subtitle || formatSubtitle(h) }}</div>
              </div>
            </div>
          </aside>
        </div>
      </section>

      <!-- SHOP / CONTACT: 占位页面 -->
      <section v-else class="placeholder">
        <div class="ph-inner">
          <div class="ph-title">{{ currentNavLabel }}</div>
          <div class="ph-text">
            <template v-if="activeNav === 'shop'">
              Prints and merchandise coming soon.
            </template>
            <template v-else-if="activeNav === 'contact'">
              For inquiries or collaborations, please reach out via email.
            </template>
          </div>
        </div>
      </section>
    </main>

    <!-- 合集详情弹层 -->
    <transition name="fade">
      <div v-if="activeCollection" class="collection-view" @click.self="activeCollection = null">
        <div class="cv-inner">
          <button class="cv-close" @click="activeCollection = null">×</button>
          <div class="cv-header">
            <div class="cv-title">{{ activeCollection.title }}</div>
            <div class="cv-desc" v-if="activeCollection.description">{{ activeCollection.description }}</div>
          </div>
          <div class="cv-grid">
            <div
              v-for="photo in activeCollection.photos"
              :key="photo.filename"
              class="cv-photo"
              @click="openPreview(photo)"
            >
              <img :src="photo.url" :alt="photo.filename" loading="lazy" />
              <div v-if="photo.description" class="cv-photo-desc">{{ photo.description }}</div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 大图预览 -->
    <transition name="fade">
      <div v-if="preview" class="preview-mask" @click="closePreview">
        <div class="preview-content" @click.stop>
          <img :src="preview.url" />
          <div v-if="preview.description" class="preview-desc">{{ preview.description }}</div>
          <button class="preview-close" @click="closePreview">×</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'GuestGallery',
  data() {
    return {
      navItems: [
        { key: 'portfolio', label: 'PORTFOLIO' },
        { key: 'selected', label: 'SELECTED WORKS' },
        { key: 'shop', label: 'SHOP' },
        { key: 'news', label: 'NEWS' },
        { key: 'about', label: 'ABOUT' },
        { key: 'contact', label: 'CONTACT' }
      ],
      activeNav: 'portfolio',
      collections: [],
      collectionsDetailed: [],
      loading: false,
      activeCollection: null,
      preview: null,
      displayName: localStorage.getItem('displayName') || 'Guest',
      about: { text: '', imageUrl: null, imageCaption: '' },
      aboutLoading: false,
      newsItems: [],
      hotItems: [],
      newsLoading: false,
      newsBannerUrl: null,
      newsScrollPos: 0,
      searchQuery: '',
      suggestions: [],
      showSuggest: false,
      searchDebounce: null
    }
  },
  computed: {
    currentNavLabel() {
      const it = this.navItems.find(n => n.key === this.activeNav)
      return it ? it.label : ''
    },
    allPhotos() {
      const list = []
      for (const c of this.collectionsDetailed) {
        for (const p of (c.photos || [])) list.push(p)
      }
      return list
    },
    aboutParagraphs() {
      if (!this.about.text) return []
      return this.about.text.split(/\n+/).map(s => s.trim()).filter(Boolean)
    }
  },
  mounted() {
    this.fetchCollections()
    this.fetchAbout()
    // 如果 URL 有 ?tab=news，直接切到 news
    const q = this.$route.query || {}
    if (q.tab && this.navItems.find(n => n.key === q.tab)) {
      this.setActive(q.tab)
    }
  },
  methods: {
    setActive(key) {
      this.activeNav = key
      if (key === 'selected' && this.collectionsDetailed.length === 0) {
        this.fetchAllDetails()
      }
      if (key === 'about') {
        this.fetchAbout()
      }
      if (key === 'news') {
        this.fetchNews()
      }
    },
    async fetchAbout() {
      this.aboutLoading = true
      try {
        const res = await axios.get('/api/about')
        if (res.data.success) {
          this.about = {
            text: res.data.text || '',
            imageUrl: res.data.imageUrl || null,
            imageCaption: res.data.imageCaption || ''
          }
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.aboutLoading = false
      }
    },
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
    async fetchAllDetails() {
      this.loading = true
      try {
        const details = []
        for (const c of this.collections) {
          const res = await axios.get(`/api/collections/${c.id}`)
          if (res.data.success) details.push(res.data.collection)
        }
        this.collectionsDetailed = details
      } finally {
        this.loading = false
      }
    },
    async openCollection(c) {
      try {
        const res = await axios.get(`/api/collections/${c.id}`)
        if (res.data.success) this.activeCollection = res.data.collection
      } catch (e) {
        console.error(e)
      }
    },
    openPreview(photo) {
      this.preview = photo
    },
    closePreview() {
      this.preview = null
    },
    logout() {
      localStorage.clear()
      this.$router.push('/login')
    },
    // News
    async fetchNews() {
      this.newsLoading = true
      try {
        const [listRes, hotRes, bannerRes] = await Promise.all([
          axios.get('/api/news/list'),
          axios.get('/api/news/hot'),
          axios.get('/api/settings/news-banner')
        ])
        if (listRes.data.success) this.newsItems = listRes.data.items
        if (hotRes.data.success) this.hotItems = hotRes.data.items
        if (bannerRes.data.success) this.newsBannerUrl = bannerRes.data.imageUrl
      } catch (e) {
        console.error(e)
      } finally {
        this.newsLoading = false
        // 恢复滚动位置
        const saved = parseInt(sessionStorage.getItem('newsScroll') || '0', 10)
        if (saved > 0) {
          this.$nextTick(() => {
            setTimeout(() => window.scrollTo(0, saved), 60)
          })
        }
      }
    },
    goDetail(id) {
      this.showSuggest = false
      // 记住滚动位置
      sessionStorage.setItem('newsScroll', String(window.scrollY || window.pageYOffset || 0))
      this.$router.push(`/news/${id}`)
    },
    onSearchInput() {
      if (this.searchDebounce) clearTimeout(this.searchDebounce)
      this.searchDebounce = setTimeout(this.runSearch, 180)
    },
    onSearchFocus() {
      if (this.searchQuery) this.runSearch()
      else this.showSuggest = false
    },
    async runSearch() {
      const q = this.searchQuery.trim()
      if (!q) {
        this.suggestions = []
        this.showSuggest = false
        return
      }
      try {
        const res = await axios.get('/api/news/search', { params: { q } })
        if (res.data.success) {
          this.suggestions = res.data.items.slice(0, 8)
          this.showSuggest = true
        }
      } catch (e) {
        console.error(e)
      }
    },
    closeSuggest() {
      this.showSuggest = false
    },
    highlight(text) {
      if (!text || !this.searchQuery) return text
      const q = this.searchQuery.trim()
      if (!q) return text
      const escaped = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      return text.replace(new RegExp(`(${escaped})`, 'ig'), '<mark>$1</mark>')
    },
    formatSubtitle(n) {
      const d = n.publishedAt ? new Date(n.publishedAt) : null
      const date = d ? d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : ''
      return [date, n.location].filter(Boolean).join(' · ')
    },
    truncate(text, len) {
      if (!text) return ''
      return text.length > len ? text.slice(0, len) + '...' : text
    },
    cleanContentPreview(text) {
      if (!text) return ''
      return text.replace(/\[\[image:[^\]]+\]\]/g, '').replace(/\s+/g, ' ').trim()
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
.guest {
  min-height: 100vh;
  background: #fff;
  color: #1a1a1a;
  font-family: 'Helvetica Neue', Arial, sans-serif;
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
  font-family: 'Playfair Display', 'Cormorant Garamond', Georgia, 'Times New Roman', serif;
  font-size: 32px;
  font-weight: 500;
  letter-spacing: 1px;
  color: #1a1a1a;
  font-style: italic;
}

.site-nav {
  display: flex;
  gap: 32px;
}

.nav-item {
  font-size: 12px;
  letter-spacing: 2px;
  color: #1a1a1a;
  text-transform: uppercase;
  cursor: pointer;
  padding: 4px 0;
  position: relative;
  transition: color 0.2s;
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 1px;
  background: #1a1a1a;
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.25s;
}

.nav-item:hover::after,
.nav-item.active::after {
  transform: scaleX(1);
}

.nav-item.active {
  font-weight: 600;
}

.account {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 11px;
  color: #909399;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.logout {
  background: transparent;
  color: #606266;
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  transition: background 0.15s;
}

.logout:hover {
  background: #f5f7fa;
}

.site-main {
  padding: 40px 48px 80px;
}

.state {
  padding: 80px 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
  letter-spacing: 1px;
}

/* Portfolio grid */
.portfolio-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

@media (max-width: 900px) {
  .portfolio-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 520px) {
  .portfolio-grid { grid-template-columns: 1fr; }
}

.portfolio-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.portfolio-card:hover {
  transform: translateY(-4px);
}

.pc-cover {
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #f5f7fa;
}

.pc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s;
}

.portfolio-card:hover .pc-cover img {
  transform: scale(1.04);
}

.pc-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 14px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.pc-title {
  margin-top: 12px;
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 18px;
  color: #1a1a1a;
}

.pc-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* Selected works */
.selected-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

@media (max-width: 900px) {
  .selected-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 520px) {
  .selected-grid { grid-template-columns: 1fr; }
}

.sw-item {
  aspect-ratio: 1;
  overflow: hidden;
  cursor: pointer;
  background: #f5f7fa;
}

.sw-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s;
}

.sw-item:hover img {
  transform: scale(1.05);
}

/* Placeholder */
.placeholder {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.ph-inner {
  max-width: 600px;
  text-align: center;
}

.ph-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 40px;
  color: #1a1a1a;
  margin-bottom: 20px;
  letter-spacing: 2px;
}

.ph-text {
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
}

/* About section */
.about-section {
  padding: 60px 0;
}

.about-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 60px;
  align-items: center;
}

@media (max-width: 780px) {
  .about-inner {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

.about-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 42px;
  color: #1a1a1a;
  margin-bottom: 24px;
  letter-spacing: 2px;
}

.about-body p {
  font-size: 15px;
  color: #4a4a4a;
  line-height: 1.85;
  margin-bottom: 14px;
}

.about-empty {
  color: #909399;
  font-size: 14px;
}

.about-image-column {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.about-image {
  width: 280px;
  max-width: 100%;
  aspect-ratio: 3 / 4;
  background: #f5f7fa;
  overflow: hidden;
}

.about-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.about-image-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 13px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.about-caption {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  font-style: italic;
  text-align: center;
  line-height: 1.5;
  max-width: 280px;
}

/* Collection view */
.collection-view {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.98);
  z-index: 30;
  overflow-y: auto;
}

.cv-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 48px;
  position: relative;
}

.cv-close {
  position: absolute;
  top: 20px;
  right: 20px;
  background: transparent;
  font-size: 28px;
  color: #1a1a1a;
  width: 40px;
  height: 40px;
}

.cv-header {
  text-align: center;
  margin-bottom: 40px;
  padding-bottom: 24px;
  border-bottom: 1px solid #ebeef5;
}

.cv-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 40px;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.cv-desc {
  font-size: 14px;
  color: #606266;
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.7;
}

.cv-grid {
  columns: 2;
  column-gap: 20px;
}

@media (max-width: 700px) {
  .cv-grid { columns: 1; }
}

.cv-photo {
  break-inside: avoid;
  margin-bottom: 20px;
  cursor: pointer;
}

.cv-photo img {
  width: 100%;
  display: block;
  transition: opacity 0.3s;
}

.cv-photo:hover img {
  opacity: 0.9;
}

.cv-photo-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  font-style: italic;
  line-height: 1.5;
}

/* Preview */
.preview-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.9);
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
  max-height: 78vh;
}

.preview-desc {
  margin-top: 16px;
  padding: 10px 20px;
  color: #fff;
  font-size: 13px;
  max-width: 600px;
  text-align: center;
  line-height: 1.6;
  letter-spacing: 0.5px;
}

.preview-close {
  position: absolute;
  top: -40px;
  right: 0;
  background: transparent;
  color: #fff;
  font-size: 32px;
  padding: 4px 12px;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* News */
.news-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 0 40px;
}

.news-banner {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 1;
  max-height: 380px;
  overflow: hidden;
  margin-bottom: 32px;
}

.news-banner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.news-banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0.15) 0%, rgba(0,0,0,0.55) 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 40px 48px;
}

.news-banner-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 56px;
  color: #fff;
  letter-spacing: 3px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.35);
}

.news-banner-sub {
  font-size: 13px;
  letter-spacing: 3px;
  text-transform: uppercase;
  color: rgba(255,255,255,0.9);
  margin-top: 8px;
}

.news-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 0 0 20px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 32px;
  gap: 24px;
  min-height: 44px;
}

.news-heading.collapsed {
  flex: 1;
}

.news-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 40px;
  color: #1a1a1a;
  letter-spacing: 2px;
}

.news-subtitle {
  font-size: 12px;
  letter-spacing: 2px;
  color: #909399;
  text-transform: uppercase;
  margin-top: 4px;
}

.news-search {
  position: relative;
  width: 320px;
  max-width: 40%;
}

.search-wrap {
  position: relative;
}

.news-search input {
  width: 100%;
  padding: 10px 36px 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  font-family: inherit;
  font-size: 13px;
  background: #fff;
}

.news-search input:focus {
  outline: none;
  border-color: #1a1a1a;
}

.search-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #909399;
  font-size: 16px;
  pointer-events: none;
}

.suggest {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  z-index: 40;
  max-height: 360px;
  overflow-y: auto;
}

.suggest-empty {
  padding: 14px 16px;
  color: #909399;
  font-size: 13px;
}

.suggest-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f7fa;
  transition: background 0.15s;
}

.suggest-item:last-child { border-bottom: none; }

.suggest-item:hover {
  background: #fafafa;
}

.suggest-title {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}

.suggest-title :deep(mark) {
  background: #ffe58f;
  color: #1a1a1a;
  padding: 0 2px;
  border-radius: 2px;
}

.suggest-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.news-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 48px;
  align-items: start;
}

@media (max-width: 900px) {
  .news-grid { grid-template-columns: 1fr; gap: 32px; }
  .news-search { width: 100%; max-width: 100%; }
  .news-top { flex-direction: column; align-items: stretch; }
  .news-banner { aspect-ratio: 16 / 9; }
  .news-banner-overlay { padding: 24px; }
  .news-banner-title { font-size: 36px; }
}

.news-main {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.news-card {
  display: flex;
  flex-direction: column;
  padding-bottom: 32px;
  border-bottom: 1px solid #ebeef5;
}

.news-card:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.nc-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 30px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.25;
  margin-bottom: 6px;
}

.nc-sub {
  font-size: 12px;
  color: #a0a4ab;
  letter-spacing: 0.5px;
  margin-bottom: 18px;
}

.nc-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f5f7fa;
  margin-bottom: 18px;
}

.nc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.nc-summary {
  font-size: 15px;
  color: #4a4a4a;
  line-height: 1.75;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.learn-more {
  background: transparent;
  color: #1a1a1a;
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  padding: 6px 0;
  border-bottom: 1px solid #1a1a1a;
  border-radius: 0;
  align-self: flex-start;
  transition: opacity 0.2s;
}

.learn-more:hover { opacity: 0.6; }

.news-aside {
  position: sticky;
  top: 100px;
  padding: 20px 20px 24px;
  background: #fafafa;
  border-radius: 3px;
}

.aside-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 22px;
  color: #1a1a1a;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 14px;
}

.aside-empty {
  color: #909399;
  font-size: 13px;
}

.aside-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.aside-item {
  cursor: pointer;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
  transition: transform 0.15s;
}

.aside-item:last-child { border-bottom: none; }

.aside-item:hover {
  transform: translateX(3px);
}

.aside-item-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 17px;
  color: #1a1a1a;
  font-weight: 500;
  line-height: 1.35;
}

.aside-item-sub {
  margin-top: 4px;
  font-size: 11px;
  color: #a0a4ab;
  letter-spacing: 0.5px;
}
</style>
