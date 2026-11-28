import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Empty, Form, Input, Modal, Radio, Select, Spin } from 'antd'
import dayjs from 'dayjs'
import { errorCode, errorMessage } from '@/shared/api/http'
import { useEffect } from 'react'
import { receptionApi, type StaffBookingRequest } from './api'

/** So khớp họ tên: bỏ khoảng trắng thừa, không phân biệt hoa thường (giống PatientService.normalizeName). */
const normalizeName = (s?: string | null) => (s ?? '').normalize('NFC').trim().replace(/\s+/g, ' ').toLocaleLowerCase('vi')

/** Đặt được hôm nay → 6 ngày tới (giống backend). */
const DAYS_AHEAD = 6
const PHONE_RE = /^0\d{9}$/
const WEEKDAY = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

interface FormValues {
  /** YYYY-MM-DD */
  date: string
  workShiftId?: number
  specialtyId?: number
  doctorUserId?: number
  startTime?: string
  phone?: string
  patientId?: number
  fullName?: string
}

const hm = (t: string) => t.slice(0, 5)

/** Nhân viên đặt lịch hộ khách: ngày → buổi → bác sĩ trực → giờ trống → SĐT → hồ sơ bệnh nhân. */
export default function BookingCreateDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const [form] = Form.useForm<FormValues>()
  const date = Form.useWatch('date', form)
  const workShiftId = Form.useWatch('workShiftId', form)
  const specialtyId = Form.useWatch('specialtyId', form)
  const doctorUserId = Form.useWatch('doctorUserId', form)
  const startTime = Form.useWatch('startTime', form)
  const phone = Form.useWatch('phone', form)
  const patientId = Form.useWatch('patientId', form)
  const phoneOk = !!phone && PHONE_RE.test(phone)

  const today = dayjs().startOf('day')
  const days = Array.from({ length: DAYS_AHEAD + 1 }, (_, i) => today.add(i, 'day'))

  const shifts = useQuery({ queryKey: ['reception', 'shifts'], queryFn: receptionApi.shifts, enabled: open, staleTime: 60_000 })
  const specialties = useQuery({
    queryKey: ['reception', 'specialties'],
    queryFn: receptionApi.specialties,
    enabled: open,
    staleTime: 60_000,
  })
  const doctors = useQuery({
    queryKey: ['reception', 'doctors-on-shift', date, workShiftId, specialtyId],
    queryFn: () => receptionApi.doctorsOnShift(date!, workShiftId!, specialtyId),
    enabled: open && !!date && !!workShiftId,
  })
  const slots = useQuery({
    queryKey: ['reception', 'slots', date, workShiftId, doctorUserId],
    queryFn: () => receptionApi.slots(date!, workShiftId!, doctorUserId!),
    enabled: open && !!date && !!workShiftId && !!doctorUserId,
    retry: false,
  })
  const patients = useQuery({
    queryKey: ['reception', 'patients', phone],
    queryFn: () => receptionApi.patients(phone!),
    enabled: open && phoneOk,
  })

  const create = useMutation({
    mutationFn: (body: StaffBookingRequest) => receptionApi.create(body),
    onSuccess: (a) => {
      message.success(`Đã đặt lịch ${a.bookingCode} cho ${a.patientName}, ${dayjs(a.startAt).format('HH:mm DD/MM/YYYY')}`)
      qc.invalidateQueries({ queryKey: ['reception'] })
      close()
    },
    onError: (e) => {
      message.error(errorMessage(e))
      if (errorCode(e) === 'SLOT_TAKEN' || errorCode(e) === 'SLOT_INVALID') {
        form.setFieldValue('startTime', undefined)
        slots.refetch()
      }
    },
  })

  const close = () => {
    form.resetFields()
    onClose()
  }

  // Đổi bước trước thì xóa lựa chọn ở các bước sau
  const onValuesChange = (changed: Partial<FormValues>) => {
    if ('date' in changed || 'workShiftId' in changed || 'specialtyId' in changed) {
      form.setFieldsValue({ doctorUserId: undefined, startTime: undefined })
    }
    if ('doctorUserId' in changed) form.setFieldValue('startTime', undefined)
    // Đổi SĐT hoặc sửa họ tên khác hồ sơ đã chọn = khách mới
    if ('phone' in changed) form.setFieldValue('patientId', undefined)
    if ('fullName' in changed) {
      const picked = patients.data?.find((p) => p.id === form.getFieldValue('patientId'))
      if (picked && normalizeName(picked.fullName) !== normalizeName(changed.fullName)) {
        form.setFieldValue('patientId', undefined)
      }
      autoPick()
    }
  }

  /**
   * Tự tích hồ sơ khi tìm thấy: họ tên khớp một hồ sơ của SĐT (không phân biệt hoa thường / khoảng trắng); chưa nhập
   * họ tên mà SĐT chỉ có một hồ sơ thì chọn luôn và điền tên. Backend cũng dùng lại hồ sơ trùng tên nếu bỏ sót.
   */
  const autoPick = () => {
    const list = patients.data
    if (!list?.length || form.getFieldValue('patientId')) return
    const name = normalizeName(form.getFieldValue('fullName'))
    const match = name ? list.find((p) => normalizeName(p.fullName) === name) : list.length === 1 ? list[0] : undefined
    if (match) form.setFieldsValue({ patientId: match.id, fullName: match.fullName })
  }

  // Kết quả tìm theo SĐT về sau khi đã gõ họ tên
  useEffect(autoPick, [patients.data])

  const submit = (v: FormValues) => {
    const isNew = !v.patientId
    create.mutate({
      date: v.date,
      workShiftId: v.workShiftId!,
      specialtyId: v.doctorUserId ? undefined : v.specialtyId,
      doctorUserId: v.doctorUserId,
      startTime: v.doctorUserId && v.startTime ? hm(v.startTime) : undefined,
      phone: v.phone!.trim(),
      patientId: isNew ? undefined : v.patientId,
      newPatient: isNew
        ? { fullName: v.fullName!.trim() }
        : undefined,
    })
  }

  const doctorName = doctors.data?.find((d) => d.userId === doctorUserId)?.fullName
  const shift = shifts.data?.find((s) => s.id === workShiftId)
  const specialtyName = specialties.data?.find((s) => s.id === specialtyId)?.name
  const day = date ? dayjs(date).format('DD/MM') : ''
  const summary = !shift
    ? 'Chưa chọn buổi khám'
    : doctorUserId
      ? `${startTime ? hm(startTime) : 'Chưa chọn giờ'} · ${day} · ${doctorName ?? ''}`
      : `${shift.name} ${day} · ${specialtyName ?? 'Chưa chọn khoa'} · phòng khám xếp bác sĩ`

  const pickPatient = (id: number) => {
    const p = patients.data?.find((x) => x.id === id)
    if (p) form.setFieldsValue({ patientId: p.id, fullName: p.fullName })
  }

  const patientOptions = (patients.data ?? []).map((p) => ({
    value: p.id,
    label: (
      <span className="booking-patient">
        <b>{p.fullName}</b>
        <span>
          {[p.patientCode, p.dateOfBirth && dayjs(p.dateOfBirth).format('DD/MM/YYYY'), p.gender].filter(Boolean).join(' · ')}
        </span>
      </span>
    ),
  }))

  return (
    <Modal
      title="Đặt lịch hộ khách"
      open={open}
      width={720}
      centered
      rootClassName="booking-modal"
      onCancel={close}
      destroyOnClose
      footer={
        <div className="booking-footer">
          <span className="booking-footer-summary">{summary}</span>
          <Button onClick={close}>Đóng</Button>
          <Button type="primary" loading={create.isPending} onClick={() => form.submit()}>
            Đặt lịch
          </Button>
        </div>
      }
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        initialValues={{ date: today.format('YYYY-MM-DD') }}
        onValuesChange={onValuesChange}
        onFinish={submit}
      >
        <section className="booking-section">
          <h3>Thời gian khám</h3>
          <Form.Item name="date" rules={[{ required: true, message: 'Chọn ngày khám' }]}>
            <Radio.Group
              id="booking-date"
              aria-label="Ngày khám"
              optionType="button"
              className="booking-chips booking-days"
              options={days.map((d, i) => ({
                value: d.format('YYYY-MM-DD'),
                label: (
                  <span className="booking-chip-2l">
                    <small>{i === 0 ? 'Hôm nay' : WEEKDAY[d.day()]}</small>
                    {d.format('DD/MM')}
                  </span>
                ),
              }))}
            />
          </Form.Item>

          <Form.Item name="workShiftId" rules={[{ required: true, message: 'Chọn buổi khám' }]}>
            <Radio.Group
              id="booking-shift"
              aria-label="Buổi khám"
              optionType="button"
              className="booking-chips booking-shifts"
              options={shifts.data?.map((s) => ({
                value: s.id,
                label: (
                  <span className="booking-chip-2l">
                    <small>{hm(s.startTime)}–{hm(s.endTime)}</small>
                    {s.name}
                  </span>
                ),
              }))}
            />
          </Form.Item>

          <div className="booking-form-row booking-form-row-2">
          <Form.Item name="specialtyId" label="Chuyên khoa">
            <Select
              id="booking-specialty"
              allowClear
              showSearch
              optionFilterProp="label"
              placeholder="Chưa chọn"
              options={specialties.data?.map((s) => ({ value: s.id, label: s.name }))}
            />
          </Form.Item>
          <Form.Item name="doctorUserId" label="Bác sĩ khách chỉ định">
            <Select
              id="booking-doctor-pick"
              allowClear
              showSearch
              optionFilterProp="label"
              disabled={!workShiftId}
              loading={doctors.isFetching}
              placeholder={workShiftId ? 'Không chỉ định' : 'Chọn buổi trước'}
              notFoundContent={doctors.isFetching ? <Spin size="small" /> : 'Không có bác sĩ trực buổi này'}
              options={doctors.data?.map((d) => ({
                value: d.userId,
                label: d.specialtyName && !specialtyId ? `${d.fullName} — ${d.specialtyName}` : d.fullName,
              }))}
            />
          </Form.Item>
          </div>

          {doctorUserId && (
            <Form.Item
              name="startTime"
              label={
                slots.data
                  ? `Giờ khám · ${slots.data.slotMinutes} phút/lượt${slots.data.roomName ? ` · phòng ${slots.data.roomName}` : ''}`
                  : 'Giờ khám'
              }
              rules={[{ required: true, message: 'Chọn giờ khám' }]}
            >
              {slots.isFetching ? (
                <div className="booking-loading"><Spin size="small" /></div>
              ) : slots.isError ? (
                <Alert type="warning" showIcon message={errorMessage(slots.error)} />
              ) : slots.data?.slots.length ? (
                <Radio.Group
                  id="booking-time"
                  optionType="button"
                  className="booking-chips booking-slots"
                  options={slots.data.slots.map((s) => ({ value: s.time, label: hm(s.time), disabled: !s.available }))}
                />
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Buổi này không còn giờ trống" />
              )}
            </Form.Item>
          )}
        </section>

        <section className="booking-section">
          <h3>Người khám</h3>
          <div className="booking-form-row booking-form-row-2">
            <Form.Item
              name="phone"
              rules={[
                { required: true, message: 'Nhập số điện thoại' },
                { pattern: PHONE_RE, message: 'Số điện thoại gồm 10 số, bắt đầu bằng 0' },
              ]}
            >
              <Input id="booking-phone" aria-label="Số điện thoại" inputMode="numeric" maxLength={10} placeholder="Số điện thoại" />
            </Form.Item>
            <Form.Item
              name="fullName"
              rules={[
                { required: true, whitespace: true, message: 'Nhập họ và tên' },
                { max: 120, message: 'Họ tên tối đa 120 ký tự' },
              ]}
            >
              <Input id="booking-fullname" aria-label="Họ và tên" maxLength={120} placeholder="Họ và tên" />
            </Form.Item>
          </div>
          {/* patientId chỉ giữ giá trị, chọn qua các thẻ hồ sơ bên dưới */}
          <Form.Item name="patientId" hidden><Input /></Form.Item>

          {phoneOk && patients.isFetching && <div className="booking-loading"><Spin size="small" /></div>}
          {phoneOk && !!patients.data?.length && (
            <div className="booking-existing">
              <div className="booking-hint">Số này đã có hồ sơ, bấm để chọn (hoặc nhập họ tên khác để tạo hồ sơ mới):</div>
              <Radio.Group
                id="booking-patient"
                aria-label="Hồ sơ đã có"
                className="booking-patients"
                value={patientId}
                onChange={(e) => pickPatient(e.target.value)}
                options={patientOptions}
              />
            </div>
          )}
        </section>
      </Form>
    </Modal>
  )
}
