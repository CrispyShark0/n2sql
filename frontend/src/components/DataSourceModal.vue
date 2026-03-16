<template>
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
        <div class="modal-container">
          <div class="modal-header">
            <h3>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg>
              Add Data Source
            </h3>
            <button class="close-btn" @click="$emit('close')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <div class="modal-body">
            <div class="form-group">
              <label>Connection Name</label>
              <input v-model="form.name" placeholder="e.g. My Sales DB" />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Database Type</label>
                <select v-model="form.dbType">
                  <option value="MYSQL">MySQL</option>
                  <option value="POSTGRESQL">PostgreSQL</option>
                </select>
              </div>
              <div class="form-group">
                <label>Port</label>
                <input v-model.number="form.port" type="number" placeholder="3306" />
              </div>
            </div>

            <div class="form-group">
              <label>Host</label>
              <input v-model="form.host" placeholder="localhost" />
            </div>

            <div class="form-group">
              <label>Database Name</label>
              <input v-model="form.dbName" placeholder="database_name" />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Username</label>
                <input v-model="form.username" placeholder="root" />
              </div>
              <div class="form-group">
                <label>Password</label>
                <input v-model="form.password" type="password" placeholder="••••••••" />
              </div>
            </div>

            <!-- Test Result -->
            <div v-if="testResult" class="test-result" :class="testResult.type">
              <span v-if="testResult.type === 'success'">✅</span>
              <span v-else>❌</span>
              {{ testResult.message }}
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-secondary" @click="testConn" :disabled="testing">
              <svg v-if="!testing" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <span v-if="testing" class="spinner"></span>
              {{ testing ? 'Testing...' : 'Test Connection' }}
            </button>
            <button class="btn-primary" @click="save" :disabled="saving">
              <span v-if="saving" class="spinner"></span>
              {{ saving ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'test', 'save'])

const form = ref(getDefaultForm())
const testing = ref(false)
const saving = ref(false)
const testResult = ref(null)

function getDefaultForm() {
  return {
    name: '',
    dbType: 'MYSQL',
    host: 'localhost',
    port: 3306,
    dbName: '',
    username: '',
    password: ''
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    form.value = getDefaultForm()
    testResult.value = null
  }
})

async function testConn() {
  testing.value = true
  testResult.value = null
  try {
    emit('test', form.value, (success, message) => {
      testResult.value = { type: success ? 'success' : 'error', message }
      testing.value = false
    })
  } catch {
    testing.value = false
  }
}

async function save() {
  saving.value = true
  emit('save', form.value, () => {
    saving.value = false
  })
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-container {
  background: #1e1e1e;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px;
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 25px 60px rgba(0,0,0,0.5);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.modal-header h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: #e5e5e5;
  font-size: 17px;
  font-weight: 600;
}
.close-btn {
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
.close-btn:hover {
  background: rgba(255,255,255,0.08);
  color: #b4b4b4;
}

.modal-body {
  padding: 20px 24px;
}
.form-group {
  margin-bottom: 16px;
  flex: 1;
}
.form-row {
  display: flex;
  gap: 12px;
}
label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #9ca3af;
  margin-bottom: 6px;
}
input, select {
  width: 100%;
  padding: 10px 14px;
  background: #2a2a2a;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  color: #e5e5e5;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
input:focus, select:focus {
  border-color: #10a37f;
}
input::placeholder {
  color: #4a4a4a;
}
select {
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b6b6b' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
}

.test-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
}
.test-result.success {
  background: rgba(16, 163, 127, 0.1);
  border: 1px solid rgba(16, 163, 127, 0.3);
  color: #10a37f;
}
.test-result.error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 20px;
}
.btn-secondary, .btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}
.btn-secondary {
  background: #2a2a2a;
  color: #b4b4b4;
  border: 1px solid rgba(255,255,255,0.1);
}
.btn-secondary:hover {
  background: #333;
  color: #e5e5e5;
}
.btn-primary {
  background: #10a37f;
  color: #fff;
}
.btn-primary:hover {
  background: #0e8f6e;
}
.btn-secondary:disabled, .btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Transition */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
}
</style>
