<template>
  <div class="app-root">
    <!-- Sidebar -->
    <Sidebar
      :dataSources="dataSources"
      :currentId="currentDataSourceId"
      :collapsed="sidebarCollapsed"
      @toggle="sidebarCollapsed = !sidebarCollapsed"
      @select="selectDataSource"
      @addDataSource="dialogVisible = true"
      @deleteDataSource="deleteDataSource"
      @newChat="clearChat"
    />

    <!-- Main Area -->
    <div class="main-area">
      <!-- Top Bar (only when sidebar collapsed) -->
      <div class="topbar" v-if="sidebarCollapsed">
        <button class="topbar-toggle" @click="sidebarCollapsed = false">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
        </button>
        <div class="topbar-title">NL2SQL</div>
      </div>

      <!-- Connection Status Bar -->
      <div class="status-bar" v-if="currentDataSource">
        <div class="status-dot connected"></div>
        <span>Connected to <strong>{{ currentDataSource.name }}</strong></span>
        <span class="status-type">{{ currentDataSource.dbType }}</span>
      </div>

      <!-- Messages Area -->
      <div class="messages-area" ref="messagesArea">
        <!-- Welcome Screen -->
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-icon">⚡</div>
          <h1>NL2SQL Engine</h1>
          <p class="welcome-subtitle">Natural Language to SQL — Powered by DeepSeek LLM</p>

          <div class="feature-grid">
            <div class="feature-card" @click="tryExample('查询销售额最高的前5个产品')">
              <div class="feature-icon">📊</div>
              <div class="feature-text">
                <div class="feature-title">Aggregate Query</div>
                <div class="feature-desc">"查询销售额最高的前5个产品"</div>
              </div>
            </div>
            <div class="feature-card" @click="tryExample('统计每个部门的平均工资')">
              <div class="feature-icon">📈</div>
              <div class="feature-text">
                <div class="feature-title">Group Analysis</div>
                <div class="feature-desc">"统计每个部门的平均工资"</div>
              </div>
            </div>
            <div class="feature-card" @click="tryExample('查找所有订单金额超过1000元的客户信息')">
              <div class="feature-icon">🔍</div>
              <div class="feature-text">
                <div class="feature-title">Multi-table Join</div>
                <div class="feature-desc">"查找所有订单金额超过1000元的客户信息"</div>
              </div>
            </div>
            <div class="feature-card" @click="tryExample('查询比平均工资高的员工姓名和工资')">
              <div class="feature-icon">🧮</div>
              <div class="feature-text">
                <div class="feature-title">Nested Query</div>
                <div class="feature-desc">"查询比平均工资高的员工姓名和工资"</div>
              </div>
            </div>
          </div>

          <div class="capabilities">
            <div class="cap-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10a37f" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              Self-correction with feedback loop
            </div>
            <div class="cap-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10a37f" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              Multi-database dialect support (MySQL, PostgreSQL)
            </div>
            <div class="cap-item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10a37f" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              Complex query handling (JOIN, Nested, Aggregation)
            </div>
          </div>
        </div>

        <!-- Chat Messages -->
        <template v-for="(msg, index) in messages" :key="index">
          <div class="message-band" :class="msg.type">
            <ChatMessage :msg="msg" @copySql="copySql" />
          </div>
        </template>

        <!-- Loading -->
        <div v-if="loading" class="message-band ai">
          <div class="loading-message">
            <div class="loading-avatar">🤖</div>
            <div class="loading-content">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
              <span class="loading-text">Analyzing your question and generating SQL...</span>
            </div>
          </div>
        </div>

        <div ref="scrollAnchor"></div>
      </div>

      <!-- Input Area -->
      <div class="input-wrapper">
        <div class="input-container">
          <div class="input-box">
            <textarea
              ref="inputEl"
              v-model="questionInput"
              placeholder="Ask anything about your database..."
              @keydown.enter.exact="handleEnter"
              @input="autoResize"
              :disabled="loading || !currentDataSourceId"
              rows="1"
            ></textarea>
            <button
              class="send-btn"
              @click="sendMessage"
              :disabled="loading || !currentDataSourceId || !questionInput.trim()"
            >
              <svg v-if="!loading" width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
              <span v-else class="send-spinner"></span>
            </button>
          </div>
          <div class="input-hint">
            NL2SQL can make mistakes. Please verify generated SQL before running on production databases.
          </div>
        </div>
      </div>
    </div>

    <!-- Add Data Source Modal -->
    <DataSourceModal
      :visible="dialogVisible"
      @close="dialogVisible = false"
      @test="handleTestConnection"
      @save="handleSaveDataSource"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import Sidebar from './components/Sidebar.vue'
