import type { Role } from './api'

/** Tên hiển thị của vai trò (khớp bảng roles ở backend). */
export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Quản trị viên',
  DOCTOR: 'Bác sĩ',
  RECEPTIONIST: 'Lễ tân',
  PHARMACIST: 'Dược sĩ',
  PATIENT: 'Bệnh nhân',
}
