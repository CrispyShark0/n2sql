import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api', // Proxy will handle it
  headers: {
    'Content-Type': 'application/json'
  }
});

apiClient.interceptors.response.use(
  response => {
    const res = response.data;
    // 后端统一用 ApiResult 包装: { code: 200, message: "...", data: ... }
    // code === 200 表示接口调用成功，直接取 data 返回
    // 对于 NL2SQL 接口，data 内部有自己的 success 字段，由前端业务层判断
    if (res.code === 200) {
      return res.data;
    }
    // 业务错误（如参数校验失败 code=400，系统错误 code=500）
    const errMsg = res.message || '请求失败';
    console.error('API Business Error:', errMsg);
    return Promise.reject(new Error(errMsg));
  },
  error => {
    console.error('API Network Error:', error.message || 'Request Failed');
    return Promise.reject(error);
  }
);

export default {
  // Data Source Management
  listDataSources() {
    return apiClient.get('/datasource');
  },
  createDataSource(data) {
    return apiClient.post('/datasource', data);
  },
  testConnection(data) {
    return apiClient.post('/datasource/test', data);
  },
  deleteDataSource(id) {
    return apiClient.delete(`/datasource/${id}`);
  },

  // NL2SQL
  askQuestion(dataSourceId, question) {
    return apiClient.post('/nl2sql', { dataSourceId, question });
  },

  // Schema
  getSchema(dataSourceId) {
    return apiClient.get(`/datasource/${dataSourceId}/schema`);
  }
};
