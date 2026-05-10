import axios from 'axios';

class ApiClient {
  static instance = null;

  static getInstance() {
    if (!ApiClient.instance) {
      ApiClient.instance = axios.create({
        baseURL:
          process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api',
      });

      ApiClient.instance.interceptors.request.use((config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      });
    }
    return ApiClient.instance;
  }
}

export default ApiClient;