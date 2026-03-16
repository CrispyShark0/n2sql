<template>
  <div class="sidebar" :class="{ collapsed: collapsed }">
    <!-- Toggle Button (visible when collapsed) -->
    <button v-if="collapsed" class="sidebar-toggle-open" @click="$emit('toggle')">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
    </button>

    <!-- Sidebar Content -->
    <div v-show="!collapsed" class="sidebar-inner">
      <!-- Header -->
      <div class="sidebar-header">
        <div class="brand">
          <div class="brand-icon">⚡</div>
          <span class="brand-text">NL2SQL</span>
        </div>
        <button class="sidebar-close" @click="$emit('toggle')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
        </button>
      </div>

      <!-- New Chat Button -->
      <button class="new-chat-btn" @click="$emit('newChat')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        New Chat
      </button>

      <!-- Data Sources Section -->
      <div class="section-title">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg>
        DATA SOURCES
        <button class="add-ds-btn" @click="$emit('addDataSource')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>

      <div class="ds-list">
        <div
          v-for="ds in dataSources"
          :key="ds.id"
          class="ds-item"
          :class="{ active: currentId === ds.id }"
          @click="$emit('select', ds.id)"
        >
          <div class="ds-item-icon" :class="ds.dbType?.toLowerCase()">
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

      <!-- Footer -->
      <div class="sidebar-footer">
        <div class="footer-info">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
          Powered by DeepSeek
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  dataSources: { type: Array, default: () => [] },
  currentId: { type: String, default: '' },
  collapsed: { type: Boolean, default: false }
})

defineEmits(['toggle', 'select', 'addDataSource', 'deleteDataSource', 'newChat'])
</script>

<style scoped>
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1e3a5f 0%, #152d4a 100%);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(59, 130, 246, 0.15);
  transition: width 0.3s ease;
  position: relative;
  flex-shrink: 0;
}
.sidebar.collapsed {
  width: 0;
  border-right: none;
  overflow: hidden;
}

.sidebar-toggle-open {
  position: absolute;
  top: 16px;
  left: 8px;
  z-index: 100;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(59, 130, 246, 0.2);
  color: #3b82f6;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.sidebar-toggle-open:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 12px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0 16px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-icon {
  font-size: 22px;
  filter: drop-shadow(0 0 8px rgba(96, 165, 250, 0.5));
}
.brand-text {
  font-size: 17px;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: -0.5px;
}
.sidebar-close {
  background: none;
  border: none;
  color: rgba(148, 163, 184, 0.6);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.sidebar-close:hover {
  background: rgba(255,255,255,0.08);
  color: #93c5fd;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(96, 165, 250, 0.25);
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.1);
  color: #93c5fd;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 20px;
}
.new-chat-btn:hover {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(96, 165, 250, 0.4);
  color: #bfdbfe;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(148, 163, 184, 0.6);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 0 4px;
  margin-bottom: 8px;
}
.add-ds-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: rgba(148, 163, 184, 0.6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.add-ds-btn:hover {
  color: #60a5fa;
  background: rgba(59, 130, 246, 0.15);
}

.ds-list {
  flex: 1;
  overflow-y: auto;
}
.ds-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 2px;
  position: relative;
}
.ds-item:hover {
  background: rgba(59, 130, 246, 0.1);
}
.ds-item.active {
  background: rgba(59, 130, 246, 0.2);
  border: 1px solid rgba(96, 165, 250, 0.2);
}
.ds-item-icon {
  font-size: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  flex-shrink: 0;
}
.ds-item-info {
  flex: 1;
  min-width: 0;
}
.ds-item-name {
  font-size: 14px;
  color: #e2e8f0;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ds-item-type {
  font-size: 11px;
  color: rgba(148, 163, 184, 0.6);
  margin-top: 1px;
}
.ds-delete-btn {
  opacity: 0;
  background: none;
  border: none;
  color: rgba(148, 163, 184, 0.6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.ds-item:hover .ds-delete-btn {
  opacity: 1;
}
.ds-delete-btn:hover {
  color: #f87171;
  background: rgba(239, 68, 68, 0.15);
}

.empty-ds {
  text-align: center;
  padding: 30px 10px;
  color: rgba(148, 163, 184, 0.4);
  font-size: 13px;
}
.empty-ds .hint {
  color: rgba(148, 163, 184, 0.3);
  font-size: 12px;
}

.sidebar-footer {
  padding: 12px 4px;
  border-top: 1px solid rgba(96, 165, 250, 0.1);
  margin-top: auto;
}
.footer-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.4);
}
</style>
