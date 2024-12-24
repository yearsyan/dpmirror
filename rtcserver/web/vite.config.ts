import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/rtc-ws': {
        target: 'http://localhost:8033',
        changeOrigin: true
      }
    }
  }
})
