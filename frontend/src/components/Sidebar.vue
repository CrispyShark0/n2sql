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
  background: #171717;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255,255,255,0.08);
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
  background: #2a2a2a;
  border: 1px solid rgba(255,255,255,0.1);
  color: #b4b4b4;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.sidebar-toggle-open:hover {
  background: #3a3a3a;
  color: #fff;
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
}
.brand-text {
  font-size: 17px;
  font-weight: 700;
  color: #e5e5e5;
  letter-spacing: -0.5px;
}
.sidebar-close {
  background: none;
  border: none;
  color: #6b6b6b;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.sidebar-close:hover {
  background: rgba(255,255,255,0.08);
  color: #b4b4b4;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 10px;
  background: transparent;
  color: #e5e5e5;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 20px;
}
.new-chat-btn:hover {
  background: rgba(255,255,255,0.06);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-weight: 600;
  color: #6b6b6b;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 0 4px;
  margin-bottom: 8px;
}
.add-ds-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: #6b6b6b;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.add-ds-btn:hover {
  color: #10a37f;
  background: rgba(16, 163, 127, 0.1);
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
  background: rgba(255,255,255,0.06);
}
.ds-item.active {
  background: rgba(255,255,255,0.1);
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
  color: #e5e5e5;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ds-item-type {
  font-size: 11px;
  color: #6b6b6b;
  margin-top: 1px;
}
.ds-delete-btn {
  opacity: 0;
  background: none;
  border: none;
  color: #6b6b6b;
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
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.empty-ds {
  text-align: center;
  padding: 30px 10px;
  color: #4a4a4a;
  font-size: 13px;
}
.empty-ds .hint {
  color: #3a3a3a;
  font-size: 12px;
}

.sidebar-footer {
  padding: 12px 4px;
  border-top: 1px solid rgba(255,255,255,0.06);
  margin-top: auto;
}
.footer-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4a4a4a;
}
</style>
