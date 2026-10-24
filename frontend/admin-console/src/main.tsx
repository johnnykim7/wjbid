import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Toaster } from '@therecommerce-develop/ds-ui'
import '@therecommerce-develop/ds-ui/dist/style.css'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
    <Toaster />
  </StrictMode>,
)
