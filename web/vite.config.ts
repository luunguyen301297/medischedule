import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Cổng 5175 khớp CORS_ORIGINS trong .env; /api chuyển tiếp sang backend 8085
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5175,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8085',
    },
  },
})
