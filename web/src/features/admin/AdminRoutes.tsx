import { Navigate, Route, Routes } from 'react-router-dom'
import BookingPage from './booking/BookingPage'
import HomepageSettingsPage from './homepage/HomepageSettingsPage'
import ComingSoonPage from './placeholder/ComingSoonPage'
import AdminLayout from './layout/AdminLayout'
import SpecialtyPage from './specialty/SpecialtyPage'
import StaffPage from './staff/StaffPage'
import WorkSchedulePage from './workshift/WorkSchedulePage'
import WorkShiftPage from './workshift/WorkShiftPage'

/** Toàn bộ route của khu quản trị, gắn ở /admin/* (tải lười, xem app/router.tsx). */
export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<Navigate to="bookings" replace />} />
        <Route path="bookings" element={<BookingPage />} />
        <Route path="reception" element={<ComingSoonPage key="reception" title="Lễ tân" />} />
        <Route path="doctor" element={<ComingSoonPage key="doctor" title="Bác sĩ" />} />
        <Route path="pharmacy" element={<ComingSoonPage key="pharmacy" title="Dược sĩ" />} />
        <Route path="staff" element={<StaffPage key="staff" group="staff" />} />
        <Route path="patients" element={<StaffPage key="patient" group="patient" />} />
        <Route path="homepage" element={<HomepageSettingsPage />} />
        <Route path="specialties" element={<SpecialtyPage />} />
        <Route path="work-shifts" element={<WorkShiftPage />} />
        <Route path="work-schedule" element={<WorkSchedulePage />} />
      </Route>
    </Routes>
  )
}
