<template>
  <div class="login-container" :style="bgStyle">
    <div class="login-box">
      <div class="brand">
        <div class="brand-name">Haoxin Xia</div>
        <div class="brand-sub">Photography</div>
      </div>

      <div class="tabs">
        <button
          class="tab"
          :class="{ active: mode === 'login' }"
          @click="mode = 'login'"
        >Sign In</button>
        <button
          class="tab"
          :class="{ active: mode === 'register' }"
          @click="mode = 'register'"
        >Register</button>
      </div>

      <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="form">
        <div class="form-group">
          <label>Username</label>
          <input v-model="username" type="text" placeholder="Enter username" required />
        </div>
        <div class="form-group">
          <label>Password</label>
          <input v-model="password" type="password" placeholder="Enter password" required />
        </div>
        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
        <div class="hint-line">
          Admin: <code>admin</code> · Guest: <code>guest / 123456</code>
        </div>
      </form>

      <form v-else @submit.prevent="handleRegister" class="form">
        <div class="form-group">
          <label>Username</label>
          <input v-model="regUsername" type="text" placeholder="Choose a username" required />
        </div>
        <div class="form-group">
          <label>Display Name (optional)</label>
          <input v-model="regDisplayName" type="text" placeholder="How should we call you?" />
        </div>
        <div class="form-group">
          <label>Password</label>
          <input v-model="regPassword" type="password" placeholder="At least 4 characters" required />
        </div>
        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? 'Creating...' : 'Create Account' }}
        </button>
        <div class="hint-line">
          New accounts have guest access.
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'LoginView',
  data() {
    return {
      mode: 'login',
      username: '',
      password: '',
      regUsername: '',
      regPassword: '',
      regDisplayName: '',
      errorMsg: '',
      loading: false,
      bgUrl: null
    }
  },
  computed: {
    bgStyle() {
      const url = this.bgUrl || '/login-bg.jpg'
      return {
        backgroundImage: `url('${url}')`
      }
    }
  },
  mounted() {
    this.fetchBg()
  },
  watch: {
    mode() {
      this.errorMsg = ''
    }
  },
  methods: {
    async fetchBg() {
      try {
        const res = await axios.get('/api/settings/login-bg')
        if (res.data.success && res.data.imageUrl) {
          this.bgUrl = res.data.imageUrl
        }
      } catch (e) {
        // ignore, fall back to default
      }
    },
    saveSession(data) {
      localStorage.setItem('token', data.token)
      localStorage.setItem('username', data.username)
      localStorage.setItem('displayName', data.displayName || data.username)
      localStorage.setItem('role', data.role || 'guest')
    },
    async handleLogin() {
      this.errorMsg = ''
      this.loading = true
      try {
        const res = await axios.post('/api/auth/login', {
          username: this.username,
          password: this.password
        })
        if (res.data.success) {
          this.saveSession(res.data)
          this.$router.push(res.data.role === 'admin' ? '/admin' : '/gallery')
        } else {
          this.errorMsg = res.data.message || 'Login failed'
        }
      } catch (err) {
        this.errorMsg = 'Network error. Please make sure the backend is running.'
      } finally {
        this.loading = false
      }
    },
    async handleRegister() {
      this.errorMsg = ''
      this.loading = true
      try {
        const res = await axios.post('/api/auth/register', {
          username: this.regUsername,
          password: this.regPassword,
          displayName: this.regDisplayName
        })
        if (res.data.success) {
          this.saveSession(res.data)
          this.$router.push('/gallery')
        } else {
          this.errorMsg = res.data.message || 'Registration failed'
        }
      } catch (err) {
        this.errorMsg = 'Network error. Please make sure the backend is running.'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background-color: #1a1a1a;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

.login-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
  pointer-events: none;
}

.login-box {
  position: relative;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  padding: 48px 40px;
  border-radius: 6px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  width: 420px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.brand {
  text-align: center;
  margin-bottom: 28px;
}

.brand-name {
  font-family: 'Playfair Display', Georgia, 'Times New Roman', serif;
  font-size: 32px;
  color: #1a1a1a;
  letter-spacing: 1px;
  font-weight: 500;
}

.brand-sub {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  font-size: 11px;
  color: #8a8a8a;
  letter-spacing: 4px;
  text-transform: uppercase;
  margin-top: 4px;
}

.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  border-bottom: 1px solid #ebeef5;
}

.tab {
  flex: 1;
  padding: 10px 0;
  background: transparent;
  color: #909399;
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}

.tab.active {
  color: #1a1a1a;
  border-bottom-color: #1a1a1a;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 11px;
  color: #606266;
  margin-bottom: 6px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #1a1a1a;
}

.error {
  color: #f56c6c;
  font-size: 13px;
  margin-bottom: 12px;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: #1a1a1a;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  letter-spacing: 3px;
  text-transform: uppercase;
  transition: background 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: #333;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hint-line {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #a0a4ab;
}

code {
  background: #f4f5f7;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  color: #606266;
}
</style>
