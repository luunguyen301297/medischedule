import { Spin } from 'antd'
import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage, RegisterPage } from '@/features/auth'
import { HomePage } from '@/features/home'
import { RequireAuth } from './guards'

// Mỗi khu theo vai trò tải lười: bệnh nhân vào trang giới thiệu không phải tải code quản trị.
const AdminRoutes = lazy(() => import('@/features/admin'))

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Spin fullscreen />}>
        <Routes>
          {/* Trang giới thiệu công khai; người đã đăng nhập vẫn xem được (admin dùng để xem trước) */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<RequireAuth><HomePage /></RequireAuth>} />
          <Route path="/admin/*" element={<RequireAuth><AdminRoutes /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
