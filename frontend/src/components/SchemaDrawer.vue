<template>
  <teleport to="body">
    <transition name="drawer-fade">
      <div v-if="visible" class="drawer-overlay" @click.self="$emit('close')">
        <transition name="drawer-slide">
          <div v-if="visible" class="drawer-panel">
            <div class="drawer-header">
              <h3>📋 Database Schema</h3>
              <button class="close-btn" @click="$emit('close')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>

            <div v-if="loading" class="drawer-loading">
              <div class="spinner"></div>
              <span>Loading schema...</span>
            </div>

            <div v-else-if="schema" class="drawer-body">
              <div class="schema-meta">
                <span class="db-name">{{ schema.databaseName }}</span>
                <span class="db-type-badge">{{ schema.dbType }}</span>
                <span class="table-count">{{ schema.tables?.length || 0 }} tables</span>
              </div>

              <div v-for="table in schema.tables" :key="table.tableName" class="table-card">
                <div class="table-header" @click="toggleTable(table.tableName)">
                  <svg class="expand-icon" :class="{ expanded: expandedTables[table.tableName] }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                  <span class="table-icon">🗂️</span>
                  <span class="table-name">{{ table.tableName }}</span>
                  <span v-if="table.tableComment" class="table-comment">{{ table.tableComment }}</span>
                  <span class="col-count">{{ table.columns?.length || 0 }} cols</span>
                </div>

                <transition name="expand">
                  <div v-if="expandedTables[table.tableName]" class="table-columns">
                    <div v-for="col in table.columns" :key="col.columnName" class="col-row">
                      <span class="col-key" v-if="table.primaryKeys?.includes(col.columnName)">🔑</span>
                      <span class="col-key col-fk" v-else-if="isForeignKey(table, col.columnName)">🔗</span>
                      <span class="col-key" v-else>&nbsp;&nbsp;</span>
                      <span class="col-name" :class="{ pk: table.primaryKeys?.includes(col.columnName) }">{{ col.columnName }}</span>
                      <span class="col-type">{{ col.dataType }}</span>
                      <span class="col-null" v-if="!col.nullable">NOT NULL</span>
                      <span class="col-comment" v-if="col.comment">{{ col.comment }}</span>
                    </div>

                    <div v-if="table.foreignKeys?.length > 0" class="fk-section">
                      <div class="fk-title">Foreign Keys</div>
                      <div v-for="fk in table.foreignKeys" :key="fk" class="fk-row">
                        🔗 {{ fk }}
                      </div>
                    </div>
                  </div>
                </transition>
              </div>
            </div>

            <div v-else class="drawer-empty">
              <p>No schema data available.</p>
              <p class="hint">Select a data source first.</p>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  dataSourceId: { type: String, default: '' }
})
defineEmits(['close'])

const schema = ref(null)
const loading = ref(false)
const expandedTables = ref({})

function toggleTable(name) {
  expandedTables.value[name] = !expandedTables.value[name]
}

function isForeignKey(table, colName) {
  return table.foreignKeys?.some(fk => fk.startsWith(colName + ' '))
}

watch(() => [props.visible, props.dataSourceId], async ([vis, dsId]) => {
  if (vis && dsId) {
    loading.value = true
    schema.value = null
    expandedTables.value = {}
    try {
      const res = await api.getSchema(dsId)
      schema.value = res
      // 默认展开第一张表
      if (res?.tables?.length > 0) {
        expandedTables.value[res.tables[0].tableName] = true
      }
    } catch (e) {
      console.error('Failed to load schema:', e)
    } finally {
      loading.value = false
    }
  }
}, { immediate: true })
</script>

