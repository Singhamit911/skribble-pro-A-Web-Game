import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    setupFiles: ['./src/test/setup.js']
  },
  server: {
    port: 5173,
    proxy: {
      '/sessions': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/rooms': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/games': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      }
    }
  }
});
