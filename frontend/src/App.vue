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
      <!-- Top Bar -->
      <div class="topbar">
        <button v-if="sidebarCollapsed" class="topbar-toggle" @click="sidebarCollapsed = false">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
        </button>
        <div class="topbar-brand">NL2SQL</div>
        <div class="topbar-spacer"></div>
        <div class="status-pill" v-if="currentDataSource">
          <span class="status-led"></span>
          <span>{{ currentDataSource.name }}</span>
          <span class="pill-badge">{{ currentDataSource.dbType }}</span>
        </div>
      </div>

      <!-- Messages Area -->
      <div class="messages-area" ref="messagesArea">
        <!-- Welcome Screen -->
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-hero">
            <div class="hero-icon">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <rect width="48" height="48" rx="16" fill="url(#hero-grad)"/>
                <path d="M16 24h16M24 16v16" stroke="#fff" stroke-width="2.5" stroke-linecap="round"/>
                <defs><linearGradient id="hero-grad" x1="0" y1="0" x2="48" y2="48"><stop stop-color="#4285f4"/><stop offset="1" stop-color="#669df6"/></linearGradient></defs>
              </svg>
            </div>
            <h1>Hi, how can I query your data?</h1>
            <p class="hero-sub">I can translate your natural language questions into SQL and run them instantly.</p>
          </div>

          <div class="suggestion-chips">
            <button class="chip" @click="tryExample('查询销售额最高的前5个产品')">
              <span class="chip-icon">📊</span> 销售额最高的前5个产品
            </button>
            <button class="chip" @click="tryExample('统计每个部门的平均工资')">
              <span class="chip-icon">📈</span> 每个部门的平均工资
            </button>
            <button class="chip" @click="tryExample('查找所有订单金额超过10000元的客户信息')">
              <span class="chip-icon">🔗</span> 订单金额超过10000的客户
            </button>
            <button class="chip" @click="tryExample('查询每个部门薪资最高的员工')">
              <span class="chip-icon">🧮</span> 每个部门薪资最高的员工
            </button>
          </div>

          <div class="tech-tags">
            <span class="tag">Self-correction Loop</span>
            <span class="tag">Soft Prompt Strategy</span>
            <span class="tag">MySQL / PostgreSQL</span>
            <span class="tag">AST Validation</span>
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
            <div class="loading-avatar">✦</div>
            <div class="loading-content">
              <div class="typing-dots"><span></span><span></span><span></span></div>
              <span class="loading-text">Generating SQL...</span>
            </div>
          </div>
        </div>

        <div ref="scrollAnchor"></div>
      </div>

      <!-- Input Area -->
      <div class="input-wrapper">
        <div class="input-container">
          <div class="input-box" :class="{ focused: inputFocused }">
            <textarea
              ref="inputEl"
              v-model="questionInput"
              placeholder="Describe the data you need..."
              @keydown.enter.exact="handleEnter"
              @input="autoResize"
              @focus="inputFocused = true"
              @blur="inputFocused = false"
              :disabled="loading || !currentDataSourceId"
              rows="1"
            ></textarea>
            <button
              class="send-btn"
              @click="sendMessage"
              :disabled="loading || !currentDataSourceId || !questionInput.trim()"
            >
              <svg v-if="!loading" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
              <span v-else class="send-spinner"></span>
            </button>
          </div>
          <div class="input-hint">
            NL2SQL may produce inaccurate SQL. Always verify before running on production data.
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

const dataSources = ref([])
const currentDataSourceId = ref('')
const messages = ref([])
const questionInput = ref('')
const loading = ref(false)
const sidebarCollapsed = ref(false)
const dialogVisible = ref(false)
const inputFocused = ref(false)

const messagesArea = ref(null)
const scrollAnchor = ref(null)
const inputEl = ref(null)

const currentDataSource = computed(() =>
  dataSources.value.find(ds => ds.id === currentDataSourceId.value)
)

onMounted(() => { loadDataSources() })

