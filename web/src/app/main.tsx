import React from 'react'
import ReactDOM from 'react-dom/client'
import { applyCssVars } from '@/shared/theme/theme'
import '@/shared/theme/base.css'
import Providers from './providers'
import AppRouter from './router'

applyCssVars()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Providers>
      <AppRouter />
    </Providers>
  </React.StrictMode>,
)
