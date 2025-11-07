import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Proxy API requests to backend to avoid CORS while developing
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
