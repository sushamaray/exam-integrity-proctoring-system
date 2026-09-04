import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

// Self-hosted so the typeface is bundled with the app. A webfont fetched from a
// CDN at runtime is one campus-wifi hiccup away from falling back mid-demo.
import '@fontsource-variable/inter'

import App from './App.jsx'
import { AuthProvider } from './auth/AuthProvider.jsx'
import './index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
