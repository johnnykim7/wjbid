import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// 가상 화면 시험 설정. 서비스 빌드 설정(vite.config.ts)과 나눠 둔다.
// 대상 서버는 VTEST_API 로 바꾼다. 레포에 서버 주소를 박지 않고 기본은 로컬 서버다
export default defineConfig({
  base: '/admin/',
  plugins: [react()],
  resolve: {
    dedupe: ['react', 'react-dom', 'radix-ui', 'react/jsx-runtime'],
  },
  test: {
    environment: 'jsdom',
    environmentOptions: { jsdom: { url: 'http://localhost/admin/' } },
    include: ['vtest/**/*.spec.tsx'],
    exclude: ['vtest/**/_*'],
    setupFiles: ['vtest/setup.ts'],
    // 관리자 시험 계정 하나로 돌아 파일끼리 로그인을 나눠 쓰지 않게 한 줄로 돈다
    fileParallelism: false,
    testTimeout: 15 * 60 * 1000,
    css: false,
    env: {
      VITE_API_BASE_URL: process.env.VTEST_API ?? 'http://localhost:8183/api',
    },
    // 디자인시스템은 자기 node_modules 의 react 를 따로 들고 있다. 그 안의 모듈까지 Vite 가 풀어 dedupe 로 이 앱의 react 하나만 쓰게 한다
    server: { deps: { inline: [/design-system/, '@therecommerce-develop/ds-ui'] } },
  },
})
