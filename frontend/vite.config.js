import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  // Use a relative base so built assets work when served from Spring Boot static
  base: './',
  server: {
    // Proxy API requests to backend to avoid CORS while developing
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})

