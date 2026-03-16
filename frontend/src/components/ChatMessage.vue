<template>
  <div class="message-row" :class="msg.type">
    <!-- Avatar -->
    <div class="avatar" :class="msg.type">
      <span v-if="msg.type === 'user'">👤</span>
      <span v-else>🤖</span>
    </div>

    <!-- Content -->
    <div class="message-content">
      <!-- User Message -->
      <template v-if="msg.type === 'user'">
        <p class="user-text">{{ msg.content }}</p>
      </template>

      <!-- AI Message -->
      <template v-else>
        <!-- Error Banner -->
        <div v-if="!msg.success && msg.errorMessage" class="error-block">
          <div class="error-icon">⚠️</div>
          <div class="error-text">{{ msg.errorMessage }}</div>
        </div>

        <!-- SQL Block -->
        <div v-if="msg.sql" class="sql-block">
          <div class="sql-block-header">
            <span class="sql-label">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/></svg>
              Generated SQL
            </span>
            <button class="copy-btn" @click="$emit('copySql', msg.sql)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
              Copy
            </button>
          </div>
          <pre class="sql-code"><code v-html="highlightedSql"></code></pre>
        </div>

        <!-- Result Table -->
        <div v-if="msg.table && msg.table.rows && msg.table.rows.length > 0" class="result-section">
          <div class="result-header">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>
            Query Results
            <span class="row-count">({{ msg.table.rows.length }} rows)</span>
          </div>
          <div class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th v-for="col in msg.table.columns" :key="col">{{ col }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in displayedRows" :key="idx">
                  <td v-for="col in msg.table.columns" :key="col">{{ row[col] ?? '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="msg.table.rows.length > 10" class="show-more">
            <button @click="showAll = !showAll">
              {{ showAll ? 'Show Less' : `Show All ${msg.table.rows.length} Rows` }}
            </button>
          </div>
        </div>
        <div v-else-if="msg.success && msg.sql && (!msg.table || !msg.table.rows || msg.table.rows.length === 0)" class="empty-result">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
          Query executed successfully but returned no data.
        </div>

        <!-- Correction History -->
        <div v-if="msg.meta && msg.meta.retryCount > 0" class="correction-info">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg>
          Self-corrected {{ msg.meta.retryCount }} time(s)
        </div>

        <!-- Meta Footer -->
        <div v-if="msg.meta" class="meta-info">
          <span class="meta-item">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            {{ msg.meta.executeTime || 0 }}ms
          </span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import hljs from 'highlight.js'

const props = defineProps({
  msg: { type: Object, required: true }
})

defineEmits(['copySql'])

const showAll = ref(false)

const displayedRows = computed(() => {
  if (!props.msg.table || !props.msg.table.rows) return []
  return showAll.value ? props.msg.table.rows : props.msg.table.rows.slice(0, 10)
})

const highlightedSql = computed(() => {
  if (!props.msg.sql) return ''
  return hljs.highlight(props.msg.sql, { language: 'sql' }).value
})
</script>

<style scoped>
.message-row {
  display: flex;
  gap: 16px;
  padding: 24px 0;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.message-row.user {
  padding-top: 24px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.avatar.user {
  background: linear-gradient(135deg, #7c3aed, #a855f7);
}
.avatar.ai {
  background: linear-gradient(135deg, #10a37f, #19c99d);
}

.message-content {
  flex: 1;
  min-width: 0;
  color: #ececf1;
  font-size: 15px;
  line-height: 1.7;
}

.user-text {
  margin: 0;
  padding-top: 6px;
}

/* Error */
.error-block {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.error-icon { font-size: 18px; }
.error-text { color: #fca5a5; font-size: 14px; }

/* SQL Block */
.sql-block {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid #374151;
}
.sql-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #1e1e2e;
  border-bottom: 1px solid #374151;
  font-size: 13px;
  color: #9ca3af;
}
.sql-label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.copy-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}
.copy-btn:hover {
  background: rgba(255,255,255,0.1);
  color: #e5e7eb;
}
.sql-code {
  margin: 0;
  padding: 16px;
  background: #0d1117;
  overflow-x: auto;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13.5px;
  line-height: 1.6;
}
.sql-code code {
  font-family: inherit;
}

/* Result Table */
.result-section {
  margin-bottom: 16px;
  border: 1px solid #374151;
  border-radius: 8px;
  overflow: hidden;
}
.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #1e1e2e;
  color: #9ca3af;
  font-size: 13px;
  border-bottom: 1px solid #374151;
}
.row-count {
  color: #6b7280;
  font-size: 12px;
}
.table-wrapper {
  overflow-x: auto;
  max-height: 400px;
  overflow-y: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
thead {
  position: sticky;
  top: 0;
  z-index: 1;
}
th {
  background: #161b22;
  color: #9ca3af;
  padding: 10px 16px;
  text-align: left;
  font-weight: 600;
  border-bottom: 1px solid #374151;
  white-space: nowrap;
}
td {
  padding: 8px 16px;
  border-bottom: 1px solid rgba(55, 65, 81, 0.5);
  color: #d1d5db;
  white-space: nowrap;
}
tbody tr:hover {
  background: rgba(255, 255, 255, 0.02);
}
.show-more {
  padding: 8px 16px;
  text-align: center;
  background: #161b22;
}
.show-more button {
  background: none;
  border: 1px solid #374151;
  color: #9ca3af;
  padding: 6px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}
.show-more button:hover {
  background: rgba(255,255,255,0.05);
  color: #e5e7eb;
}

.empty-result {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
  padding: 12px 0;
}

.correction-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f59e0b;
  font-size: 13px;
  margin-bottom: 8px;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid rgba(55, 65, 81, 0.5);
  margin-top: 8px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6b7280;
  font-size: 12px;
}
</style>
