<template>
  <div class="sidebar" :class="{ collapsed }">
    <button v-if="collapsed" class="sidebar-toggle-open" @click="$emit('toggle')">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
    </button>
    <div v-show="!collapsed" class="sidebar-inner">
      <!-- Header -->
      <div class="sidebar-header">
        <div class="brand">
          <svg width="22" height="22" viewBox="0 0 48 48" fill="none"><rect width="48" height="48" rx="12" fill="#1a73e8"/><path d="M16 24h16M24 16v16" stroke="#fff" stroke-width="3" stroke-linecap="round"/></svg>
          <span class="brand-text">NL2SQL</span>
        </div>
        <button class="sidebar-close" @click="$emit('toggle')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
        </button>
      </div>

      <!-- New Chat -->
      <button class="new-chat-btn" @click="$emit('newChat')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        New Chat
      </button>

      <!-- Chat History -->
      <div v-if="chatHistory.length > 0" class="section-title">CHAT HISTORY</div>
      <div v-if="chatHistory.length > 0" class="chat-list">
        <div
          v-for="chat in chatHistory" :key="chat.id"
          class="chat-item" :class="{ active: currentChatId === chat.id }"
          @click="$emit('selectChat', chat.id)"
        >
          <svg class="chat-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          <span class="chat-title">{{ chat.title }}</span>
          <button class="chat-delete-btn" @click.stop="$emit('deleteChat', chat.id)" title="Delete">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </div>

      <!-- Data Sources -->
      <div class="section-title">
        DATA SOURCES
        <button class="add-ds-btn" @click="$emit('addDataSource')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>

      <div class="ds-list">
        <div
          v-for="ds in dataSources" :key="ds.id"
          class="ds-item" :class="{ active: currentId === ds.id }"
          @click="$emit('select', ds.id)"
        >
          <div class="ds-item-icon">
            <span v-if="ds.dbType === 'MYSQL'">🐬</span>
            <span v-else-if="ds.dbType === 'POSTGRESQL'">🐘</span>
            <span v-else>💾</span>
          </div>
          <div class="ds-item-info">
            <div class="ds-item-name">{{ ds.name }}</div>
            <div class="ds-item-type">{{ ds.dbType }} · {{ ds.dbName }}</div>
          </div>
          <button class="ds-delete-btn" @click.stop="$emit('deleteDataSource', ds.id)" title="Delete">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
          </button>
        </div>
        <div v-if="dataSources.length === 0" class="empty-ds">
          <p>No data sources yet.</p>
          <p class="hint">Click + to add one.</p>
        </div>
      </div>

      <div class="sidebar-footer">
        Powered by DeepSeek LLM
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  dataSources: { type: Array, default: () => [] },
  currentId: { type: String, default: '' },
  collapsed: { type: Boolean, default: false },
  chatHistory: { type: Array, default: () => [] },
  currentChatId: { type: String, default: '' }
})
defineEmits(['toggle', 'select', 'addDataSource', 'deleteDataSource', 'newChat', 'selectChat', 'deleteChat'])
</script>

<style scoped>
.sidebar {
  width: 260px; background: #E8EEF4; display: flex; flex-direction: column;
  border-right: 1px solid #e8eaed; transition: width 0.3s ease;
  position: relative; flex-shrink: 0;
}
.sidebar.collapsed { width: 0; border-right: none; overflow: hidden; }

.sidebar-toggle-open {
  position: absolute; top: 12px; left: 8px; z-index: 100;
  background: #fff; border: 1px solid #e8eaed; color: #5f6368;
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s; box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.sidebar-toggle-open:hover { background: #f1f3f4; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }

.sidebar-inner { display: flex; flex-direction: column; height: 100%; padding: 12px; }

.sidebar-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 0 16px;
}
.brand { display: flex; align-items: center; gap: 10px; }
.brand svg { width: 22px; height: 22px; }
.brand-text { font-size: 16px; font-weight: 600; color: #1f1f1f; }
.sidebar-close {
  background: none; border: none; color: #9aa0a6; cursor: pointer;
  padding: 6px; border-radius: 50%; display: flex; align-items: center;
  transition: all 0.2s;
}
.sidebar-close:hover { background: #e8eaed; color: #5f6368; }

.new-chat-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; padding: 10px; border: 1px solid #dadce0;
  border-radius: 100px; background: #fff; color: #1f1f1f;
  font-size: 14px; cursor: pointer; transition: all 0.2s; margin-bottom: 16px;
  font-family: inherit;
}
.new-chat-btn:hover { background: #e8eaed; border-color: #bdc1c6; }

.section-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; font-weight: 600; color: #9aa0a6;
  text-transform: uppercase; letter-spacing: 0.8px;
  padding: 0 4px; margin-bottom: 8px;
}
.add-ds-btn {
  margin-left: auto; background: none; border: none; color: #9aa0a6;
  cursor: pointer; padding: 4px; border-radius: 50%;
  display: flex; align-items: center; transition: all 0.2s;
}
.add-ds-btn:hover { color: #1a73e8; background: #e8f0fe; }

/* Chat History */
.chat-list { margin-bottom: 16px; max-height: 200px; overflow-y: auto; }
.chat-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 10px; cursor: pointer;
  transition: all 0.15s; margin-bottom: 2px;
}
.chat-item:hover { background: #e0e4e8; }
.chat-item.active { background: #e8f0fe; }
.chat-icon { flex-shrink: 0; color: #9aa0a6; }
.chat-item.active .chat-icon { color: #1a73e8; }
.chat-title {
  flex: 1; font-size: 13px; color: #3c4043; min-width: 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.chat-item.active .chat-title { color: #1a73e8; font-weight: 500; }
.chat-delete-btn {
  opacity: 0; background: none; border: none; color: #9aa0a6;
  cursor: pointer; padding: 4px; border-radius: 50%;
  display: flex; align-items: center; transition: all 0.2s; flex-shrink: 0;
}
.chat-item:hover .chat-delete-btn { opacity: 1; }
.chat-delete-btn:hover { color: #d93025; background: #fce8e6; }

/* Data Sources */
.ds-list { flex: 1; overflow-y: auto; }
.ds-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 12px; cursor: pointer;
  transition: all 0.15s; margin-bottom: 2px; position: relative;
}
.ds-item:hover { background: #e8eaed; }
.ds-item.active { background: #e8f0fe; }
.ds-item-icon { font-size: 18px; width: 28px; text-align: center; flex-shrink: 0; }
.ds-item-info { flex: 1; min-width: 0; }
.ds-item-name {
  font-size: 14px; color: #1f1f1f; font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ds-item-type { font-size: 11px; color: #9aa0a6; margin-top: 1px; }
.ds-delete-btn {
  opacity: 0; background: none; border: none; color: #9aa0a6;
  cursor: pointer; padding: 4px; border-radius: 50%;
  display: flex; align-items: center; transition: all 0.2s;
}
.ds-item:hover .ds-delete-btn { opacity: 1; }
.ds-delete-btn:hover { color: #d93025; background: #fce8e6; }

.empty-ds { text-align: center; padding: 30px 10px; color: #9aa0a6; font-size: 13px; }
.empty-ds .hint { font-size: 12px; color: #bdc1c6; }

.sidebar-footer {
  padding: 12px 4px; border-top: 1px solid #e8eaed; margin-top: auto;
  font-size: 12px; color: #9aa0a6; text-align: center;
}
</style>
