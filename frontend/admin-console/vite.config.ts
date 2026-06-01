import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  // admin-console는 nginx에서 /admin/ 서브경로로 서빙됨 → asset 경로를 /admin/ 기준으로 빌드
  base: '/admin/',
  plugins: [react()],
})
