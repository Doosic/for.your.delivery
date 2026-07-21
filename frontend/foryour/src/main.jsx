import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { SwalProvider } from '@/shared/hooks/useAlert.jsx'

const basename = window.location.pathname.startsWith('/delivery') ? '/delivery' : undefined

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <SwalProvider>
      <BrowserRouter basename={basename}>
        <App />
      </BrowserRouter>
    </SwalProvider>
  </StrictMode>,
)
