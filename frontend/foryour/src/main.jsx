import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { SwalProvider } from '@/shared/hooks/useAlert.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <SwalProvider>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </SwalProvider>
  </StrictMode>,
)
