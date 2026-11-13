import { Navigate, Route, Routes } from 'react-router-dom'
import HomepageSettingsPage from './homepage/HomepageSettingsPage'
import AdminLayout from './layout/AdminLayout'
import StaffPage from './staff/StaffPage'

/** Toàn bộ route của khu quản trị, gắn ở /admin/* (tải lười, xem app/router.tsx). */
export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<Navigate to="staff" replace />} />
        <Route path="staff" element={<StaffPage />} />
        <Route path="homepage" element={<HomepageSettingsPage />} />
      </Route>
    </Routes>
  )
}
