import type { Role } from '@/features/auth'
import { http } from '@/shared/api/http'

export interface WorkShift {
  id: number
  name: string
  /** "HH:mm:ss" */
  startTime: string
  endTime: string
  /** #rrggbb */
  color: string
  active: boolean
}

export interface WorkShiftRequest {
  name: string
  /** "HH:mm" */
  startTime: string
  endTime: string
  color: string
}

export interface Assignment {
  id: number
  /** YYYY-MM-DD */
  date: string
  shiftId: number
  userId: number
  fullName: string
  role: Role | null
}

export interface StaffOption {
  userId: number
  fullName: string
  role: Role
}

export const workShiftApi = {
  list: () => http.get<WorkShift[]>('/admin/work-shifts').then((r) => r.data),
  create: (body: WorkShiftRequest) => http.post<WorkShift>('/admin/work-shifts', body).then((r) => r.data),
  update: (id: number, body: WorkShiftRequest) => http.put<WorkShift>(`/admin/work-shifts/${id}`, body).then((r) => r.data),
  setActive: (id: number, value: boolean) =>
    http.patch<WorkShift>(`/admin/work-shifts/${id}/active`, null, { params: { value } }).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/admin/work-shifts/${id}`).then((r) => r.data),

  schedule: (from: string, to: string) =>
    http.get<Assignment[]>('/admin/work-schedule', { params: { from, to } }).then((r) => r.data),
  staff: () => http.get<StaffOption[]>('/admin/work-schedule/staff').then((r) => r.data),
  assign: (body: { date: string; shiftId: number; userIds: number[] }) =>
    http.post<Assignment[]>('/admin/work-schedule', body).then((r) => r.data),
  unassign: (id: number) => http.delete<void>(`/admin/work-schedule/${id}`).then((r) => r.data),
}

/** "07:00:00" → "07:00" */
export const hhmm = (t: string) => t.slice(0, 5)
export const shiftTime = (s: WorkShift) => `${hhmm(s.startTime)}–${hhmm(s.endTime)}`