import ChatMessage from './components/ChatMessage.vue'
import DataSourceModal from './components/DataSourceModal.vue'
import api from './api'

// --- State ---
const dataSources = ref([])
const currentDataSourceId = ref('')
const messages = ref([])
const questionInput = ref('')
const loading = ref(false)
const sidebarCollapsed = ref(false)
const dialogVisible = ref(false)

// Refs
const messagesArea = ref(null)
const scrollAnchor = ref(null)
const inputEl = ref(null)

// --- Computed ---
const currentDataSource = computed(() => {
  return dataSources.value.find(ds => ds.id === currentDataSourceId.value)
})

// --- Lifecycle ---
onMounted(() => {
  loadDataSources()
})

// --- Methods ---
const loadDataSources = async () => {
  try {
    const res = await api.listDataSources()
    if (res && Array.isArray(res)) {
      dataSources.value = res
      if (dataSources.value.length > 0 && !currentDataSourceId.value) {
        currentDataSourceId.value = dataSources.value[0].id
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const selectDataSource = (id) => {
  currentDataSourceId.value = id
}

const clearChat = () => {
  messages.value = []
}

const deleteDataSource = async (id) => {
  try {
    await api.deleteDataSource(id)
    await loadDataSources()
    if (currentDataSourceId.value === id) {
      currentDataSourceId.value = dataSources.value.length > 0 ? dataSources.value[0].id : ''
    }
  } catch (e) {
    console.error(e)
  }
}

const handleTestConnection = async (formData, callback) => {
  try {
    const res = await api.testConnection(formData)
    if (res) {
      callback(true, 'Connection successful!')
    } else {
      callback(false, 'Connection failed.')
    }
  } catch (e) {
    callback(false, e.message || 'Connection failed.')
  }
}

const handleSaveDataSource = async (formData, doneCallback) => {
  try {
    const res = await api.createDataSource(formData)
    if (res) {
      dialogVisible.value = false
      await loadDataSources()
      if (res.id) currentDataSourceId.value = res.id
    }
  } catch (e) {
    // handled
  } finally {
    doneCallback()
  }
}

const tryExample = (text) => {
  if (!currentDataSourceId.value) return
  questionInput.value = text
  nextTick(() => sendMessage())
}

const handleEnter = (e) => {
  if (!e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const autoResize = () => {
  if (inputEl.value) {
    inputEl.value.style.height = 'auto'
    inputEl.value.style.height = Math.min(inputEl.value.scrollHeight, 200) + 'px'
  }
}

const sendMessage = async () => {
  if (!questionInput.value.trim() || !currentDataSourceId.value) return

  const question = questionInput.value.trim()
  messages.value.push({ type: 'user', content: question })
  questionInput.value = ''
  loading.value = true

  // Reset textarea height
  if (inputEl.value) inputEl.value.style.height = 'auto'

  scrollToBottom()

  try {
    const res = await api.askQuestion(currentDataSourceId.value, question)

    const aiMsg = {
      type: 'ai',
      success: res?.success ?? false,
      errorMessage: res?.errorMessage,
      sql: res?.generatedSql,
      table: res?.queryResult,
      meta: {
        executeTime: res?.queryResult?.executeTimeMs || 0,
        retryCount: res?.retryCount || 0
      }
    }

    messages.value.push(aiMsg)
  } catch (error) {
    messages.value.push({
      type: 'ai',
      success: false,
      errorMessage: 'Network error or server unavailable.',
      sql: null,
      table: null,
      meta: { executeTime: 0, retryCount: 0 }
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

const copySql = (sql) => {
  navigator.clipboard.writeText(sql).then(() => {
    // Could add a toast here
  })
}
</script>

<style>
/* ========== Global Reset & Base ========== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  background: #f0f4f8;
  color: #1e293b;
  font-family: 'Inter', 'Söhne', 'ui-sans-serif', 'system-ui', -apple-system, 'Segoe UI', Roboto, Ubuntu, Cantarell, 'Noto Sans', sans-serif, 'Helvetica Neue', Arial;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  height: 100%;
}

/* Scrollbar Styles */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.2);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(59, 130, 246, 0.35);
}

/* hljs overrides for light theme */
.hljs {
  background: transparent !important;
  color: #1e293b !important;
}
</style>

<style scoped>
.app-root {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ========== Main Area ========== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: linear-gradient(180deg, #f0f4f8 0%, #e8eef6 100%);
}

/* Top Bar */
.topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  height: 52px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}
.topbar-toggle {
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.topbar-toggle:hover {
  background: rgba(59, 130, 246, 0.08);
  color: #3b82f6;
}
.topbar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e40af;
}

/* Status Bar */
.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 24px;
  font-size: 13px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(59, 130, 246, 0.08);
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-dot.connected {
  background: #3b82f6;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.5);
}
.status-bar strong {
  color: #1e40af;
}
.status-type {
  background: rgba(59, 130, 246, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #3b82f6;
}

/* ========== Messages Area ========== */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}

/* Message Band */
.message-band {
  padding: 0 24px;
}
.message-band.user {
  background: transparent;
}
.message-band.ai {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(6px);
  border-bottom: 1px solid rgba(59, 130, 246, 0.06);
}

/* ========== Welcome Screen ========== */
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px 24px;
  text-align: center;
}
.welcome-icon {
  font-size: 52px;
  margin-bottom: 16px;
  filter: drop-shadow(0 0 24px rgba(59, 130, 246, 0.4));
}
.welcome-screen h1 {
  font-size: 34px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e40af, #3b82f6, #60a5fa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}
.welcome-subtitle {
  color: #64748b;
  font-size: 15px;
  margin-bottom: 40px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 620px;
  width: 100%;
  margin-bottom: 32px;
}
.feature-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  text-align: left;
  backdrop-filter: blur(8px);
}
.feature-card:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.12);
}
.feature-icon {
  font-size: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}
.feature-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 4px;
}
.feature-desc {
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.4;
}

.capabilities {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}
.cap-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}
.cap-item svg {
  stroke: #3b82f6;
}

/* ========== Loading Message ========== */
.loading-message {
  display: flex;
  gap: 16px;
  padding: 24px 0;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}
.loading-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}
.loading-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 6px;
}
.loading-text {
  color: #64748b;
  font-size: 14px;
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
}
.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #3b82f6;
  border-radius: 50%;
  animation: typingBounce 1.4s infinite ease-in-out;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ========== Input Area ========== */
.input-wrapper {
  padding: 12px 24px 20px;
  background: linear-gradient(180deg, transparent 0%, rgba(240, 244, 248, 0.95) 30%);
}
.input-container {
  max-width: 800px;
  margin: 0 auto;
}
.input-box {
  display: flex;
  align-items: flex-end;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 16px;
  padding: 10px 12px 10px 18px;
  transition: all 0.25s ease;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.08);
  backdrop-filter: blur(12px);
}
.input-box:focus-within {
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 4px 24px rgba(59, 130, 246, 0.15);
}
.input-box textarea {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  color: #1e293b;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  max-height: 200px;
  font-family: inherit;
  padding: 4px 0;
}
.input-box textarea::placeholder {
  color: #94a3b8;
}
.input-box textarea:disabled {
  opacity: 0.4;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  transform: translateY(-1px);
}
.send-btn:disabled {
  background: #cbd5e1;
  color: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}

.send-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.input-hint {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
  .welcome-screen h1 {
    font-size: 24px;
  }
}
</style>
