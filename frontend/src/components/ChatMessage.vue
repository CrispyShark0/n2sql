<template>
  <div class="message-row" :class="msg.type">
    <!-- Avatar -->
    <div class="avatar" :class="msg.type">
      <span v-if="msg.type === 'user'">👤</span>
      <span v-else>✦</span>
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
          <span class="error-icon">⚠</span>
          <span class="error-text">{{ msg.errorMessage }}</span>
        </div>

        <!-- SQL Block -->
        <div v-if="msg.sql" class="sql-block">
          <div class="sql-header">
            <span class="sql-label">Generated SQL</span>
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
            <span class="result-label">Query Results</span>
            <span class="row-badge">{{ msg.table.rows.length }} rows</span>
          </div>
          <div class="table-wrapper">
            <table>
              <thead>
                <tr><th v-for="col in msg.table.columns" :key="col">{{ col }}</th></tr>
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
          Query executed successfully but returned no data.
        </div>

        <!-- Chart Visualization -->
        <ChartDisplay
          v-if="msg.table && msg.table.rows && msg.table.rows.length > 1 && msg.table.columns && msg.table.columns.length >= 2"
          :columns="msg.table.columns"
          :rows="msg.table.rows"
        />

        <!-- Correction Info -->
        <div v-if="msg.meta && msg.meta.retryCount > 0" class="correction-info">
          ↻ Self-corrected {{ msg.meta.retryCount }} time(s)
        </div>

        <!-- Meta -->
        <div v-if="msg.meta" class="meta-info">
          <span class="meta-item">⏱ {{ msg.meta.executeTime || 0 }}ms</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import hljs from 'highlight.js'
import ChartDisplay from './ChartDisplay.vue'

const props = defineProps({ msg: { type: Object, required: true } })
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
  display: flex; gap: 14px; padding: 20px 0;
  max-width: 800px; margin: 0 auto; width: 100%;
}

.avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; flex-shrink: 0;
}
.avatar.user { background: #e8eaed; color: #5f6368; }
.avatar.ai {
  background: linear-gradient(135deg, #4285f4, #669df6);
  color: #fff; font-size: 13px;
}

.message-content { flex: 1; min-width: 0; font-size: 15px; line-height: 1.7; color: #1f1f1f; }
.user-text { margin: 0; padding-top: 4px; color: #3c4043; }

/* Error */
.error-block {
  display: flex; align-items: flex-start; gap: 8px;
  background: #fce8e6; border: 1px solid #f5c6cb;
  border-radius: 12px; padding: 12px 16px; margin-bottom: 14px;
}
.error-icon { font-size: 16px; }
.error-text { color: #c5221f; font-size: 14px; }

/* SQL Block */
.sql-block {
  border-radius: 12px; overflow: hidden; margin-bottom: 14px;
  border: 1px solid #e8eaed;
}
.sql-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 16px; background: #202124; font-size: 13px; color: #9aa0a6;
}
.sql-label { font-weight: 500; }
.copy-btn {
  display: flex; align-items: center; gap: 4px;
  background: none; border: none; color: #9aa0a6; cursor: pointer;
  font-size: 13px; padding: 4px 8px; border-radius: 6px; transition: all 0.2s;
}
.copy-btn:hover { background: rgba(255,255,255,0.08); color: #e8eaed; }
.sql-code {
  margin: 0; padding: 16px; background: #292a2d; overflow-x: auto;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13.5px; line-height: 1.6; color: #e8eaed;
}
.sql-code code { font-family: inherit; color: #e8eaed; }

/* Result Table */
.result-section {
  margin-bottom: 14px; border: 1px solid #e8eaed;
  border-radius: 12px; overflow: hidden;
}
.result-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; background: #f8f9fa; border-bottom: 1px solid #e8eaed;
  font-size: 13px; font-weight: 500; color: #3c4043;
}
.row-badge {
  font-size: 12px; color: #1a73e8; background: #e8f0fe;
  padding: 2px 10px; border-radius: 100px; font-weight: 500;
}
.table-wrapper { overflow-x: auto; max-height: 400px; overflow-y: auto; background: #fff; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
thead { position: sticky; top: 0; z-index: 1; }
th {
  background: #f1f3f4; color: #3c4043; padding: 10px 16px;
  text-align: left; font-weight: 600; border-bottom: 1px solid #e8eaed;
  white-space: nowrap;
}
td {
  padding: 8px 16px; border-bottom: 1px solid #f1f3f4;
  color: #3c4043; white-space: nowrap;
}
tbody tr:hover { background: #f8f9fa; }
.show-more { padding: 8px 16px; text-align: center; background: #f8f9fa; }
.show-more button {
  background: none; border: 1px solid #dadce0; color: #1a73e8;
  padding: 6px 16px; border-radius: 100px; cursor: pointer;
  font-size: 12px; transition: all 0.2s; font-family: inherit;
}
.show-more button:hover { background: #e8f0fe; border-color: #1a73e8; }

.empty-result { color: #9aa0a6; font-size: 14px; padding: 8px 0; }

.correction-info { color: #e37400; font-size: 13px; margin-bottom: 6px; font-weight: 500; }

.meta-info {
  display: flex; align-items: center; gap: 16px;
  padding-top: 8px; border-top: 1px solid #f1f3f4; margin-top: 8px;
}
.meta-item { color: #9aa0a6; font-size: 12px; }
</style>
