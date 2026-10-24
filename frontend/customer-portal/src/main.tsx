import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@therecommerce-develop/ds-ui/dist/style.css'
import '@therecommerce-develop/ds-ui/dist/themes/wjbid.css'
import './index.css'
import App from './App.tsx'
import { startSitePhotos } from './hooks/useSitePhoto'

// 화면 그리기를 기다리게 하지 않고 사진 목록을 먼저 받기 시작한다(CR-153)
startSitePhotos()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