const loadDataSources = async () => {
  try {
    const res = await api.listDataSources()
    if (res && Array.isArray(res)) {
      dataSources.value = res
      if (dataSources.value.length > 0 && !currentDataSourceId.value)
        currentDataSourceId.value = dataSources.value[0].id
    }
  } catch (e) { console.error(e) }
}
const selectDataSource = (id) => { currentDataSourceId.value = id }
const clearChat = () => { messages.value = [] }
const deleteDataSource = async (id) => {
  try {
    await api.deleteDataSource(id)
    await loadDataSources()
    if (currentDataSourceId.value === id)
      currentDataSourceId.value = dataSources.value.length > 0 ? dataSources.value[0].id : ''
  } catch (e) { console.error(e) }
}
const handleTestConnection = async (formData, callback) => {
  try {
    const res = await api.testConnection(formData)
    res ? callback(true, 'Connection successful!') : callback(false, 'Connection failed.')
  } catch (e) { callback(false, e.message || 'Connection failed.') }
}
const handleSaveDataSource = async (formData, doneCallback) => {
  try {
    const res = await api.createDataSource(formData)
    if (res) { dialogVisible.value = false; await loadDataSources(); if (res.id) currentDataSourceId.value = res.id }
  } catch (e) { /* handled */ } finally { doneCallback() }
}
const tryExample = (text) => {
  if (!currentDataSourceId.value) return
  questionInput.value = text
  nextTick(() => sendMessage())
}
const handleEnter = (e) => { if (!e.shiftKey) { e.preventDefault(); sendMessage() } }
const autoResize = () => {
  if (inputEl.value) { inputEl.value.style.height = 'auto'; inputEl.value.style.height = Math.min(inputEl.value.scrollHeight, 200) + 'px' }
}
const sendMessage = async () => {
  if (!questionInput.value.trim() || !currentDataSourceId.value) return
  const question = questionInput.value.trim()
  messages.value.push({ type: 'user', content: question })
  questionInput.value = ''
  loading.value = true
  if (inputEl.value) inputEl.value.style.height = 'auto'
  scrollToBottom()
  try {
    const res = await api.askQuestion(currentDataSourceId.value, question)
    messages.value.push({
      type: 'ai', success: res?.success ?? false, errorMessage: res?.errorMessage,
      sql: res?.generatedSql, table: res?.queryResult,
      meta: { executeTime: res?.queryResult?.executeTimeMs || 0, retryCount: res?.retryCount || 0 }
    })
  } catch (error) {
    messages.value.push({ type: 'ai', success: false, errorMessage: 'Network error or server unavailable.', sql: null, table: null, meta: { executeTime: 0, retryCount: 0 } })
  } finally { loading.value = false; scrollToBottom() }
}
const scrollToBottom = () => { nextTick(() => { scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' }) }) }
const copySql = (sql) => { navigator.clipboard.writeText(sql) }
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body {
  height: 100%;
  background: #fff;
  color: #1f1f1f;
  font-family: 'Google Sans', 'Inter', 'Segoe UI', Roboto, -apple-system, sans-serif;
  -webkit-font-smoothing: antialiased;
}
#app { height: 100%; }
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #dadce0; border-radius: 10px; }
::-webkit-scrollbar-thumb:hover { background: #bdc1c6; }
.hljs { background: transparent !important; color: #e8eaed !important; }
</style>

<style scoped>
.app-root { display: flex; height: 100vh; overflow: hidden; }

/* ===== Main Area ===== */
.main-area {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
  background: #fff;
}

/* Top Bar */
.topbar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 20px; height: 56px;
  border-bottom: 1px solid #e8eaed;
  background: #fff;
}
.topbar-toggle {
  background: none; border: none; color: #5f6368; cursor: pointer;
  padding: 8px; border-radius: 50%; display: flex; align-items: center;
  transition: background 0.2s;
}
.topbar-toggle:hover { background: #f1f3f4; }
.topbar-brand { font-size: 18px; font-weight: 600; color: #1a73e8; }
.topbar-spacer { flex: 1; }
.status-pill {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 100px;
  background: #f1f3f4; font-size: 13px; color: #3c4043;
}
.status-led {
  width: 7px; height: 7px; border-radius: 50%; background: #34a853;
  box-shadow: 0 0 6px rgba(52, 168, 83, 0.4);
}
.pill-badge {
  font-size: 11px; font-weight: 600; color: #1a73e8;
  background: #e8f0fe; padding: 1px 8px; border-radius: 100px;
}

/* ===== Messages ===== */
.messages-area { flex: 1; overflow-y: auto; }
.message-band { padding: 0 24px; }
.message-band.user { background: transparent; }
.message-band.ai { background: #f8f9fa; }

/* ===== Welcome ===== */
.welcome-screen {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; height: 100%; padding: 40px 24px; text-align: center;
}
.welcome-hero { margin-bottom: 40px; }
.hero-icon { margin-bottom: 24px; }
.hero-icon svg { filter: drop-shadow(0 4px 12px rgba(66, 133, 244, 0.25)); }
.welcome-screen h1 {
  font-size: 36px; font-weight: 400; color: #1f1f1f;
  margin-bottom: 12px; line-height: 1.3;
}
.hero-sub { font-size: 16px; color: #5f6368; max-width: 480px; line-height: 1.6; }

.suggestion-chips {
  display: flex; flex-wrap: wrap; gap: 10px;
  justify-content: center; max-width: 700px; margin-bottom: 36px;
}
.chip {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 10px 20px; border-radius: 100px;
  border: 1px solid #dadce0; background: #fff;
  color: #3c4043; font-size: 14px; cursor: pointer;
  transition: all 0.2s ease; font-family: inherit;
}
.chip:hover {
  background: #f1f3f4; border-color: #bdc1c6;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.chip-icon { font-size: 16px; }

.tech-tags { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.tag {
  font-size: 12px; color: #5f6368; padding: 4px 12px;
  border-radius: 100px; background: #f1f3f4;
}

/* ===== Loading ===== */
.loading-message {
  display: flex; gap: 14px; padding: 24px 0;
  max-width: 800px; margin: 0 auto; width: 100%;
}
.loading-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: linear-gradient(135deg, #4285f4, #669df6);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; flex-shrink: 0;
}
.loading-content { display: flex; align-items: center; gap: 10px; padding-top: 4px; }
.loading-text { color: #5f6368; font-size: 14px; }
.typing-dots { display: flex; gap: 4px; }
.typing-dots span {
  width: 7px; height: 7px; background: #4285f4; border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}
.typing-dots span:nth-child(2) { animation-delay: 0.16s; }
.typing-dots span:nth-child(3) { animation-delay: 0.32s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

/* ===== Input ===== */
.input-wrapper { padding: 12px 24px 24px; background: #fff; }
.input-container { max-width: 800px; margin: 0 auto; }
.input-box {
  display: flex; align-items: flex-end;
  background: #f8f9fa; border: 1px solid #e8eaed;
  border-radius: 28px; padding: 8px 12px 8px 20px;
  transition: all 0.25s ease;
}
.input-box.focused {
  background: #fff; border-color: #1a73e8;
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.15);
}
.input-box textarea {
  flex: 1; background: none; border: none; outline: none;
  color: #1f1f1f; font-size: 15px; line-height: 1.5;
  resize: none; max-height: 200px; font-family: inherit; padding: 6px 0;
}
.input-box textarea::placeholder { color: #9aa0a6; }
.input-box textarea:disabled { opacity: 0.4; }
.send-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: #1a73e8; color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; flex-shrink: 0;
}
.send-btn:hover:not(:disabled) { background: #1557b0; }
.send-btn:disabled { background: #e8eaed; color: #9aa0a6; cursor: not-allowed; }
.send-spinner {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.input-hint { text-align: center; font-size: 12px; color: #9aa0a6; margin-top: 10px; }

@media (max-width: 768px) {
  .suggestion-chips { flex-direction: column; align-items: center; }
  .welcome-screen h1 { font-size: 26px; }
}
</style>
