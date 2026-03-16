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
    if (res.code === 200 || res.code === 0) { // Assuming 200/0 is success
        return res.data;
    } else {
        // If success is false in Nl2SqlResponse but HTTP is 200
        // Return the data anyway so the frontend can handle the error display
        if (typeof res.success === 'boolean' && !res.success) {
            return res;
        }
        return res.data; // Return data part anyway
    }
  },
  error => {
    console.error('API Error:', error.message || 'Request Failed');
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
  }
};
