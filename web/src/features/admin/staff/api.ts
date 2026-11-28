import type { Role, UserResponse } from '@/features/auth'
import { http } from '@/shared/api/http'

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

/** staff = mọi vai trò trừ bệnh nhân; patient = chỉ bệnh nhân. */
export type UserGroup = 'staff' | 'patient'

export const STAFF_ROLES: Role[] = ['DOCTOR', 'RECEPTIONIST', 'PHARMACIST', 'ADMIN']

export interface StaffProfile {
  userId: number
  fullName: string
  phone: string | null
  specialtyName: string | null
  specialtyId: number | null
  degree: string | null
  licenseNo: string | null
  acceptsDesignation: boolean
  active: boolean
  avatarUrl: string | null
}

export type PatientRelation = 'SELF' | 'CHILD' | 'PARENT' | 'OTHER'

export interface PatientProfile {
  id: number
  patientCode: string
  relation: PatientRelation
  fullName: string
  dateOfBirth: string | null
  gender: string | null
  nationalId: string | null
  phone: string
  address: string | null
  bhytNumber: string | null
  bhytValidTo: string | null
  bhytRate: number | null
  allergyNote: string | null
  source: 'GUEST' | 'APP' | 'RECEPTION' | null
  createdAt: string
  avatarUrl: string | null
}

export interface UpdateUserRequest {
  fullName: string
  email: string
  username?: string | null
  phone?: string | null
}

export interface StaffProfileRequest {
  specialtyId: number | null
  degree: string | null
  licenseNo: string | null
  acceptsDesignation: boolean
  active: boolean
  avatarUrl: string | null
}

export type PatientRequest = Omit<PatientProfile, 'id' | 'patientCode' | 'source' | 'createdAt'> & { ownerUserId?: number }

export const profilesApi = {
  staff: (userId: number) => http.get<StaffProfile>(`/admin/staff-profiles/${userId}`).then((r) => r.data),
  saveStaff: (userId: number, body: StaffProfileRequest) =>
    http.put<StaffProfile>(`/admin/staff-profiles/${userId}`, body).then((r) => r.data),
  patients: (ownerUserId: number) =>
    http.get<PatientProfile[]>('/admin/patients', { params: { ownerUserId } }).then((r) => r.data),
  createPatient: (body: PatientRequest) => http.post<PatientProfile>('/admin/patients', body).then((r) => r.data),
  updatePatient: (id: number, body: PatientRequest) => http.put<PatientProfile>(`/admin/patients/${id}`, body).then((r) => r.data),
}

export const usersApi = {
  list: (p: { group?: UserGroup; role?: Role; q?: string; page: number; size: number }) =>
    http.get<Page<UserResponse>>('/admin/users', { params: p }).then((r) => r.data),
  update: (id: number, body: UpdateUserRequest) => http.put<UserResponse>(`/admin/users/${id}`, body).then((r) => r.data),
  create: (body: CreateUserRequest) => http.post<UserResponse>('/admin/users', body).then((r) => r.data),
  changeRole: (id: number, role: Role) => http.patch<UserResponse>(`/admin/users/${id}/role`, { role }).then((r) => r.data),
  setEnabled: (id: number, value: boolean) =>
    http.patch<UserResponse>(`/admin/users/${id}/enabled`, null, { params: { value } }).then((r) => r.data),
}
