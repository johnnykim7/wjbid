import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3183,
    // CR-153: 올린 고객 화면 사진 주소는 서버가 /api/... 로 준다. 운영은 같은 주소에서 열려 그대로 보이고,
    // 개발 서버에서는 /api 를 로컬 서버로 넘겨 같은 주소로 보이게 한다
    proxy: {
      '/api': process.env.VITE_API_BASE_URL ? new URL(process.env.VITE_API_BASE_URL).origin : 'http://localhost:8183',
    },
  },
  // CR-124 — DS를 file:로 로컬 연결하면 DS가 자기 node_modules의 react/radix를
  // 따로 번들해 "React 인스턴스 2개 → Invalid hook call(회색화면)"이 난다.
  // dedupe로 react/radix를 소비앱 것 하나로 강제한다.
  resolve: {
    dedupe: ['react', 'react-dom', 'radix-ui', 'react/jsx-runtime'],
  },
  optimizeDeps: {
    include: ['@therecommerce-develop/ds-ui', 'radix-ui'],
  },
})
