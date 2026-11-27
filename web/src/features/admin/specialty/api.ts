import { http } from '@/shared/api/http'

export interface Specialty {
  id: number
  name: string
  active: boolean
  roomCount: number
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
  create: (name: string) => http.post<Specialty>('/admin/specialties', { name }).then((r) => r.data),
  rename: (id: number, name: string) => http.put<Specialty>(`/admin/specialties/${id}`, { name }).then((r) => r.data),
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
