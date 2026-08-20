import React from 'react'
import ReactDOM from 'react-dom/client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import './i18n'
import App from './App'
import './index.css'

// Global 401 Unauthorized / Token Expiration Interceptor
const originalFetch = window.fetch
window.fetch = async (...args) => {
  try {
    const response = await originalFetch(...args)
    if (response.status === 401) {
      const url = typeof args[0] === 'string' ? args[0] : (args[0] as Request)?.url || ''
      if (!url.includes('/api/auth/login') && !url.includes('/api/auth/register')) {
        const token = localStorage.getItem('templ_token')
        if (token) {
          localStorage.removeItem('templ_token')
          window.dispatchEvent(new CustomEvent('auth_expired', {
            detail: { message: '로그인 세션이 만료되었습니다. 다시 로그인해 주세요.' }
          }))
        }
      }
    }
    return response
  } catch (err) {
    throw err
  }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId="317439837379-8l70kn4fjt7dno1oii3pufievbjh2vl3.apps.googleusercontent.com">
      <App />
    </GoogleOAuthProvider>
  </React.StrictMode>,
)

