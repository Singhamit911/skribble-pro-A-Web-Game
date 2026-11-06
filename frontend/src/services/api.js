import axios from 'axios';

const api = axios.create({
  baseURL: '/',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Automatically inject session token if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('skribblepro_session_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
