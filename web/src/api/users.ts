import type { Role, UserResponse } from './auth'
import { http } from './http'

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface CreateUserRequest {
  fullName: string
  email: string
  username?: string
  phone?: string
  role: Role
  password: string
}

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Quản trị viên',
  DOCTOR: 'Bác sĩ',
  RECEPTIONIST: 'Lễ tân',
  PHARMACIST: 'Dược sĩ',
  PATIENT: 'Bệnh nhân',
}

export const STAFF_ROLES: Role[] = ['DOCTOR', 'RECEPTIONIST', 'PHARMACIST', 'ADMIN']

export const usersApi = {
  list: (p: { role?: Role; q?: string; page: number; size: number }) =>
    http.get<Page<UserResponse>>('/admin/users', { params: p }).then((r) => r.data),
  create: (body: CreateUserRequest) => http.post<UserResponse>('/admin/users', body).then((r) => r.data),
  changeRole: (id: number, role: Role) => http.patch<UserResponse>(`/admin/users/${id}/role`, { role }).then((r) => r.data),
  setEnabled: (id: number, value: boolean) =>
    http.patch<UserResponse>(`/admin/users/${id}/enabled`, null, { params: { value } }).then((r) => r.data),
}
