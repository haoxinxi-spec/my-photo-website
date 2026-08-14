import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../views/Login.vue'
import AdminCollections from '../views/AdminCollections.vue'
import AdminCollectionDetail from '../views/AdminCollectionDetail.vue'
import AdminAbout from '../views/AdminAbout.vue'
import AdminAppearance from '../views/AdminAppearance.vue'
import AdminNews from '../views/AdminNews.vue'
import GuestGallery from '../views/GuestGallery.vue'
import NewsDetail from '../views/NewsDetail.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: Login },
  {
    path: '/admin',
    name: 'AdminCollections',
    component: AdminCollections,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/collections/:id',
    name: 'AdminCollectionDetail',
    component: AdminCollectionDetail,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/about',
    name: 'AdminAbout',
    component: AdminAbout,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/appearance',
    name: 'AdminAppearance',
    component: AdminAppearance,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/news',
    name: 'AdminNews',
    component: AdminNews,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/gallery',
    name: 'GuestGallery',
    component: GuestGallery,
    meta: { requiresAuth: true }
  },
  {
    path: '/news/:id',
    name: 'NewsDetail',
    component: NewsDetail,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }
  if (to.meta.requiresAdmin && role !== 'admin') {
    return next('/gallery')
  }
  next()
})

export default router
