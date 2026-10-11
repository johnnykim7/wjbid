import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  // admin-console는 nginx에서 /admin/ 서브경로로 서빙됨 → asset 경로를 /admin/ 기준으로 빌드
  base: '/admin/',
  plugins: [react()],
  // CR-153: 고객 화면 사진 주소는 서버가 /api/... 와 /img/... 로 준다. 운영은 같은 주소에서 열려 그대로 보이고,
  // 개발 서버에서는 /api 를 로컬 서버로, /img 를 고객 포털 개발 서버로 넘겨 같은 주소로 보이게 한다
  server: {
    proxy: {
      '/api': process.env.VITE_API_BASE_URL ? new URL(process.env.VITE_API_BASE_URL).origin : 'http://localhost:8183',
      '/img': process.env.PORTAL_DEV_URL ?? 'http://localhost:3183',
    },
  },
  // CR-132: 디자인시스템(ds-ui)을 file: 로 연결하면 DS 가 자기 node_modules 의 react/radix 를 따로 번들해
  // React 인스턴스가 둘이 된다. dedupe 로 이 앱의 것 하나로 맞춘다(고객 포털 CR-124 와 같은 설정).
  resolve: {
    dedupe: ['react', 'react-dom', 'radix-ui', 'react/jsx-runtime'],
  },
  optimizeDeps: {
    include: ['@therecommerce-develop/ds-ui', 'radix-ui'],
  },
})
