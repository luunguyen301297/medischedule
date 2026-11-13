import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { tokenStore } from './api/http'
import AdminLayout from './pages/admin/AdminLayout'
import HomepageSettingsPage from './pages/admin/HomepageSettingsPage'
import StaffPage from './pages/admin/StaffPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import { antTheme, applyCssVars } from './theme'
import './styles.css'

applyCssVars()

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } })

function RequireAuth({ children }: { children: React.ReactElement }) {
  return tokenStore.get() ? children : <Navigate to="/login" replace />
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={viVN} theme={antTheme}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <Routes>
              {/* Trang giới thiệu công khai; người đã đăng nhập vẫn xem được (admin dùng để xem trước) */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/" element={<RequireAuth><HomePage /></RequireAuth>} />
              <Route path="/admin" element={<RequireAuth><AdminLayout /></RequireAuth>}>
                <Route index element={<Navigate to="staff" replace />} />
                <Route path="staff" element={<StaffPage />} />
                <Route path="homepage" element={<HomepageSettingsPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  </React.StrictMode>,
)
