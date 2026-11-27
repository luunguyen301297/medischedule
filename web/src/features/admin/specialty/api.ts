import { http } from '@/shared/api/http'

export interface Specialty {
  id: number
  name: string
  active: boolean
  roomCount: number
  /** Chỉ có khi đúng 1 phòng */
  singleRoomName: string | null
  /** Bác sĩ đang hoạt động thuộc khoa (users.id) */
  doctorUserIds: number[]
  /** Chỉ có khi đúng 1 bác sĩ */
  singleDoctorName: string | null
}

export interface SpecialtyRequest {
  name: string
  doctorUserIds: number[]
}

/** Bác sĩ đang hoạt động; specialtyId null = chưa có khoa */
export interface DoctorOption {
  userId: number
  fullName: string
  specialtyId: number | null
  specialtyName: string | null
}

export interface Room {
  id: number
  name: string
  specialtyId: number
  active: boolean
}

export interface RoomRequest {
  name: string
  specialtyId: number
}

export const specialtyApi = {
  list: () => http.get<Specialty[]>('/admin/specialties').then((r) => r.data),
  doctors: () => http.get<DoctorOption[]>('/admin/doctors').then((r) => r.data),
  create: (body: SpecialtyRequest) => http.post<Specialty>('/admin/specialties', body).then((r) => r.data),
  update: (id: number, body: SpecialtyRequest) => http.put<Specialty>(`/admin/specialties/${id}`, body).then((r) => r.data),
  setActive: (id: number, value: boolean) =>
    http.patch<Specialty>(`/admin/specialties/${id}/active`, null, { params: { value } }).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/admin/specialties/${id}`).then((r) => r.data),

  rooms: (specialtyId: number) => http.get<Room[]>(`/admin/specialties/${specialtyId}/rooms`).then((r) => r.data),
  createRoom: (body: RoomRequest) => http.post<Room>('/admin/rooms', body).then((r) => r.data),
  updateRoom: (id: number, body: RoomRequest) => http.put<Room>(`/admin/rooms/${id}`, body).then((r) => r.data),
  setRoomActive: (id: number, value: boolean) =>
    http.patch<Room>(`/admin/rooms/${id}/active`, null, { params: { value } }).then((r) => r.data),
  removeRoom: (id: number) => http.delete<void>(`/admin/rooms/${id}`).then((r) => r.data),
}