<style scoped>
.drawer-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.25);
  z-index: 1000; display: flex; justify-content: flex-end;
}
.drawer-panel {
  width: 420px; max-width: 90vw; height: 100vh;
  background: #fff; display: flex; flex-direction: column;
  box-shadow: -4px 0 24px rgba(0,0,0,0.12);
}
.drawer-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px; border-bottom: 1px solid #e8eaed;
}
.drawer-header h3 { margin: 0; font-size: 16px; color: #1f1f1f; font-weight: 600; }
.close-btn {
  background: none; border: none; color: #9aa0a6; cursor: pointer;
  padding: 6px; border-radius: 50%; display: flex; align-items: center; transition: all 0.2s;
}
.close-btn:hover { background: #f1f3f4; color: #5f6368; }

.drawer-loading {
  display: flex; align-items: center; justify-content: center; gap: 10px;
  padding: 60px 20px; color: #5f6368; font-size: 14px;
}
.spinner {
  width: 20px; height: 20px; border: 2px solid #e8eaed;
  border-top-color: #1a73e8; border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.drawer-body { flex: 1; overflow-y: auto; padding: 16px 20px; }

.schema-meta {
  display: flex; align-items: center; gap: 8px; margin-bottom: 16px;
  padding-bottom: 12px; border-bottom: 1px solid #f1f3f4;
}
.db-name { font-size: 15px; font-weight: 600; color: #1f1f1f; }
.db-type-badge {
  font-size: 11px; font-weight: 600; color: #1a73e8;
  background: #e8f0fe; padding: 2px 8px; border-radius: 100px;
}
.table-count { font-size: 12px; color: #9aa0a6; margin-left: auto; }

.table-card {
  border: 1px solid #e8eaed; border-radius: 10px; margin-bottom: 10px;
  overflow: hidden; transition: box-shadow 0.2s;
}
.table-card:hover { box-shadow: 0 1px 4px rgba(0,0,0,0.06); }

.table-header {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; cursor: pointer; transition: background 0.15s;
  background: #f8f9fa;
}
.table-header:hover { background: #f1f3f4; }

.expand-icon { transition: transform 0.2s; flex-shrink: 0; color: #9aa0a6; }
.expand-icon.expanded { transform: rotate(90deg); }
.table-icon { font-size: 14px; }
.table-name { font-size: 13px; font-weight: 600; color: #1f1f1f; }
.table-comment { font-size: 11px; color: #9aa0a6; }
.col-count { font-size: 11px; color: #9aa0a6; margin-left: auto; }

.table-columns { padding: 6px 0; }

.col-row {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 14px; font-size: 12px; transition: background 0.1s;
}
.col-row:hover { background: #f8f9fa; }

.col-key { width: 18px; font-size: 11px; flex-shrink: 0; text-align: center; }
.col-name {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  color: #3c4043; min-width: 100px;
}
.col-name.pk { color: #1a73e8; font-weight: 600; }
.col-type {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  color: #9aa0a6; font-size: 11px;
}
.col-null { font-size: 10px; color: #ea4335; font-weight: 500; }
.col-comment { font-size: 11px; color: #5f6368; margin-left: auto; }

.fk-section { padding: 6px 14px 8px; border-top: 1px dashed #e8eaed; margin-top: 4px; }
.fk-title { font-size: 11px; font-weight: 600; color: #9aa0a6; margin-bottom: 4px; }
.fk-row { font-size: 11px; color: #5f6368; padding: 2px 0; }

.drawer-empty { text-align: center; padding: 60px 20px; color: #9aa0a6; font-size: 14px; }
.drawer-empty .hint { font-size: 12px; color: #bdc1c6; margin-top: 4px; }

/* Transitions */
.drawer-fade-enter-active, .drawer-fade-leave-active { transition: opacity 0.25s ease; }
.drawer-fade-enter-from, .drawer-fade-leave-to { opacity: 0; }
.drawer-slide-enter-active { transition: transform 0.3s ease; }
.drawer-slide-leave-active { transition: transform 0.2s ease; }
.drawer-slide-enter-from { transform: translateX(100%); }
.drawer-slide-leave-to { transform: translateX(100%); }
.expand-enter-active, .expand-leave-active { transition: all 0.2s ease; overflow: hidden; }
.expand-enter-from, .expand-leave-to { opacity: 0; max-height: 0; }
.expand-enter-to, .expand-leave-from { opacity: 1; max-height: 800px; }
</style>
