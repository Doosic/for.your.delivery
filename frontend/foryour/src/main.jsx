import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { SwalProvider } from '@/shared/hooks/useAlert.jsx'
import AuthInitializer from '@/store/AuthInitializer.jsx'
import { store } from '@/store/store.js'

const basename = window.location.pathname.startsWith('/delivery') ? '/delivery' : undefined

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <AuthInitializer>
        <SwalProvider>
          <BrowserRouter basename={basename}>
            <App />
          </BrowserRouter>
        </SwalProvider>
      </AuthInitializer>
    </Provider>
  </StrictMode>,
)
