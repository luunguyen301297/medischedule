import { http } from '@/shared/api/http'
import type { Page } from '../staff/api'

export type AppointmentStatus = 'BOOKED' | 'CHECKED_IN' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED' | 'NO_SHOW'
export type AppointmentChannel = 'APP' | 'GUEST' | 'AI' | 'WALK_IN' | 'STAFF'

export const STATUS_LABEL: Record<AppointmentStatus, string> = {
  BOOKED: 'Đã đặt',
  CHECKED_IN: 'Đã check-in',
  IN_PROGRESS: 'Đang khám',
  DONE: 'Đã khám',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến',
}

export const CHANNEL_LABEL: Record<AppointmentChannel, string> = {
  APP: 'App',
  GUEST: 'Web',
  AI: 'Trợ lý AI',
  WALK_IN: 'Tại quầy',
  STAFF: 'Nhân viên đặt',
}

/** Một dòng lịch hẹn; thời điểm dạng ISO có offset. Chưa xếp bác sĩ: startAt, doctor*, roomName = null. */
export interface AppointmentRow {
  id: number
  bookingCode: string
  /** YYYY-MM-DD */
  visitDate: string
  workShiftName: string | null
  /** "HH:mm" */
  workShiftStart: string | null
  workShiftEnd: string | null
  startAt: string | null
  endAt: string | null
  status: AppointmentStatus
  channel: AppointmentChannel
  /** Khách chỉ định bác sĩ */
  designated: boolean
  patientId: number
  patientCode: string
  patientName: string
  phone: string
  doctorUserId: number | null
  doctorName: string | null
  specialtyName: string | null
  roomName: string | null
  createdByName: string | null
  cancelReason: string | null
  createdAt: string
}

export interface AppointmentFilter {
  /** YYYY-MM-DD */
  from: string
  to: string
  doctorUserId?: number
  status?: AppointmentStatus
  q?: string
  page: number
  size: number
}

export interface NewPatient { fullName: string; dateOfBirth?: string | null; gender?: string | null }

export interface StaffBookingRequest {
  date: string
  workShiftId: number
  specialtyId?: number
  /** Bỏ trống = không chỉ định, phòng khám xếp bác sĩ khi check-in */
  doctorUserId?: number
  /** "HH:mm"; bắt buộc khi có bác sĩ */
  startTime?: string
  phone: string
  patientId?: number
  newPatient?: NewPatient
}

export interface Slot { /** "HH:mm:ss" */ time: string; available: boolean }
export interface SlotGrid { doctorName: string; specialtyName: string; roomName: string | null; slotMinutes: number; slots: Slot[] }

export interface PatientLite {
  id: number
  patientCode: string
  fullName: string
  dateOfBirth: string | null
  gender: string | null
  phone: string
}

/** Buổi (ca làm việc) đang bật và bác sĩ trực: dùng lại endpoint public của form đặt lịch. */
export interface ShiftOption { id: number; name: string; startTime: string; endTime: string }
export interface DoctorOnShift { userId: number; fullName: string; specialtyId: number | null; specialtyName: string | null }
export interface DoctorItem { userId: number; fullName: string }

export const receptionApi = {
  list: (f: AppointmentFilter) =>
    http.get<Page<AppointmentRow>>('/reception/appointments', { params: { ...f, q: f.q || undefined } }).then((r) => r.data),
  create: (body: StaffBookingRequest) => http.post<AppointmentRow>('/reception/appointments', body).then((r) => r.data),
  cancel: (id: number, reason: string) =>
    http.post<AppointmentRow>(`/reception/appointments/${id}/cancel`, { reason }).then((r) => r.data),
  slots: (date: string, workShiftId: number, doctorUserId: number) =>
    http.get<SlotGrid>('/reception/slots', { params: { date, workShiftId, doctorUserId } }).then((r) => r.data),
  patients: (phone: string) =>
    http.get<PatientLite[]>('/reception/patients', { params: { phone } }).then((r) => r.data),
  shifts: () => http.get<ShiftOption[]>('/public/booking/shifts').then((r) => r.data),
  doctorsOnShift: (date: string, shiftId: number, specialtyId?: number) =>
    http.get<DoctorOnShift[]>('/public/booking/doctors', { params: { date, shiftId, specialtyId } }).then((r) => r.data),
  specialties: () => http.get<{ id: number; name: string }[]>('/public/specialties').then((r) => r.data),
  doctors: () => http.get<DoctorItem[]>('/public/doctors').then((r) => r.data),
}
